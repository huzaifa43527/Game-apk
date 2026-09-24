package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AchievementEntity
import com.example.data.db.PlayerProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileShopScreen(
    profile: PlayerProfile?,
    achievements: List<AchievementEntity>,
    onUpdateName: (String) -> Unit,
    onSelectTheme: (String, Int) -> Unit,
    onSelectAvatar: (String, Int) -> Unit
) {
    var selectedSection by remember { mutableStateOf(0) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(profile?.playerName ?: "Huzaifa") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Player & Rewards",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        // Subtabs: Profile & Shop vs Achievements
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = { Text("Profile & Shop", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_shop")
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = { Text("Achievements (${achievements.count { it.isUnlocked }}/${achievements.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_achievements")
            )
        }

        if (selectedSection == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Profile Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AvatarBadge(avatarId = profile?.activeAvatar ?: "ROOKIE", size = 52)
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = profile?.playerName ?: "Huzaifa",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = {
                                                    editedName = profile?.playerName ?: "Huzaifa"
                                                    showEditNameDialog = true
                                                },
                                                modifier = Modifier.size(24.dp).testTag("edit_player_name_btn")
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit name", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text(
                                            text = "Level ${profile?.level ?: 1} Detective",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFFFFD600).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "${profile?.coins ?: 0}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFFD600)
                                        )
                                    }
                                }
                            }

                            // XP bar
                            val currentLevelXp = (profile?.xp ?: 0) % 500
                            val xpProgress = currentLevelXp / 500f
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "XP Progress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "$currentLevelXp / 500 XP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { xpProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }

                // Avatars Section
                item {
                    Text(
                        text = "Unlockable Avatars",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    val avatars = listOf(
                        Triple("ROOKIE", "Rookie Guesser", 0),
                        Triple("DETECTIVE", "Number Detective", 100),
                        Triple("WIZARD", "Math Wizard", 250),
                        Triple("ROBOT", "Logic Android", 400),
                        Triple("CHAMPION", "Crowned Champ", 600),
                        Triple("NINJA", "Shadow Ninja", 800)
                    )

                    val unlockedList = (profile?.unlockedAvatars ?: "ROOKIE").split(",")

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        avatars.forEach { (id, name, cost) ->
                            val isUnlocked = unlockedList.contains(id)
                            val isActive = profile?.activeAvatar == id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectAvatar(id, cost) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AvatarBadge(avatarId = id, size = 40)
                                        Column {
                                            Text(text = name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (isUnlocked) "Unlocked" else "$cost Coins",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isUnlocked) Color(0xFF00E676) else Color(0xFFFFD600)
                                            )
                                        }
                                    }

                                    if (isActive) {
                                        Surface(color = Color(0xFF00E676).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00E676),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else if (isUnlocked) {
                                        OutlinedButton(onClick = { onSelectAvatar(id, cost) }) {
                                            Text("Equip")
                                        }
                                    } else {
                                        val canAfford = (profile?.coins ?: 0) >= cost
                                        Button(
                                            onClick = { onSelectAvatar(id, cost) },
                                            enabled = canAfford
                                        ) {
                                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("$cost 🪙")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Themes Section
                item {
                    Text(
                        text = "Visual Themes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    val themes = listOf(
                        Triple("INDIGO", "Classic Modern Indigo", 0),
                        Triple("CYBERPUNK", "Cyberpunk Neon", 200),
                        Triple("EMERALD", "Emerald Forest", 350),
                        Triple("SUNSET", "Sunset Gold", 500)
                    )

                    val unlockedThemes = (profile?.unlockedThemes ?: "INDIGO").split(",")

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        themes.forEach { (id, name, cost) ->
                            val isUnlocked = unlockedThemes.contains(id)
                            val isActive = profile?.activeTheme?.uppercase() == id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectTheme(id, cost) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = when (id) {
                                                "CYBERPUNK" -> Color(0xFF00F0FF)
                                                "EMERALD" -> Color(0xFF00C853)
                                                "SUNSET" -> Color(0xFFFF6D00)
                                                else -> Color(0xFF3D5AFE)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {}
                                        Column {
                                            Text(text = name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (isUnlocked) "Unlocked" else "$cost Coins",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isUnlocked) Color(0xFF00E676) else Color(0xFFFFD600)
                                            )
                                        }
                                    }

                                    if (isActive) {
                                        Surface(color = Color(0xFF00E676).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00E676),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else if (isUnlocked) {
                                        OutlinedButton(onClick = { onSelectTheme(id, cost) }) {
                                            Text("Apply")
                                        }
                                    } else {
                                        val canAfford = (profile?.coins ?: 0) >= cost
                                        Button(
                                            onClick = { onSelectTheme(id, cost) },
                                            enabled = canAfford
                                        ) {
                                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("$cost 🪙")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        } else {
            // Achievements Gallery
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(achievements) { ach ->
                    AchievementItemCard(achievement = ach)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Player Name") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { if (it.length <= 16) editedName = it },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateName(editedName)
                        showEditNameDialog = false
                    },
                    modifier = Modifier.testTag("save_player_name_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AchievementItemCard(achievement: AchievementEntity) {
    val dateStr = if (achievement.unlockedAt != null) {
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(achievement.unlockedAt))
    } else {
        null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (achievement.isUnlocked) Color(0xFFFFD600).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (achievement.isUnlocked) "🏆" else "🔒",
                        fontSize = 22.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (dateStr != null) {
                    Text(
                        text = "Unlocked on $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                color = Color(0xFFFFD600).copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "+${achievement.coinReward} 🪙", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFD600))
                    Text(text = "+${achievement.xpReward} XP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
