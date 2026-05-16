package com.example.zell

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zell.ui.theme.ZellTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        // Schedule background worker that auto-deletes stories older than 24 hours
        StoryExpiryWorker.schedule(this)
        setContent {
            ZellTheme {
                ZellApp(this)
            }
        }
    }
}

@Composable
fun ZellApp(context: Context) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val userViewModel: UserViewModel = viewModel()
    val networkViewModel: NetworkViewModel = viewModel()
    val spacesViewModel: SpacesViewModel = viewModel() // shared across nav graph

    val currentUserProfile by userViewModel.currentUserProfile

    val sharedPrefs = remember { context.getSharedPreferences("zell_prefs", Context.MODE_PRIVATE) }
    val hasSeenOnboarding = remember { mutableStateOf(sharedPrefs.getBoolean("onboarding_complete", false)) }

    val connectivityManager = remember { NetworkConnectivityManager(context) }

    // Start connectivity monitoring and clean it up when the composable leaves
    DisposableEffect(Unit) {
        connectivityManager.startMonitoring()
        networkViewModel.updateConnectionState(connectivityManager.isOnline())
        onDispose { connectivityManager.stopMonitoring() }
    }

    // Push every connectivity change into the ViewModel
    val isSystemOnline by connectivityManager.isConnected
    LaunchedEffect(isSystemOnline) {
        networkViewModel.updateConnectionState(isSystemOnline)
    }

    // Track user online/offline status in Firestore
    DisposableEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).update("isOnline", true)
        }
        onDispose {
            if (uid != null) {
                db.collection("users").document(uid).update("isOnline", false)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "welcome",
            enterTransition = { fadeIn(tween(300)) + slideInHorizontally { it / 2 } },
            exitTransition = { fadeOut(tween(300)) + slideOutHorizontally { -it / 2 } }
        ) {

            composable("welcome") {
                WelcomeScreen(onTimeout = {
                    val target = when {
                        auth.currentUser != null -> "dashboard"
                        !hasSeenOnboarding.value -> "onboard"
                        else -> "signup_flow"
                    }
                    navController.navigate(target) { popUpTo("welcome") { inclusive = true } }
                })
            }

            composable("onboard") {
                OnBoardScreen(onNavigateToSignUp = {
                    sharedPrefs.edit().putBoolean("onboarding_complete", true).apply()
                    hasSeenOnboarding.value = true
                    navController.navigate("signup_flow")
                })
            }

            composable("signup_flow") {
                SignUpFlow(
                    onSignInClick = { navController.popBackStack() },
                    onComplete = {
                        navController.navigate("profile") {
                            popUpTo("signup_flow") { inclusive = true }
                        }
                    }
                )
            }

            composable("profile") {
                ProfileSetupScreen { navController.navigate("dashboard") { popUpTo(0) } }
            }

            composable("dashboard") {
                MainDashboard(
                    // Chat click now passes the full SpaceChat object so ChatScreen
                    // has the avatar, name, online status without another Firestore lookup
                    onChatClick = { chat ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("chat_object", chat)
                        navController.navigate("chat/${chat.id}")
                    },
                    onStoryClick = { index -> navController.navigate("stories/$index") },
                    onNotificationClick = { navController.navigate("notifications") },
                    onComposeClick = { navController.navigate("compose_post") },
                    onPostClick = { postId -> navController.navigate("post_detail/$postId") },
                    onPinClick = { item -> navController.navigate("pin_detail/${item.id}") },
                    onCreatePinClick = { navController.navigate("create_pin") },
                    onSearchClick = { navController.navigate("search") },
                    onCreateSpaceClick = { navController.navigate("create_space") },
                    onExternalProfileClick = { userId -> navController.navigate("external_profile/$userId") },
                    onSettingsClick = { navController.navigate("settings") },
                    onEditProfileClick = { navController.navigate("edit_profile") },
                    onAddStoryClick = { navController.navigate("create_story") },
                    onCommentClick = { postId -> navController.navigate("post_detail/$postId") },
                    currentUser = currentUserProfile,
                    spacesViewModel = spacesViewModel
                )
            }

            composable("create_pin") {
                CreatePinScreen(onDismiss = { navController.popBackStack() })
            }

            composable("edit_profile") {
                EditProfileScreen(
                    initialUser = currentUserProfile,
                    onBack = { navController.popBackStack() },
                    onSave = { name, bio, location, website, about, skills, institution, themeColor, uri ->
                        userViewModel.updateProfile(
                            name, bio, location, website, about,
                            skills, institution, themeColor, uri, context
                        ) { success ->
                            // Only navigate away once the Firestore write confirmed
                            if (success) navController.popBackStack()
                        }
                    }
                )
            }

            // Chat screen — get SpaceChat from SavedStateHandle, fallback to looking
            // it up from SpacesViewModel by otherUserId, or a minimal placeholder
            composable("chat/{otherUserId}") { backStackEntry ->
                val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""

                // Try to get the full chat object passed from the previous screen
                val savedChat = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<SpaceChat>("chat_object")

                val chat = savedChat
                    ?: spacesViewModel.conversations.find { it.id == otherUserId }
                    ?: SpaceChat(id = otherUserId, name = "Chat")

                ChatScreen(
                    chat = chat,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("stories/{groupIndex}") { backStackEntry ->
                val groupIndex = backStackEntry.arguments?.getString("groupIndex")?.toIntOrNull() ?: 0
                StoryViewer(
                    storyGroups = mockStories.map { user ->
                        StoryGroup(
                            userId = user.id,
                            userAvatar = user.avatarUrl,
                            userName = user.name,
                            stories = user.stories.filterNotExpired().map { Story(it.id, it.url) }
                        )
                    }.filter { it.stories.isNotEmpty() },
                    initialGroupIndex = groupIndex,
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable("create_story") {
                CreateStoryScreen(
                    onDismiss = { navController.popBackStack() },
                    onStoryPosted = { navController.popBackStack() }
                )
            }

            composable("notifications") {
                NotificationScreen(onBack = { navController.popBackStack() })
            }

            composable("compose_post") {
                ComposePostScreen(onDismiss = { navController.popBackStack() })
            }

            composable("post_detail/{postId}") { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                PostDetailScreen(postId = postId, onBack = { navController.popBackStack() })
            }

            composable("search") {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { navController.navigate("external_profile/$it") }
                )
            }

            // Create space — wire SpacesViewModel so tapping a contact creates/opens the chat
            composable("create_space") {
                CreateSpaceScreen(
                    onBack = { navController.popBackStack() },
                    onContactClick = { otherUserId ->
                        // Navigate directly — SpacesViewModel will create the conversation
                        // when the first message is sent (lazy creation)
                        navController.navigate("chat/$otherUserId")
                    }
                )
            }

            composable("external_profile/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                ExternalProfileScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onPostClick = { postId -> navController.navigate("post_detail/$postId") },
                    onChatClick = { targetId -> navController.navigate("chat/$targetId") }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        auth.signOut()
                        navController.navigate("welcome") { popUpTo(0) }
                    },
                    onEditProfile = { navController.navigate("edit_profile") }
                )
            }

            composable("pin_detail/{pinId}") { backStackEntry ->
                val pinId = backStackEntry.arguments?.getString("pinId") ?: ""
                val item = mockDiscover.find { it.id == pinId } ?: mockDiscover.first()
                PinDetailScreen(item = item, onBack = { navController.popBackStack() })
            }
        }

        // Root-level banner — floats over every screen automatically.
        // Red while offline, green "Back online!" for 2.5 s when connection restores.
        OfflineBanner(
            isConnected         = networkViewModel.isConnected.value,
            showRestoredBanner  = networkViewModel.showRestoredBanner.value,
            onRestoredDismissed = { networkViewModel.onRestoredBannerDismissed() }
        )
    }
}
