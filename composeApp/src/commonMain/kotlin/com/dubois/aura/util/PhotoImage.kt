package com.dubois.aura.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Encodage / décodage des photos de profil stockées en base64 dans Firestore.
 * Les images sont réduites en miniature avant encodage pour tenir largement
 * sous la limite de 1 Mo d'un document Firestore.
 */

@OptIn(ExperimentalEncodingApi::class)
fun encodeImageBase64(bytes: ByteArray): String = Base64.Default.encode(bytes)

/** Décode une chaîne base64 en [ImageBitmap], ou null si vide/invalide. */
@OptIn(ExperimentalEncodingApi::class)
fun decodeImageBitmap(base64: String): ImageBitmap? =
    if (base64.isBlank()) {
        null
    } else {
        try {
            Base64.Default.decode(base64).decodeToImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
