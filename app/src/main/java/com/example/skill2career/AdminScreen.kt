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
        listOf("Overview", "Applications", "Manage", "Super Admin")
    } else {
        listOf("Overview", "Applications", "Manage")
    }

    var showAddOpportunity by remember { mutableStateOf(false) }
    
    // Load super admin data when tab is selected
    LaunchedEffect(selectedTab) {
        if (isSuperAdmin && selectedTab == 3) {
            mainViewModel.fetchSuperAdminNotifications()
            mainViewModel.fetchPendingAdminRequests()
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
                            Text("Admin Console", fontWeight = FontWeight.ExtraBold)
                            Text("Skill2Career Management", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (selectedTab == 2) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddOpportunity = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Post Opportunity") },
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White
                    )
                }
            },
            containerColor = Color(0xFFF8F9FA)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF1A73E8)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> AdminOverview(onPostClick = { showAddOpportunity = true }, mainViewModel = mainViewModel)
                    1 -> AdminApplicationsList(mainViewModel = mainViewModel)
                    2 -> AdminManageContent(mainViewModel = mainViewModel)
                    3 -> if (isSuperAdmin) SuperAdminPanel(mainViewModel = mainViewModel) else AdminOverview(onPostClick = { showAddOpportunity = true }, mainViewModel = mainViewModel)
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
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "S2C Admin",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A73E8)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        val adminItems = listOf(
            Triple("Dashboard", Icons.Default.Dashboard, 0),
            Triple("Review Apps", Icons.Default.AssignmentInd, 1),
            Triple("Manage Content", Icons.Default.ManageAccounts, 2),
        )

        adminItems.forEach { (title, icon, tabIndex) ->
            DrawerItem(title, icon) {
                scope.launch {
                    onTabSelect(tabIndex)
                    drawerState.close()
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        DrawerItem("Logout", Icons.AutoMirrored.Filled.ExitToApp) {
            scope.launch {
                mainViewModel.logout()
                drawerState.close()
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
fun AdminOverview(onPostClick: () -> Unit, mainViewModel: MainViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredStudents = mainViewModel.allStudents.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Platform Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AdminStatCard("Total Students", mainViewModel.allStudents.size.toString(), Icons.Default.People, Color(0xFF1A73E8), Modifier.weight(1f))
                AdminStatCard("Total Apps", mainViewModel.allApplications.size.toString(), Icons.Default.Description, Color(0xFF34A853), Modifier.weight(1f))
            }
        }

        item {
            Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminActionRow("Post New Opportunity", Icons.Default.AddBusiness, Color(0xFF1A73E8), onPostClick)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Registered Students", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${filteredStudents.size} found", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name or email...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                singleLine = true
            )
        }
        
        items(filteredStudents) { student ->
            AdminStudentRow(student, mainViewModel)
        }
    }
}

@Composable
fun AdminApplicationsList(mainViewModel: MainViewModel) {
    if (mainViewModel.allApplications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No applications to review", color = Color.Gray)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFF1A73E8).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.applicantName.take(1).uppercase(), color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.applicantName, fontWeight = FontWeight.Bold)
                    Text(app.opportunity.title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Surface(
                    color = when(app.status) {
                        "Accepted" -> Color(0xFFE6F4EA)
                        "Pending" -> Color(0xFFFEF7E0)
                        else -> Color(0xFFFCE8E6)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        app.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(app.status) {
                            "Accepted" -> Color(0xFF137333)
                            "Pending" -> Color(0xFFB06000)
                            else -> Color(0xFFC5221F)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text("Reason: ${app.whyApply}", style = MaterialTheme.typography.bodySmall, maxLines = 2)

            if (app.resumeFileName != null || app.aadharCardFileName != null || app.marksheetFileName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Attachments:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)),
                    enabled = app.status != "Accepted"
                ) { Text("Approve") }
                
                OutlinedButton(
                    onClick = { mainViewModel.updateApplicationStatus(app.id, "Rejected") }, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Active Opportunities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        items(mainViewModel.opportunities) { opp ->
            ListItem(
                headlineContent = { Text(opp.title, fontWeight = FontWeight.Bold) },
                supportingContent = { Text(opp.company) },
                leadingContent = { 
                    Icon(
                        if (opp.type == OpportunityType.Scholarship) Icons.Default.School else Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFF1A73E8)
                    )
                },
                trailingContent = {
                    IconButton(onClick = { mainViewModel.deleteOpportunity(opp.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f))
                    }
                },
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White)
            )
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
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(count, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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
            Text(title, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
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
