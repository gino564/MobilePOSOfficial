package com.project.dba_delatorre_dometita_ramirez_tan

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    navController: NavController,
    viewModel3: ProductViewModel
) {
    LaunchedEffect(Unit) {
        viewModel3.getAllProducts()
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val products = viewModel3.productList

    var productToDelete by remember { mutableStateOf<Entity_Products?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    val chipOptions = listOf("All", "Beverages", "Pastries")
    var selectedOption by remember { mutableStateOf("All") }

    val selectedChipColor = Color(0xFF6F4E37)
    val unselectedChipColor = Color(0xFFEEE0CB)
    val selectedTextColor = Color.White
    val unselectedTextColor = Color.Black

    val filteredProducts = products.filter {
        it.name.contains(searchText.text, ignoreCase = true) &&
                (selectedOption == "All" || it.category.equals(selectedOption, ignoreCase = true))
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { SidebarDrawer(navController) }
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("AddProductScreen") },
                    containerColor = Color(0xFFA26153),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            },
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
                                text = "Inventory List",
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
                        .verticalScroll(rememberScrollState())
                ) {

                    // 🔍 Search
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search product...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // 🏷️ Categories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        chipOptions.forEach { option ->
                            FilterChip(
                                selected = selectedOption == option,
                                onClick = { selectedOption = option },
                                label = {
                                    Text(
                                        option,
                                        color = if (selectedOption == option) selectedTextColor else unselectedTextColor
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 1.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = unselectedChipColor,
                                    selectedContainerColor = selectedChipColor
                                ),
                                border = null
                            )
                        }
                    }

                    // 📦 Products
                    if (filteredProducts.isEmpty()) {
                        Text(
                            text = "No products available.",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        filteredProducts.forEach { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = CardDefaults.cardElevation(4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // 🖼️ Image
                                    if (product.imageUri.isNotEmpty()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(File(product.imageUri)),
                                            contentDescription = "Product Image",
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.LightGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No Image", fontSize = 12.sp, color = Color.DarkGray)
                                        }
                                    }

                                    // 📄 Details
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text(product.category, fontSize = 14.sp, color = Color(0xFF4E342E))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${product.quantity} pcs", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("₱${product.price}", fontSize = 14.sp)
                                        }
                                    }

                                    // 🛠️ Actions
                                    Column {
                                        IconButton(onClick = {
                                            navController.navigate("EditProductScreen/${product.id}")
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF6D4C41))
                                        }
                                        IconButton(onClick = {
                                            productToDelete = product
                                            showDeleteDialog = true
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 🔔 Delete confirmation dialog
                    if (showDeleteDialog && productToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Confirm Delete", fontFamily = FontFamily.Serif) },
                            text = { Text("Are you sure you want to delete this record?", fontFamily = FontFamily.Serif) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel3.deleteProduct(productToDelete!!)
                                        showDeleteDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723))
                                ) {
                                    Text("Yes", color = Color.White)
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showDeleteDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548))
                                ) {
                                    Text("Cancel", color = Color.White)
                                }
                            }
                        )
                    }
                }
            }
        )
    }
}
