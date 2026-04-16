package com.example.skill2career

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

/**
 * -----------------------------------------------------------------------------------------
 *  BACKEND NOTE: DATA MODELS
 *  These classes represent the data structures for your API.
 *  You should create corresponding models in your backend (e.g., in a NoSQL DB like Firestore
 *  or a SQL DB). The 'id' fields will be crucial for database lookups.
 * -----------------------------------------------------------------------------------------
 */

data class Application(
    val id: String = "",
    val opportunity: Opportunity = Opportunity(),
    val applicantName: String = "",
    val applicantEmail: String = "",
    val whyApply: String = "",
    val resumeFileName: String? = null,
    val familyIncome: String? = null,
    val aadharCardFileName: String? = null,
    val marksheetFileName: String? = null,
    var status: String = "Pending" // "Pending", "Accepted", "Rejected"
)

data class User(
    val name: String = "",
    val email: String = "",
    val role: String = "Student", // "Student" or "Admin"
    val phoneNumber: String = "",
    val branch: String = ""
)

/**
 * -----------------------------------------------------------------------------------------
 *  BACKEND NOTE: GLOBAL STATE / REPOSITORY
 *  Currently, these lists are in-memory (volatile). 
 *  TODO: Replace these with calls to a Repository/Service that fetches data from an API 
 *  (e.g., using Retrofit, Ktor, or Firebase SDK).
 * -----------------------------------------------------------------------------------------
 */
// Removed static global state as it's now managed by MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(navController: NavController, mainViewModel: MainViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val appliedApplicationsList = mainViewModel.myApplications

    /**
     * -----------------------------------------------------------------------------------------
     *  BACKEND NOTE: DATA FETCHING
     *  Use a LaunchedEffect here to fetch applications for the specific user from the backend.
     *  Example: viewModel.getApplicationsForUser(currentUser.value.email)
     * -----------------------------------------------------------------------------------------
     */

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Sidebar(navController, drawerState, mainViewModel)
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Applications") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Application Status",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (appliedApplicationsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("You haven't applied for anything yet", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(appliedApplicationsList) { app ->
                            ApplicationStatusCard(app)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationStatusCard(application: Application) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = application.opportunity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = when(application.status) {
                        "Pending" -> Color(0xFFFEF7E0)
                        "Accepted" -> Color(0xFFE6F4EA)
                        else -> Color(0xFFFCE8E6)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = application.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(application.status) {
                            "Pending" -> Color(0xFFB06000)
                            "Accepted" -> Color(0xFF137333)
                            else -> Color(0xFFC5221F)
                        }
                    )
                }
            }
            Text(
                text = application.opportunity.company,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
            
            Text(
                text = "Applied as: ${application.applicantName}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Email: ${application.applicantEmail}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            if (application.resumeFileName != null) {
                Text(
                    text = "Resume: ${application.resumeFileName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            if (application.opportunity.type == OpportunityType.Scholarship) {
                 Text(
                    text = "Income: ${application.familyIncome}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (application.aadharCardFileName != null) {
                    Text(
                        text = "Aadhar Card: ${application.aadharCardFileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                if (application.marksheetFileName != null) {
                    Text(
                        text = "Marksheet: ${application.marksheetFileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
