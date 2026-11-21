package com.dubois.aura.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun shareProfileCard(
    context: Context,
    name: String,
    tierName: String,
    score: Int,
    rank: Int,
    tierHex: String,
) {
    val bitmap = drawProfileCard(name, tierName, score, rank, tierHex)
    shareCard(context, bitmap, "Mon aura sur Aura 🔥")
}

fun shareBattleCard(
    context: Context,
    hostName: String,
    battleScore: Int,
    supporters: Int,
    bestCombo: Int,
    auraAwarded: Int,
    accentHex: String,
) {
    val bitmap = drawBattleCard(hostName, battleScore, supporters, bestCombo, auraAwarded, accentHex)
    shareCard(context, bitmap, "Ma battle d'aura ⚔️")
}

fun shareGroupInvite(context: Context, groupName: String, token: String) {
    val text = "Rejoins mon groupe \"$groupName\" sur Aura 🔥\n\n" +
        "Ouvre ce lien si tu as l'app :\naura://join/$token\n\n" +
        "Sans l'app, télécharge Aura :\nhttps://play.google.com/store/apps/details?id=com.dubois.aura"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Inviter dans le groupe"))
}

fun inviteToBattle(context: Context, hostName: String) {
    val text = "🔥 $hostName est en battle d'aura en ce moment ! Rejoins la hype sur l'appli Aura !"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Inviter des amis"))
}

// ── Dessin des cartes ──────────────────────────────────────────────────────────

private fun parseHex(hex: String): Int = try {
    Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
} catch (_: Exception) {
    Color.parseColor("#FFA31A")
}

private fun formatScore(score: Int): String =
    score.toString().reversed().chunked(3).joinToString(" ").reversed()

private fun drawProfileCard(name: String, tierName: String, score: Int, rank: Int, tierHex: String): Bitmap {
    val w = 1080; val h = 1080
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val tierColor = parseHex(tierHex)

    // Background dégradé sombre
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.parseColor("#1E1005"), Color.parseColor("#0C0906")),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
    })

    // Halo de couleur de tier
    c.drawCircle(w / 2f, h * 0.42f, h * 0.44f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            w / 2f, h * 0.42f, h * 0.44f,
            intArrayOf(Color.argb(50, Color.red(tierColor), Color.green(tierColor), Color.blue(tierColor)), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
    })

    // Titre
    c.drawText("⚡  AURA  ⚡", w / 2f, 170f, textPaint(80f, tierColor, center = true))

    // Nom
    c.drawText(name.take(20), w / 2f, 310f, textPaint(68f, Color.parseColor("#FBF4E8"), bold = true, serif = true, center = true))

    // Tier
    c.drawText(tierName.uppercase(), w / 2f, 390f, textPaint(42f, tierColor, letterSpacing = 0.12f, center = true))

    // Score
    c.drawText(formatScore(score), w / 2f, 570f, textPaint(132f, tierColor, bold = true, serif = true, center = true))

    // Label AURA
    c.drawText("A U R A", w / 2f, 635f, textPaint(34f, Color.parseColor("#7A6D5D"), letterSpacing = 0.25f, center = true))

    // Rang
    c.drawText("Rang  #$rank", w / 2f, 730f, textPaint(44f, Color.parseColor("#AB9D8A"), center = true))

    // Séparateur
    c.drawLine(w * 0.12f, 820f, w * 0.88f, 820f, Paint().apply {
        color = Color.parseColor("#33261A"); strokeWidth = 2f
    })

    // Footer
    c.drawText("🔥 Rejoins-moi sur l'appli Aura", w / 2f, 940f, textPaint(38f, Color.parseColor("#7A6D5D"), center = true))

    return bmp
}

private fun drawBattleCard(
    hostName: String,
    battleScore: Int,
    supporters: Int,
    bestCombo: Int,
    auraAwarded: Int,
    accentHex: String,
): Bitmap {
    val w = 1080; val h = 1080
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val accent = parseHex(accentHex)

    // Background
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.parseColor("#1E1005"), Color.parseColor("#0C0906")),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
    })

    // Halo accent
    c.drawCircle(w / 2f, h * 0.40f, h * 0.44f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            w / 2f, h * 0.40f, h * 0.44f,
            intArrayOf(Color.argb(50, Color.red(accent), Color.green(accent), Color.blue(accent)), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
    })

    // Titre
    c.drawText("⚔️  BATTLE D'AURA  ⚔️", w / 2f, 170f, textPaint(60f, accent, center = true))

    // Hôte
    c.drawText(hostName.take(20), w / 2f, 310f, textPaint(68f, Color.parseColor("#FBF4E8"), bold = true, serif = true, center = true))

    // Score
    c.drawText(formatScore(battleScore), w / 2f, 490f, textPaint(132f, accent, bold = true, serif = true, center = true))
    c.drawText("BATTLE SCORE", w / 2f, 555f, textPaint(32f, Color.parseColor("#7A6D5D"), letterSpacing = 0.2f, center = true))

    // Aura gagnée
    if (auraAwarded > 0) {
        c.drawText("+${formatScore(auraAwarded)} aura gagnée", w / 2f, 650f, textPaint(48f, Color.parseColor("#4ADE80"), center = true))
    }

    // Stats
    val statsY = if (auraAwarded > 0) 740f else 670f
    c.drawText("$supporters supporters  ·  combo x$bestCombo", w / 2f, statsY, textPaint(40f, Color.parseColor("#AB9D8A"), center = true))

    // Séparateur
    c.drawLine(w * 0.12f, 820f, w * 0.88f, 820f, Paint().apply {
        color = Color.parseColor("#33261A"); strokeWidth = 2f
    })

    // Footer
    c.drawText("⚡ Rejoins la prochaine battle sur Aura", w / 2f, 940f, textPaint(38f, Color.parseColor("#7A6D5D"), center = true))

    return bmp
}

// ── Helpers Paint ──────────────────────────────────────────────────────────────

private fun textPaint(
    size: Float,
    color: Int,
    bold: Boolean = false,
    serif: Boolean = false,
    letterSpacing: Float = 0f,
    center: Boolean = false,
): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    textSize = size
    if (center) textAlign = Paint.Align.CENTER
    typeface = when {
        serif && bold -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        serif -> Typeface.SERIF
        bold -> Typeface.DEFAULT_BOLD
        else -> Typeface.DEFAULT
    }
    if (letterSpacing != 0f) this.letterSpacing = letterSpacing
}

// ── Partage ────────────────────────────────────────────────────────────────────

private fun shareCard(context: Context, bitmap: Bitmap, chooserTitle: String) {
    val file = File(context.cacheDir, "aura_share.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "🔥 Télécharge l'appli Aura !")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
