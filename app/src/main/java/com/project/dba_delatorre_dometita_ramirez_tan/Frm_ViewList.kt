package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults.shape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import coil.compose.rememberAsyncImagePainter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewListScreen(viewModel2: ViewModel_users, navController: NavController) {
    val users = viewModel2.users.collectAsState().value
    var userToDelete by remember { mutableStateOf<Entity_Users?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedRole by remember { mutableStateOf("All") }

    val selectedChipColor = Color(0xFF6F4E37) // Coffee brown
    val unselectedChipColor = Color(0xFFEEE0CB) // Light beige
    val selectedTextColor = Color.White
    val unselectedTextColor = Color.Black

    val filteredUsers = users.filter {
        (searchQuery.text.isEmpty() ||
                it.Entity_fname.contains(searchQuery.text, ignoreCase = true) ||
                it.Entity_lname.contains(searchQuery.text, ignoreCase = true) ||
                it.Entity_username.contains(searchQuery.text, ignoreCase = true))
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawer(navController)
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
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
                                text = "Registered Users",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White
                        )
                    )
                }
            },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = gradient)
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search user") },

                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 5.dp, vertical = 8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.LightGray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth() // optional, can be wrapContentWidth too
                            .padding(start = 0.dp), // explicitly remove left padding
                        horizontalArrangement = Arrangement.Start
                    ) {
                        listOf("All", "Manager", "Owner", "Staff").forEach { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = {
                                    Text(
                                        role,
                                        color = if (selectedRole == role) selectedTextColor else unselectedTextColor
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 6.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = unselectedChipColor,
                                    selectedContainerColor = selectedChipColor
                                ),
                                border = null // Removes the outline
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredUsers) { user ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                                elevation = CardDefaults.cardElevation(6.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val painter = if (!user.profileImageUri.isNullOrBlank()) {
                                        rememberAsyncImagePainter(model = user.profileImageUri)
                                    } else {
                                        painterResource(id = R.drawable.ic_placeholder_profile) // your fallback image
                                    }

                                    Image(
                                        painter = painter,
                                        contentDescription = "Profile Image",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )


                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "${user.Entity_fname} ${user.Entity_lname}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = Color(0xFF3E2723)
                                    )
                                    Text(
                                        text = user.Entity_username,
                                        fontSize = 14.sp,
                                        color = Color(0xFF4E342E)
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        IconButton(onClick = {
                                            viewModel2.viewModel_userToEdit(user)
                                            navController.navigate(Routes.R_EditAccountScreen.routes)
                                        }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = Color(0xFF6D4C41)
                                            )
                                        }
                                        IconButton(onClick = {
                                            userToDelete = user
                                            showDeleteDialog = true
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFF6D4C41)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showDeleteDialog && userToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Confirm Delete", fontFamily = FontFamily.Serif) },
                            text = { Text("Are you sure you want to delete this record?", fontFamily = FontFamily.Serif) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel2.viewModel_delete(userToDelete!!)
                                        showDeleteDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3E2723),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showDeleteDialog = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF795548),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        )
    }
}
