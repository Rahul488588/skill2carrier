package com.example.skill2career

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.skill2career.network.SkillsGapAnalysis
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

enum class AnalysisTab {
    ResumeAnalysis,
    SkillsGap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIResumeAnalysisScreen(navController: NavController, mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Tab state
    var selectedTab by remember { mutableStateOf(AnalysisTab.ResumeAnalysis) }
    
    // Common state
    var resumeText by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    
    // Resume Analysis tab state
    var analysisResult by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // Skills Gap tab state
    var targetRole by remember { mutableStateOf("") }
    var skillsGapResult by remember { mutableStateOf<SkillsGapAnalysis?>(null) }
    var isAnalyzingGap by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Resume Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A73E8),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI Resume Analysis",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A73E8)
                    )
                    Text(
                        text = "Upload your resume or paste the text to get AI-powered analysis. Switch tabs to get career recommendations or identify skills gaps for your target role.",
                        fontSize = 14.sp,
                        color = Color(0xFF5F6368)
                    )
                }
            }
            
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.White,
                contentColor = Color(0xFF1A73E8)
            ) {
                Tab(
                    selected = selectedTab == AnalysisTab.ResumeAnalysis,
                    onClick = { selectedTab = AnalysisTab.ResumeAnalysis },
                    text = { Text("Resume Analysis") }
                )
                Tab(
                    selected = selectedTab == AnalysisTab.SkillsGap,
                    onClick = { selectedTab = AnalysisTab.SkillsGap },
                    text = { Text("Skills Gap") }
                )
            }

            // Professional File Upload Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF1A73E8)
                        )
                        Column {
                            Text(
                                text = "Upload Your Resume",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF202124)
                            )
                            Text(
                                text = "PDF, DOCX, DOC, or TXT files supported",
                                fontSize = 12.sp,
                                color = Color(0xFF5F6368)
                            )
                        }
                    }
                    
                    Divider(color = Color(0xFFE0E0E0))
                    
                    // File Picker Button
                    val filePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            selectedFileUri = it
                            val fileName = getFileName(context, it) ?: "resume.pdf"
                            selectedFileName = fileName
                            uploadError = null
                            
                            // Auto-upload and extract text (background processing)
                            scope.launch {
                                isUploading = true
                                try {
                                    val extractedText = uploadAndExtractText(context, mainViewModel, it, fileName)
                                    if (extractedText != null) {
                                        resumeText = extractedText
                                        uploadError = null
                                    } else {
                                        uploadError = "Failed to extract text from file"
                                    }
                                } catch (e: Exception) {
                                    uploadError = "Error: ${e.message}"
                                } finally {
                                    isUploading = false
                                }
                            }
                        }
                    }
                    
                    // Upload Area
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clickable(enabled = !isUploading) { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedFileName != null) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                        border = if (selectedFileName != null) 
                            BorderStroke(2.dp, Color(0xFF4CAF50))
                        else 
                            BorderStroke(2.dp, Color(0xFF1A73E8).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when {
                                isUploading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = Color(0xFF1A73E8),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Processing Resume...",
                                        fontSize = 14.sp,
                                        color = Color(0xFF5F6368)
                                    )
                                }
                                selectedFileName != null -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = selectedFileName!!,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = "Click to change file",
                                        fontSize = 12.sp,
                                        color = Color(0xFF5F6368)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = Color(0xFF1A73E8)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Click to select file",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF202124)
                                    )
                                    Text(
                                        text = "Drop your resume here or browse",
                                        fontSize = 12.sp,
                                        color = Color(0xFF5F6368)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Show error
                    if (uploadError != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uploadError!!,
                                fontSize = 13.sp,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    // File format hints
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf("PDF", "DOCX", "DOC", "TXT").forEach { format ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE0E0E0)
                            ) {
                                Text(
                                    text = format,
                                    fontSize = 11.sp,
                                    color = Color(0xFF5F6368),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Resume Analysis Tab Content
            if (selectedTab == AnalysisTab.ResumeAnalysis) {
                // Analyze Button
                Button(
                    onClick = {
                        if (resumeText.isNotBlank()) {
                            isAnalyzing = true
                            mainViewModel.analyzeResume(resumeText) { success, result ->
                                isAnalyzing = false
                                if (success && result != null) {
                                    analysisResult = """
                                        Summary:
                                        ${result.summary}
                                        
                                        Strengths:
                                        ${result.strengths.joinToString("\n- ", "- ")}
                                        
                                        Areas for Improvement:
                                        ${result.improvements.joinToString("\n- ", "- ")}
                                        
                                        Recommended Careers:
                                        ${result.recommendedCareers.joinToString("\n- ", "- ")}
                                    """.trimIndent()
                                } else {
                                    analysisResult = "Failed to analyze resume. Please try again."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = resumeText.isNotBlank() && !isAnalyzing
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...")
                    } else {
                        Text("Analyze Resume")
                    }
                }

                // Analysis Result
                if (analysisResult.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Analysis Results",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A73E8)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = analysisResult,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
            
            // Skills Gap Tab Content
            if (selectedTab == AnalysisTab.SkillsGap) {
                // Target Role Input
                OutlinedTextField(
                    value = targetRole,
                    onValueChange = { targetRole = it },
                    label = { Text("Target Role (e.g., Senior Software Engineer)") },
                    placeholder = { Text("Enter the job role you want to apply for...") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAnalyzingGap,
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Analyze Skills Gap Button
                Button(
                    onClick = {
                        if (resumeText.isNotBlank() && targetRole.isNotBlank()) {
                            isAnalyzingGap = true
                            mainViewModel.analyzeSkillsGap(resumeText, targetRole) { success, result ->
                                isAnalyzingGap = false
                                if (success) {
                                    skillsGapResult = result
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = resumeText.isNotBlank() && targetRole.isNotBlank() && !isAnalyzingGap,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853))
                ) {
                    if (isAnalyzingGap) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing Gap...")
                    } else {
                        Text("Analyze Skills Gap")
                    }
                }
                
                // Skills Gap Result
                skillsGapResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Title
                            Text(
                                text = "Skills Gap Analysis: ${result.targetRole}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A73E8)
                            )
                            
                            // Current Skills
                            Text(
                                text = "✅ Your Current Skills:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF34A853)
                            )
                            Text(
                                text = result.currentSkills.joinToString(", "),
                                fontSize = 13.sp,
                                color = Color(0xFF5F6368)
                            )
                            
                            // Missing Skills (Highlighted in red)
                            if (result.missingSkills.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "⚠️ Skills You Need to Learn:",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFC62828)
                                        )
                                        result.missingSkills.forEach { skill ->
                                            Text(
                                                text = "• $skill",
                                                fontSize = 13.sp,
                                                color = Color(0xFFC62828)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Recommendations
                            if (result.recommendations.isNotEmpty()) {
                                Text(
                                    text = "💡 Recommendations:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF202124)
                                )
                                result.recommendations.forEach { rec ->
                                    Text(
                                        text = "• $rec",
                                        fontSize = 13.sp,
                                        color = Color(0xFF5F6368)
                                    )
                                }
                            }
                            
                            // Learning Resources
                            if (result.learningResources.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "📚 Learning Resources:",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1565C0)
                                        )
                                        result.learningResources.forEach { resource ->
                                            Text(
                                                text = "• $resource",
                                                fontSize = 13.sp,
                                                color = Color(0xFF1565C0)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Using existing getFileName from opportunities.kt

// 📄 Helper function to upload file and extract text
suspend fun uploadAndExtractText(
    context: android.content.Context,
    mainViewModel: MainViewModel,
    uri: Uri,
    fileName: String
): String? {
    return try {
        // Copy file to temp location
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, fileName)
        FileOutputStream(tempFile).use { output ->
            inputStream?.copyTo(output)
        }
        inputStream?.close()
        
        // Upload file and extract text
        val response = mainViewModel.extractTextFromFile(tempFile)
        
        // Clean up temp file
        tempFile.delete()
        
        response
    } catch (e: Exception) {
        println("❌ Error uploading file: ${e.message}")
        e.printStackTrace()
        null
    }
}
