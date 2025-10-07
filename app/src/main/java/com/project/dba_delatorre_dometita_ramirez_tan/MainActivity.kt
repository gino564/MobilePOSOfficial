    package com.project.dba_delatorre_dometita_ramirez_tan

    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.enableEdgeToEdge
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.tooling.preview.Preview
    import androidx.lifecycle.ViewModelProvider
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.rememberNavController
    import com.project.dba_delatorre_dometita_ramirez_tan.ui.theme.DBA_delatorre_dometita_ramirez_tanTheme
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.navigation.NavController
    import com.jakewharton.threetenabp.AndroidThreeTen

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            AndroidThreeTen.init(this)
            enableEdgeToEdge()
            val db = Database_Users.getDatabase(applicationContext)
            val userdao = db.dao_users()
            val repo = RepositoryUsers(userdao)
            val factory = viewModel_Factory(repo)
            val userViewModel = ViewModelProvider(this, factory)[ViewModel_users::class.java]
            val db2 = Database_Products.getDatabase(applicationContext)
            val repository = ProductRepository(db2.dao_products(), db2.dao_salesReport())
            val productViewModel = ViewModelProvider(this, ProductViewModelFactory(repository))[ProductViewModel::class.java]
            val salesReportRepository = SalesReportRepository(db2.dao_salesReport())
            val salesReportViewModel = ViewModelProvider(this, SalesReportViewModelFactory(salesReportRepository))[SalesReportViewModel::class.java]

            setContent {
                val viewModel: frm_RegViewModel =  viewModel()
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Routes.R_Logo.routes) {
                    composable(Routes.Screen1.routes){
                        Screen1(navController = navController , viewModel = viewModel, viewModel2 = userViewModel)
                    }

                    composable(Routes.UserList.routes){
                        ViewListScreen(viewModel2 = userViewModel, navController = navController)
                    }
                    composable(Routes.R_Login.routes){
                        Login(navController = navController)
                    }
                    composable(Routes.R_DashboardScreen.routes){
                        dashboard(navController = navController, viewModel = salesReportViewModel)
                    }
                    composable(Routes.R_InventoryList.routes){
                        InventoryListScreen(navController = navController, viewModel3 = productViewModel)
                    }

                    composable(Routes.R_SalesReport.routes){
                        SalesReportScreen(navController = navController, salesViewModel = salesReportViewModel)
                    }

                    composable(Routes.R_AddProduct.routes){
                        AddProductScreen(navController = navController, viewModel3 = productViewModel)
                    }
                    composable(Routes.R_Logo.routes){
                        WelcomeLogo(navController = navController)
                    }
                    composable("EditProductScreen/{productId}") { backStackEntry ->
                        val productId = backStackEntry.arguments?.getString("productId")?.toIntOrNull()
                        val products = productViewModel.productList

                        val product = products.find { it.id == productId }

                        if (product != null) {
                            EditProductScreen(
                                navController = navController,
                                viewModel3 = productViewModel,
                                productToEdit = product
                            )
                        } else {
                            // Optional: fallback while loading or if product not found
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Product not found or still loading...", color = Color.Gray)
                            }
                        }
                    }
                    composable(Routes.OrderProcess.routes) {
                        OrderProcessScreen(navController = navController,
                            viewModel3 = productViewModel)
                    }
                    composable(Routes.R_EditAccountScreen.routes){
                        EditAccountScreen(navController = navController , viewModel = viewModel, viewModel2 = userViewModel)
                    }
                }
            }
        }
    }