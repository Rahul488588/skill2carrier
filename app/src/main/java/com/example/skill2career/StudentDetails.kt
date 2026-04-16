package com.example.skill2career

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun StudentDetails(navController: NavController) {

    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text("Student Details", fontSize = 24.sp)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )

        OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text("Mobile Number") }
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") }
        )

        OutlinedTextField(
            value = cgpa,
            onValueChange = { cgpa = it },
            label = { Text("CGPA") }
        )

        OutlinedTextField(
            value = skills,
            onValueChange = { skills = it },
            label = { Text("Skills") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                navController.navigate("studentScreen")
            }
        ) {
            Text("Submit")
        }
    }
}