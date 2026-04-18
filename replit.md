# SKILL2career

A full-stack Kotlin platform connecting students with career opportunities — internships, scholarships, jobs. Features AI-powered resume analysis, CGPA tracking, application management, and role-based access (Student, Admin, SuperAdmin).

## Architecture

- **Backend**: Kotlin/Ktor server (Netty engine) running on port 5000
- **Mobile Client**: Android app (Jetpack Compose) — built separately in Android Studio
- **Database**: H2 embedded file-based database (`s2c_database.mv.db`)
- **Auth**: JWT tokens + BCrypt password hashing
- **AI**: OpenRouter/OpenAI API for resume/skills gap analysis
- **Documents**: Apache PDFBox + Apache POI for PDF/DOCX parsing

## Project Structure

```
server/          - Ktor backend server
  src/main/kotlin/com/example/skill2career/server/
    Application.kt       - Main server, routing, DB schema
    OpenAIService.kt     - AI integration (resume analysis)
app/             - Android mobile application (Jetpack Compose)
gradle/          - Gradle wrapper and version catalog
s2c_database.mv.db - H2 embedded database (persistent)
uploads/         - Uploaded resume/document storage
```

## Running Locally

The workflow `Start application` builds and starts the server:
```
java -jar gradle/wrapper/gradle-wrapper.jar :server:installDist --no-daemon
server/build/install/server/bin/server
```

Server runs at `http://0.0.0.0:5000`.

## Key Endpoints

- `GET /` — Health check
- `POST /auth/login` — Login (returns JWT)
- `POST /auth/register` — Register student
- `POST /admin/setup` — First-run admin setup
- `GET /opportunities` — List opportunities (auth required)
- `POST /resume/analyze` — AI resume analysis (auth required)

## SuperAdmin

Hardcoded credentials in `Application.kt` (`SuperAdminConfig`). Should be changed in production.

## Build System

- Gradle 9.1.0 with Kotlin DSL
- Java runtime: GraalVM 22.3.1 (JDK 19)
- The `:app` Android module requires Android SDK (not available in Replit); only `:server` is built here

## Deployment

Configured as autoscale deployment. Build command compiles the Ktor server; run command starts the installed binary.
