package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun Screen2(dometita_lname:String,dometita_fname:String, dometita_mname:String, dometita_username:String,
            dometita_password:String, navController: NavController){
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0E1CC), Color(0xFFD18F79))
    )

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
            .background(gradient)
    ){
        Text(
            text = "VALUES FROM SCREEN 1 \nFull Name: $dometita_lname, $dometita_fname $dometita_mname" +
                    "\nUsername: $dometita_username\nPassword: $dometita_password",
            color = Color.Black,
            fontSize = 24.sp,
            textAlign = TextAlign.Left,
            fontFamily = FontFamily.Monospace
        )
        androidx.compose.material3.Button(
            onClick = {
 //               navController.navigate(Routes.R_Screen1)
            }
        ) {
            Text(text = "Go Back to Screen 1")
        }
    }
}