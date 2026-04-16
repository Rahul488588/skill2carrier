package com.example.skill2career.server

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import at.favre.lib.crypto.bcrypt.BCrypt
import java.net.InetAddress
import java.net.NetworkInterface
import java.io.File
import java.util.Date
import java.util.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// 🔹 Domain Models
data class User(val name: String, val email: String, val role: String, val phoneNumber: String = "", val branch: String = "")
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val token: String, val user: User)

// 🔐 SUPER ADMIN CONFIGURATION - Hardcoded for security
// Only super admin can create other admins directly
object SuperAdminConfig {
    const val EMAIL = "ra@gm.com"
    const val PASSWORD = "Rahul@2006"  // Change this in production!
    const val NAME = "Super Administrator"
}

object JwtConfig {
    const val ISSUER = "skill2career"
    const val AUDIENCE = "skill2career-app"
    const val REALM = "skill2career"
    // In production: load from env var / secret manager.
    const val SECRET = "CHANGE_ME_SKILL2CAREER_JWT_SECRET"

    private val algorithm: Algorithm = Algorithm.HMAC256(SECRET)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()

    fun createToken(email: String, role: String): String {
        val now = System.currentTimeMillis()
        val expiresAt = Date(now + 1000L * 60L * 60L * 24L) // 24 hours
        return JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(email)
            .withClaim("role", role)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }
}

fun isBcryptHash(value: String): Boolean {
    return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$")
}

fun hashPassword(plain: String): String {
    return BCrypt.withDefaults().hashToString(12, plain.toCharArray())
}

fun verifyPasswordAndUpgradeIfLegacy(email: String, provided: String, stored: String): Boolean {
    if (stored.isBlank()) return false
    if (isBcryptHash(stored)) {
        return BCrypt.verifyer().verify(provided.toCharArray(), stored).verified
    }

    // Legacy plaintext password support: verify once then upgrade to bcrypt.
    val ok = stored == provided
    if (ok) {
        val hashed = hashPassword(provided)
        transaction {
            UsersTable.update({ UsersTable.email eq email }) {
                it[UsersTable.password] = hashed
            }
        }
    }
    return ok
}
data class SignUpRequest(val user: User, val password: String, val adminSecret: String? = null)
data class AdminSetupRequest(val setupToken: String, val user: User, val password: String)
data class AdminApprovalRequest(val requestId: String, val approve: Boolean, val adminEmail: String)
data class PendingAdminRequest(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val branch: String,
    val password: String,
    val requestDate: String
)
data class Opportunity(
    val id: String,
    val title: String,
    val company: String,
    val type: String,
    val tags: List<String>,
    val location: String,
    val stipendOrSalary: String?,
    val date: String,
    val minCgpa: Double? = null
)
data class Application(
    val id: String,
    val opportunity: Opportunity,
    val applicantName: String,
    val applicantEmail: String,
    val whyApply: String,
    val status: String,
    val resumeFileName: String? = null,
    val familyIncome: String? = null,
    val aadharCardFileName: String? = null,
    val marksheetFileName: String? = null
)

// 🔹 Database Schema (Exposed)
object UsersTable : Table("users") {
    val email = varchar("email", 128)
    val name = varchar("name", 128)
    val role = varchar("role", 20)
    val password = varchar("password", 128)
    val phoneNumber = varchar("phoneNumber", 20)
    val branch = varchar("branch", 50)
    override val primaryKey = PrimaryKey(email)
}

// 🔹 Config Table - Store admin secret and other settings in database
object ConfigTable : Table("config") {
    val key = varchar("key", 50)
    val value = varchar("value", 255)
    override val primaryKey = PrimaryKey(key)
}

// 🔹 Admin Approval Table - Pending admin registrations
object AdminApprovalTable : Table("admin_approvals") {
    val id = varchar("id", 50)
    val name = varchar("name", 128)
    val email = varchar("email", 128)
    val phoneNumber = varchar("phoneNumber", 20)
    val branch = varchar("branch", 50)
    val password = varchar("password", 128)
    val requestDate = varchar("requestDate", 50)
    val status = varchar("status", 20) // "pending", "approved", "rejected"
    override val primaryKey = PrimaryKey(id)
}

// 🔹 Notifications Table - For Super Admin notifications
object NotificationsTable : Table("notifications") {
    val id = varchar("id", 50)
    val type = varchar("type", 50) // "admin_request", "system", etc.
    val title = varchar("title", 255)
    val message = text("message")
    val requestId = varchar("requestId", 50).nullable() // Link to admin request
    val createdAt = varchar("createdAt", 50)
    val isRead = bool("isRead").default(false)
    override val primaryKey = PrimaryKey(id)
}

// 🔹 ADMIN Tables - Completely isolated from Student data
object AdminOpportunitiesTable : Table("admin_opportunities") {
    val id = varchar("id", 50)
    val title = varchar("title", 255)
    val company = varchar("company", 255)
    val type = varchar("type", 50)
    val tags = text("tags") // JSON
    val location = varchar("location", 255)
    val stipendOrSalary = varchar("stipendOrSalary", 100).nullable()
    val date = varchar("date", 100)
    val minCgpa = double("minCgpa").nullable()
    val createdBy = varchar("createdBy", 128) // Admin email who created it
    val createdAt = varchar("createdAt", 50)
    override val primaryKey = PrimaryKey(id)
}

object AdminApplicationsTable : Table("admin_applications") {
    val id = varchar("id", 50)
    val opportunityId = varchar("opportunityId", 50)
    val opportunityTitle = varchar("opportunityTitle", 255)
    val opportunityCompany = varchar("opportunityCompany", 255)
    val opportunityType = varchar("opportunityType", 50)
    val applicantName = varchar("applicantName", 255)
    val applicantEmail = varchar("applicantEmail", 128)
    val whyApply = text("whyApply")
    val status = varchar("status", 50)
    val resumeFileName = varchar("resumeFileName", 255).nullable()
    val familyIncome = varchar("familyIncome", 50).nullable()
    val aadharCardFileName = varchar("aadharCardFileName", 255).nullable()
    val marksheetFileName = varchar("marksheetFileName", 255).nullable()
    val appliedAt = varchar("appliedAt", 50)
    override val primaryKey = PrimaryKey(id)
}

// 🔹 STUDENT Tables - Completely isolated from Admin data  
object StudentOpportunitiesTable : Table("student_opportunities") {
    val id = varchar("id", 50)
    val title = varchar("title", 255)
    val company = varchar("company", 255)
    val type = varchar("type", 50)
    val tags = text("tags") // JSON
    val location = varchar("location", 255)
    val stipendOrSalary = varchar("stipendOrSalary", 100).nullable()
    val date = varchar("date", 100)
    val minCgpa = double("minCgpa").nullable()
    val createdBy = varchar("createdBy", 128) // Student email who created it
    val createdAt = varchar("createdAt", 50)
    override val primaryKey = PrimaryKey(id)
}

