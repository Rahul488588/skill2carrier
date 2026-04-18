package com.example.skill2career.server

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

// 🔐 OpenRouter API Configuration
object OpenAIConfig {
    private fun loadDotEnv(): Map<String, String> {
        val candidates = listOf(
            File(".env"),
            File("server/.env")
        )

        val file = candidates.firstOrNull { it.exists() && it.isFile } ?: return emptyMap()
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim().trim('"')
                if (key.isBlank()) null else key to value
            }
            .toMap()
    }

    private val dotEnv: Map<String, String> = runCatching { loadDotEnv() }.getOrDefault(emptyMap())

    private fun env(name: String): String? = System.getenv(name) ?: dotEnv[name]

    // Set your OpenRouter API key in environment variable OPENROUTER_API_KEY
    val API_KEY: String = env("OPENROUTER_API_KEY") ?: ""
    
    // Models for different AI operations
    private val DEFAULT_MODEL_RESUME_ANALYSIS = "openai/gpt-4o-mini"
    private val DEFAULT_MODEL_SKILL_EXTRACTION = "openai/gpt-4o-mini"
    private val DEFAULT_MODEL_CAREER_SUGGESTIONS = "openai/gpt-4o-mini"
    private val DEFAULT_MODEL_OPPORTUNITY_SEARCH = "openai/gpt-4o-mini"

    val MODEL_RESUME_ANALYSIS: String = env("OPENROUTER_MODEL_RESUME_ANALYSIS") ?: DEFAULT_MODEL_RESUME_ANALYSIS
    val MODEL_SKILL_EXTRACTION: String = env("OPENROUTER_MODEL_SKILL_EXTRACTION") ?: DEFAULT_MODEL_SKILL_EXTRACTION
    val MODEL_CAREER_SUGGESTIONS: String = env("OPENROUTER_MODEL_CAREER_SUGGESTIONS") ?: DEFAULT_MODEL_CAREER_SUGGESTIONS
    val MODEL_OPPORTUNITY_SEARCH: String = env("OPENROUTER_MODEL_OPPORTUNITY_SEARCH") ?: DEFAULT_MODEL_OPPORTUNITY_SEARCH

    val BASE_URL: String = env("OPENROUTER_BASE_URL")
        ?: "https://openrouter.ai/api/v1"

    // Optional (recommended by OpenRouter for analytics/rate limiting)
    val HTTP_REFERER: String? = env("OPENROUTER_HTTP_REFERER")
    val X_TITLE: String? = env("OPENROUTER_X_TITLE")
    
    init {
        if (API_KEY.isBlank() || API_KEY.startsWith("YOUR_")) {
            println("⚠️ WARNING: OpenRouter API key not configured. Please set OPENROUTER_API_KEY environment variable.")
        }

        println("✅ OpenRouter config: baseUrl=$BASE_URL")
        println("✅ OpenRouter config: modelResumeAnalysis=$MODEL_RESUME_ANALYSIS")
        println("✅ OpenRouter config: modelSkillExtraction=$MODEL_SKILL_EXTRACTION")
        println("✅ OpenRouter config: modelCareerSuggestions=$MODEL_CAREER_SUGGESTIONS")
        println("✅ OpenRouter config: modelOpportunitySearch=$MODEL_OPPORTUNITY_SEARCH")
    }
}

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)

@Serializable
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>? = null
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage? = null
)

