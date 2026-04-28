package com.example.zell

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

object ShareUtils {
    fun sharePost(context: Context, content: String, postId: String) {
        val postLink = "https://zell.app/post/$postId"
        val shareText = "$content\n\nRead more on Zell: $postLink"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share post via"))
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Zell Link", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
