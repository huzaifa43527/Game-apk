package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.db.PlayerProfile
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun home_screen_screenshot() {
        val mockProfile = PlayerProfile(
            id = 1,
            playerName = "Huzaifa",
            level = 2,
            xp = 350,
            coins = 120,
            currentStreak = 3,
            highestStreak = 5,
            activeAvatar = "ROOKIE",
            activeTheme = "INDIGO"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    profile = mockProfile,
                    onSelectMode = {},
                    onStartReverseMode = {},
                    onOpenSettings = {},
                    onOpenHowToPlay = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_screen.png")
    }
}
