package com.example.skill2career

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * -----------------------------------------------------------------------------------------
 *  BACKEND NOTE: STUDENT DASHBOARD
 *  This screen represents the main entry point for Students.
 *  TODO: Fetch real statistics (Applied count, etc.) from backend.
 * -----------------------------------------------------------------------------------------
 */

data class Stat(
    val count: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreen(navController: NavController, mainViewModel: MainViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Handle back button - close drawer if open, otherwise exit app
    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            // Exit the app when on main screen
            (navController.context as? android.app.Activity)?.finish()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Sidebar(navController, drawerState, mainViewModel)
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Dashboard",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color(0xFFF8F9FA)
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Decorative Element
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF1A73E8).copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    GreetingSection(mainViewModel)

                    Spacer(modifier = Modifier.height(24.dp))

                    StatsGrid(navController, mainViewModel)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    SectionHeader("Recommended for you")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    RecommendationCard()
                }
            }
        }
    }
}

@Composable
fun GreetingSection(mainViewModel: MainViewModel) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { 
        delay(100)
        visible = true 
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(1000)) + slideInHorizontally(tween(800)) { -100 }
    ) {
        Column {
            Text(
                text = "Welcome back, ${mainViewModel.currentUser.value?.name ?: "Student"} 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF202124)
            )
            Text(
                text = "Let's find your dream career today.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF202124)
    )
}

@Composable
fun RecommendationCard() {
    var isHovered by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(if (isHovered) 1.02f else 1f, label = "cardScale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .graphicsLayer(scaleX = cardScale, scaleY = cardScale)
            .shadow(if (isHovered) 12.dp else 4.dp, RoundedCornerShape(20.dp))
            .clickable { isHovered = !isHovered },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F0FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text("AI Match: Software Intern", fontWeight = FontWeight.Bold)
                Text("Based on your skills", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(40.dp, 16.dp).background(Color(0xFFE6F4EA), RoundedCornerShape(4.dp)))
                    Box(modifier = Modifier.size(60.dp, 16.dp).background(Color(0xFFFEF7E0), RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}

@Composable
fun Sidebar(
    navController: NavController,
    drawerState: DrawerState,
    mainViewModel: MainViewModel
) {
    val scope = rememberCoroutineScope()

    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(Color(0xFF1A73E8), Color(0xFF4285F4))),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S2C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Skill2Career",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A73E8)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        val menuItems = listOf(
            Triple("Dashboard", Icons.Default.Home, "studentScreen"),
            Triple("Profile", Icons.Default.Person, "profile"),
            Triple("Opportunities", Icons.Default.Work, "opportunities"),
            Triple("My Applications", Icons.Default.Assignment, "myApplications"),
            Triple("Resume Builder", Icons.Default.Description, "resumeBuilder"),
            Triple("AI Resume Analysis", Icons.Default.AutoAwesome, "aiResumeAnalysis"),
            Triple("CGPA Tracker", Icons.Default.BarChart, "cgpaTracker"),
            Triple("Saved", Icons.Default.Bookmark, "saved")
        )

        menuItems.forEach { (title, icon, route) ->
            DrawerItem(title, icon) {
                scope.launch {
                    drawerState.close()
                    navController.navigate(route) {
                        if (route == "studentScreen") popUpTo("studentScreen") { inclusive = true }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        DrawerItem("Logout", Icons.Default.ExitToApp) {
            scope.launch {
                drawerState.close()
                mainViewModel.logout()
                navController.navigate("login") {
                    // Clear entire back stack
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DrawerItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF3C4043),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatsGrid(navController: NavController, mainViewModel: MainViewModel) {
    val appliedCount = mainViewModel.myApplications.size.toString()
    val scholarshipCount = mainViewModel.myApplications.count { it.opportunity.type == OpportunityType.Scholarship }.toString()
    val opportunitiesCount = mainViewModel.opportunities.size.toString()
    
    val stats = listOf(
        Stat(opportunitiesCount, "Opportunities", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF34A853), "opportunities"),
        Stat(appliedCount, "Applied", Icons.Default.CheckCircle, Color(0xFF1A73E8), "myApplications"),
        Stat(scholarshipCount, "Scholarships", Icons.Default.School, Color(0xFFFBBC04), "opportunities/Scholarship"),
        Stat("3", "Notifications", Icons.Default.Notifications, Color(0xFFEA4335), null)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.height(280.dp) 
    ) {
        itemsIndexed(stats) { index, item ->
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(index * 150L + 200L)
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.8f) + slideInVertically(tween(600)) { 50 }
            ) {
                StatCard(item) {
                    item.route?.let { navController.navigate(it) }
                }
            }
        }
    }
}

@Composable
fun StatCard(stat: Stat, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (pressed) 4.dp else 10.dp, 
                shape = RoundedCornerShape(24.dp), 
                ambientColor = stat.color.copy(alpha = 0.5f), 
                spotColor = stat.color.copy(alpha = 0.5f)
            )
            .clickable {
                pressed = true
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        LaunchedEffect(pressed) {
            if (pressed) {
                delay(100)
                pressed = false
            }
        }

        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(stat.color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = stat.icon,
                    contentDescription = null,
                    tint = stat.color,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stat.count,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp
                ),
                color = Color(0xFF202124)
            )

            Text(
                text = stat.title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
