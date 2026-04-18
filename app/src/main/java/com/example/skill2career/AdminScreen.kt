package com.example.skill2career

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import kotlinx.coroutines.launch

// ─── Classical Professional Palette (shared with StudentScreen) ────────────────
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController, mainViewModel: MainViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    val isSuperAdmin = mainViewModel.isSuperAdmin()
    val tabs = if (isSuperAdmin) {
        listOf("Overview", "Applications", "Manage", "AI Discover", "Super Admin")
    } else {
        listOf("Overview", "Applications", "Manage", "AI Discover")
    }

    var showAddOpportunity by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        if (isSuperAdmin && selectedTab == 4) {
            mainViewModel.fetchSuperAdminNotifications()
            mainViewModel.fetchPendingAdminRequests()
        }
        if (selectedTab == 3) {
            mainViewModel.fetchPendingAIOpportunities()
        }
        if (selectedTab == 2) {
            mainViewModel.refreshOpportunities()
        }
    }

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
            AdminSidebar(
                navController = navController,
                drawerState = drawerState,
                onTabSelect = { selectedTab = it },
                mainViewModel = mainViewModel
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Admin Console",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                "Skill2Career Management",
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
            floatingActionButton = {
                if (selectedTab == 2) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddOpportunity = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Post Opportunity") },
                        containerColor = Gold,
                        contentColor = NavyDeep
                    )
                }
            },
            containerColor = Ivory
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = NavyDeep,
                    contentColor = Gold,
                    divider = { HorizontalDivider(color = NavyMid) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedTab == index) Gold else SidebarIconInactive
                                )
                            },
                            selectedContentColor = Gold,
                            unselectedContentColor = SidebarIconInactive
                        )
                    }
                }

                when (selectedTab) {
                    0 -> AdminOverview(onPostClick = { showAddOpportunity = true }, mainViewModel = mainViewModel)
                    1 -> AdminApplicationsList(mainViewModel = mainViewModel)
                    2 -> AdminManageContent(mainViewModel = mainViewModel)
                    3 -> AIDiscoverPanel(mainViewModel = mainViewModel)
                    4 -> if (isSuperAdmin) SuperAdminPanel(mainViewModel = mainViewModel) else AdminOverview(onPostClick = { showAddOpportunity = true }, mainViewModel = mainViewModel)
                }
            }
        }

        if (showAddOpportunity) {
            AddOpportunityDialog(onDismiss = { showAddOpportunity = false }, mainViewModel = mainViewModel)
        }
    }
}

