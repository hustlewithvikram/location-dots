package com.vikram.locationdots.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes a sampled avatar off the main thread to avoid UI stalls for large photos. */
@Composable
fun ProfileAvatar(photoUri: String?, name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = produceState<Bitmap?>(initialValue = null, key1 = photoUri) {
        value = if (photoUri.isNullOrBlank() || photoUri.startsWith("doodle:")) null else withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(photoUri)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null else {
                    val targetSize = 256
                    var sampleSize = 1
                    while (bounds.outWidth / (sampleSize * 2) >= targetSize &&
                        bounds.outHeight / (sampleSize * 2) >= targetSize
                    ) sampleSize *= 2
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                }
            }.getOrNull()
        }
    }
    Surface(modifier = modifier.clip(CircleShape), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        val imageBitmap = bitmap.value
        val doodle = photoUri?.takeIf { it.startsWith("doodle:") }?.removePrefix("doodle:")
        if (doodle != null) {
            val avatarIcon = when (doodle) {
                "eco" -> Icons.Default.Eco
                "night" -> Icons.Default.DarkMode
                "sun" -> Icons.Default.LightMode
                "water" -> Icons.Default.Water
                "flower" -> Icons.Default.LocalFlorist
                "sparkle" -> Icons.Default.AutoAwesome
                "pets" -> Icons.Default.Pets
                "world" -> Icons.Default.Public
                else -> null
            }
            Box(contentAlignment = Alignment.Center) {
                if (avatarIcon != null) {
                    Icon(
                        imageVector = avatarIcon,
                        contentDescription = "Profile avatar",
                        modifier = Modifier.fillMaxSize(0.48f),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(doodle, style = MaterialTheme.typography.headlineMedium)
                }
            }
        } else if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap.asImageBitmap(),
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.trim().firstOrNull()?.uppercase() ?: "L",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
