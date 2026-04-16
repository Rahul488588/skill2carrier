package com.example.skill2career

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Course(
    var name: String = "",
    var credits: String = "",
    var gradePoints: String = ""
)

data class Semester(
    val id: Int,
    val courses: MutableList<Course> = mutableStateListOf(Course())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CGPATrackerScreen(navController: NavController, mainViewModel: MainViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val semesters = remember { mutableStateListOf(Semester(1)) }
    
    // Goal related states
    var targetCgpa by remember { mutableStateOf("8.5") }
    var totalSemesters by remember { mutableStateOf("8") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Sidebar(navController, drawerState, mainViewModel)
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CGPA Tracker & Planner", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { semesters.clear(); semesters.add(Semester(1)) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color(0xFFF8F9FA)
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Decorative gradient background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF1A73E8).copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    var showSummary by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { showSummary = true }

                    // Summary Card with Goal Tracker
                    AnimatedVisibility(
                        visible = showSummary,
                        enter = fadeIn(tween(800)) + expandVertically(tween(800))
                    ) {
                        CGPASummaryCard(semesters, targetCgpa.toFloatOrNull() ?: 0f)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Goal Settings Card
                    GoalSettingsCard(
                        targetCgpa = targetCgpa,
                        onTargetChange = { targetCgpa = it },
                        totalSemesters = totalSemesters,
                        onTotalSemChange = { totalSemesters = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // AI Goal Advisor
                    GoalAdvisorCard(semesters, targetCgpa.toFloatOrNull() ?: 0f, totalSemesters.toIntOrNull() ?: 8)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Your Semesters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF202124)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    semesters.forEachIndexed { index, semester ->
                        var itemVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(semester) { 
                            delay(index * 100L)
                            itemVisible = true 
                        }

                        AnimatedVisibility(
                            visible = itemVisible,
                            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 20 }
                        ) {
                            SemesterCard(
                                semester = semester,
                                onRemove = if (semesters.size > 1) { { semesters.removeAt(index) } } else null
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = { semesters.add(Semester(semesters.size + 1)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Semester", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun GoalSettingsCard(
    targetCgpa: String,
    onTargetChange: (String) -> Unit,
    totalSemesters: String,
    onTotalSemChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Your Goals", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = targetCgpa,
                    onValueChange = onTargetChange,
                    label = { Text("Target CGPA") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1A73E8),
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = totalSemesters,
                    onValueChange = onTotalSemChange,
                    label = { Text("Total Semesters") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1A73E8),
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun GoalAdvisorCard(semesters: List<Semester>, targetCgpa: Float, totalSems: Int) {
    var totalGradePoints = 0f
    var totalCredits = 0f
    val completedSems = semesters.size

    semesters.forEach { sem ->
        sem.courses.forEach { course ->
            val credits = course.credits.toFloatOrNull() ?: 0f
            val points = course.gradePoints.toFloatOrNull() ?: 0f
            totalGradePoints += points * credits
            totalCredits += credits
        }
    }

    val currentCgpa = if (totalCredits > 0) totalGradePoints / totalCredits else 0f
    val remainingSems = totalSems - completedSems
    
    val advice: String
    val adviceColor: Color
    
    if (targetCgpa <= 0f) {
        advice = "Set a target CGPA to get reachability advice."
        adviceColor = Color.Gray
    } else if (remainingSems <= 0) {
        advice = if (currentCgpa >= targetCgpa) "Congratulations! You reached your goal." else "Degree completed. You missed your target by ${(targetCgpa - currentCgpa).format(2)} points."
        adviceColor = if (currentCgpa >= targetCgpa) Color(0xFF34A853) else Color(0xFFEA4335)
    } else {
        val avgCreditsPerSem = if (completedSems > 0) totalCredits / completedSems else 20f
        val estimatedTotalCredits = totalSems * avgCreditsPerSem
        val pointsNeeded = (targetCgpa * estimatedTotalCredits) - totalGradePoints
        val requiredFutureSgpa = pointsNeeded / (remainingSems * avgCreditsPerSem)
        
        when {
            requiredFutureSgpa <= 0 -> {
                advice = "You've already surpassed your goal! Keep it up to finish even stronger."
                adviceColor = Color(0xFF34A853)
            }
            requiredFutureSgpa <= currentCgpa -> {
                advice = "On Track! You need an SGPA of ${requiredFutureSgpa.format(2)} in the next $remainingSems semesters. This is lower than your current performance."
                adviceColor = Color(0xFF1A73E8)
            }
            requiredFutureSgpa <= 10.0f -> {
                advice = "Push Harder! You need an SGPA of ${requiredFutureSgpa.format(2)} in the next $remainingSems semesters to reach your target of $targetCgpa."
                adviceColor = Color(0xFFFBBC04)
            }
            else -> {
                advice = "Mathematically Impossible: Even with a perfect 10.0 SGPA, you can't reach $targetCgpa. Try adjusting your target to something realistic."
                adviceColor = Color(0xFFEA4335)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = adviceColor.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, adviceColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = adviceColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("AI Goal Advisor", fontWeight = FontWeight.ExtraBold, color = adviceColor, fontSize = 14.sp)
                Text(advice, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3C4043), lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun CGPASummaryCard(semesters: List<Semester>, target: Float) {
    var totalGradePoints = 0f
    var totalCredits = 0f

    semesters.forEach { sem ->
        sem.courses.forEach { course ->
            val credits = course.credits.toFloatOrNull() ?: 0f
            val points = course.gradePoints.toFloatOrNull() ?: 0f
            totalGradePoints += points * credits
            totalCredits += credits
        }
    }

    val cgpa = if (totalCredits > 0) totalGradePoints / totalCredits else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0xFF1A73E8), spotColor = Color(0xFF1A73E8)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A73E8))
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("OVERALL CGPA", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(
                text = "%.2f".format(cgpa),
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            if (target > 0) {
                val progress by animateFloatAsState(
                    targetValue = (cgpa / target).coerceIn(0f, 1f),
                    animationSpec = tween(1000, easing = FastOutSlowInEasing),
                    label = "progress"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Goal: $target (${(progress * 100).toInt()}% achieved)",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStat("Total Credits", totalCredits.toInt().toString())
                VerticalDivider(modifier = Modifier.height(30.dp), color = Color.White.copy(alpha = 0.2f))
                SummaryStat("Semesters", semesters.size.toString())
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
fun SemesterCard(semester: Semester, onRemove: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp, 24.dp).background(Color(0xFF1A73E8), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Semester ${semester.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF202124)
                    )
                }
                if (onRemove != null) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove Semester", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            semester.courses.forEachIndexed { index, course ->
                CourseRow(
                    course = course,
                    onUpdate = { updatedCourse -> semester.courses[index] = updatedCourse },
                    onDelete = if (semester.courses.size > 1) { { semester.courses.removeAt(index) } } else null
                )
            }

            TextButton(
                onClick = { semester.courses.add(Course()) },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1A73E8))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Course", fontWeight = FontWeight.Bold)
            }
            
            // SGPA Calculation for Semester
            var semPoints = 0f
            var semCredits = 0f
            semester.courses.forEach { 
                val c = it.credits.toFloatOrNull() ?: 0f
                val g = it.gradePoints.toFloatOrNull() ?: 0f
                semPoints += c * g
                semCredits += c
            }
            val sgpa = if (semCredits > 0) semPoints / semCredits else 0f
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.2f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Semester SGPA: ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(
                    text = "%.2f".format(sgpa),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A73E8)
                )
            }
        }
    }
}

@Composable
fun CourseRow(course: Course, onUpdate: (Course) -> Unit, onDelete: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = course.name,
            onValueChange = { onUpdate(course.copy(name = it)) },
            label = { Text("Course", fontSize = 11.sp) },
            modifier = Modifier.weight(2.2f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1A73E8),
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f)
            )
        )
        OutlinedTextField(
            value = course.credits,
            onValueChange = { onUpdate(course.copy(credits = it)) },
            label = { Text("Credits", fontSize = 11.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1A73E8),
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f)
            )
        )
        OutlinedTextField(
            value = course.gradePoints,
            onValueChange = { onUpdate(course.copy(gradePoints = it)) },
            label = { Text("Grade", fontSize = 11.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1A73E8),
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f)
            )
        )
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove Course", tint = Color.Gray.copy(alpha = 0.6f))
            }
        }
    }
}

fun Float.format(digits: Int) = "%.${digits}f".format(this)