@Composable
fun AdminSidebar(
    navController: NavController,
    drawerState: DrawerState,
    onTabSelect: (Int) -> Unit,
    mainViewModel: MainViewModel
) {
    val scope = rememberCoroutineScope()
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
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "S2C Admin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Management Console",
                        style = MaterialTheme.typography.bodySmall,
                        color = SidebarTextInactive
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = DividerWarm)
        Spacer(modifier = Modifier.height(12.dp))

        val adminItems = listOf(
            Triple("Dashboard", Icons.Default.Dashboard, 0),
            Triple("Review Apps", Icons.Default.AssignmentInd, 1),
            Triple("Manage Content", Icons.Default.ManageAccounts, 2),
            Triple("AI Discover", Icons.Default.AutoAwesome, 3)
        )

        adminItems.forEach { (title, icon, tabIndex) ->
            AdminDrawerItem(title, icon) {
                scope.launch {
                    onTabSelect(tabIndex)
                    drawerState.close()
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = DividerWarm)
        Spacer(modifier = Modifier.height(8.dp))

        AdminDrawerItem("Logout", Icons.AutoMirrored.Filled.Logout) {
            scope.launch {
                mainViewModel.logout()
                drawerState.close()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun AdminDrawerItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
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
                tint = SidebarIconInactive,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SidebarTextInactive,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AdminOverview(onPostClick: () -> Unit, mainViewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                "Platform Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NavyDeep
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard("Total Students", mainViewModel.allStudents.size.toString(), Icons.Default.People, NavyDeep, Modifier.weight(1f))
                AdminStatCard("Total Apps", mainViewModel.allApplications.size.toString(), Icons.Default.Description, ForestGreen, Modifier.weight(1f))
                AdminStatCard("Opportunities", mainViewModel.opportunities.size.toString(), Icons.Default.Work, Gold, Modifier.weight(1f))
            }
        }

        item {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NavyDeep
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminActionRow("Post New Opportunity", Icons.Default.AddBusiness, NavyDeep, onPostClick)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Students", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NavyDeep)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    mainViewModel.allStudents.take(5).forEach { student ->
                        StudentListItem(student, mainViewModel)
                        if (student != mainViewModel.allStudents.lastOrNull()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DividerLight)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentListItem(student: User, mainViewModel: MainViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(NavyDeep.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                student.name.firstOrNull()?.toString()?.uppercase() ?: "S",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NavyDeep
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                student.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                student.email,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            student.role,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
fun AdminApplicationsList(mainViewModel: MainViewModel) {
    if (mainViewModel.allApplications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No applications to review", color = TextMuted)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mainViewModel.allApplications) { app ->
                AdminAppReviewCard(app, mainViewModel)
            }
        }
    }
}

@Composable
fun AdminAppReviewCard(app: Application, mainViewModel: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(NavyDeep.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.applicantName.take(1).uppercase(), color = NavyDeep, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.applicantName, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(app.opportunity.title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Surface(
                    color = when (app.status) {
                        "Accepted" -> ForestGreenBg
                        "Pending"  -> GoldLight
                        else       -> BurgundyBg
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        app.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (app.status) {
                            "Accepted" -> ForestGreen
                            "Pending"  -> Gold
                            else       -> Burgundy
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Reason: ${app.whyApply}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)

            if (app.resumeFileName != null || app.aadharCardFileName != null || app.marksheetFileName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Attachments:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    app.resumeFileName?.let { AttachmentChip("Resume", it, mainViewModel) }
                    app.aadharCardFileName?.let { AttachmentChip("Aadhar", it, mainViewModel) }
                    app.marksheetFileName?.let { AttachmentChip("Marksheet", it, mainViewModel) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { mainViewModel.updateApplicationStatus(app.id, "Accepted") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    enabled = app.status != "Accepted"
                ) { Text("Approve", color = Color.White) }

                OutlinedButton(
                    onClick = { mainViewModel.updateApplicationStatus(app.id, "Rejected") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Burgundy),
                    enabled = app.status != "Rejected"
                ) { Text("Reject") }
            }
        }
    }
}

@Composable
fun AttachmentChip(label: String, fileName: String, mainViewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        onClick = { mainViewModel.downloadAndViewFile(context, fileName) },
        color = NavyDeep.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyDeep.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyDeep)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = NavyDeep)
        }
    }
}

@Composable
fun AdminManageContent(mainViewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Active Opportunities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NavyDeep
            )
        }
        items(mainViewModel.opportunities) { opp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                ListItem(
                    headlineContent = { Text(opp.title, fontWeight = FontWeight.Medium, color = TextPrimary) },
                    supportingContent = {
                        Column {
                            Text(opp.company, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                opp.safeType.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (opp.safeType) {
                                        OpportunityType.Internship  -> NavyDeep.copy(alpha = 0.10f)
                                        OpportunityType.Job         -> GoldLight
                                        OpportunityType.Scholarship -> ForestGreenBg
                                    },
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (opp.safeType == OpportunityType.Scholarship) Icons.Default.School else Icons.Default.Work,
                                contentDescription = null,
                                tint = when (opp.safeType) {
                                    OpportunityType.Internship  -> NavyDeep
                                    OpportunityType.Job         -> Gold
                                    OpportunityType.Scholarship -> ForestGreen
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { mainViewModel.deleteOpportunity(opp.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Burgundy)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AddOpportunityDialog(onDismiss: () -> Unit, mainViewModel: MainViewModel) {
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(OpportunityType.Internship) }
    var location by remember { mutableStateOf("") }
    var minCgpa by remember { mutableStateOf("") }
    var stipendOrSalary by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            LazyColumn(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text(
                        "Post Opportunity",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NavyDeep
                    )
                }

                item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = stipendOrSalary, onValueChange = { stipendOrSalary = it }, label = { Text("Stipend/Salary") }, modifier = Modifier.fillMaxWidth()) }

                item {
                    OutlinedTextField(
                        value = minCgpa,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) minCgpa = it },
                        label = { Text("Minimum CGPA (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OpportunityType.entries.forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.name) }
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val newOpp = Opportunity(
                                id = System.currentTimeMillis().toString(),
                                title = title,
                                company = company,
                                type = type,
                                tags = listOf(),
                                location = location,
                                stipendOrSalary = stipendOrSalary.ifBlank { null },
                                date = "Just now",
                                minCgpa = minCgpa.toDoubleOrNull()
                            )
                            mainViewModel.postOpportunity(newOpp)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = title.isNotBlank() && company.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
                    ) { Text("Post Now", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun AdminStudentRow(user: User, mainViewModel: MainViewModel) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Box {
        ListItem(
            headlineContent = { Text(user.name, fontWeight = FontWeight.Medium, color = TextPrimary) },
            supportingContent = { Text(user.email, color = TextSecondary) },
            leadingContent = {
                Box(
                    modifier = Modifier.size(40.dp).background(NavyDeep.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NavyDeep)
                }
            },
            trailingContent = {
                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Burgundy,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Delete, contentDescription = "Delete student", tint = Burgundy.copy(alpha = 0.7f))
                    }
                }
            },
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(CardSurface)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Delete Student", color = NavyDeep, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to delete this student?", color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: ${user.name}", fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("Email: ${user.email}", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This action cannot be undone.", color = Burgundy, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        mainViewModel.deleteStudent(user.email) { success, error ->
                            isDeleting = false
                            showDeleteDialog = false
                            showMessage(if (success) "Student deleted successfully" else error ?: "Failed to delete student")
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text("Cancel", color = NavyDeep)
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(title: String, count: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun AdminActionRow(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() },
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerLight)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminPanel(mainViewModel: MainViewModel) {
    val notifications = mainViewModel.superAdminNotifications
    val pendingRequests = mainViewModel.pendingAdminRequests
    val unreadCount = mainViewModel.unreadNotificationCount.intValue
    var selectedRequest by remember { mutableStateOf<com.example.skill2career.network.AdminRequest?>(null) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var tempPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(message: String, isError: Boolean = false) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (isError) "Dismiss" else null,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        containerColor = Ivory
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Gold,
                    trackColor = NavyDeep.copy(alpha = 0.12f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NavyDeep),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Super Admin Console",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Manage admin access and notifications",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuperAdminStatCard(
                    title = "Pending Requests",
                    count = pendingRequests.size.toString(),
                    icon = Icons.Default.PersonAdd,
                    color = Gold,
                    modifier = Modifier.weight(1f)
                )
                SuperAdminStatCard(
                    title = "Unread Notifications",
                    count = unreadCount.toString(),
                    icon = Icons.Default.Notifications,
                    color = Burgundy,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Pending Admin Access Requests",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = NavyDeep,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (pendingRequests.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No pending admin requests",
                        modifier = Modifier.padding(16.dp),
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pendingRequests) { request ->
                        PendingAdminRequestCard(
                            request = request,
                            onApprove = {
                                selectedRequest = request
                                tempPassword = ""
                                passwordVisible = false
                                showApproveDialog = true
                            },
                            onReject = {
                                selectedRequest = request
                                showRejectDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Recent Notifications",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = NavyDeep,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (notifications.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No notifications",
                        modifier = Modifier.padding(16.dp),
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications.take(10)) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                if (!notification.isRead) {
                                    mainViewModel.markNotificationRead(notification.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showApproveDialog && selectedRequest != null) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showApproveDialog = false },
            title = { Text("Approve Admin Access", color = NavyDeep, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Email: ${selectedRequest!!.email}", color = TextPrimary)
                    Text("Branch: ${selectedRequest!!.branch}", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text("Set Temporary Password (min 8 chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        isError = tempPassword.isNotBlank() && tempPassword.length < 8,
                        supportingText = {
                            if (tempPassword.isNotBlank() && tempPassword.length < 8) {
                                Text("Password must be at least 8 characters", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPassword.length < 8) {
                            showMessage("Password must be at least 8 characters", true)
                            return@Button
                        }
                        isLoading = true
                        mainViewModel.approveAdminRequest(selectedRequest!!.id, tempPassword) { success, error ->
                            isLoading = false
                            showApproveDialog = false
                            showMessage(if (success) "Admin access approved successfully" else error ?: "Failed to approve request", !success)
                        }
                    },
                    enabled = !isLoading && tempPassword.length >= 8,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Approve", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }, enabled = !isLoading) {
                    Text("Cancel", color = NavyDeep)
                }
            }
        )
    }

    if (showRejectDialog && selectedRequest != null) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showRejectDialog = false },
            title = { Text("Reject Admin Request", color = Burgundy, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to reject the admin access request from ${selectedRequest!!.email}? This action cannot be undone.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        mainViewModel.rejectAdminRequest(selectedRequest!!.id) { success, error ->
                            isLoading = false
                            showRejectDialog = false
                            showMessage(if (success) "Request rejected successfully" else error ?: "Failed to reject request", !success)
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Reject", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }, enabled = !isLoading) {
                    Text("Cancel", color = NavyDeep)
                }
            }
        )
    }
}

@Composable
fun SuperAdminStatCard(title: String, count: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(count, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
            Text(title, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
fun PendingAdminRequestCard(
    request: com.example.skill2career.network.AdminRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NavyDeep.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.email, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("Branch: ${request.branch}", fontSize = 12.sp, color = TextSecondary)
                    Text("Requested: ${request.requestDate.take(10)}", fontSize = 12.sp, color = TextMuted)
                }
                Surface(
                    color = GoldLight,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "PENDING",
                        color = Gold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", color = Color.White)
                }
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Burgundy),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: com.example.skill2career.network.Notification, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) CardSurface else NavyDeep.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (notification.type) {
                "admin_request" -> Icons.Default.PersonAdd
                "approval"      -> Icons.Default.CheckCircle
                "rejection"     -> Icons.Default.Cancel
                else            -> Icons.Default.Notifications
            }
            val iconColor = when (notification.type) {
                "admin_request" -> Gold
                "approval"      -> ForestGreen
                "rejection"     -> Burgundy
                else            -> TextMuted
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextPrimary)
                Text(notification.message, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
                Text(notification.createdAt.take(16), fontSize = 10.sp, color = TextMuted)
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Gold, CircleShape)
                )
            }
        }
    }
}

@Composable
fun AIDiscoverPanel(mainViewModel: MainViewModel) {
    val opportunities = mainViewModel.aiDiscoveredOpportunities.toList()
    val isFetching = mainViewModel.isFetchingAIOpportunities.value
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(opportunities.size) {
        println("🎯 AI Discover Panel: ${opportunities.size} opportunities displayed")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavyDeep)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AI Opportunity Discovery",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                Text(
                    text = "Use AI to discover current internships and scholarships from around the world. Review and approve opportunities before they become visible to students.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        Button(
            onClick = {
                errorMessage = null
                successMessage = null
                mainViewModel.fetchAIOpportunities { success, error ->
                    if (success) {
                        successMessage = "Successfully fetched opportunities! Review them below."
                    } else {
                        errorMessage = error ?: "Failed to fetch opportunities"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isFetching,
            colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
        ) {
            if (isFetching) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Gold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetching Opportunities...", color = Color.White)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Gold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetch Latest Opportunities", color = Color.White)
            }
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BurgundyBg)
            ) {
                Text(
                    text = "❌ $errorMessage",
                    modifier = Modifier.padding(12.dp),
                    color = Burgundy,
                    fontSize = 14.sp
                )
            }
        }

        if (successMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ForestGreenBg)
            ) {
                Text(
                    text = "✅ $successMessage",
                    modifier = Modifier.padding(12.dp),
                    color = ForestGreen,
                    fontSize = 14.sp
                )
            }
        }

        Text(
            text = "Pending Review (${opportunities.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDeep
        )

        if (opportunities.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No opportunities pending review",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Click 'Fetch Latest Opportunities' to discover new ones",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(opportunities) { opp ->
                    AIOpportunityCard(
                        opportunity = opp,
                        onApprove = { id ->
                            mainViewModel.approveAIOpportunity(id) { success, error ->
                                if (success) successMessage = "Opportunity approved and published!"
                                else errorMessage = error ?: "Failed to approve"
                            }
                        },
                        onReject = { id ->
                            mainViewModel.rejectAIOpportunity(id) { success, error ->
                                if (success) successMessage = "Opportunity rejected"
                                else errorMessage = error ?: "Failed to reject"
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AIOpportunityCard(
    opportunity: Map<String, String>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = opportunity["title"] ?: "Untitled",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = opportunity["organization"] ?: "Unknown Organization",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                val type = opportunity["type"] ?: "internship"
                val badgeColor = if (type.lowercase() == "scholarship") ForestGreen else NavyDeep
                val badgeBg = if (type.lowercase() == "scholarship") ForestGreenBg else NavyDeep.copy(alpha = 0.10f)
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = type.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = badgeColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                opportunity["location"]?.let {
                    if (it.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextMuted)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(it, fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
                opportunity["stipend"]?.let {
                    if (it.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextMuted)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(it, fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
                opportunity["description"]?.let {
                    if (it.isNotBlank()) {
                        Text(it, fontSize = 13.sp, color = TextSecondary, maxLines = 3)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onApprove(opportunity["id"] ?: "") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", color = Color.White)
                }
                OutlinedButton(
                    onClick = { onReject(opportunity["id"] ?: "") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Burgundy)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
}
