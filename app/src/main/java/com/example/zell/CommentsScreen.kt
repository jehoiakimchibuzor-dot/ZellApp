package com.example.zell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatar: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val isLiked: Boolean = false,
    val replies: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    postId: String,
    onBackClick: () -> Unit,
    currentUser: StoryUser
) {
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var newCommentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isPostingComment by remember { mutableStateOf(false) }
    var appError by remember { mutableStateOf<AppError?>(null) }

    LaunchedEffect(postId) {
        try {
            appError = null
            val commentsSnapshot = RetryHelper.firebaseRetry {
                db.collection("posts").document(postId).collection("comments")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
            }
            comments = commentsSnapshot.documents.mapNotNull { it.toObject(Comment::class.java) }
            isLoading = false
        } catch (e: Exception) {
            appError = ErrorHandler.classifyException(e)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comments", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (appError != null) {
                ErrorBanner(error = appError, onDismiss = { appError = null })
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else if (comments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No comments yet. Be the first!", 
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    items(comments) { comment ->
                        CommentItem(
                            comment = comment,
                            postId = postId,
                            onLikeClick = { commentId ->
                                scope.launch {
                                    try {
                                        val index = comments.indexOfFirst { it.id == commentId }
                                        if (index >= 0) {
                                            val updatedComment = comments[index].copy(
                                                isLiked = !comments[index].isLiked,
                                                likes = if (comments[index].isLiked) comments[index].likes - 1 else comments[index].likes + 1
                                            )
                                            val newCommentsList = comments.toMutableList()
                                            newCommentsList[index] = updatedComment
                                            comments = newCommentsList

                                            RetryHelper.firebaseRetry {
                                                db.collection("posts").document(postId).collection("comments")
                                                    .document(commentId)
                                                    .update("likes", updatedComment.likes).await()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        appError = ErrorHandler.classifyException(e)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(currentUser.avatarUrl),
                    contentDescription = "Your Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Add a comment...") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = false,
                    enabled = !isPostingComment,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            isPostingComment = true
                            appError = null
                            scope.launch {
                                try {
                                    val newComment = Comment(
                                        id = UUID.randomUUID().toString(),
                                        postId = postId,
                                        authorId = currentUser.id,
                                        authorName = currentUser.name,
                                        authorHandle = "@${currentUser.name.lowercase().replace(" ", "")}",
                                        authorAvatar = currentUser.avatarUrl,
                                        content = newCommentText,
                                        timestamp = System.currentTimeMillis()
                                    )

                                    RetryHelper.firebaseRetry {
                                        db.collection("posts").document(postId).collection("comments")
                                            .document(newComment.id)
                                            .set(newComment)
                                            .await()

                                        val postsRef = db.collection("posts").document(postId)
                                        val doc = postsRef.get().await()
                                        val currentComments = doc.getLong("comments")?.toInt() ?: 0
                                        postsRef.update("comments", currentComments + 1).await()
                                    }

                                    comments = listOf(newComment) + comments
                                    newCommentText = ""
                                    CrashlyticsLogger.i("CommentsScreen", "Comment posted successfully")
                                } catch (e: Exception) {
                                    appError = ErrorHandler.classifyException(e)
                                    CrashlyticsLogger.e("CommentsScreen", "Failed to post comment", e)
                                } finally {
                                    isPostingComment = false
                                }
                            }
                        }
                    },
                    enabled = newCommentText.isNotBlank() && !isPostingComment
                ) {
                    if (isPostingComment) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, 
                            contentDescription = "Send", 
                            tint = if (newCommentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    postId: String,
    onLikeClick: (String) -> Unit
) {
    var isLiked by remember { mutableStateOf(comment.isLiked) }
    var likesCount by remember { mutableStateOf(comment.likes) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = rememberAsyncImagePainter(comment.authorAvatar),
                    contentDescription = comment.authorName,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        comment.authorName, 
                        fontWeight = FontWeight.Bold, 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        comment.authorHandle, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    formatTimestamp(comment.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                comment.content, 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        likesCount = if (isLiked) likesCount + 1 else likesCount - 1
                        onLikeClick(comment.id)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(18.dp),
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = likesCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Reply",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = comment.replies.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
