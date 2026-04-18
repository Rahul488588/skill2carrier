package com.example.skill2career.network

import com.example.skill2career.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("signup")
    suspend fun signUp(@Body request: SignUpRequest): Response<User>
    
    // 🔐 ADMIN LOGIN - Step 1: Email only
    @POST("admin/login-email")
    suspend fun adminEmailLogin(@Body request: Map<String, String>): Response<Map<String, Any>>
    
    // 🔐 ADMIN LOGIN - Step 2: Password
    @POST("admin/login-password")
    suspend fun adminPasswordLogin(@Body request: Map<String, String>): Response<AuthResponse>

    // 🔹 ISOLATED: Pass role to get opportunities from appropriate table (Admin/Student)
    @GET("opportunities")
    suspend fun getOpportunities(@Query("role") role: String, @Query("email") email: String): Response<List<Opportunity>>

    // 🔹 ISOLATED: Pass role and email to save opportunity to appropriate table
    @POST("opportunities")
    suspend fun postOpportunity(@Body opportunity: Opportunity, @Query("role") role: String, @Query("email") email: String): Response<Unit>

    // 🔹 ISOLATED: Pass role to get applications from appropriate table
    @GET("applications/{email}")
    suspend fun getApplications(@Path("email") email: String, @Query("role") role: String): Response<List<Application>>

    // 🔹 ISOLATED: Pass role to save application to appropriate table
    @POST("apply")
    suspend fun applyForOpportunity(@Body application: Application, @Query("role") role: String): Response<Unit>

    // 🔹 ISOLATED: Pass role to get all applications from appropriate table
    @GET("applications/all")
    suspend fun getAllApplications(@Query("role") role: String): Response<List<Application>>

    // 🔹 ISOLATED: Pass role to update application in appropriate table
    @PUT("applications/{id}/status")
    suspend fun updateApplicationStatus(@Path("id") id: String, @Query("status") status: String, @Query("role") role: String): Response<Unit>

    @GET("students")
    suspend fun getAllStudents(): Response<List<User>>
    
    @DELETE("students/{email}")
    suspend fun deleteStudent(@Path("email") email: String): Response<Map<String, Any>>

    @Multipart
    @POST("upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<FileUploadResponse>

    @GET("uploads/{name}")
    suspend fun downloadFile(@Path("name") name: String): Response<ResponseBody>
    
    // 🔐 SUPER ADMIN ENDPOINTS
    @GET("superadmin/notifications")
    suspend fun getSuperAdminNotifications(
        @Query("unread") unread: Boolean = false
    ): Response<List<Notification>>
    
    @GET("superadmin/pending-requests")
    suspend fun getPendingAdminRequests(): Response<List<AdminRequest>>
    
    @POST("superadmin/approve-request")
    suspend fun approveAdminRequest(@Body request: Map<String, String>): Response<Map<String, Any>>
    
    @POST("superadmin/reject-request")
    suspend fun rejectAdminRequest(@Body request: Map<String, String>): Response<Map<String, Any>>
    
    @POST("superadmin/mark-notification-read")
    suspend fun markNotificationRead(@Body request: Map<String, String>): Response<Unit>
    
    // 🤖 AI Resume Analysis
    @POST("ai/analyze-resume")
    suspend fun analyzeResume(@Body request: Map<String, String>): Response<ResumeAnalysis>
    
    // � Skills Gap Analysis
    @POST("ai/skills-gap")
    suspend fun analyzeSkillsGap(@Body request: Map<String, String>): Response<SkillsGapAnalysis>
    
    // �📄 Extract text from uploaded PDF/DOCX files
    @Multipart
    @POST("extract-text")
    suspend fun extractTextFromFile(@Part file: MultipartBody.Part): Response<Map<String, String>>
    
    // 🤖 AI Opportunity Discovery (Admin only)
    @POST("admin/fetch-opportunities")
    suspend fun fetchAIOpportunities(): Response<Map<String, Any>>
    
    @GET("admin/ai/opportunities/pending")
    suspend fun getPendingAIOpportunities(): Response<List<Map<String, String>>>
    
    @POST("admin/ai/opportunities/{id}/approve")
    suspend fun approveAIOpportunity(@Path("id") id: String): Response<Map<String, Any>>
    
    @POST("admin/ai/opportunities/{id}/reject")
    suspend fun rejectAIOpportunity(@Path("id") id: String): Response<Map<String, Any>>
}

data class LoginRequest(
    val email: String, 
    val password: String
    // Role is NOT sent by client - server determines it from database
)

data class AuthResponse(
    val token: String,
    val user: User
)
data class SignUpRequest(
    val user: User, 
    val password: String,
    val adminSecret: String? = null
)
data class FileUploadResponse(val fileName: String)

// 🔐 SUPER ADMIN DATA CLASSES
data class Notification(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val requestId: String? = null,
    val createdAt: String,
    val isRead: Boolean = false
)

data class AdminRequest(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val branch: String,
    val requestDate: String,
    val status: String
)

// 🤖 AI Resume Analysis Data Class
data class ResumeAnalysis(
    val summary: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val recommendedCareers: List<String>
)

// 📊 Skills Gap Analysis Data Class
data class SkillsGapAnalysis(
    val targetRole: String,
    val currentSkills: List<String>,
    val requiredSkills: List<String>,
    val missingSkills: List<String>,
    val recommendations: List<String>,
    val learningResources: List<String>
)
