package com.example.habithive

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habithive.ui.theme.HabitHiveTheme
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // If not logged in, go back to Login
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            HabitHiveTheme {
                HomeScreen(
                    onLogout = {
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

data class HabitUi(
    val id: Int,
    val name: String,
    val description: String,
    var isDoneToday: Boolean,
    val streakDays: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    // Temporary dummy list – later this will come from Room DB
    val habits = remember {
        mutableStateListOf(
            HabitUi(1, "Morning Walk", "Walk for 20 minutes", false, 4),
            HabitUi(2, "Drink Water", "8 glasses a day", true, 10),
            HabitUi(3, "Read Book", "Read for 15 minutes", false, 2)
        )
    }

    val gradientColors = listOf(
        Color(0xFFFFF7C2),   // light yellow
        Color(0xFFB4D9FF)    // light blue
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HabitHive",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    TextButton(onClick = { onLogout() }) {
                        Text(text = "Logout", color = Color(0xFF1A4B7A))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Later: navigate to AddHabitActivity
                    Toast.makeText(context, "Add Habit screen coming soon", Toast.LENGTH_SHORT)
                        .show()
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                // Greeting section
                Text(
                    text = "Hello ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D2D2D)
                )

                Text(
                    text = "Here are your habits for today:",
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF3D3D3D)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Simple summary
                val completedCount = habits.count { it.isDoneToday }
                val totalCount = habits.size

                Text(
                    text = "Progress: $completedCount / $totalCount habits done",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A4B7A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Habits list
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(habits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            onToggleDone = {
                                val index = habits.indexOfFirst { h -> h.id == habit.id }
                                if (index != -1) {
                                    val current = habits[index]
                                    habits[index] = current.copy(
                                        isDoneToday = !current.isDoneToday
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCard(
    habit: HabitUi,
    onToggleDone: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF).copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = habit.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D2D2D)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = habit.description,
                    fontSize = 14.sp,
                    color = Color(0xFF555555)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Streak: ${habit.streakDays} days ",
                    fontSize = 13.sp,
                    color = Color(0xFF1A4B7A),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Checkbox(
                    checked = habit.isDoneToday,
                    onCheckedChange = { onToggleDone() }
                )
                Text(
                    text = if (habit.isDoneToday) "Done" else "Today",
                    fontSize = 12.sp
                )
            }
        }
    }
}
