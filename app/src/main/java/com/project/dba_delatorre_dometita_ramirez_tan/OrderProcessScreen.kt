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
fun OrderProcessScreen(navController: NavController, viewModel3: ProductViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var cartVisible by remember { mutableStateOf(false) }
    var cartItems by remember { mutableStateOf<List<Entity_Products>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val searchQuery = remember { mutableStateOf("") }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var cashReceived by remember { mutableStateOf("") }


    val totalPrice = cartItems.sumOf { it.price.toInt() }
    val gradient = Brush.verticalGradient(listOf(Color(0xFFF3D3BD), Color(0xFF837060)))

    val products = viewModel3.productList.filter {
        it.name.contains(searchQuery.value, ignoreCase = true) ||
                it.category.contains(searchQuery.value, ignoreCase = true)
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
                            val categories = viewModel3.productList.map { it.category }.distinct()
                            items(categories.size) { i ->
                                val chipColor = when (categories[i]) {
                                    "Pastries" -> Color(0xFF8D6E63)
                                    "Drinks" -> Color(0xFFBCAAA4)
                                    "Desserts" -> Color(0xFF8D6E63)
                                    else -> Color(0xFFA88164)
                                }

                                AssistChip(
                                    onClick = { searchQuery.value = categories[i] },
                                    label = { Text(categories[i], color = Color.White) },
                                    modifier = Modifier.padding(end = 8.dp),
                                    colors = AssistChipDefaults.assistChipColors(containerColor = chipColor)
                                )

                            }
                        }

                        products.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowItems.forEach { product ->
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(200.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (product.imageUri.isNotEmpty()) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(product.imageUri),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .height(90.dp)
                                                        .fillMaxWidth()
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("₱${product.price}", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    cartItems = cartItems + product
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("${product.name} added to cart")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(0.9f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA88164))
                                            ) {
                                                Text("Add", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        Text("Order Summary", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                        cartItems.groupBy { it.id }.forEach { (_, items) ->
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

                                    // Add (+) and (-) buttons to adjust quantity
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

                        // ✅ Added total display here
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
                            onValueChange = { cashReceived = it },
                            label = { Text("Enter cash received") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                val cashAmount = cashReceived.toIntOrNull() ?: 0
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
                title = { Text("Receipt", fontWeight = FontWeight.Bold) },
                text = {
                    val cashAmount = cashReceived.toIntOrNull() ?: 0
                    val change = cashAmount - totalPrice

                    Column {
                        cartItems.groupBy { it.id }.forEach { (_, items) ->
                            val product = items.first()
                            val quantity = items.size
                            Text("${product.name} x$quantity - ₱${product.price.toInt() * quantity}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total: ₱$totalPrice", fontWeight = FontWeight.Bold)
                        Text("Cash: ₱$cashAmount")
                        Text(
                            "Change: ₱${if (change >= 0) change else 0}",
                            fontWeight = FontWeight.Bold,
                            color = if (change >= 0) Color.Black else Color.Red
                        )
                        if (change < 0) {
                            Text("Not enough cash!", color = Color.Red)
                        }
                    }
                }
                ,
                confirmButton = {
                    TextButton(onClick = {
                        val cashAmount = cashReceived.toIntOrNull() ?: 0
                        val change = cashAmount - totalPrice

                        if (change < 0) return@TextButton // Don't proceed if cash is not enough

                        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                        cartItems.groupBy { it.id }.forEach { (_, items) ->
                            val product = items.first()
                            val quantity = items.size
                            val sale = Entity_SalesReport(
                                productName = product.name,
                                quantity = quantity,
                                price = product.price,
                                orderDate = currentDate
                            )
                            viewModel3.insertSalesReport(sale)
                        }

                        cartItems = emptyList()
                        cashReceived = ""
                        showReceiptDialog = false
                        cartVisible = false
                    }) {
                        Text("Done")
                    }
                }

            )
        }
    }
}

