package com.example.skill2career

import android.content.Context
import androidx.room.*
import androidx.room.Delete
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// 🔹 Room Entities
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val role: String,
    val password: String,
    val phoneNumber: String,
    val branch: String
)

@Entity(tableName = "opportunities")
data class OpportunityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val company: String,
    val type: String,
    val tagsJson: String,
    val location: String,
    val stipendOrSalary: String?,
    val date: String,
    val minCgpa: Double?
)

@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey val id: String,
    val opportunityId: String,
    val opportunityTitle: String,
    val opportunityCompany: String,
    val opportunityType: String,
    val applicantName: String,
    val applicantEmail: String,
    val whyApply: String,
    val status: String,
    val resumeFileName: String?,
    val familyIncome: String?,
    val aadharCardFileName: String?,
    val marksheetFileName: String?
)

// 🔹 DAOs
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'Student'")
    suspend fun getAllStudents(): List<UserEntity>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface OpportunityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpportunity(opp: OpportunityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpportunities(opps: List<OpportunityEntity>)

    @Query("SELECT * FROM opportunities")
    suspend fun getAllOpportunities(): List<OpportunityEntity>

    @Query("DELETE FROM opportunities WHERE id = :id")
    suspend fun deleteOpportunity(id: String)
}

@Dao
interface ApplicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(app: ApplicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(apps: List<ApplicationEntity>)

    @Query("SELECT * FROM applications WHERE applicantEmail = :email")
    suspend fun getApplicationsByEmail(email: String): List<ApplicationEntity>

    @Query("SELECT * FROM applications")
    suspend fun getAllApplications(): List<ApplicationEntity>

    @Query("UPDATE applications SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

// 🔹 Database
@Database(entities = [UserEntity::class, OpportunityEntity::class, ApplicationEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun opportunityDao(): OpportunityDao
    abstract fun applicationDao(): ApplicationDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "s2c_db")
                    .fallbackToDestructiveMigration() // Clear old data for security
                    .build().also { instance = it }
            }
    }
}
