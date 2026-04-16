package com.example.skill2career

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

// Removed static global list as it's now managed by MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(navController: NavController, mainViewModel: MainViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showApplyDialog by remember { mutableStateOf(false) }
    var selectedOpportunity by remember { mutableStateOf<Opportunity?>(null) }
    val savedOpportunitiesList = mainViewModel.savedOpportunities

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Sidebar(navController, drawerState, mainViewModel)
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Saved") },
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
                    text = "Saved Items",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (savedOpportunitiesList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No saved opportunities yet", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(savedOpportunitiesList, key = { it.id }) { opp ->
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
