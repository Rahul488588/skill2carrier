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
