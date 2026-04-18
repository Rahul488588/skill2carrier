package com.example.skill2career

import android.app.Application as AndroidApp
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.skill2career.network.AdminRequest
import com.example.skill2career.network.AuthResponse
import com.example.skill2career.network.Notification
import com.example.skill2career.network.ResumeAnalysis
import com.example.skill2career.network.SkillsGapAnalysis
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.skill2career.network.LoginRequest
import com.example.skill2career.network.RetrofitClient
import com.example.skill2career.network.SignUpRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class MainViewModel(application: AndroidApp) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val api = RetrofitClient.instance
    private val gson = Gson()

    var currentUser = mutableStateOf<User?>(null)
    val opportunities = mutableStateListOf<Opportunity>()
    val myApplications = mutableStateListOf<Application>()
    val allApplications = mutableStateListOf<Application>()
    val allStudents = mutableStateListOf<User>()
    val savedOpportunities = mutableStateListOf<Opportunity>()

    init {
        // SECURITY: Clear any admin accounts from local DB - admins MUST be server-verified only
        viewModelScope.launch {
            val localUsers = db.userDao().getAllUsers()
            localUsers.filter { it.role == "Admin" }.forEach { adminUser ->
                db.userDao().deleteUser(adminUser)
            }
        }
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            // Load local data immediately for instant UI response
            loadLocalOpportunities()
            loadLocalStudents()
            loadLocalApplications()

            // Fetch Opportunities in background (ISOLATED by role)
            launch {
                try {
                    val userRole = currentUser.value?.role ?: "Student"
                    val userEmail = currentUser.value?.email ?: ""
                    val response = api.getOpportunities(userRole, userEmail)
                    if (response.isSuccessful && response.body() != null) {
                        val remoteOpps = response.body()!!
                        opportunities.clear()
                        opportunities.addAll(remoteOpps)
                        db.opportunityDao().insertOpportunities(remoteOpps.map { it.toEntity(gson) })
                    }
                } catch (e: Exception) {
                    // Fail silently as we already have local data
                }
            }

            // Fetch Students in background
            launch {
                try {
                    val studentResponse = api.getAllStudents()
                    if (studentResponse.isSuccessful && studentResponse.body() != null) {
                        val remoteStudents = studentResponse.body()!!
                        allStudents.clear()
                        allStudents.addAll(remoteStudents)
                        db.userDao().insertUsers(remoteStudents.map { it.toEntity("") })
                    }
                } catch (e: Exception) {}
            }

            // Fetch All Applications in background (ISOLATED by role)
            launch {
                try {
                    val userRole = currentUser.value?.role ?: "Student"
                    val appResponse = api.getAllApplications(userRole)
                    if (appResponse.isSuccessful && appResponse.body() != null) {
                        val remoteApps = appResponse.body()!!
                        allApplications.clear()
                        allApplications.addAll(remoteApps)
                        db.applicationDao().insertApplications(remoteApps.map { it.toEntity() })
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private suspend fun loadLocalStudents() {
        val students = db.userDao().getAllStudents()
        allStudents.clear()
        allStudents.addAll(students.map { it.toDomain() })
    }

    private suspend fun loadLocalApplications() {
        val apps = db.applicationDao().getAllApplications()
        allApplications.clear()
        allApplications.addAll(apps.map { it.toDomain() })
    }

    private suspend fun loadLocalOpportunities() {
        val opps = db.opportunityDao().getAllOpportunities()
        opportunities.clear()
        opportunities.addAll(opps.map { it.toDomain(gson) })
    }

    // 🔐 SEPARATE LOGIN SYSTEM - Server determines role from database, NOT from client selection
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // Server determines the actual role from database - client cannot spoof it
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    RetrofitClient.setAuthToken(auth.token)
                    val user = auth.user
                    currentUser.value = user
                    // Server returned the actual role - navigation should use user.role
                    fetchUserApplications(email)
                    refreshData()
                    onResult(true, null)
                } else {
                    // Handle different HTTP error codes
                    val errorMessage = when (response.code()) {
                        401 -> "Invalid email or password"
                        403 -> response.errorBody()?.string()?.takeIf { it.isNotBlank() } 
                            ?: "Access denied."
                        404 -> "User not found. Please sign up first."
                        400 -> "Invalid request. Please check your inputs."
                        500, 502, 503 -> "Server error. Please try again later."
                        else -> response.errorBody()?.string()?.takeIf { it.isNotBlank() } 
                            ?: "Login failed. Please try again."
                    }
                    onResult(false, errorMessage)
                }
            } catch (e: java.net.UnknownHostException) {
                // No internet connection or server address not found
                onResult(false, "Cannot reach server. Please check:\n1. Server is running\n2. WiFi IP address is correct in code")
            } catch (e: java.net.ConnectException) {
                // Server is not running or refused connection
                onResult(false, "Server not responding. Please start the server or check the IP address.")
            } catch (e: java.net.SocketTimeoutException) {
                // Connection timed out - server might be slow or wrong IP
                onResult(false, "Server is taking too long to respond. Check if server IP is correct.")
            } catch (e: Exception) {
                // SECURITY: Removed local fallback - admin secret must be validated by server only
                onResult(false, "Cannot connect to server. Admin login requires server connection for security verification.")
            }
        }
    }
    
    // 🔐 ADMIN EMAIL LOGIN - Step 1: Submit email only
    data class AdminEmailLoginResult(val status: String, val message: String?, val requestId: String? = null)
    
    fun adminEmailLogin(email: String, onResult: (AdminEmailLoginResult) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.adminEmailLogin(mapOf("email" to email))
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    val status = result["status"] as? String ?: "unknown"
                    val message = result["message"] as? String
                    val requestId = result["requestId"] as? String
                    onResult(AdminEmailLoginResult(status, message, requestId))
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to submit email"
                    onResult(AdminEmailLoginResult("error", errorMsg, null))
                }
            } catch (e: Exception) {
                onResult(AdminEmailLoginResult("error", "Cannot connect to server", null))
            }
        }
    }
    
    // 🔐 ADMIN PASSWORD LOGIN - Step 2: Submit password
    fun adminPasswordLogin(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.adminPasswordLogin(mapOf("email" to email, "password" to password))
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    RetrofitClient.setAuthToken(auth.token)
                    val user = auth.user
                    currentUser.value = user
                    fetchUserApplications(email)
                    refreshData()
                    onResult(true, null)
                } else {
                    onResult(false, "Invalid password")
                }
            } catch (e: Exception) {
                onResult(false, "Cannot connect to server")
            }
        }
    }

    fun signUp(user: User, password: String, adminSecret: String? = null, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.signUp(SignUpRequest(user, password, adminSecret))
                if (response.isSuccessful && response.body() != null) {
                    val remoteUser = response.body()!!
                    currentUser.value = remoteUser
                    // SECURITY: Only save students locally, never admins
                    if (user.role == "Student") {
                        db.userDao().insertUser(user.toEntity(password))
                    }
                    allStudents.add(remoteUser)
                    onResult(true, null)
                } else {
                    // Handle different HTTP error codes
                    val errorMessage = when (response.code()) {
                        403 -> "Invalid admin secret code. Only authorized college staff can register as admin."
                        409 -> "Email already exists. Please use a different email or login."
                        400 -> "Invalid information. Please check all fields."
                        500, 502, 503 -> "Server error. Please try again later."
                        else -> response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                            ?: "Sign up failed. Please try again."
                    }
                    onResult(false, errorMessage)
                }
            } catch (e: java.net.UnknownHostException) {
                // No internet connection
                onResult(false, "No internet connection. Please check your network.")
            } catch (e: java.net.ConnectException) {
                // Server not running
                onResult(false, "Server is not available. Please try again later.")
            } catch (e: java.net.SocketTimeoutException) {
                onResult(false, "Connection timed out. Please try again.")
            } catch (e: Exception) {
                // Don't fallback to local - signup requires server
                onResult(false, "Unable to connect to server. Please check your internet connection.")
            }
        }
    }

    private fun fetchUserApplications(email: String) {
        viewModelScope.launch {
            try {
                val userRole = currentUser.value?.role ?: "Student"
                val response = api.getApplications(email, userRole)
                if (response.isSuccessful && response.body() != null) {
                    val remoteApps = response.body()!!
                    myApplications.clear()
                    myApplications.addAll(remoteApps)
                    // Sync locally
                    db.applicationDao().insertApplications(remoteApps.map { it.toEntity() })
                } else {
                    val apps = db.applicationDao().getApplicationsByEmail(email)
                    myApplications.clear()
                    myApplications.addAll(apps.map { it.toDomain() })
                }
            } catch (e: Exception) {
                val apps = db.applicationDao().getApplicationsByEmail(email)
                myApplications.clear()
                myApplications.addAll(apps.map { it.toDomain() })
            }
        }
    }

    fun logout() {
        currentUser.value = null
        myApplications.clear()
    }

    fun postOpportunity(opportunity: Opportunity) {
        viewModelScope.launch {
            try {
                val userRole = currentUser.value?.role ?: "Student"
                val userEmail = currentUser.value?.email ?: "anonymous"
                val response = api.postOpportunity(opportunity, userRole, userEmail)
                if (response.isSuccessful) {
                    // Refresh from server to stay in sync
                    val refreshResponse = api.getOpportunities(userRole, userEmail)
                    val updatedOpps = refreshResponse.body()
                    if (updatedOpps != null) {
                        opportunities.clear()
                        opportunities.addAll(updatedOpps)
                    }
                }
                // Always save locally too
                db.opportunityDao().insertOpportunity(opportunity.toEntity(gson))
            } catch (e: Exception) {
                // Fallback to local only if server is down
                db.opportunityDao().insertOpportunity(opportunity.toEntity(gson))
                opportunities.add(0, opportunity)
            }
        }
    }

    fun deleteOpportunity(opportunityId: String) {
        viewModelScope.launch {
            db.opportunityDao().deleteOpportunity(opportunityId)
            opportunities.removeAll { it.id == opportunityId }
        }
    }
    
    fun deleteStudent(email: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                // Call server API to delete student
                val response = api.deleteStudent(email)
                if (response.isSuccessful) {
                    // Remove from local lists
                    allStudents.removeAll { it.email == email }
                    // Also delete from local database
                    db.userDao().getUserByEmail(email)?.let { userEntity ->
                        db.userDao().deleteUser(userEntity)
                    }
                    onResult(true, null)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to delete student"
                    onResult(false, errorMsg)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Network error")
            }
        }
    }

    fun applyForOpportunity(application: Application, uris: Map<String, Uri?> = emptyMap()) {
        viewModelScope.launch {
            try {
                var finalApp = application

                // Upload files if present
                val context = getApplication<AndroidApp>().applicationContext
                val updatedUris = mutableMapOf<String, String?>()

                uris.forEach { (key, uri) ->
                    if (uri != null) {
                        val file = uriToFile(context, uri, key)
                        val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                        val uploadResponse = api.uploadFile(body)
                        if (uploadResponse.isSuccessful) {
                            updatedUris[key] = uploadResponse.body()?.fileName
                        }
                    }
                }

                finalApp = application.copy(
                    resumeFileName = updatedUris["resume"] ?: application.resumeFileName,
                    aadharCardFileName = updatedUris["aadhar"] ?: application.aadharCardFileName,
                    marksheetFileName = updatedUris["marksheet"] ?: application.marksheetFileName
                )

                val userRole = currentUser.value?.role ?: "Student"
                val response = api.applyForOpportunity(finalApp, userRole)
                if (response.isSuccessful) {
                    myApplications.add(0, finalApp)
                    allApplications.add(0, finalApp)
                }
                // Always save locally too
                db.applicationDao().insertApplication(finalApp.toEntity())
            } catch (e: Exception) {
                db.applicationDao().insertApplication(application.toEntity())
                myApplications.add(0, application)
                allApplications.add(0, application)
            }
        }
    }

    private fun uriToFile(context: android.content.Context, uri: Uri, prefix: String): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.pdf")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    fun updateApplicationStatus(applicationId: String, status: String) {
        viewModelScope.launch {
            try {
                val userRole = currentUser.value?.role ?: "Student"
                val response = api.updateApplicationStatus(applicationId, status, userRole)
                if (response.isSuccessful) {
                    val index = allApplications.indexOfFirst { it.id == applicationId }
                    if (index != -1) {
                        allApplications[index] = allApplications[index].copy(status = status)
                    }
                    val myIndex = myApplications.indexOfFirst { it.id == applicationId }
                    if (myIndex != -1) {
                        myApplications[myIndex] = myApplications[myIndex].copy(status = status)
                    }
                }
                // Always update locally
                db.applicationDao().updateStatus(applicationId, status)
            } catch (e: Exception) {
                db.applicationDao().updateStatus(applicationId, status)
                val index = allApplications.indexOfFirst { it.id == applicationId }
                if (index != -1) {
                    allApplications[index] = allApplications[index].copy(status = status)
                }
            }
        }
    }

    fun toggleSaveOpportunity(opportunity: Opportunity) {
        if (savedOpportunities.any { it.id == opportunity.id }) {
            savedOpportunities.removeAll { it.id == opportunity.id }
        } else {
            savedOpportunities.add(opportunity)
        }
    }

    fun downloadAndViewFile(context: Context, fileName: String) {
        viewModelScope.launch {
            try {
                val response = api.downloadFile(fileName)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val file = File(context.cacheDir, fileName)
                    val inputStream = body.byteStream()
                    FileOutputStream(file).use { output ->
                        inputStream.use { input ->
                            input.copyTo(output)
                        }
                    }

                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open PDF"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // 🔐 SUPER ADMIN FUNCTIONS
    
    // State for super admin notifications and requests
    val superAdminNotifications = mutableStateListOf<Notification>()
    val pendingAdminRequests = mutableStateListOf<AdminRequest>()
    val unreadNotificationCount = mutableIntStateOf(0)
    
    // Check if current user is super admin (based on JWT role claim)
    fun isSuperAdmin(): Boolean {
        val user = currentUser.value
        return user?.role == "SuperAdmin"  // Role from JWT token
    }
    
    // Fetch super admin notifications
    fun fetchSuperAdminNotifications(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                if (!isSuperAdmin()) {
                    onResult(false, "Not authorized")
                    return@launch
                }
                
                val response = api.getSuperAdminNotifications(false)
                if (response.isSuccessful && response.body() != null) {
                    superAdminNotifications.clear()
                    superAdminNotifications.addAll(response.body()!!)
                    unreadNotificationCount.intValue = superAdminNotifications.count { !it.isRead }
                    onResult(true, null)
                } else {
                    onResult(false, response.errorBody()?.string() ?: "Failed to fetch notifications")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
    
    // Fetch pending admin requests
    fun fetchPendingAdminRequests(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                if (!isSuperAdmin()) {
                    onResult(false, "Not authorized")
                    return@launch
                }
                
                val response = api.getPendingAdminRequests()
                if (response.isSuccessful && response.body() != null) {
                    pendingAdminRequests.clear()
                    pendingAdminRequests.addAll(response.body()!!)
                    onResult(true, null)
                } else {
                    onResult(false, response.errorBody()?.string() ?: "Failed to fetch requests")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
    
    // Approve admin request
    fun approveAdminRequest(requestId: String, tempPassword: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                if (!isSuperAdmin()) {
                    onResult(false, "Not authorized")
                    return@launch
                }
                
                val request = mapOf(
                    "requestId" to requestId,
                    "approve" to "true",
                    "tempPassword" to tempPassword
                )
                
                val response = api.approveAdminRequest(request)
                if (response.isSuccessful) {
                    // Refresh lists
                    fetchPendingAdminRequests()
                    fetchSuperAdminNotifications()
                    onResult(true, null)
                } else {
                    onResult(false, response.errorBody()?.string() ?: "Failed to approve request")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
    
    // Reject admin request
    fun rejectAdminRequest(requestId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                if (!isSuperAdmin()) {
                    onResult(false, "Not authorized")
                    return@launch
                }
                
                val request = mapOf(
                    "requestId" to requestId,
                    "approve" to "false"
                )
                
                val response = api.rejectAdminRequest(request)
                if (response.isSuccessful) {
                    fetchPendingAdminRequests()
                    fetchSuperAdminNotifications()
                    onResult(true, null)
                } else {
                    onResult(false, response.errorBody()?.string() ?: "Failed to reject request")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
    
    // Mark notification as read
    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            try {
                if (!isSuperAdmin()) return@launch
                
                val request = mapOf(
                    "notificationId" to notificationId
                )
                
                api.markNotificationRead(request)
                // Update local state
                val index = superAdminNotifications.indexOfFirst { it.id == notificationId }
                if (index != -1) {
                    val updated = superAdminNotifications[index].copy(isRead = true)
                    superAdminNotifications[index] = updated
                    unreadNotificationCount.intValue = superAdminNotifications.count { !it.isRead }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // 🤖 AI Resume Analysis
    fun analyzeResume(resumeText: String, onResult: (Boolean, ResumeAnalysis?) -> Unit) {
        viewModelScope.launch {
            try {
                val request = mapOf("resumeText" to resumeText)
                val response = api.analyzeResume(request)
                if (response.isSuccessful) {
                    val analysis = response.body()
                    onResult(true, analysis)
                } else {
                    println("❌ Resume analysis failed: ${response.errorBody()?.string()}")
                    onResult(false, null)
                }
            } catch (e: Exception) {
                println("❌ Resume analysis error: ${e.message}")
                onResult(false, null)
            }
        }
    }
    
    // 📊 Skills Gap Analysis
    fun analyzeSkillsGap(resumeText: String, targetRole: String, onResult: (Boolean, SkillsGapAnalysis?) -> Unit) {
        viewModelScope.launch {
            try {
                val request = mapOf("resumeText" to resumeText, "targetRole" to targetRole)
                val response = api.analyzeSkillsGap(request)
                if (response.isSuccessful) {
                    val analysis = response.body()
                    onResult(true, analysis)
                } else {
                    println("❌ Skills gap analysis failed: ${response.errorBody()?.string()}")
                    onResult(false, null)
                }
            } catch (e: Exception) {
                println("❌ Skills gap analysis error: ${e.message}")
                e.printStackTrace()
                onResult(false, null)
            }
        }
    }
    
    // 🤖 AI Opportunity Discovery (Admin only)
    val aiDiscoveredOpportunities = mutableStateListOf<Map<String, String>>()
    var isFetchingAIOpportunities = mutableStateOf(false)
    
    fun fetchAIOpportunities(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                isFetchingAIOpportunities.value = true
                val response = api.fetchAIOpportunities()
                
                if (response.isSuccessful) {
                    fetchPendingAIOpportunities()
                    refreshOpportunities()
                    onResult(true, null)
                } else {
                    val error = response.errorBody()?.string() ?: "Failed to fetch opportunities"
                    onResult(false, error)
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                isFetchingAIOpportunities.value = false
            }
        }
    }
    
    fun fetchPendingAIOpportunities() {
        viewModelScope.launch {
            try {
                println("🔄 Fetching pending AI opportunities...")
                val response = api.getPendingAIOpportunities()
                if (response.isSuccessful) {
                    val opportunities = response.body() ?: emptyList()
                    println("✅ Fetched ${opportunities.size} pending opportunities from server")
                    aiDiscoveredOpportunities.clear()
                    aiDiscoveredOpportunities.addAll(opportunities)
                    println("📋 Updated aiDiscoveredOpportunities list with ${aiDiscoveredOpportunities.size} items")
                } else {
                    println("❌ Failed to fetch pending opportunities: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                println("❌ Error fetching pending AI opportunities: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun approveAIOpportunity(id: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.approveAIOpportunity(id)
                if (response.isSuccessful) {
                    fetchPendingAIOpportunities()
                    // Refresh main opportunities list to show newly approved opportunities
                    refreshOpportunities()
                    onResult(true, null)
                } else {
                    onResult(false, response.errorBody()?.string() ?: "Failed to approve")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun refreshOpportunities() {
        viewModelScope.launch {
            try {
                val userRole = currentUser.value?.role ?: "Student"
                val userEmail = currentUser.value?.email ?: ""
                val response = api.getOpportunities(userRole, userEmail)
                if (response.isSuccessful && response.body() != null) {
                    val remoteOpps = response.body()!!
                    opportunities.clear()
                    opportunities.addAll(remoteOpps)
                    db.opportunityDao().insertOpportunities(remoteOpps.map { it.toEntity(gson) })
                    println("✅ Refreshed opportunities list: ${opportunities.size} items")
                }
            } catch (e: Exception) {
                println("❌ Error refreshing opportunities: ${e.message}")
            }
        }
    }
    
    fun rejectAIOpportunity(id: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.rejectAIOpportunity(id)
                if (response.isSuccessful) {
                    fetchPendingAIOpportunities()
                    refreshOpportunities()
                    onResult(true, null)
                } else {
                    onResult(false, response.errorBody()?.string() ?: "Failed to reject")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
    
    // 📄 Extract text from uploaded file (PDF, DOCX, etc.)
    suspend fun extractTextFromFile(file: File): String? {
        return try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val response = api.extractTextFromFile(filePart)
            if (response.isSuccessful) {
                response.body()?.get("text")
            } else {
                println("❌ Failed to extract text: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            println("❌ Error extracting text: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

// 🔹 Extension Functions for Conversion
fun User.toEntity(password: String) = UserEntity(email, name, role, password, phoneNumber, branch)
fun UserEntity.toDomain() = User(name, email, role, phoneNumber, branch)

fun Opportunity.toEntity(gson: Gson) = OpportunityEntity(id, title, company, (type ?: OpportunityType.Internship).name, gson.toJson(tags), location, stipendOrSalary, date, minCgpa)
fun OpportunityEntity.toDomain(gson: Gson) = Opportunity(id, title, company, OpportunityType.safeValueOf(type), gson.fromJson(tagsJson, object : TypeToken<List<String>>() {}.type), location, stipendOrSalary, date, minCgpa)

fun Application.toEntity() = ApplicationEntity(
    id = id,
    opportunityId = opportunity.id,
    opportunityTitle = opportunity.title,
    opportunityCompany = opportunity.company,
    opportunityType = opportunity.safeType.name,
    applicantName = applicantName,
    applicantEmail = applicantEmail,
    whyApply = whyApply,
    status = status,
    resumeFileName = resumeFileName,
    familyIncome = familyIncome,
    aadharCardFileName = aadharCardFileName,
    marksheetFileName = marksheetFileName
)

fun ApplicationEntity.toDomain() = Application(
    id = id,
    opportunity = Opportunity(id = opportunityId, title = opportunityTitle, company = opportunityCompany, type = OpportunityType.safeValueOf(opportunityType)),
    applicantName = applicantName,
    applicantEmail = applicantEmail,
    whyApply = whyApply,
    status = status,
    resumeFileName = resumeFileName,
    familyIncome = familyIncome,
    aadharCardFileName = aadharCardFileName,
    marksheetFileName = marksheetFileName
)
