package com.project.dba_delatorre_dometita_ramirez_tan

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Login(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userDao = Database_Users.getDatabase(context).dao_users()

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0E1CC), Color(0xFFD18F79))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.beans),
                contentDescription = "Beans",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 280.dp)
        ) {
            Card(
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        Text("Welcome Back", fontSize = 24.sp, color = Color(0xFF6B3E2E))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Login to continue", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            trailingIcon = {
                                val icon = if (passwordVisible)
                                    painterResource(id = R.drawable.ic_visibility_off)
                                else
                                    painterResource(id = R.drawable.ic_visibility)

                                val description = if (passwordVisible) "Hide password" else "Show password"

                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(painter = icon, contentDescription = description)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Button at bottom
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    when {
                                        username.isBlank() || password.isBlank() -> {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Please enter both username and password.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                        else -> {
                                            val user = userDao.DaoGetUserByCredentials(username, password)
                                            withContext(Dispatchers.Main) {
                                                if (user != null) {
                                                    navController.navigate(Routes.R_DashboardScreen.routes)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Invalid username or password.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6B3E2E),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Login", fontSize = 18.sp)
                        }

                        TextButton(
                            onClick = {
                                navController.navigate(Routes.Screen1.routes)
                            },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "Don't have an account? Register",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