object StudentApplicationsTable : Table("student_applications") {
    val id = varchar("id", 50)
    val opportunityId = varchar("opportunityId", 50)
    val opportunityTitle = varchar("opportunityTitle", 255)
    val opportunityCompany = varchar("opportunityCompany", 255)
    val opportunityType = varchar("opportunityType", 50)
    val applicantName = varchar("applicantName", 255)
    val applicantEmail = varchar("applicantEmail", 128)
    val whyApply = text("whyApply")
    val status = varchar("status", 50)
    val resumeFileName = varchar("resumeFileName", 255).nullable()
    val familyIncome = varchar("familyIncome", 50).nullable()
    val aadharCardFileName = varchar("aadharCardFileName", 255).nullable()
    val marksheetFileName = varchar("marksheetFileName", 255).nullable()
    val appliedAt = varchar("appliedAt", 50)
    override val primaryKey = PrimaryKey(id)
}

// 🔹 LEGACY Tables (for backward compatibility during migration)
object OpportunitiesTable : Table("opportunities") {
    val id = varchar("id", 50)
    val title = varchar("title", 255)
    val company = varchar("company", 255)
    val type = varchar("type", 50)
    val tags = text("tags") // JSON
    val location = varchar("location", 255)
    val stipendOrSalary = varchar("stipendOrSalary", 100).nullable()
    val date = varchar("date", 100)
    val minCgpa = double("minCgpa").nullable()
    override val primaryKey = PrimaryKey(id)
}

object ApplicationsTable : Table("applications") {
    val id = varchar("id", 50)
    val opportunityId = varchar("opportunityId", 50)
    val opportunityTitle = varchar("opportunityTitle", 255)
    val opportunityCompany = varchar("opportunityCompany", 255)
    val opportunityType = varchar("opportunityType", 50)
    val applicantName = varchar("applicantName", 255)
    val applicantEmail = varchar("applicantEmail", 128)
    val whyApply = text("whyApply")
    val status = varchar("status", 50)
    val resumeFileName = varchar("resumeFileName", 255).nullable()
    val familyIncome = varchar("familyIncome", 50).nullable()
    val aadharCardFileName = varchar("aadharCardFileName", 255).nullable()
    val marksheetFileName = varchar("marksheetFileName", 255).nullable()
    override val primaryKey = PrimaryKey(id)
}

val gson = Gson()

fun getLocalIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) continue
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (addr is java.net.Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()

    }
    return "localhost"
}

// 🔹 Helper Functions for Config Management
fun getConfigValue(key: String): String? = transaction {
    ConfigTable.select { ConfigTable.key eq key }
        .map { it[ConfigTable.value] }
        .singleOrNull()
}

fun setConfigValue(key: String, value: String) = transaction {
    val exists = ConfigTable.select { ConfigTable.key eq key }.count() > 0
    if (exists) {
        ConfigTable.update({ ConfigTable.key eq key }) {
            it[ConfigTable.value] = value
        }
    } else {
        ConfigTable.insert {
            it[ConfigTable.key] = key
            it[ConfigTable.value] = value
        }
    }
}

fun getAdminSecret(): String? = getConfigValue("admin_secret")
fun setAdminSecret(secret: String) = setConfigValue("admin_secret", secret)
fun getSetupToken(): String? = getConfigValue("setup_token")

fun generateSetupToken(): String {
    val token = UUID.randomUUID().toString().substring(0, 8).uppercase()
    setConfigValue("setup_token", token)
    return token
}

fun hasAnyAdmin(): Boolean = transaction {
    UsersTable.select { UsersTable.role eq "Admin" }.count() > 0
}

