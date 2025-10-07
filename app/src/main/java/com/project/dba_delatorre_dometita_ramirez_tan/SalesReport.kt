    package com.project.dba_delatorre_dometita_ramirez_tan

    import android.graphics.Color as AndroidColor
    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Menu
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import com.github.mikephil.charting.charts.BarChart
    import com.github.mikephil.charting.data.BarData
    import com.github.mikephil.charting.data.BarDataSet
    import com.github.mikephil.charting.data.BarEntry
    import androidx.compose.ui.viewinterop.AndroidView
    import androidx.compose.ui.text.font.FontWeight
    import androidx.navigation.NavController
    import kotlinx.coroutines.launch
    import java.text.SimpleDateFormat
    import java.util.*
    import android.app.DatePickerDialog
    import androidx.compose.foundation.shape.RoundedCornerShape

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SalesReportScreen(navController: NavController, salesViewModel: SalesReportViewModel) {
        val gradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF967F4D), Color(0xFFD18F79))
        )
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val scrollState = rememberScrollState()

        val sales = salesViewModel.salesList
        val dateFormatter = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
        val groupedSales = sales.groupBy { dateFormatter.format(dateFormatter.parse(it.orderDate) ?: Date()) }

        val entries = groupedSales.entries.sortedBy { it.key }.mapIndexed { index, entry ->
            val monthlyTotal = entry.value.sumOf { it.price * it.quantity }
            BarEntry(index.toFloat(), monthlyTotal.toFloat())
        }

        val totalSales = sales.sumOf { it.price * it.quantity }
        val totalItemsSold = sales.sumOf { it.quantity }

        var startDate by remember { mutableStateOf("") }
        var endDate by remember { mutableStateOf("") }

        val context = LocalContext.current
        val startCalendar = Calendar.getInstance()
        val endCalendar = Calendar.getInstance()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = { SidebarDrawer(navController) }
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        Surface(
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                            shadowElevation = 8.dp,
                        ){
                        TopAppBar(
                            title = { Text("Sales Report") },
                            navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF6F4E37),
                                titleContentColor = Color.White
                            )
                        )
                    }},
                    content = { paddingValues ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFAF1E6))
                                .padding(16.dp)
                                .padding(paddingValues)
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = "Sales Summary",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5D4037)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            startCalendar.set(year, month, day)
                                            startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startCalendar.time)
                                        },
                                        startCalendar.get(Calendar.YEAR),
                                        startCalendar.get(Calendar.MONTH),
                                        startCalendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text(
                                    text = if (startDate.isBlank()) "Select Start Date" else "Start Date: $startDate",
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            endCalendar.set(year, month, day)
                                            endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endCalendar.time)
                                        },
                                        endCalendar.get(Calendar.YEAR),
                                        endCalendar.get(Calendar.MONTH),
                                        endCalendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text(
                                    text = if (endDate.isBlank()) "Select End Date" else "End Date: $endDate",
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Button(
                                    onClick = {
                                        if (startDate.isNotBlank() && endDate.isNotBlank()) {
                                            salesViewModel.filterSalesByRange(startDate, endDate)
                                        } else {
                                            salesViewModel.getAllSales()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA26153))
                                ) {
                                    Text("Apply Filter", color = Color.White)
                                }

                                Button(
                                    onClick = { salesViewModel.clearSales() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
                                ) {
                                    Text("Clear Sales", color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Total Sales: ₱%.2f".format(totalSales),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF3E2723)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            AndroidView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                factory = { context ->
                                    BarChart(context).apply {
                                        val dataSet = BarDataSet(entries, "Monthly Sales").apply {
                                            color = AndroidColor.parseColor("#6D4C41") // Brown
                                        }
                                        data = BarData(dataSet)
                                        description.text = "Monthly Sales"
                                        animateY(1000)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Total Items Sold: $totalItemsSold", fontSize = 16.sp, color = Color.Black)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Top Items Sold",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4E342E)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .background(Color(0xFFFFFFFF), shape = MaterialTheme.shapes.medium)
                                    .padding(12.dp)
                            ) {
                                val topItems = sales.groupBy { it.productName }
                                    .mapValues { it.value.sumOf { sale -> sale.quantity } }
                                    .toList()
                                    .sortedByDescending { it.second }
                                    .take(5)

                                topItems.forEach { (product, qty) ->
                                    Text("$product - $qty", modifier = Modifier.padding(4.dp), color = Color.Black)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
