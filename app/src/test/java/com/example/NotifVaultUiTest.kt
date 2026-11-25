package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NotificationEntity
import com.example.ui.navigation.NotifVaultNavHost
import com.example.ui.theme.NotifVaultTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class NotifVaultUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: NotifVaultApp
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        runBlocking {
            app.container.notificationRepository.deleteAll()
            app.container.notificationRepository.saveIfNotDuplicate(
                NotificationEntity(
                    packageName = "com.whatsapp",
                    appName = "WhatsApp",
                    sender = "أحمد علي",
                    title = "أحمد علي",
                    message = "مرحبًا، لا تنسَ اجتماع الفريق غدًا",
                    receivedAt = System.currentTimeMillis()
                )
            )
        }
        viewModel = MainViewModel(app, app.container)
    }

    @Test
    fun `navigate across dashboard log detail apps favorites search and settings`() {
        composeTestRule.setContent {
            NotifVaultTheme {
                NotifVaultNavHost(
                    viewModel = viewModel,
                    isDarkTheme = false,
                    skipSplash = true
                )
            }
        }
        composeTestRule.waitForIdle()

        // Wait for Room Flow emission on Dashboard
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("أحمد علي").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("permission_status_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("أحمد علي").assertIsDisplayed()

        // Open Log tab
        composeTestRule.onNodeWithTag("nav_log").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("مرحبًا، لا تنسَ اجتماع الفريق غدًا")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("مرحبًا، لا تنسَ اجتماع الفريق غدًا").assertIsDisplayed()

        // Open detail, mark as favorite, and navigate back
        composeTestRule.onNodeWithText("أحمد علي").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodes(hasText("مرحبًا، لا تنسَ اجتماع الفريق غدًا"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("detail_message_body").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_favorite_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("detail_back_button").performClick()
        composeTestRule.waitForIdle()

        // Open Favorites tab and verify starred message appears
        composeTestRule.onNodeWithTag("nav_favorites").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("أحمد علي").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("أحمد علي").assertIsDisplayed()

        // Open Monitored Apps tab and verify WhatsApp is listed
        composeTestRule.onNodeWithTag("nav_apps").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("WhatsApp").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("app_card_com.whatsapp").assertIsDisplayed()

        // Open Dashboard -> Search screen and search locally
        composeTestRule.onNodeWithTag("nav_dashboard").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_search_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("local_search_input").performTextInput("اجتماع")
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("أحمد علي").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("أحمد علي").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_back_button").performClick()
        composeTestRule.waitForIdle()

        // Open Settings tab
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_otp_switch").assertIsDisplayed()
    }
}
