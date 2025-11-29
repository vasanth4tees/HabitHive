package com.example.habithive

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habithive.ui.theme.HabitHiveTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HabitHiveTheme {
                SplashScreen {
                    // After splash → move to next activity (LoginActivity)
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {

    // Splash screen duration
    LaunchedEffect(true) {
        delay(2000)    // 2 seconds
        onTimeout()
    }

    // Light yellow + blue gradient
    val gradientColors = listOf(
        Color(0xFFFFF7C2),   // very light yellow
        Color(0xFFB4D9FF)    // very light sky blue
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(gradientColors)
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Title
            Text(
                text = "HabitHive",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D2D2D)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Caption (italic)
            Text(
                text = "Build better habits, one day at a time",
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF3D3D3D)
            )
        }
    }
}