// 🤖 OpenAI Service
class OpenAIService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private val minRequestInterval = 1000L // 1 second between requests to avoid rate limiting

    // 🔍 Search for Opportunities using AI
    suspend fun searchAIOpportunities(query: String): String {
        val prompt = """
            You are a career opportunity researcher. Search for current, real opportunities based on query: "$query"
            
            IMPORTANT FILTERING CRITERIA:
            - For SCHOLARSHIPS: ONLY include scholarships from Indian colleges and universities (e.g., IITs, NITs, IISc, BITS, Delhi University, etc.)
            - For INTERNSHIPS: Include internships WORLDWIDE (any country/location)
            
            Return a JSON array of opportunities with these fields:
            - title: string
            - type: "Internship" or "Scholarship"
            - organization: string
            - location: string
            - deadline: string (YYYY-MM-DD format)
            - link: string (URL to apply)
            - description: string (brief description)
            
            Requirements:
            - Return ONLY current opportunities (not outdated)
            - Focus on real and verifiable sources
            - Avoid fake or duplicate entries
            - Limit to 5-10 best opportunities (mix of internships and scholarships)
            - For Indian scholarships, specify location as the Indian city/institution name
            - Return ONLY valid JSON. Do NOT use markdown code blocks or ```json fences.
            - Provide raw JSON response only, nothing else
            
            Example format:
            [
                {
                    "title": "Software Engineering Internship",
                    "type": "Internship",
                    "organization": "Google",
                    "location": "Mountain View, CA",
                    "deadline": "2025-06-30",
                    "link": "https://careers.google.com/jobs",
                    "description": "Work on real projects with experienced engineers"
                },
                {
                    "title": "IIT Delhi Merit Scholarship",
                    "type": "Scholarship",
                    "organization": "IIT Delhi",
                    "location": "New Delhi, India",
                    "deadline": "2025-07-15",
                    "link": "https://iitd.ac.in/scholarships",
                    "description": "Merit-based scholarship for undergraduate students"
                }
            ]
        """.trimIndent()

        return sendChatRequest(prompt, OpenAIConfig.MODEL_OPPORTUNITY_SEARCH)
    }

    // 📊 Analyze Resume and Get Career Suggestions
    suspend fun analyzeResume(resumeText: String): ResumeAnalysis {
        val prompt = """
            You are a professional career counselor. Analyze resume below and create a career analysis.

            DO NOT repeat resume text. Instead, provide your original analysis.

            Create a JSON object with these fields:
            - summary: A 2-3 sentence summary of candidate's profile
            - strengths: 3-5 key strengths (as strings in an array)
            - improvements: 2-3 areas for improvement (as strings in an array)
            - careers: 3-5 recommended career paths (as strings in an array)

            Example format:
            {
              "summary": "Candidate has strong technical skills in Java and Python with 5 years of experience.",
              "strengths": ["Strong programming skills", "Good communication", "Team leadership"],
              "improvements": ["Need more cloud experience", "Improve presentation skills"],
              "careers": ["Software Engineer", "Technical Lead", "Solutions Architect"]
            }

            Resume to analyze:
            $resumeText

            Return ONLY valid JSON. Do NOT use markdown code blocks or ```json fences.
            Provide raw JSON response only, nothing else.
        """.trimIndent()

        val response = sendChatRequest(prompt, OpenAIConfig.MODEL_RESUME_ANALYSIS)

        println("📝 Resume Analysis Response: $response")

        // Parse JSON response (simplified parsing)
        return parseResumeAnalysis(response)
    }

    // 🎯 Extract Skills from Resume
    suspend fun extractSkills(resumeText: String): List<String> {
        val prompt = """
            Extract technical skills from following resume. Return only skills as a JSON array of strings.
            
            Resume:
            $resumeText
            
            Return ONLY valid JSON array. Do NOT use markdown code blocks or ```json fences.
            Provide raw JSON response only, nothing else.
            Example: ["Java", "Python", "SQL", "Machine Learning"]
        """.trimIndent()

        val response = sendChatRequest(prompt, OpenAIConfig.MODEL_SKILL_EXTRACTION)
        
        return parseSkills(response)
    }
    
    // 📊 Analyze Skills Gap for Target Role
    suspend fun analyzeSkillsGap(resumeText: String, targetRole: String): SkillsGapAnalysis {
        val prompt = """
            You are a career development expert. Analyze the resume below and identify the skills gap for the target role: "$targetRole"
            
            Provide a detailed analysis in JSON format with these fields:
            - targetRole: The target role name
            - currentSkills: List of skills already present in the resume
            - requiredSkills: List of skills typically required for the target role
            - missingSkills: List of skills the candidate needs to learn (difference between required and current)
            - recommendations: List of actionable recommendations to fill the gap
            - learningResources: List of specific resources to learn missing skills (courses, certifications, tutorials)
            
            Return ONLY valid JSON. Do NOT use markdown code blocks or ```json fences.
            Provide raw JSON response only, nothing else.
            
            Example format:
            {
              "targetRole": "Senior Software Engineer",
              "currentSkills": ["Java", "Spring Boot", "MySQL"],
              "requiredSkills": ["Java", "Spring Boot", "MySQL", "Docker", "Kubernetes", "AWS", "System Design"],
              "missingSkills": ["Docker", "Kubernetes", "AWS", "System Design"],
              "recommendations": [
                "Learn containerization with Docker",
                "Study Kubernetes for container orchestration",
                "Get AWS Certified Solutions Architect",
                "Practice system design interviews"
              ],
              "learningResources": [
                "Docker for Beginners course on Udemy",
                "Kubernetes for Developers course",
                "AWS Solutions Architect Associate certification",
                "System Design Interview book by Alex Xu"
              ]
            }
            
            Resume to analyze:
            $resumeText
            
            Target Role: $targetRole
        """.trimIndent()
        
        val response = sendChatRequest(prompt, OpenAIConfig.MODEL_RESUME_ANALYSIS)
        
        println("📊 Skills Gap Analysis Response: $response")
        
        return parseSkillsGapAnalysis(response, targetRole)
    }

    // 🔧 Send Chat Request to OpenRouter
    private suspend fun sendChatRequest(prompt: String, model: String): String {
        // Rate limiting
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < minRequestInterval) {
                delay(minRequestInterval - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()
        }

        val request = OpenRouterRequest(
            model = model,
            messages = listOf(
                OpenRouterMessage(
                    role = "user",
                    content = prompt
                )
            )
        )

        val url = "${OpenAIConfig.BASE_URL}/chat/completions"

        println("🔵 OpenRouter API Request: $url")

        // Serialize request manually to avoid Ktor serialization issues
        val requestJson = Json.encodeToString(OpenRouterRequest.serializer(), request)
        
        val responseText: String = client.post(url) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${OpenAIConfig.API_KEY}")
            OpenAIConfig.HTTP_REFERER?.let { header("HTTP-Referer", it) }
            OpenAIConfig.X_TITLE?.let { header("X-Title", it) }
            setBody(requestJson)
        }.body()

        println("🔵 OpenRouter API Raw Response: $responseText")

        val jsonParser = Json { ignoreUnknownKeys = true }
        val response: OpenRouterResponse = jsonParser.decodeFromString(responseText)

        // Check for API errors
        if (response.choices.isNullOrEmpty()) {
            throw Exception("OpenRouter API returned no choices. Response: $responseText")
        }

        val choice = response.choices.first()
        if (choice.message?.content == null) {
            throw Exception("OpenRouter API returned no content. Response: $responseText")
        }

        // Extract JSON from markdown code blocks if present
        val cleanContent = extractJsonFromMarkdown(choice.message!!.content)
        return cleanContent
    }
    
    // 🔧 Extract JSON content from markdown code blocks
    private fun extractJsonFromMarkdown(content: String): String {
        // Check if content is wrapped in markdown code blocks
        val trimmed = content.trim()
        
        // Pattern: ```json ... ``` or ``` ... ```
        val codeBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        val match = codeBlockRegex.find(trimmed)
        
        return if (match != null) {
            // Extract content from inside code block
            match.groupValues[1].trim()
        } else {
            // No code block found, return trimmed content
            trimmed
        }
    }

    // 📝 Parse Resume Analysis Response
    private fun parseResumeAnalysis(json: String): ResumeAnalysis {
        println("🔍 Parsing resume analysis response...")

        // Simplified JSON parsing
        val summary = extractField(json, "summary")
        val strengths = extractListField(json, "strengths")
        val improvements = extractListField(json, "improvements")
        val careers = extractListField(json, "careers")

        // If JSON parsing failed, use raw response as summary
        val finalSummary = if (summary.isNullOrEmpty() || summary == "....") {
            println("⚠️ JSON parsing failed, using raw response")
            json.take(500) // Use first 500 chars of raw response
        } else {
            summary
        }

        return ResumeAnalysis(
            summary = finalSummary,
            strengths = strengths ?: emptyList(),
            improvements = improvements ?: emptyList(),
            recommendedCareers = careers ?: emptyList()
        )
    }

    // 🎯 Parse Skills Response
    private fun parseSkills(json: String): List<String> {
        return extractListField(json) ?: emptyList()
    }

    // 🔧 Helper to extract field from JSON-like string
    private fun extractField(json: String, fieldName: String): String? {
        val pattern = "\"$fieldName\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)
    }

    // 🔧 Helper to extract list field from JSON-like string
    private fun extractListField(json: String, fieldName: String? = null): List<String>? {
        val pattern = if (fieldName != null) {
            "\"$fieldName\"\\s*:\\s*\\[([^\\]]+)\\]".toRegex()
        } else {
            "\\[([^\\]]+)\\]".toRegex()
        }
        val match = pattern.find(json)
        return match?.groupValues?.get(1)
            ?.split(",")
            ?.map { it.trim().replace("\"", "") }
            ?.filter { it.isNotEmpty() }
    }
    
    // 📝 Parse Skills Gap Analysis Response
    private fun parseSkillsGapAnalysis(json: String, targetRole: String): SkillsGapAnalysis {
        println("🔍 Parsing skills gap analysis response...")
        
        // Use Gson for robust JSON parsing
        return try {
            val gson = com.google.gson.Gson()
            gson.fromJson(json, SkillsGapAnalysis::class.java)
        } catch (e: Exception) {
            println("⚠️ JSON parsing failed, using manual parsing: ${e.message}")
            // Fallback to manual parsing
            SkillsGapAnalysis(
                targetRole = targetRole,
                currentSkills = extractListField(json, "currentSkills") ?: emptyList(),
                requiredSkills = extractListField(json, "requiredSkills") ?: emptyList(),
                missingSkills = extractListField(json, "missingSkills") ?: emptyList(),
                recommendations = extractListField(json, "recommendations") ?: emptyList(),
                learningResources = extractListField(json, "learningResources") ?: emptyList()
            )
        }
    }
}

// 📊 Data Classes
data class ResumeAnalysis(
    val summary: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val recommendedCareers: List<String>
)

data class SkillsGapAnalysis(
    val targetRole: String,
    val currentSkills: List<String>,
    val requiredSkills: List<String>,
    val missingSkills: List<String>,
    val recommendations: List<String>,
    val learningResources: List<String>
)
