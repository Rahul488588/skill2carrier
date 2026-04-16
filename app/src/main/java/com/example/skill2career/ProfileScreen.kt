package com.example.skill2career

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, mainViewModel: MainViewModel) {
    val user = mainViewModel.currentUser.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Not logged in")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture Header
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(Color(0xFF1A73E8), Color(0xFF4285F4))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(user.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(user.role, color = Color.Gray, style = MaterialTheme.typography.bodyLarge)

                Spacer(modifier = Modifier.height(32.dp))

                // Info Cards
                ProfileInfoCard(
                    items = listOf(
                        ProfileInfoItem("Email", user.email, Icons.Default.Email),
                        ProfileInfoItem("Phone", user.phoneNumber, Icons.Default.Phone),
                        ProfileInfoItem("Branch", user.branch, Icons.Default.School),
                        ProfileInfoItem("Role", user.role, Icons.Default.Badge)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileStatCard("Applications", mainViewModel.myApplications.size.toString(), Icons.Default.Assignment, Modifier.weight(1f))
                    ProfileStatCard("Saved", mainViewModel.savedOpportunities.size.toString(), Icons.Default.Bookmark, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        mainViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProfileInfoCard(items: List<ProfileInfoItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1A73E8).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(item.label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(item.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
                if (index < items.size - 1) {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun ProfileStatCard(title: String, count: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1A73E8))
            Spacer(modifier = Modifier.height(8.dp))
            Text(count, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        }
    }
}

data class ProfileInfoItem(val label: String, val value: String, val icon: ImageVector)
