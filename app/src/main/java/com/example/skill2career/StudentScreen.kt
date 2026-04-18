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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
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

// ─── Classical Professional Palette ────────────────────────────────────────────
private val NavyDeep        = Color(0xFF1B2A4A)
private val NavyMid         = Color(0xFF2E3D5E)
private val NavyLight       = Color(0xFF3A4F7A)
private val Gold            = Color(0xFFC9A84C)
private val GoldLight       = Color(0xFFF0EAD5)
private val Ivory           = Color(0xFFF5F0E8)
private val CardSurface     = Color(0xFFFAF8F4)
private val TextPrimary     = Color(0xFF1A1A2E)
private val TextSecondary   = Color(0xFF4A4F6A)
private val TextMuted       = Color(0xFF8B8FA8)
private val DividerWarm     = Color(0xFF2E3D5E)
private val DividerLight    = Color(0xFFD4C5A9)
private val ForestGreen     = Color(0xFF2D5A3D)
private val ForestGreenBg   = Color(0xFFD5E8DC)
private val Burgundy        = Color(0xFF7A2A35)
private val BurgundyBg      = Color(0xFFF0D5D5)
private val SidebarBg       = NavyDeep
private val SidebarSelected = Color(0xFF243552)
private val SidebarIconInactive = Color(0xFF8BA3CC)
private val SidebarTextInactive = Color(0xFFB0BDD0)
// ───────────────────────────────────────────────────────────────────────────────

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

    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
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
                        Column {
                            Text(
                                "Dashboard",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                mainViewModel.currentUser.value?.name ?: "Student",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFADB8CC)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Gold)
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Gold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NavyDeep,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Ivory
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                GreetingSection(mainViewModel)

                Spacer(modifier = Modifier.height(24.dp))

                StatsGrid(navController, mainViewModel)

                Spacer(modifier = Modifier.height(32.dp))

                SectionHeader("Quick Actions")

                Spacer(modifier = Modifier.height(16.dp))

                QuickActionsGrid(navController)

                Spacer(modifier = Modifier.height(32.dp))

                SectionHeader("Recent Opportunities")

                Spacer(modifier = Modifier.height(16.dp))

                RecentOpportunitiesCard(navController, mainViewModel)
            }
        }
    }
}

@Composable
fun GreetingSection(mainViewModel: MainViewModel) {
    val userName = mainViewModel.currentUser.value?.name ?: "Student"
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    Column {
        Text(
            text = "$greeting, $userName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Here's what's happening with your career journey",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = NavyDeep
        )
    }
}

@Composable
fun QuickActionsGrid(navController: NavController) {
    val actions = listOf(
        Triple("Browse Jobs", Icons.Default.Work, "opportunities"),
        Triple("My Applications", Icons.Default.Assignment, "myApplications"),
        Triple("Resume Builder", Icons.Default.Description, "resumeBuilder"),
        Triple("CGPA Tracker", Icons.Default.BarChart, "cgpaTracker")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(actions) { index, (title, icon, route) ->
            QuickActionCard(title, icon) {
                try {
                    navController.navigate(route)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(NavyDeep.copy(alpha = 0.09f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NavyDeep,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun RecentOpportunitiesCard(navController: NavController, mainViewModel: MainViewModel) {
    val recentOpportunities = mainViewModel.opportunities.take(3)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Latest Opportunities",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyDeep
                )
                TextButton(
                    onClick = {
                        try {
                            navController.navigate("opportunities")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                ) {
                    Text(
                        "View All",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recentOpportunities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No opportunities available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    recentOpportunities.forEach { opp ->
                        RecentOpportunityItem(opp) {
                            try {
                                navController.navigate("opportunities")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentOpportunityItem(opportunity: Opportunity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    when (opportunity.safeType) {
                        OpportunityType.Internship -> NavyDeep.copy(alpha = 0.10f)
                        OpportunityType.Job        -> GoldLight
                        OpportunityType.Scholarship -> ForestGreenBg
                    },
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (opportunity.safeType) {
                    OpportunityType.Internship -> Icons.Default.Work
                    OpportunityType.Job        -> Icons.Default.Business
                    OpportunityType.Scholarship -> Icons.Default.School
                },
                contentDescription = null,
                tint = when (opportunity.safeType) {
                    OpportunityType.Internship -> NavyDeep
                    OpportunityType.Job        -> Gold
                    OpportunityType.Scholarship -> ForestGreen
                },
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = opportunity.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = opportunity.company,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextMuted
        )
    }
}

@Composable
fun Sidebar(
    navController: NavController,
    drawerState: DrawerState,
    mainViewModel: MainViewModel
) {
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntry?.destination?.route

    ModalDrawerSheet(
        drawerContainerColor = SidebarBg,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(Gold, Color(0xFFB8942E))),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S2C", color = NavyDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Skill2Career",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = mainViewModel.currentUser.value?.name ?: "Student",
                        style = MaterialTheme.typography.bodySmall,
                        color = SidebarTextInactive
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = DividerWarm)
        Spacer(modifier = Modifier.height(12.dp))

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
            DrawerItem(title, icon, currentRoute == route) {
                scope.launch {
                    drawerState.close()
                    try {
                        navController.navigate(route) {
                            if (route == "studentScreen") popUpTo("studentScreen") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = DividerWarm)
        Spacer(modifier = Modifier.height(8.dp))

        DrawerItem("Logout", Icons.AutoMirrored.Filled.Logout, false) {
            scope.launch {
                drawerState.close()
                mainViewModel.logout()
                try {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun DrawerItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) SidebarSelected else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Gold else SidebarIconInactive,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Gold else SidebarTextInactive,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
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
        Stat(opportunitiesCount, "Opportunities", Icons.AutoMirrored.Filled.TrendingUp, ForestGreen, "opportunities"),
        Stat(appliedCount, "Applied", Icons.Default.CheckCircle, NavyDeep, "myApplications"),
        Stat(scholarshipCount, "Scholarships", Icons.Default.School, Gold, "opportunities?filter=Scholarship"),
        Stat("3", "Notifications", Icons.Default.Notifications, Burgundy, null)
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
                StatCard(item, navController) {
                    item.route?.let {
                        try {
                            navController.navigate(it)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(stat: Stat, navController: NavController, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    onClick()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(stat.color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = stat.icon,
                        contentDescription = null,
                        tint = stat.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stat.count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = stat.title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
