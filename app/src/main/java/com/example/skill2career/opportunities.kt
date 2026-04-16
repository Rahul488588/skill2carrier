package com.example.skill2career

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OpportunityType { Internship, Job, Scholarship }

data class Opportunity(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val type: OpportunityType = OpportunityType.Internship,
    val tags: List<String> = emptyList(),
    val location: String = "",
    val stipendOrSalary: String? = null,
    val date: String = "",
    val minCgpa: Double? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunitiesScreen(navController: NavController, initialFilter: String = "All", mainViewModel: MainViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(initialFilter) }
    val filters = listOf("All", "Internship", "Scholarship", "Job")

    var showApplyDialog by remember { mutableStateOf(false) }
    var selectedOpportunity by remember { mutableStateOf<Opportunity?>(null) }

    val allOpportunities = mainViewModel.opportunities

    val filteredOpportunities = allOpportunities.filter {
        (selectedFilter == "All" || it.type.name == selectedFilter) &&
                (it.title.contains(searchQuery, ignoreCase = true) || it.company.contains(searchQuery, ignoreCase = true))
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { Sidebar(navController, drawerState, mainViewModel) }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Opportunities", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color(0xFFF8F9FA)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Search and Filter Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1.3f).height(52.dp),
                        placeholder = { Text("Search career...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF1A73E8)) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f),
                            focusedBorderColor = Color(0xFF1A73E8)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedFilter = filter },
                            color = if (isSelected) Color(0xFF1A73E8) else Color.White,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            shadowElevation = if (isSelected) 4.dp else 0.dp
                        ) {
                            Text(
                                text = filter,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (isSelected) Color.White else Color.DarkGray,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredOpportunities.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No opportunities found", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredOpportunities, key = { it.id }) { opp ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(opp) { visible = true }
                            
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 20 }
                            ) {
                                OpportunityCard(
                                    opportunity = opp,
                                    mainViewModel = mainViewModel,
                                    onApplyClick = {
                                        selectedOpportunity = opp
                                        showApplyDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showApplyDialog && selectedOpportunity != null) {
            ApplyFormDialog(
                opportunity = selectedOpportunity!!,
                mainViewModel = mainViewModel,
                onDismiss = { showApplyDialog = false },
                onApplySubmit = { application, uris ->
                    mainViewModel.applyForOpportunity(application, uris)
                    showApplyDialog = false
                }
            )
        }
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@Composable
fun ApplyFormDialog(
    opportunity: Opportunity,
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit,
    onApplySubmit: (Application, Map<String, Uri?>) -> Unit
) {
    var name by remember { mutableStateOf(mainViewModel.currentUser.value?.name ?: "") }
    var email by remember { mutableStateOf(mainViewModel.currentUser.value?.email ?: "") }
    var whyApply by remember { mutableStateOf("") }
    var resumeUri by remember { mutableStateOf<Uri?>(null) }
    var resumeName by remember { mutableStateOf("") }
    var familyIncome by remember { mutableStateOf("") }
    var aadharUri by remember { mutableStateOf<Uri?>(null) }
    var aadharName by remember { mutableStateOf("") }
    var marksheetUri by remember { mutableStateOf<Uri?>(null) }
    var marksheetName by remember { mutableStateOf("") }

    val context = LocalContext.current
    
    val resumeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            resumeUri = it
            resumeName = getFileName(context, it) ?: "Resume.pdf" 
        }
    }
    val marksheetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            marksheetUri = it
            marksheetName = getFileName(context, it) ?: "Marksheet.pdf" 
        }
    }
    val aadharLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            aadharUri = it
            aadharName = getFileName(context, it) ?: "Aadhar.pdf" 
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Apply for", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
                Text(opportunity.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                if (opportunity.type == OpportunityType.Scholarship) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = familyIncome, onValueChange = { familyIncome = it }, label = { Text("Annual Income") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), prefix = { Text("₹") })
                    Spacer(modifier = Modifier.height(16.dp))
                    FileUploadButton("Aadhar Card", aadharName) { aadharLauncher.launch("application/pdf") }
                    Spacer(modifier = Modifier.height(16.dp))
                    FileUploadButton("Marksheet", marksheetName) { marksheetLauncher.launch("application/pdf") }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    FileUploadButton("Resume", resumeName) { resumeLauncher.launch("application/pdf") }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = whyApply, onValueChange = { whyApply = it }, label = { Text("Why should we pick you?") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp), maxLines = 4)

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank()) {
                                val uris = mapOf(
                                    "resume" to resumeUri,
                                    "aadhar" to aadharUri,
                                    "marksheet" to marksheetUri
                                )
                                onApplySubmit(
                                    Application(
                                        id = System.currentTimeMillis().toString(), 
                                        opportunity = opportunity, 
                                        applicantName = name, 
                                        applicantEmail = email, 
                                        whyApply = whyApply, 
                                        resumeFileName = resumeName.ifEmpty { null }, 
                                        familyIncome = familyIncome.ifEmpty { null }, 
                                        aadharCardFileName = aadharName.ifEmpty { null }, 
                                        marksheetFileName = marksheetName.ifEmpty { null }
                                    ),
                                    uris
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        enabled = name.isNotBlank() && email.isNotBlank() && (opportunity.type != OpportunityType.Scholarship || (familyIncome.isNotBlank() && aadharName.isNotBlank() && marksheetName.isNotBlank()))
                    ) { Text("Submit") }
                }
            }
        }
    }
}

@Composable
fun FileUploadButton(label: String, fileName: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() },
        color = Color(0xFFF1F3F4),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (fileName.isEmpty()) Icons.Default.UploadFile else Icons.Default.CheckCircle, contentDescription = null, tint = if (fileName.isEmpty()) Color.Gray else Color(0xFF34A853))
            Spacer(modifier = Modifier.width(12.dp))
            Text(if (fileName.isEmpty()) "Upload $label" else fileName, color = if (fileName.isEmpty()) Color.Gray else Color.Black, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun OpportunityCard(opportunity: Opportunity, mainViewModel: MainViewModel, onApplyClick: () -> Unit) {
    val isSaved = mainViewModel.savedOpportunities.any { it.id == opportunity.id }
    val badgeColors = when (opportunity.type) {
        OpportunityType.Internship -> Pair(Color(0xFFE8F0FE), Color(0xFF1A73E8))
        OpportunityType.Scholarship -> Pair(Color(0xFFE6F4EA), Color(0xFF34A853))
        OpportunityType.Job -> Pair(Color(0xFFFEF7E0), Color(0xFFFBBC04))
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = badgeColors.first, shape = RoundedCornerShape(8.dp)) {
                    Text(opportunity.type.name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = badgeColors.second, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { 
                    mainViewModel.toggleSaveOpportunity(opportunity)
                }) {
                    Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = if (isSaved) Color(0xFF1A73E8) else Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(opportunity.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(opportunity.company, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoItem(Icons.Default.LocationOn, opportunity.location)
                opportunity.stipendOrSalary?.let { InfoItem(Icons.Default.Payments, it) }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onApplyClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))) {
                Text("Apply Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfoItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}
