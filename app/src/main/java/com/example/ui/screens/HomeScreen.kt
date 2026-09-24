package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlayerProfile
import com.example.game.GameModeType
import com.example.ui.components.StreakBanner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    profile: PlayerProfile?,
    onSelectMode: (GameModeType) -> Unit,
    onStartReverseMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHowToPlay: () -> Unit
) {
    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val isDailyCompleted = profile?.lastDailyCompletedDate == todayDate

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(6.dp)) }

        // Top App Bar & Profile Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile & Level
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AvatarBadge(avatarId = profile?.activeAvatar ?: "ROOKIE")
                    Column {
                        Text(
                            text = profile?.playerName ?: "Huzaifa",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LVL ${profile?.level ?: 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            val progress = ((profile?.xp ?: 0) % 500) / 500f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                // Coins & Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFFFD600).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = Color(0xFFFFD600),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${profile?.coins ?: 0}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD600)
                            )
                        }
                    }

                    IconButton(onClick = onOpenHowToPlay, modifier = Modifier.testTag("home_how_to_play_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "How to play")
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("home_settings_btn")) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        }

        // Streak Banner
        if ((profile?.currentStreak ?: 0) > 0) {
            item {
                StreakBanner(streak = profile?.currentStreak ?: 0)
            }
        }

        // Daily Challenge Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectMode(GameModeType.DAILY_CHALLENGE) }
                    .testTag("mode_daily_challenge"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                                )
                            )
                        )
                        .padding(16.dp)
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
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "DAILY CHALLENGE",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (isDailyCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isDailyCompleted) "Completed today! +100 Coins" else "1 – 500 • 7 Attempts • Bonus Reward",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = { onSelectMode(GameModeType.DAILY_CHALLENGE) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isDailyCompleted) "Play" else "Start")
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Game Modes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Easy Mode
        item {
            GameModeCard(
                title = "Easy Mode",
                subtitle = "1 – 50 • 10 attempts",
                description = "Gentle range ideal for beginners and casual play.",
                icon = Icons.Default.Star,
                accentColor = Color(0xFF00E676),
                onClick = { onSelectMode(GameModeType.EASY) },
                testTag = "mode_easy"
            )
        }

        // Medium Mode (Classic)
        item {
            GameModeCard(
                title = "Medium Mode (Classic)",
                subtitle = "1 – 200 • 10 attempts",
                description = "The authentic number guessing game experience with full intelligent hints.",
                icon = Icons.Default.QuestionMark,
                accentColor = MaterialTheme.colorScheme.primary,
                badge = "CLASSIC",
                onClick = { onSelectMode(GameModeType.MEDIUM) },
                testTag = "mode_medium"
            )
        }

        // Hard Mode
        item {
            GameModeCard(
                title = "Hard Mode",
                subtitle = "1 – 1,000 • 10 attempts",
                description = "Expansive search space for dedicated deductive thinkers.",
                icon = Icons.Default.Shield,
                accentColor = Color(0xFFFF9100),
                onClick = { onSelectMode(GameModeType.HARD) },
                testTag = "mode_hard"
            )
        }

        // Extreme Mode
        item {
            GameModeCard(
                title = "Extreme Mode",
                subtitle = "1 – 10,000 • 12 attempts",
                description = "Pure mastery challenge with 10,000 possibilities and no extra clues.",
                icon = Icons.Default.Whatshot,
                accentColor = Color(0xFFFF1744),
                badge = "EXTREME",
                onClick = { onSelectMode(GameModeType.EXTREME) },
                testTag = "mode_extreme"
            )
        }

        // Time Challenge Mode
        item {
            GameModeCard(
                title = "Time Attack Blitz",
                subtitle = "1 – 200 • 30 Seconds",
                description = "Race against the clock! Every second counts towards a high bonus.",
                icon = Icons.Default.Timer,
                accentColor = Color(0xFFFFD600),
                badge = "30s TIMER",
                onClick = { onSelectMode(GameModeType.TIME_CHALLENGE) },
                testTag = "mode_time_challenge"
            )
        }

        // Reverse Mode Card
        item {
            GameModeCard(
                title = "Reverse Guessing",
                subtitle = "Challenge the Computer",
                description = "Think of a number between 1 and 200. The AI tries to read your mind!",
                icon = Icons.Default.Psychology,
                accentColor = Color(0xFF00B0FF),
                badge = "AI MODE",
                onClick = onStartReverseMode,
                testTag = "mode_reverse"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun GameModeCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    badge: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (badge != null) {
                        Surface(
                            color = accentColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun AvatarBadge(avatarId: String, size: Int = 40) {
    val (emoji, bg) = when (avatarId) {
        "DETECTIVE" -> Pair("🕵️", Color(0xFF673AB7))
        "WIZARD" -> Pair("🧙", Color(0xFF3F51B5))
        "ROBOT" -> Pair("🤖", Color(0xFF009688))
        "NINJA" -> Pair("🥷", Color(0xFF212121))
        "CHAMPION" -> Pair("👑", Color(0xFFFF9800))
        else -> Pair("🎮", Color(0xFF3D5AFE))
    }

    Surface(
        shape = CircleShape,
        color = bg.copy(alpha = 0.25f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, bg),
        modifier = Modifier.size(size.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = (size * 0.5).sp)
        }
    }
}
