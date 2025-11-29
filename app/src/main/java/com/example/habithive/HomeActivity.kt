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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.AlertDialog


class HomeActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // If not logged in, go back to Login
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val userId = currentUser.uid
        val userEmail = currentUser.email ?: "User"

        setContent {
            HabitHiveTheme {
                HomeScreen(
                    userId = userId,
                    userEmail = userEmail,
                    db = db,
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

/**
 * UI model for a habit stored in Firestore.
 * We also track lastCompletedDate (yyyy-MM-dd) to calculate daily streaks.
 */
data class HabitUi(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val isDoneToday: Boolean = false,
    val streakDays: Long = 0L,
    val lastCompletedDate: String? = null
)

enum class BottomTab {
    TODAY, HISTORY, PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userId: String,
    userEmail: String,
    db: FirebaseFirestore,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    var habits by remember { mutableStateOf<List<HabitUi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(BottomTab.TODAY) }

    // 🔁 Dialog state – use rememberSaveable so it survives recomposition
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val gradientColors = listOf(
        Color(0xFFFFF7C2),   // light yellow
        Color(0xFFB4D9FF)    // light blue
    )

    // Date formatter for daily logic
    val dateFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    val todayString = remember {
        dateFormatter.format(Date())
    }

    // ✅ Listen to Firestore changes in this user's habits (realtime)
    DisposableEffect(userId) {
        val registration = db.collection("users")
            .document(userId)
            .collection("habits")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage = e.message
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        HabitUi(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            isDoneToday = doc.getBoolean("isDoneToday") ?: false,
                            streakDays = doc.getLong("streakDays") ?: 0L,
                            lastCompletedDate = doc.getString("lastCompletedDate")
                        )
                    }
                    habits = list
                    isLoading = false
                }
            }

        onDispose {
            // Remove Firestore listener when composable is destroyed
            registration.remove()
        }
    }

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
            if (selectedTab == BottomTab.TODAY) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.TODAY,
                    onClick = { selectedTab = BottomTab.TODAY },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                    label = { Text("Today") }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.HISTORY,
                    onClick = { selectedTab = BottomTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.PROFILE,
                    onClick = { selectedTab = BottomTab.PROFILE },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
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

            when (selectedTab) {
                BottomTab.TODAY -> TodayTabContent(
                    userEmail = userEmail,
                    habits = habits,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onToggleDone = { habit ->
                        val docRef = db.collection("users")
                            .document(userId)
                            .collection("habits")
                            .document(habit.id)

                        // ----- Daily streak logic -----
                        val currentDateStr = todayString

                        // Calculate yesterday string
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        val yesterdayStr = dateFormatter.format(cal.time)

                        val currentlyDone = habit.isDoneToday
                        val currentStreak = habit.streakDays
                        val lastDate = habit.lastCompletedDate

                        if (!currentlyDone) {
                            // Marking as done today
                            val newStreak = when {
                                lastDate == currentDateStr -> currentStreak // same day, keep streak
                                lastDate == yesterdayStr -> currentStreak + 1 // continued streak
                                else -> 1L // new streak
                            }

                            docRef.update(
                                mapOf(
                                    "isDoneToday" to true,
                                    "streakDays" to newStreak,
                                    "lastCompletedDate" to currentDateStr
                                )
                            ).addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    "Failed to update habit",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            // Unchecking for today – keep streak but mark as not done.
                            // (You could choose to reduce/reset streak here if you want.)
                            docRef.update(
                                mapOf(
                                    "isDoneToday" to false
                                )
                            ).addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    "Failed to update habit",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )

                BottomTab.HISTORY -> HistoryTabContent(habits = habits)

                BottomTab.PROFILE -> ProfileTabContent(
                    userEmail = userEmail,
                    onLogout = onLogout
                )
            }

            // ✅ Add Habit dialog – controlled by showAddDialog
            if (showAddDialog) {
                AddHabitDialog(
                    onDismiss = { showAddDialog = false },
                    onSaveHabit = { name, desc ->
                        if (name.isBlank()) {
                            Toast.makeText(
                                context,
                                "Habit name cannot be empty",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AddHabitDialog
                        }

                        val habit = hashMapOf(
                            "name" to name,
                            "description" to desc,
                            "isDoneToday" to false,
                            "streakDays" to 0L,
                            "lastCompletedDate" to null
                        )

                        db.collection("users")
                            .document(userId)
                            .collection("habits")
                            .add(habit)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Habit added",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showAddDialog = false
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    "Failed to add habit",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                )
            }
        }
    }
}

@Composable
fun TodayTabContent(
    userEmail: String,
    habits: List<HabitUi>,
    isLoading: Boolean,
    errorMessage: String?,
    onToggleDone: (HabitUi) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // Greeting section
        Text(
            text = "Hello, $userEmail ",
            fontSize = 22.sp,
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

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        if (errorMessage != null) {
            Text(
                text = "Error: $errorMessage",
                color = Color.Red
            )
            return
        }

        val completedCount = habits.count { it.isDoneToday }
        val totalCount = habits.size

        Text(
            text = "Progress: $completedCount / $totalCount habits done",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A4B7A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (habits.isEmpty()) {
            Text(
                text = "No habits yet. Tap + to add one!",
                fontStyle = FontStyle.Italic,
                color = Color(0xFF555555)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit = habit,
                        onToggleDone = { onToggleDone(habit) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    habits: List<HabitUi>
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "History & Streaks",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D2D2D)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (habits.isEmpty()) {
            Text(
                text = "No habits yet to show history.",
                fontStyle = FontStyle.Italic,
                color = Color(0xFF555555)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(habits, key = { it.id }) { habit ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFFFFF).copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = habit.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Current streak: ${habit.streakDays} days 🔥",
                                fontSize = 14.sp,
                                color = Color(0xFF1A4B7A)
                            )
                            if (!habit.lastCompletedDate.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Last done: ${habit.lastCompletedDate}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF555555)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileTabContent(
    userEmail: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Profile",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D2D2D)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Email:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = userEmail,
            fontSize = 16.sp,
            color = Color(0xFF1A4B7A)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onLogout) {
            Text("Logout")
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

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSaveHabit: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add New Habit")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaveHabit(name, description) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
