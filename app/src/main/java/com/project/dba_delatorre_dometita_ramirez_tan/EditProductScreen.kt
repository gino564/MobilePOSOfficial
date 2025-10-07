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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    navController: NavController,
    viewModel3: ProductViewModel,
    productToEdit: Entity_Products
) {
    var name by remember { mutableStateOf(TextFieldValue(productToEdit.name)) }
    var category by remember { mutableStateOf(TextFieldValue(productToEdit.category)) }
    var price by remember { mutableStateOf(TextFieldValue(productToEdit.price.toString())) }
    var quantity by remember { mutableStateOf(TextFieldValue(productToEdit.quantity.toString())) }
    var imageUri by remember { mutableStateOf(TextFieldValue(productToEdit.imageUri)) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        imageUri = TextFieldValue(uri?.toString() ?: "")
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    Scaffold(

        containerColor = Color.Transparent,
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
                    .padding(paddingValues)
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

                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(start = 8.dp, top = 16.dp)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Product Information", fontSize = 24.sp, color = Color(0xFF6B3E2E))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Update your product details", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (imageUri.text.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri.text),
                                contentDescription = "Product Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") },

                            )
                            Text(
                                text = "Tap to change product image",
                                fontSize = 12.sp,
                                color = Color(0xFF4B3832),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        val textFieldModifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Product Name") },
                            modifier = textFieldModifier,
                            shape = RoundedCornerShape(20.dp)
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = textFieldModifier,
                            shape = RoundedCornerShape(20.dp)
                        )

                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price") },
                            modifier = textFieldModifier,
                            shape = RoundedCornerShape(20.dp)
                        )

                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity") },
                            modifier = textFieldModifier,
                            shape = RoundedCornerShape(20.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val finalImageUri = selectedImageUri?.let {
                                    copyImageToInternalStorage(context, it)
                                } ?: imageUri.text

                                val updatedProduct = Entity_Products(
                                    id = productToEdit.id,
                                    name = name.text,
                                    category = category.text,
                                    price = price.text.toDoubleOrNull() ?: 0.0,
                                    quantity = quantity.text.toIntOrNull() ?: 0,
                                    imageUri = finalImageUri
                                )

                                viewModel3.updateProduct(updatedProduct)
                                navController.popBackStack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6F4E37),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    )
}