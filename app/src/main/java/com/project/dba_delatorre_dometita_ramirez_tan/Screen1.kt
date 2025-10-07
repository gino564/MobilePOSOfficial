package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

fun isPasswordValid(password: String): Boolean {
    val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{6,}$")
    return password.matches(passwordPattern)
}



@Composable
fun Screen1(
    navController: NavController,
    viewModel: frm_RegViewModel,
    viewModel2: ViewModel_users,
    onUserSaved: () -> Unit = {}
){

    var dometita_lname by remember { mutableStateOf( "") }
    var dometita_fname by remember { mutableStateOf( "") }
    var dometita_mname by remember { mutableStateOf( "") }
    var dometita_username by remember { mutableStateOf( "") }
    var dometita_password by remember { mutableStateOf( "") }
    var dometita_lnameerror by remember { mutableStateOf(false) }
    var dometita_fnameerror by remember { mutableStateOf(false) }
    var dometita_mnameerror by remember { mutableStateOf(false) }
    var dometita_usernameerror by remember { mutableStateOf(false) }
    var dometita_passworderror by remember { mutableStateOf(false) }
    val userToEdit = viewModel2.userToEdit
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0E1CC), Color(0xFFD18F79))
    )
    var passwordVisible by remember { mutableStateOf(false) }
    var  showEditDialog by remember { mutableStateOf(false) }
    LaunchedEffect(userToEdit) {
        userToEdit?.let { user ->
            viewModel.txtlnameData(user.Entity_lname)
            viewModel.txtfnameData(user.Entity_fname)
            viewModel.txtmnameData(user.Entity_mname)
            viewModel.txtusernameData(user.Entity_username)
            viewModel.txtpasswordData(user.Entity_password)

        }
    }
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
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 180.dp)
        ) {
            Card(
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()

                        .verticalScroll(scrollState)
                ){


                    Text(
                        text = "Registration\nForm",
                        fontSize = 30.sp,
                        color = Color(0xFF6B3E2E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = viewModel.dometita_lname,
                        onValueChange = { viewModel.txtlnameData(it)
                            dometita_lnameerror = false},
                        label = { Text(text = "Enter your last name.") },
                        isError = dometita_lnameerror,
                        textStyle = TextStyle(
                            color = if (dometita_lname.isNotEmpty()) Color.White else Color.Black
                        ),

                        shape = RoundedCornerShape(20.dp)
                    )
                    if (dometita_lnameerror) {
                        Text(text = "Last name is required", color = Color.Red, fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = viewModel.dometita_fname,
                        onValueChange = {viewModel.txtfnameData(it)
                            dometita_fnameerror = false},
                        label = { Text(text = "Enter your first name.")},
                        isError = dometita_fnameerror,
                        textStyle = TextStyle(
                            color = if (dometita_fname.isNotEmpty()) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    if (dometita_fnameerror) {
                        Text(text = "Last name is required", color = Color.Red, fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = viewModel.dometita_mname,
                        onValueChange = {viewModel.txtmnameData(it)
                            dometita_mnameerror = false},
                        label = { Text(text = "Enter your middle name.")},
                        isError = dometita_mnameerror,
                        textStyle = TextStyle(
                            color = if (dometita_mname.isNotEmpty()) Color.White else Color.Black
                        ),

                        shape = RoundedCornerShape(20.dp)
                    )
                    if (dometita_mnameerror) {
                        Text(text = "Last name is required", color = Color.Red, fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = viewModel.dometita_username,
                        onValueChange = {viewModel.txtusernameData(it)
                            dometita_usernameerror = false},
                        label = { Text(text = "Enter your username.")},
                        isError = dometita_usernameerror,
                        textStyle = TextStyle(
                            color = if (dometita_username.isNotEmpty()) Color.White else Color.Black
                        ),

                        shape = RoundedCornerShape(20.dp)
                    )
                    if (dometita_usernameerror) {
                        Text(text = "Last name is required", color = Color.Red, fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = viewModel.dometita_password,
                        onValueChange = {
                            viewModel.txtpasswordData(it)
                            dometita_passworderror = false
                        },
                        label = { Text(text = "Enter your password.") },
                        isError = dometita_passworderror,
                        textStyle = TextStyle(
                            color = if (dometita_password.isNotEmpty()) Color.White else Color.Black
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(20.dp),
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

                    if (dometita_passworderror) {
                        Text(
                            text = "Password must be at least 6 characters long,\ninclude uppercase, lowercase, number, and special character.",
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }


                    Button(
                        onClick = {
                            dometita_lnameerror = viewModel.dometita_lname.isBlank()
                            dometita_fnameerror = viewModel.dometita_fname.isBlank()
                            dometita_mnameerror = viewModel.dometita_mname.isBlank()
                            dometita_usernameerror = viewModel.dometita_username.isBlank()
                            dometita_passworderror = !isPasswordValid(viewModel.dometita_password.trim())

                            val isValid = !(dometita_lnameerror || dometita_fnameerror || dometita_mnameerror || dometita_usernameerror || dometita_passworderror)

                            if (isValid) {
                                val user = if (userToEdit != null) {
                                    userToEdit.copy(
                                        Entity_fname = viewModel.dometita_fname.trim(),
                                        Entity_mname = viewModel.dometita_mname.trim(),
                                        Entity_lname = viewModel.dometita_lname.trim(),
                                        Entity_username = viewModel.dometita_username.trim(),
                                        Entity_password = viewModel.dometita_password.trim()
                                    )
                                } else {
                                    Entity_Users(
                                        Entity_fname = viewModel.dometita_fname.trim(),
                                        Entity_mname = viewModel.dometita_mname.trim(),
                                        Entity_lname = viewModel.dometita_lname.trim(),
                                        Entity_username = viewModel.dometita_username.trim(),
                                        Entity_password = viewModel.dometita_password.trim()
                                    )
                                }

                                scope.launch {
                                    if (userToEdit != null) {
                                        viewModel2.viewModel_update(user)
                                    } else {
                                        viewModel2.viewModel_insert(user)
                                    }
                                    viewModel2.viewModel_userToEdit(null)
                                    showEditDialog = true
                                    onUserSaved()
                                }
                            }
                        },


                        modifier = Modifier
                            .width(280.dp)
                            .height(60.dp)
                            .align(Alignment.CenterHorizontally) // centers the button
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B3E2E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    {
                        Text("Register")
                    }

                    TextButton(
                        onClick = {
                            navController.navigate(Routes.R_Login.routes)
                        }
                    ) {
                        Text(
                            buildAnnotatedString {
                                append("Already have an account? ")
                                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                                    append("Sign in")
                                }
                            },
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    if (showEditDialog){
                        AlertDialog(
                            onDismissRequest = {showEditDialog = false},
                            confirmButton = {
                                Button(onClick = {
                                    showEditDialog = false
                                    navController.navigate(Routes.R_Login.routes)
                                },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Black,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Okay")
                                }
                            },
                            title = {Text("Success")},
                            text = { Text("User data has been saved successfully.") }
                        )
                    }
                }
            }
        }
    }
}