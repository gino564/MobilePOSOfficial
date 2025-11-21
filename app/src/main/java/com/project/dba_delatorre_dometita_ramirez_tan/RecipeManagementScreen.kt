package com.project.dba_delatorre_dometita_ramirez_tan

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeManagementScreen(
    navController: NavController,
    productViewModel: ProductViewModel,
    recipeViewModel: RecipeViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // State
    var showAddRecipeDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Entity_Products?>(null) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }

    // Fetch products
    LaunchedEffect(Unit) {
        productViewModel.getAllProducts()
    }

    // Filter products without recipes (Pastries and Beverages)
    val productsWithoutRecipes = productViewModel.productList.filter { product ->
        (product.category.equals("Pastries", ignoreCase = true) ||
         product.category.equals("Beverages", ignoreCase = true))
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { SidebarDrawer(navController) }
    ) {
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
                                text = "Recipe Management",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White
                        )
                    )
                }
            },
            snackbarHost = {
                if (showSnackbar) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = { showSnackbar = false }) {
                                Text("Dismiss", color = Color.White)
                            }
                        }
                    ) {
                        Text(snackbarMessage)
                    }
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
                    // Instructions
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "📝 Add Recipes for Pastries & Beverages",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3E2723)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Select a product below to create a recipe and add ingredients with their costs.",
                                fontSize = 14.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }

                    // Products List
                    if (productsWithoutRecipes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No Beverages or Pastries found.\nAdd products first!",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            "Select a Product to Add Recipe:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        productsWithoutRecipes.forEach { product ->
                            ProductRecipeCard(
                                product = product,
                                onAddRecipe = {
                                    selectedProduct = product
                                    showAddRecipeDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Add Recipe Dialog
                if (showAddRecipeDialog && selectedProduct != null) {
                    AddRecipeDialog(
                        product = selectedProduct!!,
                        productViewModel = productViewModel,
                        onDismiss = {
                            showAddRecipeDialog = false
                            selectedProduct = null
                        },
                        onSuccess = { message ->
                            snackbarMessage = message
                            showSnackbar = true
                            showAddRecipeDialog = false
                            selectedProduct = null
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun ProductRecipeCard(
    product: Entity_Products,
    onAddRecipe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Text(
                    text = product.category,
                    fontSize = 14.sp,
                    color = Color(0xFF6D4C41)
                )
                Text(
                    text = "₱${product.price}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B4513)
                )
            }

            Button(
                onClick = onAddRecipe,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F4E37)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recipe", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Recipe", color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeDialog(
    product: Entity_Products,
    productViewModel: ProductViewModel,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    // Get all ingredient products
    val ingredients = productViewModel.productList.filter {
        it.category.equals("Ingredients", ignoreCase = true)
    }

    // State for selected ingredients
    var selectedIngredients by remember { mutableStateOf<List<IngredientSelection>>(emptyList()) }
    var showIngredientSelector by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                "Add Recipe for ${product.name}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Select ingredients and specify quantities:",
                    fontSize = 14.sp,
                    color = Color(0xFF5D4037),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Selected Ingredients List
                if (selectedIngredients.isEmpty()) {
                    Text(
                        "No ingredients added yet",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    selectedIngredients.forEachIndexed { index, selection ->
                        IngredientRow(
                            selection = selection,
                            onQuantityChange = { newQty ->
                                selectedIngredients = selectedIngredients.toMutableList().apply {
                                    this[index] = selection.copy(quantity = newQty)
                                }
                            },
                            onRemove = {
                                selectedIngredients = selectedIngredients.toMutableList().apply {
                                    removeAt(index)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add Ingredient Button
                Button(
                    onClick = { showIngredientSelector = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F4E37))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Ingredient", color = Color.White)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedIngredients.isEmpty()) {
                        onSuccess("Please add at least one ingredient")
                        return@Button
                    }

                    scope.launch {
                        isSaving = true
                        try {
                            // Create recipe in Firebase
                            val recipeData = hashMapOf(
                                "productId" to product.id,
                                "productFirebaseId" to product.firebaseId,
                                "productName" to product.name
                            )
                            val recipeRef = firestore.collection("recipes").add(recipeData).await()
                            val recipeFirebaseId = recipeRef.id

                            // Add ingredients to Firebase
                            selectedIngredients.forEach { selection ->
                                val ingredientData = hashMapOf(
                                    "recipeFirebaseId" to recipeFirebaseId,
                                    "ingredientProductId" to selection.ingredient.firebaseId,
                                    "ingredientName" to selection.ingredient.name,
                                    "quantityNeeded" to selection.quantity,
                                    "unit" to selection.unit
                                )
                                firestore.collection("recipe_ingredients").add(ingredientData).await()
                            }

                            onSuccess("Recipe added successfully for ${product.name}!")
                        } catch (e: Exception) {
                            onSuccess("Error: ${e.message}")
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving && selectedIngredients.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Recipe", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )

    // Ingredient Selector Dialog
    if (showIngredientSelector) {
        IngredientSelectorDialog(
            ingredients = ingredients.filter { ing ->
                selectedIngredients.none { it.ingredient.firebaseId == ing.firebaseId }
            },
            onSelect = { ingredient ->
                selectedIngredients = selectedIngredients + IngredientSelection(
                    ingredient = ingredient,
                    quantity = 1.0,
                    unit = "g"
                )
                showIngredientSelector = false
            },
            onDismiss = { showIngredientSelector = false }
        )
    }
}

@Composable
fun IngredientRow(
    selection: IngredientSelection,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selection.ingredient.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Text(
                    text = "₱${selection.ingredient.price} (Total stock: ${selection.ingredient.quantity}${selection.unit})",
                    fontSize = 12.sp,
                    color = Color(0xFF6D4C41)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quantity Input
                OutlinedTextField(
                    value = selection.quantity.toString(),
                    onValueChange = {
                        it.toDoubleOrNull()?.let { qty -> onQuantityChange(qty) }
                    },
                    modifier = Modifier.width(80.dp),
                    suffix = { Text(selection.unit, fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientSelectorDialog(
    ingredients: List<Entity_Products>,
    onSelect: (Entity_Products) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Ingredient") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (ingredients.isEmpty()) {
                    Text(
                        "All ingredients have been added or no ingredients available",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                } else {
                    ingredients.forEach { ingredient ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = { onSelect(ingredient) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = ingredient.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3E2723)
                                )
                                Text(
                                    text = "Price: ₱${ingredient.price} | Stock: ${ingredient.quantity}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF5D4037)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

data class IngredientSelection(
    val ingredient: Entity_Products,
    val quantity: Double,
    val unit: String
)
