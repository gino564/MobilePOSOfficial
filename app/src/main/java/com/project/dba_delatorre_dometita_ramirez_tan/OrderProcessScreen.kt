package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderProcessScreen(navController: NavController, viewModel3: ProductViewModel, recipeViewModel: RecipeViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var cartVisible by remember { mutableStateOf(false) }
    var cartItems by remember { mutableStateOf<List<Entity_Products>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val searchQuery = remember { mutableStateOf("") }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var cashReceived by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }

    val totalPrice = cartItems.sumOf { it.price.toInt() }
    val gradient = Brush.verticalGradient(listOf(Color(0xFFF3D3BD), Color(0xFF837060)))

    val products = viewModel3.productList
        .filter { !it.category.equals("Ingredients", ignoreCase = true) }
        .filter {
            it.name.contains(searchQuery.value, ignoreCase = true) ||
                    it.category.contains(searchQuery.value, ignoreCase = true)
        }
        .map { product ->
            var availableQty by remember { mutableStateOf(product.quantity) }

            LaunchedEffect(product.firebaseId, viewModel3.productList, refreshTrigger) { // ✅ Added refreshTrigger
                availableQty = if (product.category.equals("Beverages", ignoreCase = true)) {
                    val calculated = recipeViewModel.getAvailableQuantity(product.firebaseId)
                    android.util.Log.d("OrderProcess", "🧮 ${product.name} (Beverages): Calculated = $calculated")
                    calculated
                } else {
                    android.util.Log.d("OrderProcess", "📦 ${product.name} (${product.category}): Stock = ${product.quantity}")
                    product.quantity
                }
            }

            product.copy(quantity = availableQty)
        }
        // ✅ UPDATED: First sort by availability (available first), then alphabetically by name
        .sortedWith(
            compareByDescending<Entity_Products> { it.quantity > 0 }  // Available first
                .thenBy { it.name }                                     // Then A-Z within each group
        )

    LaunchedEffect(cartVisible, refreshTrigger) {
        if (!cartVisible) {
            kotlinx.coroutines.delay(300)
            viewModel3.getAllProducts()
        }
    }

    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        SidebarDrawer(navController)
    }) {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF5D4037)
                ) {
                    TopAppBar(
                        title = {
                            Text("Order Process", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { cartVisible = !cartVisible }) {
                                BadgedBox(
                                    badge = {
                                        if (cartItems.isNotEmpty()) {
                                            Badge { Text("${cartItems.size}") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradient)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!cartVisible) {
                        OutlinedTextField(
                            value = searchQuery.value,
                            onValueChange = { searchQuery.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Search products...") },
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                            val categories = viewModel3.productList
                                .map { it.category }
                                .distinct()
                                .filter { !it.equals("Ingredients", ignoreCase = true) }

                            val allCategories = listOf("All") + categories

                            items(allCategories.size) { i ->
                                val category = allCategories[i]

                                val chipColor = when (category) {
                                    "All" -> Color(0xFF6F4E37)
                                    "Pastries" -> Color(0xFF6F4E37)
                                    "Beverages" -> Color(0xFF6F4E37)
                                    else -> Color(0xFFA88164)
                                }

                                val isSelected = if (category == "All") {
                                    searchQuery.value.isEmpty()
                                } else {
                                    searchQuery.value == category
                                }

                                AssistChip(
                                    onClick = {
                                        searchQuery.value = if (category == "All") "" else category
                                    },
                                    label = { Text(category, color = Color.White) },
                                    modifier = Modifier.padding(end = 8.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isSelected) chipColor.copy(alpha = 1f) else chipColor.copy(alpha = 0.6f)
                                    ),
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                                    } else null
                                )
                            }
                        }

                        products.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowItems.forEach { product ->
                                    // ✅ Check if product is out of stock
                                    val isOutOfStock = product.quantity <= 0

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(200.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            // ✅ Gray out if out of stock
                                            containerColor = if (isOutOfStock) Color(0xFFE0E0E0) else Color.White
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // ✅ Add overlay for out of stock items
                                            Box {
                                                if (product.imageUri.isNotEmpty()) {
                                                    Image(
                                                        painter = rememberAsyncImagePainter(
                                                            model = product.imageUri,
                                                            error = painterResource(R.drawable.ic_launcher_foreground),
                                                            placeholder = painterResource(R.drawable.ic_launcher_foreground)
                                                        ),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .height(90.dp)
                                                            .fillMaxWidth(),
                                                        contentScale = ContentScale.Crop,
                                                        // ✅ Dim image if out of stock
                                                        alpha = if (isOutOfStock) 0.4f else 1f
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .height(90.dp)
                                                            .fillMaxWidth()
                                                            .background(Color.LightGray, RoundedCornerShape(8.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("No Image", fontSize = 12.sp, color = Color.DarkGray)
                                                    }
                                                }

                                                // ✅ Show "OUT OF STOCK" badge
                                                if (isOutOfStock) {
                                                    Box(
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .background(Color.Black.copy(alpha = 0.5f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            "OUT OF STOCK",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                product.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                // ✅ Dim text if out of stock
                                                color = if (isOutOfStock) Color.Gray else Color.Black
                                            )
                                            Text(
                                                "₱${product.price}",
                                                fontSize = 12.sp,
                                                color = if (isOutOfStock) Color.Gray else Color.Black
                                            )
                                            Text(
                                                "Available: ${product.quantity}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (product.quantity > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // ✅ Disable button if out of stock
                                            Button(
                                                onClick = {
                                                    if (!isOutOfStock) {
                                                        cartItems = cartItems + product
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("${product.name} added to cart")
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(0.9f),
                                                enabled = !isOutOfStock, // ✅ KEY CHANGE: Disable when out of stock
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isOutOfStock) Color.Gray else Color(0xFFA88164),
                                                    disabledContainerColor = Color.Gray
                                                )
                                            ) {
                                                Text(
                                                    if (isOutOfStock) "Unavailable" else "Add",
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        // ✅ Cart view (unchanged)
                        Text("Order Summary", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                        cartItems.groupBy { it.firebaseId }.forEach { (_, items) ->
                            val product = items.first()
                            val quantity = items.size

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(product.name, fontWeight = FontWeight.Bold)
                                        Text("₱${product.price} x $quantity = ₱${product.price.toInt() * quantity}")
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            cartItems = cartItems + product
                                        }) {
                                            Text("+")
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(onClick = {
                                            cartItems = cartItems.toMutableList().apply { remove(product) }
                                        }) {
                                            Text("-")
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Total: ₱$totalPrice",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.End)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = cashReceived,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    cashReceived = it
                                }
                            },
                            label = { Text("Enter cash received") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val cashAmount = cashReceived.toDoubleOrNull() ?: 0.0
                                val change = cashAmount - totalPrice

                                if (change >= 0) {
                                    showReceiptDialog = true
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Not enough cash entered.")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
                        ) {
                            Text("Complete", color = Color.White)
                        }
                    }
                }
            }
        )

        if (showReceiptDialog) {
            AlertDialog(
                onDismissRequest = { showReceiptDialog = false },
                title = { Text("Order Summary", fontWeight = FontWeight.Bold) },
                text = {
                    val cashAmount = cashReceived.toDoubleOrNull() ?: 0.0
                    val change = cashAmount - totalPrice

                    Column {
                        cartItems.groupBy { it.firebaseId }.forEach { (_, items) ->
                            val product = items.first()
                            val quantity = items.size
                            Text("${product.name} x$quantity - ₱${product.price.toInt() * quantity}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total: ₱$totalPrice", fontWeight = FontWeight.Bold)
                        Text("Cash: ₱${String.format("%.2f", cashAmount)}")
                        Text(
                            "Change: ₱${String.format("%.2f", if (change >= 0) change else 0.0)}",
                            fontWeight = FontWeight.Bold,
                            color = if (change >= 0) Color.Black else Color.Red
                        )
                        if (change < 0) {
                            Text("Not enough cash!", color = Color.Red)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val cashAmount = cashReceived.toDoubleOrNull() ?: 0.0
                        val change = cashAmount - totalPrice

                        if (change < 0) return@TextButton

                        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                        cartItems.groupBy { it.firebaseId }.forEach { (_, items) ->
                            val product = items.first()
                            val quantity = items.size
                            val saleTotal = product.price * quantity

                            val sale = Entity_SalesReport(
                                productName = product.name,
                                category = product.category,
                                quantity = quantity,
                                price = product.price,
                                orderDate = currentDate,
                                productFirebaseId = product.firebaseId
                            )
                            viewModel3.insertSalesReport(sale)

                            AuditHelper.logSale(product.name, quantity, saleTotal)

                            // ✅ Use recipe-based processing for Beverages AND Pastries
                            if (product.category.equals("Beverages", ignoreCase = true) ||
                                product.category.equals("Pastries", ignoreCase = true)) {
                                android.util.Log.d("OrderProcess", "🔻 Processing recipe-based ${product.category}: ${product.name}")
                                recipeViewModel.processOrder(product.firebaseId, quantity, saveToSales = {})
                            } else {
                                // Direct stock deduction for Ingredients or other categories
                                android.util.Log.d("OrderProcess", "📦 Deducting ${product.category}: ${product.name}")
                                viewModel3.deductProductStock(product.firebaseId, quantity)
                            }
                        }

                        cartItems = emptyList()
                        cashReceived = ""
                        showReceiptDialog = false
                        cartVisible = false

                        // ✅ Refresh to update available quantities
                        refreshTrigger++
                        viewModel3.getAllProducts()
                    }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}