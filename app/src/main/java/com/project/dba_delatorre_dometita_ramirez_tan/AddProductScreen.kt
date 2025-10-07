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
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    viewModel3: ProductViewModel,
    onUserSaved: () -> Unit = {}
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    var productName by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productQuantity by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        imageError = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(gradient)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }

                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Add Product", fontSize = 24.sp, color = Color(0xFF6B3E2E))
                            Spacer(modifier = Modifier.height(16.dp))

                            // IMAGE SELECTION SECTION
                            if (selectedImageUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(selectedImageUri),
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(8.dp)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFFE0C3A1), Color(0xFFB1785F))
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = "Product Icon",
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tap to choose image",
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                if (imageError) {
                                    Text("Image is required", color = Color.Red, fontSize = 12.sp)
                                }
                            }

                            val textFieldModifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)

                            OutlinedTextField(
                                value = productName,
                                onValueChange = {
                                    productName = it; nameError = false
                                },
                                label = { Text("Product Name") },
                                isError = nameError,
                                modifier = textFieldModifier,
                                shape = RoundedCornerShape(20.dp)
                            )
                            if (nameError) Text("Required", color = Color.Red, fontSize = 12.sp)

                            OutlinedTextField(
                                value = productCategory,
                                onValueChange = {
                                    productCategory = it; categoryError = false
                                },
                                label = { Text("Category") },
                                isError = categoryError,
                                modifier = textFieldModifier,
                                shape = RoundedCornerShape(20.dp)
                            )
                            if (categoryError) Text("Required", color = Color.Red, fontSize = 12.sp)

                            OutlinedTextField(
                                value = productPrice,
                                onValueChange = {
                                    productPrice = it; priceError = false
                                },
                                label = { Text("Price") },
                                isError = priceError,
                                modifier = textFieldModifier,
                                shape = RoundedCornerShape(20.dp)
                            )
                            if (priceError) Text("Required", color = Color.Red, fontSize = 12.sp)

                            OutlinedTextField(
                                value = productQuantity,
                                onValueChange = {
                                    productQuantity = it; quantityError = false
                                },
                                label = { Text("Quantity") },
                                isError = quantityError,
                                modifier = textFieldModifier,
                                shape = RoundedCornerShape(20.dp)
                            )
                            if (quantityError) Text("Required", color = Color.Red, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    nameError = productName.isBlank()
                                    categoryError = productCategory.isBlank()
                                    priceError = productPrice.isBlank()
                                    quantityError = productQuantity.isBlank()
                                    imageError = selectedImageUri == null

                                    val isValid = !(nameError || categoryError || priceError || quantityError || imageError)

                                    if (isValid) {
                                        val imagePath = copyImageToInternalStorage(context, selectedImageUri!!)
                                        viewModel3.insertProduct(
                                            Entity_Products(
                                                name = productName.trim(),
                                                category = productCategory.trim(),
                                                price = productPrice.toDouble(),
                                                quantity = productQuantity.toInt(),
                                                imageUri = imagePath
                                            )
                                        )
                                        showDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5D4037),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Save Product")
                            }

                            OutlinedButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text("Cancel", color = Color.Black)
                            }
                        }
                    }
                }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDialog = false
                                    navController.popBackStack()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5D4037),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Okay")
                            }
                        },
                        title = { Text("Success") },
                        text = { Text("Product saved successfully!") }
                    )
                }
            }
        }
    )
}
