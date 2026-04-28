package com.example.zell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalAgreementScreen(
    title: String,
    content: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Last updated: October 2024",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = content,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
            
            Spacer(Modifier.height(48.dp))
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("I Understand & Agree")
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

val privacyPolicyContent = """
    1. Introduction
    Welcome to Zell. We respect your privacy and are committed to protecting your personal data.
    
    2. Data We Collect
    We collect information you provide directly to us when you create an account, such as your name, email, and phone number.
    
    3. How We Use Your Data
    We use your data to provide and improve our services, including real-time messaging and social interactions.
    
    4. Data Sharing
    We do not sell your personal data. We share data only with service providers like Firebase for authentication and database services.
    
    5. Your Rights
    You have the right to access, correct, or delete your personal data at any time through the app settings.
""".trimIndent()

val termsOfServiceContent = """
    1. Acceptance of Terms
    By using Zell, you agree to these terms. If you do not agree, do not use the app.
    
    2. User Conduct
    You are responsible for your content and conduct on Zell. Harassment, hate speech, and illegal activities are strictly prohibited.
    
    3. Account Security
    You are responsible for maintaining the confidentiality of your account credentials.
    
    4. Intellectual Property
    Zell and its original content are the exclusive property of Jay's Hub.
    
    5. Termination
    We reserve the right to terminate or suspend your account for violations of these terms.
""".trimIndent()
