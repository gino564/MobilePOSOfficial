package com.project.dba_delatorre_dometita_ramirez_tan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen(
    navController: NavController,
    viewModel: frm_RegViewModel,
    viewModel2: ViewModel_users,
    onUserUpdated: () -> Unit = {}
) {
    val userToEdit = viewModel2.userToEdit
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    var showEditDialog by remember { mutableStateOf(false) }
    var lnameError by remember { mutableStateOf(false) }
    var fnameError by remember { mutableStateOf(false) }
    var mnameError by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    LaunchedEffect(userToEdit) {
        userToEdit?.let { user ->
            viewModel.txtlnameData(user.Entity_lname)
            viewModel.txtfnameData(user.Entity_fname)
            viewModel.txtmnameData(user.Entity_mname)
            viewModel.txtusernameData(user.Entity_username)
            viewModel.txtpasswordData(user.Entity_password)
        }
    }

    val savedUri = userToEdit?.profileImageUri?.let { Uri.parse(it) }
    val imageUriState = remember { mutableStateOf<Uri?>(savedUri) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUriState.value = uri
    }
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF5D4037),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Edit Profile",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack() // Navigate back when the icon is clicked
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        containerColor = Color.Transparent,
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)


                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .wrapContentHeight()
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {


                        Image(
                            painter = imageUriState.value?.let { rememberAsyncImagePainter(it) }
                                ?: painterResource(R.drawable.ic_placeholder_profile),
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "Tap to change profile picture",
                            fontSize = 12.sp,
                            color = Color(0xFF4B3832),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Form fields
                        val textFieldModifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)

                        OutlinedTextField(
                            value = viewModel.dometita_lname,
                            onValueChange = {
                                viewModel.txtlnameData(it); lnameError = false
                            },
                            label = { Text("Last name") },
                            shape = RoundedCornerShape(20.dp),
                            isError = lnameError,
                            modifier = textFieldModifier
                        )
                        if (lnameError) Text("Last name is required", color = Color.Red, fontSize = 12.sp)

                        OutlinedTextField(
                            value = viewModel.dometita_fname,
                            onValueChange = {
                                viewModel.txtfnameData(it); fnameError = false
                            },
                            label = { Text("First name") },
                            shape = RoundedCornerShape(20.dp),
                            isError = fnameError,
                            modifier = textFieldModifier
                        )
                        if (fnameError) Text("First name is required", color = Color.Red, fontSize = 12.sp)

                        OutlinedTextField(
                            value = viewModel.dometita_mname,
                            onValueChange = {
                                viewModel.txtmnameData(it); mnameError = false
                            },
                            label = { Text("Middle name") },
                            shape = RoundedCornerShape(20.dp),
                            isError = mnameError,
                            modifier = textFieldModifier
                        )
                        if (mnameError) Text("Middle name is required", color = Color.Red, fontSize = 12.sp)

                        OutlinedTextField(
                            value = viewModel.dometita_username,
                            onValueChange = {
                                viewModel.txtusernameData(it); usernameError = false
                            },
                            label = { Text("Username") },
                            shape = RoundedCornerShape(20.dp),
                            isError = usernameError,
                            modifier = textFieldModifier
                        )
                        if (usernameError) Text("Username is required", color = Color.Red, fontSize = 12.sp)

                        OutlinedTextField(
                            value = viewModel.dometita_password,
                            onValueChange = {
                                viewModel.txtpasswordData(it); passwordError = false
                            },
                            label = { Text("Password") },
                            isError = passwordError,
                            shape = RoundedCornerShape(20.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = textFieldModifier
                        )
                        if (passwordError) Text("Password is required", color = Color.Red, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    // Add validation and update logic here
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6F4E37),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text("Save Changes")

                            }
                        }
                    }
                }





            if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                confirmButton = {
                    Button(onClick = {
                        showEditDialog = false
                        navController.popBackStack()
                    }) {
                        Text("OK")
                    }
                },
                title = { Text("Profile Updated") },
                text = { Text("Your account has been updated successfully.") }
            )}})}





