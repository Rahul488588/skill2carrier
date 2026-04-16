plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.example.skill2career"
version = "1.0.0"

dependencies {
    val ktor_version = "2.3.12"
    val exposed_version = "0.41.1"
    val h2_version = "2.1.214"
    
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-gson-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Database: Exposed + H2
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("com.auth0:java-jwt:4.4.0")
}

application {
    mainClass.set("com.example.skill2career.server.ApplicationKt")
}

kotlin {
    // jvmToolchain(17)
}
