package com.tumba.ai.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tumba.ai.domain.model.AppLanguage
import com.tumba.ai.ui.chat.ChatScreen
import com.tumba.ai.ui.history.ConversationDrawer
import com.tumba.ai.ui.models.LocalModelsScreen
import com.tumba.ai.ui.models.ModelsScreen
import com.tumba.ai.ui.settings.SettingsScreen
import com.tumba.ai.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

object TumbaRoutes {
    const val CHAT_PATTERN = "chat/{conversationId}"
    const val SETTINGS = "settings"
    const val MODELS = "models"
    const val LOCAL_MODELS = "local-models"

    fun chat(conversationId: Long? = null): String = "chat/${conversationId ?: -1L}"
}

@Composable
fun TumbaApp(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
    val settings = settingsState.settings

    ApplyLocale(settings.language)

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentConversationId = backStackEntry
        ?.arguments
        ?.getLong("conversationId")
        ?.takeIf { it > 0L }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    /**
     * Replaces the chat destination instead of reusing it. Reusing the same route with
     * `launchSingleTop` keeps the existing NavBackStackEntry (and therefore the old
     * ChatViewModel), which is why conversations picked from History previously did not open.
     */
    fun openChat(conversationId: Long?) {
        navController.navigate(TumbaRoutes.chat(conversationId)) {
            popUpTo(TumbaRoutes.CHAT_PATTERN) { inclusive = true }
            launchSingleTop = true
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute == TumbaRoutes.CHAT_PATTERN,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
        drawerContent = {
            ConversationDrawer(
                currentConversationId = currentConversationId,
                onNewChat = {
                    closeDrawer()
                    openChat(null)
                },
                onOpenConversation = { conversationId ->
                    closeDrawer()
                    openChat(conversationId)
                },
                onOpenSettings = {
                    closeDrawer()
                    navController.navigate(TumbaRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onConversationDeleted = { deletedId ->
                    if (deletedId == currentConversationId) {
                        closeDrawer()
                        openChat(null)
                    }
                },
                onClose = { closeDrawer() },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = TumbaRoutes.CHAT_PATTERN,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(140)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(140)) },
            ) {
                composable(
                    route = TumbaRoutes.CHAT_PATTERN,
                    arguments = listOf(
                        navArgument("conversationId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                    ),
                ) {
                    ChatScreen(
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onOpenSettings = {
                            navController.navigate(TumbaRoutes.SETTINGS) {
                                launchSingleTop = true
                            }
                        },
                        onNewChat = { openChat(null) },
                        onConversationDeleted = { openChat(null) },
                    )
                }

                composable(TumbaRoutes.SETTINGS) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { navController.popBackStack() },
                        onOpenModels = {
                            navController.navigate(TumbaRoutes.MODELS) {
                                launchSingleTop = true
                            }
                        },
                        onOpenLocalModels = {
                            navController.navigate(TumbaRoutes.LOCAL_MODELS) {
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable(TumbaRoutes.MODELS) {
                    ModelsScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(TumbaRoutes.LOCAL_MODELS) {
                    LocalModelsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun ApplyLocale(language: AppLanguage) {
    LaunchedEffect(language) {
        val current = AppCompatDelegate.getApplicationLocales()
        when {
            language == AppLanguage.SYSTEM -> {
                if (!current.isEmpty) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
            }

            current.toLanguageTags() != language.tag -> {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(language.tag),
                )
            }
        }
    }
}