fun main() {
    println("🚀 Starting Skill2Career Server...")
    
    // 🔹 Ensure uploads directory exists
    val uploadDir = File("uploads")
    if (!uploadDir.exists()) uploadDir.mkdirs()

    // 🔹 Initialize Database
    Database.connect("jdbc:h2:file:./s2c_database;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE;AUTO_RECONNECT=TRUE;", driver = "org.h2.Driver")
    transaction {
        // Create all tables including new Admin and Student isolated tables
        SchemaUtils.create(
            UsersTable, ConfigTable, AdminApprovalTable, NotificationsTable,
            OpportunitiesTable, ApplicationsTable,  // Legacy tables
            AdminOpportunitiesTable, AdminApplicationsTable,  // Admin isolated
            StudentOpportunitiesTable, StudentApplicationsTable  // Student isolated
        )
        
        // Initialize default admin secret if not exists
        val existingSecret = ConfigTable.select { ConfigTable.key eq "admin_secret" }.count()
        if (existingSecret == 0L) {
            ConfigTable.insert {
                it[ConfigTable.key] = "admin_secret"
                it[ConfigTable.value] = "S2C_ADMIN_2024" // Default secret, should be changed
            }
        }
    }

    val localIp = getLocalIpAddress()
    println("🌐 Server will be accessible at: http://$localIp:8080")
    
    // Check if setup needed
    if (!hasAnyAdmin()) {
        val setupToken = generateSetupToken()
        println("⚠️  NO ADMIN FOUND! Use setup token '$setupToken' to create first admin.")
        println("⚠️  POST to /admin/setup with: { setupToken: '$setupToken', user: {...}, password: '...' }")
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
        install(Authentication) {
            jwt("auth-jwt") {
                realm = JwtConfig.REALM
                verifier(JwtConfig.verifier)
                validate { credential ->
                    val email = credential.payload.subject
                    val role = credential.payload.getClaim("role")?.asString()
                    if (email.isNullOrBlank() || role.isNullOrBlank()) null else JWTPrincipal(credential.payload)
                }
                challenge { _, _ ->
                    call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                }
            }
        }
        routing {
            get("/") { call.respondText("Skill2Career Persistent Server is Running!") }

            // 🔐 STUDENT LOGIN - Email + Password required
            post("/login") {
                val request = call.receive<LoginRequest>()
                println("📩 Student login attempt: ${request.email}")
                
                // 🔐 Super Admin Login (Hardcoded)
                if (request.email == SuperAdminConfig.EMAIL && request.password == SuperAdminConfig.PASSWORD) {
                    val superAdmin = User(
                        name = SuperAdminConfig.NAME,
                        email = SuperAdminConfig.EMAIL,
                        role = "SuperAdmin",
                        phoneNumber = "N/A",
                        branch = "Super Admin"
                    )
                    println("👑 SUPER ADMIN login successful!")
                    val token = JwtConfig.createToken(superAdmin.email, superAdmin.role)
                    call.respond(AuthResponse(token = token, user = superAdmin))
                    return@post
                }
                
                // 🔍 Only allow STUDENTS through this endpoint
                // Reject if this email belongs to an Admin account (prevents "admin credentials" via student login)
                val isAdminEmail = transaction {
                    UsersTable.select { (UsersTable.email eq request.email) and (UsersTable.role eq "Admin") }.count() > 0
                }
                if (isAdminEmail) {
                    call.respond(HttpStatusCode.Forbidden, "Use Admin login. This account is registered as Admin.")
                    return@post
                }

                val row = transaction {
                    UsersTable.select {
                        (UsersTable.email eq request.email) and (UsersTable.role eq "Student")
                    }.singleOrNull()
                }

                val user = row?.let {
                    User(it[UsersTable.name], it[UsersTable.email], it[UsersTable.role], it[UsersTable.phoneNumber], it[UsersTable.branch])
                }

                if (user == null || row == null) {
                    println("❌ Student login failed for: ${request.email}")
                    call.respond(HttpStatusCode.Unauthorized, "Invalid student credentials")
                    return@post
                }

                val storedPassword = row[UsersTable.password]
                val ok = verifyPasswordAndUpgradeIfLegacy(user.email, request.password, storedPassword)
                if (!ok) {
                    println("❌ Student login failed for: ${request.email}")
                    call.respond(HttpStatusCode.Unauthorized, "Invalid student credentials")
                    return@post
                }
                
                println("✅ Student login success: ${user.name}")
                val token = JwtConfig.createToken(user.email, user.role)
                call.respond(AuthResponse(token = token, user = user))
            }
            
            // 🔐 ADMIN EMAIL LOGIN - Step 1: Enter email only
            post("/admin/login-email") {
                val request = call.receive<Map<String, String>>()
                val email = request["email"] ?: ""
                
                println("📩 Admin email login attempt: $email")
                
                if (email.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Email is required")
                    return@post
                }

                // Prevent students from using admin login flow
                val isStudentEmail = transaction {
                    UsersTable.select { (UsersTable.email eq email) and (UsersTable.role eq "Student") }.count() > 0
                }
                if (isStudentEmail) {
                    call.respond(HttpStatusCode.Forbidden, "This email is registered as Student. Use Student login.")
                    return@post
                }
                
                // Check if already an approved admin with password
                val existingAdmin = transaction {
                    UsersTable.select { 
                        (UsersTable.email eq email) and 
                        (UsersTable.role eq "Admin")
                    }.singleOrNull()
                }
                
                if (existingAdmin != null) {
                    val hasPassword = existingAdmin[UsersTable.password].isNotBlank()
                    if (hasPassword) {
                        // Admin exists with password - require password login
                        call.respond(mapOf(
                            "status" to "requires_password",
                            "message" to "Please enter your password",
                            "email" to email
                        ))
                        return@post
                    }
                }
                
                // Check if there's already a pending request
                val pendingRequest = transaction {
                    AdminApprovalTable.select { 
                        (AdminApprovalTable.email eq email) and 
                        (AdminApprovalTable.status eq "pending")
                    }.singleOrNull()
                }
                
                if (pendingRequest != null) {
                    call.respond(mapOf(
                        "status" to "pending_approval",
                        "message" to "Your admin access request is pending super admin approval. Please wait.",
                        "requestId" to pendingRequest[AdminApprovalTable.id]
                    ))
                    return@post
                }
                
                // Check if there's a rejected request
                val rejectedRequest = transaction {
                    AdminApprovalTable.select { 
                        (AdminApprovalTable.email eq email) and 
                        (AdminApprovalTable.status eq "rejected")
                    }.singleOrNull()
                }
                
                if (rejectedRequest != null) {
                    call.respond(mapOf(
                        "status" to "rejected",
                        "message" to "Your previous admin access request was rejected. Contact super admin for assistance."
                    ))
                    return@post
                }
                
                // New admin request - create notification for super admin
                val requestId = UUID.randomUUID().toString().substring(0, 8).uppercase()
                val currentTime = java.time.LocalDateTime.now().toString()
                
                transaction {
                    // Create pending admin request (without password - super admin will set it)
                    AdminApprovalTable.insert {
                        it[AdminApprovalTable.id] = requestId
                        it[AdminApprovalTable.name] = email.substringBefore("@") // Temporary name from email
                        it[AdminApprovalTable.email] = email
                        it[AdminApprovalTable.phoneNumber] = "Pending"
                        it[AdminApprovalTable.branch] = "New admin access request via email login"
                        it[AdminApprovalTable.password] = "" // Will be set by super admin
                        it[AdminApprovalTable.requestDate] = currentTime
                        it[AdminApprovalTable.status] = "pending"
                    }
                    
                    // Create notification for super admin
                    val notificationId = UUID.randomUUID().toString().substring(0, 8).uppercase()
                    NotificationsTable.insert {
                        it[NotificationsTable.id] = notificationId
                        it[NotificationsTable.type] = "admin_request"
                        it[NotificationsTable.title] = "New Admin Access Request"
                        it[NotificationsTable.message] = "$email has requested admin access via email login. Approve and set password."
                        it[NotificationsTable.requestId] = requestId
                        it[NotificationsTable.createdAt] = currentTime
                        it[NotificationsTable.isRead] = false
                    }
                }
                
                // 🔔 PROMINENT CONSOLE NOTIFICATION
                println("\n╔══════════════════════════════════════════════════════════════╗")
                println("║          🔔 NEW ADMIN EMAIL LOGIN REQUEST                 ║")
                println("╠══════════════════════════════════════════════════════════════╣")
                println("║  Email:    $email")
                println("║  Request:  $requestId")
                println("╠══════════════════════════════════════════════════════════════╣")
                println("║  Super Admin Action Required:                                ║")
                println("║  GET /superadmin/notifications                               ║")
                println("║  POST /superadmin/approve-request (with tempPassword)          ║")
                println("╚══════════════════════════════════════════════════════════════╝\n")
                
                call.respond(mapOf(
                    "status" to "request_created",
                    "message" to "Admin access request submitted. Super admin will review and set your password.",
                    "requestId" to requestId
                ))
            }
            
            // 🔐 ADMIN PASSWORD LOGIN - Step 2: For approved admins with password
            post("/admin/login-password") {
                val request = call.receive<Map<String, String>>()
                val email = request["email"] ?: ""
                val password = request["password"] ?: ""
                
                println("📩 Admin password login: $email")
                
                val row = transaction {
                    UsersTable.select {
                        (UsersTable.email eq email) and (UsersTable.role eq "Admin")
                    }.singleOrNull()
                }

                val user = row?.let {
                    User(it[UsersTable.name], it[UsersTable.email], it[UsersTable.role], it[UsersTable.phoneNumber], it[UsersTable.branch])
                }
                
                if (user == null || row == null) {
                    println("❌ Admin login failed for: $email")
                    call.respond(HttpStatusCode.Unauthorized, "Invalid admin credentials")
                    return@post
                }

                val storedPassword = row[UsersTable.password]
                val ok = verifyPasswordAndUpgradeIfLegacy(user.email, password, storedPassword)
                if (!ok) {
                    println("❌ Admin login failed for: $email")
                    call.respond(HttpStatusCode.Unauthorized, "Invalid admin credentials")
                    return@post
                }
                
                println("✅ Admin login success: ${user.name}")
                val token = JwtConfig.createToken(user.email, user.role)
                call.respond(AuthResponse(token = token, user = user))
            }

            // 🔹 First Admin Setup (when no admin exists)
            post("/admin/setup") {
                val request = call.receive<AdminSetupRequest>()
                
                // Check if setup token is valid
                val setupToken = getSetupToken()
                if (request.setupToken != setupToken) {
                    call.respond(HttpStatusCode.Forbidden, "Invalid setup token")
                    return@post
                }
                
                // Check if any admin already exists
                if (hasAnyAdmin()) {
                    call.respond(HttpStatusCode.Conflict, "Admin already exists. Use admin approval workflow instead.")
                    return@post
                }
                
                // Create first admin
                transaction {
                    UsersTable.insert {
                        it[email] = request.user.email
                        it[name] = request.user.name
                        it[role] = "Admin"
                        it[password] = hashPassword(request.password)
                        it[phoneNumber] = request.user.phoneNumber
                        it[branch] = request.user.branch
                    }
                }
                
                // Clear setup token after use
                setConfigValue("setup_token", "")
                
                println("✅ First admin created: ${request.user.name}")
                call.respond(request.user)
            }

            // 🔹 Student Signup Only - Admin registration is invite-only
            post("/signup") {
                val request = call.receive<SignUpRequest>()
                
                // BLOCK admin self-registration completely
                if (request.user.role == "Admin") {
                    println("❌ BLOCKED: Admin self-registration attempt by ${request.user.email}")
                    call.respond(HttpStatusCode.Forbidden, 
                        "Admin registration is invite-only. Contact super admin to receive an invitation.")
                    return@post
                }
                
                val exists = transaction { UsersTable.select { UsersTable.email eq request.user.email }.count() > 0 }
                if (exists) {
                    call.respond(HttpStatusCode.Conflict, "User already exists")
                } else {
                    transaction {
                        UsersTable.insert {
                            it[email] = request.user.email
                            it[name] = request.user.name
                            it[role] = request.user.role
                            it[password] = hashPassword(request.password)
                            it[phoneNumber] = request.user.phoneNumber
                            it[branch] = request.user.branch
                        }
                    }
                    println("✅ Student signup success: ${request.user.name}")
                    call.respond(request.user)
                }
            }
            
            // 🔹 Accept Admin Invite
            post("/admin/accept-invite") {
                val params = call.receive<Map<String, String>>()
                val token = params["token"] ?: ""
                val password = params["password"] ?: ""
                val phoneNumber = params["phoneNumber"] ?: ""
                
                if (password.length < 6) {
                    call.respond(HttpStatusCode.BadRequest, "Password must be at least 6 characters")
                    return@post
                }
                
                // Find invite
                val invite = transaction {
                    AdminApprovalTable.select { 
                        (AdminApprovalTable.id eq token) and (AdminApprovalTable.status eq "invited")
                    }.singleOrNull()
                }
                
                if (invite == null) {
                    call.respond(HttpStatusCode.NotFound, "Invalid or expired invite token")
                    return@post
                }
                
                // Check expiry
                val expiryStr = invite[AdminApprovalTable.requestDate]
                val expiry = java.time.LocalDateTime.parse(expiryStr)
                if (java.time.LocalDateTime.now().isAfter(expiry)) {
                    call.respond(HttpStatusCode.BadRequest, "Invite has expired. Contact admin for new invite.")
                    return@post
                }
                
                val email = invite[AdminApprovalTable.email]
                val name = invite[AdminApprovalTable.name]
                
                // Create admin account
                transaction {
                    UsersTable.insert {
                        it[UsersTable.email] = email
                        it[UsersTable.name] = name
                        it[UsersTable.role] = "Admin"
                        it[UsersTable.password] = hashPassword(password)
                        it[UsersTable.phoneNumber] = phoneNumber
                        it[UsersTable.branch] = "Admin"
                    }
                    
                    // Mark invite as used
                    AdminApprovalTable.update({ AdminApprovalTable.id eq token }) {
                        it[AdminApprovalTable.status] = "accepted"
                    }
                }
                
                println("✅ Admin invite accepted: $name ($email)")
                call.respond(User(name, email, "Admin", phoneNumber, "Admin"))
            }
            
            // 🔹 Super Admin: List Pending Invites
            get("/admin/invites") {
                val adminEmail = call.request.queryParameters["adminEmail"] ?: ""
                
                // Verify admin
                val isAdmin = transaction {
                    UsersTable.select { 
                        (UsersTable.email eq adminEmail) and (UsersTable.role eq "Admin") 
                    }.count() > 0
                }
                
                if (!isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, "Admin access required")
                    return@get
                }
                
                val invites = transaction {
                    AdminApprovalTable.select { AdminApprovalTable.status eq "invited" }.map {
                        mapOf(
                            "token" to it[AdminApprovalTable.id],
                            "name" to it[AdminApprovalTable.name],
                            "email" to it[AdminApprovalTable.email],
                            "expiry" to it[AdminApprovalTable.requestDate]
                        )
                    }
                }
                
                call.respond(invites)
            }
            
            // 🔹 Get/Update Admin Secret (for existing admins)
            get("/admin/config/secret") {
                val adminEmail = call.request.queryParameters["adminEmail"] ?: ""
                // Verify requester is admin
                val requester = transaction {
                    UsersTable.select { (UsersTable.email eq adminEmail) and (UsersTable.role eq "Admin") }.count()
                }
                if (requester == 0L) {
                    call.respond(HttpStatusCode.Forbidden, "Admin access required")
                }
                call.respond(mapOf("secret" to (getAdminSecret() ?: "NOT_SET")))
            }
            
            post("/admin-secret/change") {
                val params = call.receive<Map<String, String>>()
                val adminEmail = params["adminEmail"] ?: ""
                val newSecret = params["newSecret"] ?: ""
                
                if (newSecret.length < 6) {
                    call.respond(HttpStatusCode.BadRequest, "Secret must be at least 6 characters")
                    return@post
                }
                
                // Verify requester is admin
                val requester = transaction {
                    UsersTable.select { (UsersTable.email eq adminEmail) and (UsersTable.role eq "Admin") }.count()
                }
                if (requester == 0L) {
                    call.respond(HttpStatusCode.Forbidden, "Admin access required")
                    return@post
                }
                
                setAdminSecret(newSecret)
                println("✅ Admin secret changed by: $adminEmail")
                call.respond(mapOf("success" to true, "message" to "Admin secret updated"))
            }
            
            authenticate("auth-jwt") {
            // 🔹 Super Admin: Create Admin Invite
            post("/admin/invite") {
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                val callerEmail = principal?.payload?.subject ?: ""
                
                if (tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Only super admins can invite new admins")
                    return@post
                }
                
                val params = call.receive<Map<String, String>>()
                val inviteeEmail = params["inviteeEmail"] ?: ""
                val inviteeName = params["inviteeName"] ?: ""
                
                // Validate inputs
                if (inviteeEmail.isBlank() || !inviteeEmail.contains("@")) {
                    call.respond(HttpStatusCode.BadRequest, "Valid invitee email is required")
                    return@post
                }
                if (inviteeName.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Invitee name is required")
                    return@post
                }
                
                // Check if email already exists
                val exists = transaction { 
                    UsersTable.select { UsersTable.email eq inviteeEmail }.count() > 0 
                }
                if (exists) {
                    call.respond(HttpStatusCode.Conflict, "User with this email already exists")
                    return@post
                }
                
                // Generate unique invite token
                val inviteToken = UUID.randomUUID().toString().replace("-", "").substring(0, 16).uppercase()
                val expiryDate = java.time.LocalDateTime.now().plusDays(7).toString()
                
                // Store invite in database
                transaction {
                    AdminApprovalTable.insert {
                        it[id] = inviteToken
                        it[name] = inviteeName
                        it[email] = inviteeEmail
                        it[phoneNumber] = ""
                        it[branch] = "Admin"
                        it[password] = "" // Will be set by invitee
                        it[requestDate] = expiryDate
                        it[status] = "invited"
                    }
                }
                
                println("✅ Admin invite created by $callerEmail for $inviteeEmail")
                println("🔑 Invite Token: $inviteToken (Valid for 7 days)")
                println("📧 Share this link: http://yourserver:8080/admin/accept-invite?token=$inviteToken")
                
                call.respond(mapOf(
                    "success" to true,
                    "inviteToken" to inviteToken,
                    "message" to "Invite created. Share the token with $inviteeName",
                    "expiry" to expiryDate
                ))
            }
            
            // 🔹 SUPER ADMIN ONLY: Direct Admin Creation
            post("/superadmin/create-admin") {
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                if (tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Super admin access required")
                    return@post
                }

                val params = call.receive<Map<String, String>>()
                val newAdminName = params["name"] ?: ""
                val newAdminEmail = params["email"] ?: ""
                val newAdminPassword = params["password"] ?: ""
                val phoneNumber = params["phoneNumber"] ?: ""
                
                // Check if email already exists
                val exists = transaction { 
                    UsersTable.select { UsersTable.email eq newAdminEmail }.count() > 0 
                }
                if (exists) {
                    call.respond(HttpStatusCode.Conflict, "User with this email already exists")
                    return@post
                }
                
                // Create admin directly
                transaction {
                    UsersTable.insert {
                        it[UsersTable.email] = newAdminEmail
                        it[UsersTable.name] = newAdminName
                        it[UsersTable.role] = "Admin"
                        it[UsersTable.password] = hashPassword(newAdminPassword)
                        it[UsersTable.phoneNumber] = phoneNumber
                        it[UsersTable.branch] = "Admin"
                    }
                }
                
                println("👑 SUPER ADMIN created new admin: $newAdminName ($newAdminEmail)")
                call.respond(User(newAdminName, newAdminEmail, "Admin", phoneNumber, "Admin"))
            }
            
            // 🔹 College Staff: Request Admin Access
            post("/admin/request-access") {
                val params = call.receive<Map<String, String>>()
                val name = params["name"] ?: ""
                val email = params["email"] ?: ""
                val phoneNumber = params["phoneNumber"] ?: ""
                val reason = params["reason"] ?: ""
                
                // Check if already exists
                val exists = transaction { 
                    UsersTable.select { UsersTable.email eq email }.count() > 0 
                }
                if (exists) {
                    call.respond(HttpStatusCode.Conflict, "User already registered. Please login.")
                    return@post
                }
                
                // Check if request already pending
                val alreadyRequested = transaction {
                    AdminApprovalTable.select { 
                        (AdminApprovalTable.email eq email) and (AdminApprovalTable.status eq "pending") 
                    }.count() > 0
                }
                if (alreadyRequested) {
                    call.respond(HttpStatusCode.Conflict, "Admin access request already pending. Please wait for approval.")
                    return@post
                }
                
                // Create pending request
                val requestId = UUID.randomUUID().toString().substring(0, 8).uppercase()
                val currentTime = java.time.LocalDateTime.now().toString()
                
                transaction {
                    AdminApprovalTable.insert {
                        it[AdminApprovalTable.id] = requestId
                        it[AdminApprovalTable.name] = name
                        it[AdminApprovalTable.email] = email
                        it[AdminApprovalTable.phoneNumber] = phoneNumber
                        it[AdminApprovalTable.branch] = reason // Store reason in branch field
                        it[AdminApprovalTable.password] = "" // Not set yet
                        it[AdminApprovalTable.requestDate] = currentTime
                        it[AdminApprovalTable.status] = "pending"
                    }
                    
                    // Create notification for super admin
                    val notificationId = UUID.randomUUID().toString().substring(0, 8).uppercase()
                    NotificationsTable.insert {
                        it[NotificationsTable.id] = notificationId
                        it[NotificationsTable.type] = "admin_request"
                        it[NotificationsTable.title] = "New Admin Access Request"
                        it[NotificationsTable.message] = "$name ($email) has requested admin access. Reason: $reason"
                        it[NotificationsTable.requestId] = requestId
                        it[NotificationsTable.createdAt] = currentTime
                        it[NotificationsTable.isRead] = false
                    }
                }
                
                // 🔔 PROMINENT CONSOLE NOTIFICATION FOR SUPER ADMIN
                println("\n╔══════════════════════════════════════════════════════════════╗")
                println("║                � NEW ADMIN ACCESS REQUEST                  ║")
                println("╠══════════════════════════════════════════════════════════════╣")
                println("║  Name:     $name")
                println("║  Email:    $email")
                println("║  Phone:    $phoneNumber")
                println("║  Reason:   $reason")
                println("║  Request:  $requestId")
                println("╠══════════════════════════════════════════════════════════════╣")
                println("║  Super Admin Action Required:                                ║")
                println("║  GET /superadmin/notifications                               ║")
                println("║  GET /superadmin/pending-requests                            ║")
                println("║  POST /superadmin/approve-request                            ║")
                println("╚══════════════════════════════════════════════════════════════╝\n")
                
                call.respond(mapOf(
                    "success" to true,
                    "message" to "Admin access request submitted. Super admin will review.",
                    "requestId" to requestId
                ))
            }
            
            // 🔹 SUPER ADMIN ONLY: View Pending Admin Requests
            get("/superadmin/pending-requests") {
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                if (tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Super admin access required")
                    return@get
                }
                
                val requests = transaction {
                    AdminApprovalTable.select { AdminApprovalTable.status eq "pending" }.map {
                        mapOf(
                            // Match Android model: AdminRequest(id, name, email, phoneNumber, branch, requestDate, status)
                            "id" to it[AdminApprovalTable.id],
                            "name" to it[AdminApprovalTable.name],
                            "email" to it[AdminApprovalTable.email],
                            "phoneNumber" to it[AdminApprovalTable.phoneNumber],
                            "branch" to it[AdminApprovalTable.branch],
                            "requestDate" to it[AdminApprovalTable.requestDate],
                            "status" to it[AdminApprovalTable.status]
                        )
                    }
                }
                
                call.respond(requests)
            }
            
            // 🔹 SUPER ADMIN ONLY: Approve/Reject Admin Request
            post("/superadmin/approve-request") {
                val params = call.receive<Map<String, String>>()
                val requestId = params["requestId"] ?: ""
                val approve = params["approve"]?.toBoolean() ?: false
                val tempPassword = params["tempPassword"] ?: "TEMP_${System.currentTimeMillis()}"
                
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                if (tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Super admin access required")
                    return@post
                }
                
                // Find request
                val request = transaction {
                    AdminApprovalTable.select { 
                        (AdminApprovalTable.id eq requestId) and (AdminApprovalTable.status eq "pending")
                    }.singleOrNull()
                }
                
                if (request == null) {
                    call.respond(HttpStatusCode.NotFound, "Request not found or already processed")
                    return@post
                }
                
                val name = request[AdminApprovalTable.name]
                val email = request[AdminApprovalTable.email]
                val phoneNumber = request[AdminApprovalTable.phoneNumber]
                
                if (approve) {
                    // Create admin account with temp password
                    transaction {
                        UsersTable.insert {
                            it[UsersTable.email] = email
                            it[UsersTable.name] = name
                            it[UsersTable.role] = "Admin"
                            it[UsersTable.password] = hashPassword(tempPassword)
                            it[UsersTable.phoneNumber] = phoneNumber
                            it[UsersTable.branch] = "Admin"
                        }
                        
                        // Update request status
                        AdminApprovalTable.update({ AdminApprovalTable.id eq requestId }) {
                            it[AdminApprovalTable.status] = "approved"
                        }
                    }
                    
                    println("✅ SUPER ADMIN approved request $requestId for $name ($email)")
                    println("   Temp password: $tempPassword")
                    
                    call.respond(mapOf(
                        "success" to true,
                        "message" to "Admin request approved. Notify user to login and change password.",
                        "email" to email,
                        "tempPassword" to tempPassword
                    ))
                } else {
                    // Reject request
                    transaction {
                        AdminApprovalTable.update({ AdminApprovalTable.id eq requestId }) {
                            it[AdminApprovalTable.status] = "rejected"
                        }
                    }
                    
                    println("❌ SUPER ADMIN rejected request $requestId for $name ($email)")
                    call.respond(mapOf("success" to true, "message" to "Request rejected"))
                }
            }
            
            // 🔹 SUPER ADMIN ONLY: List All Admins
            get("/superadmin/admins") {
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                if (tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Super admin access required")
                    return@get
                }
                
                val admins = transaction {
                    UsersTable.select { UsersTable.role eq "Admin" }.map {
                        mapOf(
                            "name" to it[UsersTable.name],
                            "email" to it[UsersTable.email],
                            "phoneNumber" to it[UsersTable.phoneNumber],
                            "branch" to it[UsersTable.branch]
                        )
                    }
                }
                
                call.respond(admins)
            }
            
            // 🔹 SUPER ADMIN ONLY: Get Notifications
            get("/superadmin/notifications") {
                val onlyUnread = call.request.queryParameters["unread"]?.toBoolean() ?: false
                
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                if (tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Super admin access required")
                    return@get
                }
                
                val notifications = transaction {
                    val query = if (onlyUnread) {
                        NotificationsTable.select { NotificationsTable.isRead eq false }
                    } else {
                        NotificationsTable.selectAll()
                    }
                    query.orderBy(NotificationsTable.createdAt, SortOrder.DESC).map {
                        mapOf(
                            "id" to it[NotificationsTable.id],
                            "type" to it[NotificationsTable.type],
                            "title" to it[NotificationsTable.title],
                            "message" to it[NotificationsTable.message],
                            "requestId" to it[NotificationsTable.requestId],
                            "createdAt" to it[NotificationsTable.createdAt],
                            "isRead" to it[NotificationsTable.isRead]
                        )
                    }
                }
                
                // Count unread
                val unreadCount = transaction {
                    NotificationsTable.select { NotificationsTable.isRead eq false }.count()
                }
                
                // Android client expects a list
                call.respond(notifications)
            }
            
            // 🔹 SUPER ADMIN ONLY: Mark Notification as Read
            post("/superadmin/notifications/read") {
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                if (tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Super admin access required")
                    return@post
                }
                
                val params = call.receive<Map<String, String>>()
                val notificationId = params["notificationId"] ?: ""
                val markAll = params["markAll"]?.toBoolean() ?: false
                
                if (markAll) {
                    // Mark all as read
                    transaction {
                        NotificationsTable.update({ NotificationsTable.isRead eq false }) {
                            it[NotificationsTable.isRead] = true
                        }
                    }
                    println("✅ Super admin marked all notifications as read")
                    call.respond(mapOf("success" to true, "message" to "All notifications marked as read"))
                } else if (notificationId.isNotBlank()) {
                    // Mark specific notification as read
                    val updated = transaction {
                        NotificationsTable.update({ NotificationsTable.id eq notificationId }) {
                            it[NotificationsTable.isRead] = true
                        }
                    }
                    if (updated > 0) {
                        println("✅ Super admin marked notification $notificationId as read")
                        call.respond(mapOf("success" to true, "message" to "Notification marked as read"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Notification not found")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Provide notificationId or set markAll to true")
                }
            }
            
            // 🔹 SUPER ADMIN ONLY: Delete Notification
            post("/superadmin/notifications/delete") {
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                if (tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Super admin access required")
                    return@post
                }
                
                val params = call.receive<Map<String, String>>()
                val notificationId = params["notificationId"] ?: ""
                
                val deleted = transaction {
                    NotificationsTable.deleteWhere { NotificationsTable.id eq notificationId }
                }
                
                if (deleted > 0) {
                    println("🗑️ Super admin deleted notification $notificationId")
                    call.respond(mapOf("success" to true, "message" to "Notification deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Notification not found")
                }
            }
            
            // 🔹 SUPER ADMIN ONLY: Reject Admin Request (convenience endpoint)
            post("/superadmin/reject-request") {
                val params = call.receive<Map<String, String>>()
                val requestId = params["requestId"] ?: ""
                
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                if (tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Super admin access required")
                    return@post
                }
                
                val currentTime = java.time.LocalDateTime.now().toString()
                
                transaction {
                    // Update request status to rejected
                    AdminApprovalTable.update({ AdminApprovalTable.id eq requestId }) {
                        it[AdminApprovalTable.status] = "rejected"
                    }
                    
                    // Get request details for notification
                    val request = AdminApprovalTable.select { AdminApprovalTable.id eq requestId }.singleOrNull()
                    val requestEmail = request?.get(AdminApprovalTable.email) ?: "unknown"
                    
                    // Create notification for rejection
                    val notificationId = UUID.randomUUID().toString().substring(0, 8).uppercase()
                    NotificationsTable.insert {
                        it[NotificationsTable.id] = notificationId
                        it[NotificationsTable.type] = "rejection"
                        it[NotificationsTable.title] = "Admin Request Rejected"
                        it[NotificationsTable.message] = "Admin access request for $requestEmail was rejected by super admin"
                        it[NotificationsTable.requestId] = requestId
                        it[NotificationsTable.createdAt] = currentTime
                        it[NotificationsTable.isRead] = false
                    }
                }
                
                println("❌ Super admin rejected admin request $requestId")
                call.respond(mapOf("success" to true, "message" to "Request rejected"))
            }
            
            // 🔹 SUPER ADMIN ONLY: Mark Notification Read (convenience endpoint)
            post("/superadmin/mark-notification-read") {
                val params = call.receive<Map<String, String>>()
                val notificationId = params["notificationId"] ?: ""
                
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                if (tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Super admin access required")
                    return@post
                }
                
                if (notificationId.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "notificationId is required")
                    return@post
                }
                
                val updated = transaction {
                    NotificationsTable.update({ NotificationsTable.id eq notificationId }) {
                        it[NotificationsTable.isRead] = true
                    }
                }
                
                if (updated > 0) {
                    println("✅ Super admin marked notification $notificationId as read")
                    call.respond(mapOf("success" to true, "message" to "Notification marked as read"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Notification not found")
                }
            }
            }

            // 🔹 Get Opportunities - ISOLATED by role (Admin sees only admin data, Student sees only student data)
            get("/opportunities") {
                val userRole = call.request.queryParameters["role"] ?: "Student"
                val userEmail = call.request.queryParameters["email"] ?: ""
                
                println("📊 Fetching opportunities for role: $userRole, email: $userEmail")
                
                val opps = if (userRole == "Admin") {
                    // Admin sees ONLY AdminOpportunitiesTable data
                    transaction {
                        AdminOpportunitiesTable.selectAll().orderBy(AdminOpportunitiesTable.id, SortOrder.DESC).map {
                            Opportunity(
                                it[AdminOpportunitiesTable.id], 
                                it[AdminOpportunitiesTable.title], 
                                it[AdminOpportunitiesTable.company],
                                it[AdminOpportunitiesTable.type], 
                                gson.fromJson(it[AdminOpportunitiesTable.tags], object : TypeToken<List<String>>() {}.type),
                                it[AdminOpportunitiesTable.location], 
                                it[AdminOpportunitiesTable.stipendOrSalary], 
                                it[AdminOpportunitiesTable.date], 
                                it[AdminOpportunitiesTable.minCgpa]
                            )
                        }
                    }
                } else {
                    // Student sees ONLY StudentOpportunitiesTable data
                    transaction {
                        StudentOpportunitiesTable.selectAll().orderBy(StudentOpportunitiesTable.id, SortOrder.DESC).map {
                            Opportunity(
                                it[StudentOpportunitiesTable.id], 
                                it[StudentOpportunitiesTable.title], 
                                it[StudentOpportunitiesTable.company],
                                it[StudentOpportunitiesTable.type], 
                                gson.fromJson(it[StudentOpportunitiesTable.tags], object : TypeToken<List<String>>() {}.type),
                                it[StudentOpportunitiesTable.location], 
                                it[StudentOpportunitiesTable.stipendOrSalary], 
                                it[StudentOpportunitiesTable.date], 
                                it[StudentOpportunitiesTable.minCgpa]
                            )
                        }
                    }
                }
                call.respond(opps)
            }

            // 🔹 Create Opportunity - ISOLATED by role
            post("/opportunities") {
                val opp = call.receive<Opportunity>()
                val userRole = call.request.queryParameters["role"] ?: "Student"
                val userEmail = call.request.queryParameters["email"] ?: "anonymous"
                val currentTime = java.time.LocalDateTime.now().toString()
                
                println("📝 Creating opportunity for role: $userRole by $userEmail")
                
                if (userRole == "Admin") {
                    // Save to AdminOpportunitiesTable
                    transaction {
                        AdminOpportunitiesTable.insert {
                            it[AdminOpportunitiesTable.id] = opp.id
                            it[AdminOpportunitiesTable.title] = opp.title
                            it[AdminOpportunitiesTable.company] = opp.company
                            it[AdminOpportunitiesTable.type] = opp.type
                            it[AdminOpportunitiesTable.tags] = gson.toJson(opp.tags)
                            it[AdminOpportunitiesTable.location] = opp.location
                            it[AdminOpportunitiesTable.stipendOrSalary] = opp.stipendOrSalary
                            it[AdminOpportunitiesTable.date] = opp.date
                            it[AdminOpportunitiesTable.minCgpa] = opp.minCgpa
                            it[AdminOpportunitiesTable.createdBy] = userEmail
                            it[AdminOpportunitiesTable.createdAt] = currentTime
                        }
                    }
                } else {
                    // Save to StudentOpportunitiesTable
                    transaction {
                        StudentOpportunitiesTable.insert {
                            it[StudentOpportunitiesTable.id] = opp.id
                            it[StudentOpportunitiesTable.title] = opp.title
                            it[StudentOpportunitiesTable.company] = opp.company
                            it[StudentOpportunitiesTable.type] = opp.type
                            it[StudentOpportunitiesTable.tags] = gson.toJson(opp.tags)
                            it[StudentOpportunitiesTable.location] = opp.location
                            it[StudentOpportunitiesTable.stipendOrSalary] = opp.stipendOrSalary
                            it[StudentOpportunitiesTable.date] = opp.date
                            it[StudentOpportunitiesTable.minCgpa] = opp.minCgpa
                            it[StudentOpportunitiesTable.createdBy] = userEmail
                            it[StudentOpportunitiesTable.createdAt] = currentTime
                        }
                    }
                }
                call.respond(HttpStatusCode.Created)
            }

            // 🔹 Get All Applications - ISOLATED by role
            get("/applications/all") {
                val userRole = call.request.queryParameters["role"] ?: "Student"
                
                println("📊 Fetching all applications for role: $userRole")
                
                val apps = if (userRole == "Admin") {
                    // Admin sees ONLY AdminApplicationsTable
                    transaction {
                        AdminApplicationsTable.selectAll().map {
                            Application(
                                it[AdminApplicationsTable.id],
                                Opportunity(it[AdminApplicationsTable.opportunityId], it[AdminApplicationsTable.opportunityTitle], 
                                    it[AdminApplicationsTable.opportunityCompany], it[AdminApplicationsTable.opportunityType], 
                                    emptyList(), "", null, ""),
                                it[AdminApplicationsTable.applicantName], it[AdminApplicationsTable.applicantEmail], 
                                it[AdminApplicationsTable.whyApply], it[AdminApplicationsTable.status],
                                it[AdminApplicationsTable.resumeFileName], it[AdminApplicationsTable.familyIncome], 
                                it[AdminApplicationsTable.aadharCardFileName], it[AdminApplicationsTable.marksheetFileName]
                            )
                        }
                    }
                } else {
                    // Student sees ONLY StudentApplicationsTable
                    transaction {
                        StudentApplicationsTable.selectAll().map {
                            Application(
                                it[StudentApplicationsTable.id],
                                Opportunity(it[StudentApplicationsTable.opportunityId], it[StudentApplicationsTable.opportunityTitle], 
                                    it[StudentApplicationsTable.opportunityCompany], it[StudentApplicationsTable.opportunityType], 
                                    emptyList(), "", null, ""),
                                it[StudentApplicationsTable.applicantName], it[StudentApplicationsTable.applicantEmail], 
                                it[StudentApplicationsTable.whyApply], it[StudentApplicationsTable.status],
                                it[StudentApplicationsTable.resumeFileName], it[StudentApplicationsTable.familyIncome], 
                                it[StudentApplicationsTable.aadharCardFileName], it[StudentApplicationsTable.marksheetFileName]
                            )
                        }
                    }
                }
                call.respond(apps)
            }

            // 🔹 Get Applications by Email - ISOLATED by role
            get("/applications/{email}") {
                val emailParam = call.parameters["email"] ?: ""
                val userRole = call.request.queryParameters["role"] ?: "Student"
                
                println("📊 Fetching applications for $emailParam with role: $userRole")
                
                val apps = if (userRole == "Admin") {
                    // From AdminApplicationsTable
                    transaction {
                        AdminApplicationsTable.select { AdminApplicationsTable.applicantEmail eq emailParam }.map {
                            Application(
                                it[AdminApplicationsTable.id],
                                Opportunity(it[AdminApplicationsTable.opportunityId], it[AdminApplicationsTable.opportunityTitle], 
                                    it[AdminApplicationsTable.opportunityCompany], it[AdminApplicationsTable.opportunityType], 
                                    emptyList(), "", null, ""),
                                it[AdminApplicationsTable.applicantName], it[AdminApplicationsTable.applicantEmail], 
                                it[AdminApplicationsTable.whyApply], it[AdminApplicationsTable.status],
                                it[AdminApplicationsTable.resumeFileName], it[AdminApplicationsTable.familyIncome], 
                                it[AdminApplicationsTable.aadharCardFileName], it[AdminApplicationsTable.marksheetFileName]
                            )
                        }
                    }
                } else {
                    // From StudentApplicationsTable
                    transaction {
                        StudentApplicationsTable.select { StudentApplicationsTable.applicantEmail eq emailParam }.map {
                            Application(
                                it[StudentApplicationsTable.id],
                                Opportunity(it[StudentApplicationsTable.opportunityId], it[StudentApplicationsTable.opportunityTitle], 
                                    it[StudentApplicationsTable.opportunityCompany], it[StudentApplicationsTable.opportunityType], 
                                    emptyList(), "", null, ""),
                                it[StudentApplicationsTable.applicantName], it[StudentApplicationsTable.applicantEmail], 
                                it[StudentApplicationsTable.whyApply], it[StudentApplicationsTable.status],
                                it[StudentApplicationsTable.resumeFileName], it[StudentApplicationsTable.familyIncome], 
                                it[StudentApplicationsTable.aadharCardFileName], it[StudentApplicationsTable.marksheetFileName]
                            )
                        }
                    }
                }
                call.respond(apps)
            }

            // 🔹 Apply for Opportunity - ISOLATED by role
            post("/apply") {
                val app = call.receive<Application>()
                val userRole = call.request.queryParameters["role"] ?: "Student"
                val currentTime = java.time.LocalDateTime.now().toString()
                
                println("📝 Application submitted for role: $userRole by ${app.applicantEmail}")
                
                if (userRole == "Admin") {
                    // Save to AdminApplicationsTable
                    transaction {
                        AdminApplicationsTable.insert {
                            it[AdminApplicationsTable.id] = app.id
                            it[AdminApplicationsTable.opportunityId] = app.opportunity.id
                            it[AdminApplicationsTable.opportunityTitle] = app.opportunity.title
                            it[AdminApplicationsTable.opportunityCompany] = app.opportunity.company
                            it[AdminApplicationsTable.opportunityType] = app.opportunity.type
                            it[AdminApplicationsTable.applicantName] = app.applicantName
                            it[AdminApplicationsTable.applicantEmail] = app.applicantEmail
                            it[AdminApplicationsTable.whyApply] = app.whyApply
                            it[AdminApplicationsTable.status] = app.status
                            it[AdminApplicationsTable.resumeFileName] = app.resumeFileName
                            it[AdminApplicationsTable.familyIncome] = app.familyIncome
                            it[AdminApplicationsTable.aadharCardFileName] = app.aadharCardFileName
                            it[AdminApplicationsTable.marksheetFileName] = app.marksheetFileName
                            it[AdminApplicationsTable.appliedAt] = currentTime
                        }
                    }
                } else {
                    // Save to StudentApplicationsTable
                    transaction {
                        StudentApplicationsTable.insert {
                            it[StudentApplicationsTable.id] = app.id
                            it[StudentApplicationsTable.opportunityId] = app.opportunity.id
                            it[StudentApplicationsTable.opportunityTitle] = app.opportunity.title
                            it[StudentApplicationsTable.opportunityCompany] = app.opportunity.company
                            it[StudentApplicationsTable.opportunityType] = app.opportunity.type
                            it[StudentApplicationsTable.applicantName] = app.applicantName
                            it[StudentApplicationsTable.applicantEmail] = app.applicantEmail
                            it[StudentApplicationsTable.whyApply] = app.whyApply
                            it[StudentApplicationsTable.status] = app.status
                            it[StudentApplicationsTable.resumeFileName] = app.resumeFileName
                            it[StudentApplicationsTable.familyIncome] = app.familyIncome
                            it[StudentApplicationsTable.aadharCardFileName] = app.aadharCardFileName
                            it[StudentApplicationsTable.marksheetFileName] = app.marksheetFileName
                            it[StudentApplicationsTable.appliedAt] = currentTime
                        }
                    }
                }
                call.respond(HttpStatusCode.Created)
            }

            // 🔹 Update Application Status - ISOLATED by role
            put("/applications/{id}/status") {
                val appId = call.parameters["id"] ?: ""
                val newStatus = call.request.queryParameters["status"] ?: "Pending"
                val userRole = call.request.queryParameters["role"] ?: "Student"
                
                println("📝 Updating application $appId status to $newStatus for role: $userRole")
                
                if (userRole == "Admin") {
                    transaction {
                        AdminApplicationsTable.update({ AdminApplicationsTable.id eq appId }) {
                            it[AdminApplicationsTable.status] = newStatus
                        }
                    }
                } else {
                    transaction {
                        StudentApplicationsTable.update({ StudentApplicationsTable.id eq appId }) {
                            it[StudentApplicationsTable.status] = newStatus
                        }
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
            
            // 🔹 Get All Students (for Admin dashboard)
            get("/students") {
                val students = transaction {
                    UsersTable.select { UsersTable.role eq "Student" }.map {
                        User(it[UsersTable.name], it[UsersTable.email], it[UsersTable.role], it[UsersTable.phoneNumber], it[UsersTable.branch])
                    }
                }
                call.respond(students)
            }
            
            // 🔹 Delete Student (Admin only)
            authenticate("auth-jwt") {
            delete("/students/{email}") {
                val principal = call.principal<JWTPrincipal>()
                val tokenRole = principal?.payload?.getClaim("role")?.asString()
                
                // Only admins or super admins can delete students
                if (tokenRole != "Admin" && tokenRole != "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Admin access required")
                    return@delete
                }
                
                val email = call.parameters["email"] ?: ""
                if (email.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Email is required")
                    return@delete
                }
                
                // Prevent deleting admins or super admins
                val userToDelete = transaction {
                    UsersTable.select { UsersTable.email eq email }.singleOrNull()
                }
                
                if (userToDelete == null) {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                    return@delete
                }
                
                val userRole = userToDelete[UsersTable.role]
                if (userRole == "Admin" || userRole == "SuperAdmin") {
                    call.respond(HttpStatusCode.Forbidden, "Cannot delete admin users through this endpoint")
                    return@delete
                }
                
                // Delete user's applications first (foreign key constraints)
                transaction {
                    // Delete from student applications
                    StudentApplicationsTable.deleteWhere { StudentApplicationsTable.applicantEmail eq email }
                    // Delete from legacy applications table
                    ApplicationsTable.deleteWhere { ApplicationsTable.applicantEmail eq email }
                    // Delete the user
                    UsersTable.deleteWhere { UsersTable.email eq email }
                }
                
                println("🗑️ Admin deleted student: $email")
                call.respond(mapOf("success" to true, "message" to "Student deleted successfully"))
            }
            }

            post("/upload") {
                val multipartData = call.receiveMultipart()
                var fileName = ""
                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val originalName = part.originalFileName ?: "file"
                            val fileBytes = part.streamProvider().readBytes()
                            fileName = "${UUID.randomUUID()}_$originalName"
                            File("uploads/$fileName").writeBytes(fileBytes)
                        }
                        else -> {}
                    }
                    part.dispose()
                }
                call.respond(mapOf("fileName" to fileName))
            }

            get("/uploads/{name}") {
                val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.NotFound)
                val file = File("uploads/$name")
                if (file.exists()) {
                    call.respondFile(file)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }.start(wait = true)
}
