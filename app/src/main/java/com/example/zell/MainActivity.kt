package com.example.zell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.zell.ui.theme.ZellTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            ZellTheme {
                ZellApp()
            }
        }
    }
}

@Composable
fun ZellApp() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    
    var currentUserProfile by remember { 
        mutableStateOf(
            StoryUser(
                id = auth.currentUser?.uid ?: "0", 
                name = "Amara Osei", 
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                stories = emptyList(),
                hasUnread = false,
                isYou = true
            )
        ) 
    }

    // Real-time listener for current user's profile
    LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            snapshot?.toObject(StoryUser::class.java)?.let {
                currentUserProfile = it.copy(isYou = true)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "welcome",
        enterTransition = { fadeIn(tween(300)) + slideInHorizontally { it / 2 } },
        exitTransition = { fadeOut(tween(300)) + slideOutHorizontally { -it / 2 } }
    ) {
        composable("welcome") {
            WelcomeScreen(onTimeout = {
                val target = if (auth.currentUser != null) "dashboard" else "onboard"
                navController.navigate(target) { popUpTo("welcome") { inclusive = true } }
            })
        }

        composable("onboard") { OnBoardScreen { navController.navigate("signup_flow") } }

        composable("signup_flow") {
            SignUpFlow(
                onSignInClick = { navController.popBackStack() },
                onComplete = { navController.navigate("otp") { popUpTo("onboard") { inclusive = true } } }
            )
        }

        composable("otp") {
            OtpScreen(
                phoneNumber = "+234 810 000 0000",
                onBack = { navController.popBackStack() },
                onNavigateToProfile = {
                    navController.navigate("profile") { popUpTo("signup_flow") { inclusive = true } }
                }
            )
        }

        composable("profile") { ProfileSetupScreen { navController.navigate("dashboard") { popUpTo("welcome") { inclusive = true } } } }

        composable("dashboard") {
            MainDashboard(
                onChatClick = { chatId -> navController.navigate("chat/$chatId") },
                onStoryClick = { index -> navController.navigate("stories/$index") },
                onNotificationClick = { navController.navigate("notifications") },
                onComposeClick = { navController.navigate("compose_post") },
                onPostClick = { postId -> navController.navigate("post_detail/$postId") },
                onPinClick = { item -> navController.navigate("pin_detail/${item.id}") },
                onSearchClick = { navController.navigate("search") },
                onCreateSpaceClick = { navController.navigate("create_space") },
                onExternalProfileClick = { userId -> navController.navigate("external_profile/$userId") },
                onSettingsClick = { navController.navigate("settings") },
                onEditProfileClick = { navController.navigate("edit_profile") },
                onAddStoryClick = { navController.navigate("create_story") },
                currentUser = currentUserProfile
            )
        }

        composable("edit_profile") {
            EditProfileScreen(
                initialUser = currentUserProfile,
                onBack = { navController.popBackStack() },
                onSave = { name, bio, uri ->
                    val uid = auth.currentUser?.uid ?: "0"
                    val updated = currentUserProfile.copy(name = name, bio = bio, avatarUrl = uri?.toString() ?: currentUserProfile.avatarUrl)
                    db.collection("users").document(uid).set(updated)
                    navController.popBackStack()
                }
            )
        }

        composable("chat/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(chat = mockSpaces.find { it.id == chatId } ?: mockSpaces.first(), onBack = { navController.popBackStack() })
        }

        composable("stories/{groupIndex}") { backStackEntry ->
            val groupIndex = backStackEntry.arguments?.getString("groupIndex")?.toIntOrNull() ?: 0
            StoryViewer(
                storyGroups = mockStories.map { user -> 
                    StoryGroup(userId = user.id, userAvatar = user.avatarUrl, userName = user.name, stories = user.stories.map { Story(it.id, it.url) })
                }.filter { it.stories.isNotEmpty() },
                initialGroupIndex = groupIndex,
                onDismiss = { navController.popBackStack() }
            )
        }

        composable("create_story") {
            CreateStoryScreen(
                onDismiss = { navController.popBackStack() },
                onStoryPosted = { uri ->
                    if (uri != null) {
                        val uid = auth.currentUser?.uid ?: "0"
                        val newStory = StoryItem(url = uri.toString())
                        db.collection("users").document(uid).get().addOnSuccessListener {
                            val user = it.toObject(StoryUser::class.java) ?: currentUserProfile
                            db.collection("users").document(uid).set(user.copy(stories = user.stories + newStory))
                        }
                    }
                    navController.popBackStack()
                }
            )
        }

        composable("notifications") { NotificationScreen { navController.popBackStack() } }
        composable("compose_post") { ComposePostScreen(onDismiss = { navController.popBackStack() }, onPostSuccess = { _, _, _ -> navController.popBackStack() }) }
        composable("post_detail/{postId}") { PostDetailScreen(postId = it.arguments?.getString("postId") ?: "", onBack = { navController.popBackStack() }, onLikeClick = {}, onRepostClick = {}, onBookmarkClick = {}) }
        composable("search") { SearchScreen(onBack = { navController.popBackStack() }, onPostClick = {}, onUserClick = {}) }
        composable("create_space") { CreateSpaceScreen(onBack = { navController.popBackStack() }, onContactClick = { navController.navigate("chat/$it") }) }
        composable("external_profile/{userId}") { ExternalProfileScreen(userId = it.arguments?.getString("userId") ?: "", onBack = { navController.popBackStack() }, onPostClick = {}, onChatClick = {}) }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }, onLogout = { auth.signOut(); navController.navigate("welcome") { popUpTo(0) } }) }
        composable("pin_detail/{pinId}") { PinDetailScreen(item = mockDiscover.first(), onBack = { navController.popBackStack() }) }
    }
}
