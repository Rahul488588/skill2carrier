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

/**
 * -----------------------------------------------------------------------------------------
 *  BACKEND NOTE: ADMIN DASHBOARD STATE
 *  The following lists represent the data that should be fetched/stored in your database.
 *  TODO: Move these to a ViewModel and use a Repository (Retrofit/Firebase/etc) to sync.
 * -----------------------------------------------------------------------------------------
 */
// Removed static global lists as they are now managed by MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController, mainViewModel: MainViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Check if super admin
    val isSuperAdmin = mainViewModel.isSuperAdmin()
    val tabs = if (isSuperAdmin) {
        listOf("Overview", "Applications", "Manage", "AI Discover", "Super Admin")
    } else {
        listOf("Overview", "Applications", "Manage", "AI Discover")
    }

    var showAddOpportunity by remember { mutableStateOf(false) }
    
    // Load super admin data when tab is selected
    LaunchedEffect(selectedTab) {
        if (isSuperAdmin && selectedTab == 4) {
            mainViewModel.fetchSuperAdminNotifications()
            mainViewModel.fetchPendingAdminRequests()
        }
        // Load AI discovered opportunities when AI Discover tab is selected
        if (selectedTab == 3) {
            mainViewModel.fetchPendingAIOpportunities()
        }
        // Refresh main opportunities list when Manage tab is selected
        if (selectedTab == 2) {
            mainViewModel.refreshOpportunities()
        }
    }

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
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                "Skill2Career Management",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF64748B))
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF64748B))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF1E293B)
                    )
                )
            },
            floatingActionButton = {
                if (selectedTab == 2) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddOpportunity = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Post Opportunity") },
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                }
            },
            containerColor = Color(0xFFF8FAFC)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF2563EB),
                    divider = { HorizontalDivider(color = Color(0xFFE2E8F0)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedTab == index) Color(0xFF2563EB) else Color(0xFF64748B)
                                )
                            },
                            selectedContentColor = Color(0xFF2563EB),
                            unselectedContentColor = Color(0xFF64748B)
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
        drawerContainerColor = Color.White,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1E40AF))),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "S2C Admin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Management Console",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFFE2E8F0))
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
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF334155),
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
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard("Total Students", mainViewModel.allStudents.size.toString(), Icons.Default.People, Color(0xFF2563EB), Modifier.weight(1f))
                AdminStatCard("Total Apps", mainViewModel.allApplications.size.toString(), Icons.Default.Description, Color(0xFF10B981), Modifier.weight(1f))
                AdminStatCard("Opportunities", mainViewModel.opportunities.size.toString(), Icons.Default.Work, Color(0xFFF59E0B), Modifier.weight(1f))
            }
        }

        item {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminActionRow("Post New Opportunity", Icons.Default.AddBusiness, Color(0xFF2563EB), onPostClick)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Students", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    mainViewModel.allStudents.take(5).forEach { student ->
                        StudentListItem(student, mainViewModel)
                        if (student != mainViewModel.allStudents.lastOrNull()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))
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
                .background(Color(0xFFEFF6FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                student.name.firstOrNull()?.toString()?.uppercase() ?: "S",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2563EB)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                student.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A)
            )
            Text(
                student.email,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
        }
        Text(
            student.role,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
fun AdminApplicationsList(mainViewModel: MainViewModel) {
    if (mainViewModel.allApplications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No applications to review", color = Color(0xFF94A3B8))
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFF2563EB).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.applicantName.take(1).uppercase(), color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.applicantName, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    Text(app.opportunity.title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
                Surface(
                    color = when(app.status) {
                        "Accepted" -> Color(0xFFD1FAE5)
                        "Pending" -> Color(0xFFFEF3C7)
                        else -> Color(0xFFFEE2E2)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        app.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(app.status) {
                            "Accepted" -> Color(0xFF10B981)
                            "Pending" -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Reason: ${app.whyApply}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B), maxLines = 2)

            if (app.resumeFileName != null || app.aadharCardFileName != null || app.marksheetFileName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Attachments:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    enabled = app.status != "Accepted"
                ) { Text("Approve") }

                OutlinedButton(
                    onClick = { mainViewModel.updateApplicationStatus(app.id, "Rejected") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
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
        color = Color(0xFF1A73E8).copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A73E8).copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF1A73E8))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1A73E8))
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
                color = Color(0xFF111827)
            )
        }
        items(mainViewModel.opportunities) { opp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                ListItem(
                    headlineContent = { Text(opp.title, fontWeight = FontWeight.Medium, color = Color(0xFF111827)) },
                    supportingContent = { 
                        Column {
                            Text(opp.company, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                            Text(
                                opp.safeType.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    },
                    leadingContent = { 
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (opp.safeType) {
                                        OpportunityType.Internship -> Color(0xFFDBEAFE)
                                        OpportunityType.Job -> Color(0xFFFEF3C7)
                                        OpportunityType.Scholarship -> Color(0xFFD1FAE5)
                                    },
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (opp.safeType == OpportunityType.Scholarship) Icons.Default.School else Icons.Default.Work,
                                contentDescription = null,
                                tint = when (opp.safeType) {
                                    OpportunityType.Internship -> Color(0xFF3B82F6)
                                    OpportunityType.Job -> Color(0xFFF59E0B)
                                    OpportunityType.Scholarship -> Color(0xFF10B981)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { mainViewModel.deleteOpportunity(opp.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
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
            shape = RoundedCornerShape(24.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Text("Post Opportunity", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                
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
                        enabled = title.isNotBlank() && company.isNotBlank()
                    ) { Text("Post Now") }
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
    
    // Show snackbar messages
    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
    
    Box {
        ListItem(
            headlineContent = { Text(user.name, fontWeight = FontWeight.Medium) },
            supportingContent = { Text(user.email) },
            leadingContent = { 
                Box(
                    modifier = Modifier.size(40.dp).background(Color.LightGray.copy(alpha = 0.3f), CircleShape), 
                    contentAlignment = Alignment.Center
                ) { 
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) 
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
                            color = Color.Red.copy(alpha = 0.5f),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Delete, contentDescription = "Delete student", tint = Color.Red.copy(alpha = 0.5f))
                    }
                }
            },
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White)
        )
        
        // Snackbar for this row
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Delete Student") },
            text = {
                Column {
                    Text("Are you sure you want to delete this student?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Name: ${user.name}",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Email: ${user.email}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone.",
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        mainViewModel.deleteStudent(user.email) { success, error ->
                            isDeleting = false
                            showDeleteDialog = false
                            if (success) {
                                showMessage("Student deleted successfully")
                            } else {
                                showMessage(error ?: "Failed to delete student")
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(title: String, count: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        .background(color.copy(alpha = 0.1f), CircleShape),
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
                color = Color(0xFF0F172A)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun AdminActionRow(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() },
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium, color = Color(0xFF334155), modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF94A3B8))
        }
    }
}

// 🔐 SUPER ADMIN PANEL - Professional Implementation with Error Handling
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

    // Snackbar for error/success messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show error/success via snackbar helper
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
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Loading overlay
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Header
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color.White,
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
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuperAdminStatCard(
                    title = "Pending Requests",
                    count = pendingRequests.size.toString(),
                    icon = Icons.Default.PersonAdd,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                SuperAdminStatCard(
                    title = "Unread Notifications",
                    count = unreadCount.toString(),
                    icon = Icons.Default.Notifications,
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pending Admin Requests Section
            Text(
                "Pending Admin Access Requests",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (pendingRequests.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No pending admin requests",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
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

            // Notifications Section
            Text(
                "Recent Notifications",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (notifications.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No notifications",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
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

    // Approve Dialog with Password Visibility Toggle and Validation
    if (showApproveDialog && selectedRequest != null) {
        AlertDialog(
            onDismissRequest = { 
                if (!isLoading) {
                    showApproveDialog = false 
                }
            },
            title = { Text("Approve Admin Access") },
            text = {
                Column {
                    Text("Email: ${selectedRequest!!.email}")
                    Text("Branch: ${selectedRequest!!.branch}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { 
                            tempPassword = it
                        },
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
                            if (success) {
                                showMessage("Admin access approved successfully")
                            } else {
                                showMessage(error ?: "Failed to approve request", true)
                            }
                        }
                    },
                    enabled = !isLoading && tempPassword.length >= 8,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showApproveDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reject Confirmation Dialog
    if (showRejectDialog && selectedRequest != null) {
        AlertDialog(
            onDismissRequest = { 
                if (!isLoading) {
                    showRejectDialog = false 
                }
            },
            title = { Text("Reject Admin Request") },
            text = {
                Text("Are you sure you want to reject the admin access request from ${selectedRequest!!.email}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        mainViewModel.rejectAdminRequest(selectedRequest!!.id) { success, error ->
                            isLoading = false
                            showRejectDialog = false
                            if (success) {
                                showMessage("Request rejected successfully")
                            } else {
                                showMessage(error ?: "Failed to reject request", true)
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRejectDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SuperAdminStatCard(title: String, count: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(count, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(title, fontSize = 12.sp, color = Color.Gray)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF1A237E),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.email, fontWeight = FontWeight.Medium)
                    Text("Branch: ${request.branch}", fontSize = 12.sp, color = Color.Gray)
                    Text("Requested: ${request.requestDate.take(10)}", fontSize = 12.sp, color = Color.Gray)
                }
                // Status Badge
                Surface(
                    color = Color(0xFFFF9800).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "PENDING",
                        color = Color(0xFFFF9800),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                }
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
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
            containerColor = if (notification.isRead) Color.White else Color(0xFFE3F2FD)
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
            // Notification Icon based on type
            val icon = when (notification.type) {
                "admin_request" -> Icons.Default.PersonAdd
                "approval" -> Icons.Default.CheckCircle
                "rejection" -> Icons.Default.Cancel
                else -> Icons.Default.Notifications
            }
            val iconColor = when (notification.type) {
                "admin_request" -> Color(0xFFFF9800)
                "approval" -> Color(0xFF4CAF50)
                "rejection" -> Color(0xFFF44336)
                else -> Color.Gray
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(notification.message, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                Text(notification.createdAt.take(16), fontSize = 10.sp, color = Color.LightGray)
            }
            
            // Unread indicator
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF1A73E8), CircleShape)
                )
            }
        }
    }
}

// 🤖 AI Discover Panel - For fetching and managing AI-discovered opportunities
@Composable
fun AIDiscoverPanel(mainViewModel: MainViewModel) {
    // Convert to List to ensure recomposition triggers properly
    val opportunities = mainViewModel.aiDiscoveredOpportunities.toList()
    val isFetching = mainViewModel.isFetchingAIOpportunities.value
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // Debug logging
    LaunchedEffect(opportunities.size) {
        println("🎯 AI Discover Panel: ${opportunities.size} opportunities displayed")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🤖 AI Opportunity Discovery",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A73E8)
                )
                Text(
                    text = "Use AI to discover current internships and scholarships from around the world. Review and approve opportunities before they become visible to students.",
                    fontSize = 14.sp,
                    color = Color(0xFF5F6368)
                )
            }
        }
        
        // Fetch Button
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
        ) {
            if (isFetching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetching Opportunities...")
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetch Latest Opportunities")
            }
        }
        
        // Status Messages
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    text = "❌ $errorMessage",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFC62828),
                    fontSize = 14.sp
                )
            }
        }
        
        if (successMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Text(
                    text = "✅ $successMessage",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF2E7D32),
                    fontSize = 14.sp
                )
            }
        }
        
        // Opportunities List
        Text(
            text = "Pending Review (${opportunities.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF202124)
        )
        
        if (opportunities.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
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
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No opportunities pending review",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Click 'Fetch Latest Opportunities' to discover new ones",
                        fontSize = 14.sp,
                        color = Color.LightGray
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
                                if (success) {
                                    successMessage = "Opportunity approved and published!"
                                } else {
                                    errorMessage = error ?: "Failed to approve"
                                }
                            }
                        },
                        onReject = { id ->
                            mainViewModel.rejectAIOpportunity(id) { success, error ->
                                if (success) {
                                    successMessage = "Opportunity rejected"
                                } else {
                                    errorMessage = error ?: "Failed to reject"
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// AI Opportunity Card
@Composable
fun AIOpportunityCard(
    opportunity: Map<String, String>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title and Type
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
                        color = Color(0xFF202124)
                    )
                    Text(
                        text = opportunity["organization"] ?: "Unknown Organization",
                        fontSize = 14.sp,
                        color = Color(0xFF5F6368)
                    )
                }
                
                // Type Badge
                val type = opportunity["type"] ?: "internship"
                val badgeColor = if (type.lowercase() == "scholarship") Color(0xFF9C27B0) else Color(0xFF1A73E8)
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
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
            
            // Details
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                opportunity["location"]?.let {
                    if (it.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(it, fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
                
                opportunity["deadline"]?.let {
                    if (it.isNotBlank()) {
                        Text(
                            text = "Deadline: $it",
                            fontSize = 13.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Description
            opportunity["description"]?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it.take(200) + if (it.length > 200) "..." else "",
                        fontSize = 13.sp,
                        color = Color(0xFF5F6368),
                        maxLines = 3
                    )
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onApprove(opportunity["id"] ?: "") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                }
                
                OutlinedButton(
                    onClick = { onReject(opportunity["id"] ?: "") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
}
