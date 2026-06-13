package com.poem300.ui.screens.quote

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.poem300.data.model.Poem
import com.poem300.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(
    poem: Poem,
    isPremium: Boolean = false,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedStyle by remember { mutableIntStateOf(0) }
    val styles = listOf("Minimal", "Classic", "Warm")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Card") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        sharePoemCard(context, poem, selectedStyle, isPremium)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Style selector — using Row of FilterChip as fallback for compatibility
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                styles.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedStyle == index,
                        onClick = { selectedStyle = index },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card preview
            PoemQuoteCard(
                poem = poem,
                style = selectedStyle,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PoemQuoteCard(
    poem: Poem,
    style: Int,
    modifier: Modifier = Modifier
) {
    val bgColor = when (style) {
        0 -> Color(0xFFFAF8F5)  // Minimal - warm white
        1 -> Color(0xFF2C3E50)  // Classic - deep blue
        else -> Color(0xFFF5E6D3)  // Warm - cream
    }
    val textColor = when (style) {
        0 -> Color(0xFF2C2C2C)
        1 -> Color(0xFFECF0F1)
        else -> Color(0xFF5D4E37)
    }
    val accentColor = when (style) {
        0 -> Color(0xFF5B7B8A)
        1 -> Color(0xFFC9A96E)
        else -> Color(0xFFC4704B)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .aspectRatio(0.75f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = poem.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = poem.titleEn,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(1.dp)
                        .background(accentColor.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Content (first 2-3 lines)
                val lines = poem.content.split("\n").take(4)
                lines.forEach { line ->
                    Text(
                        text = line,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }

                if (poem.content.split("\n").size > 4) {
                    Text(
                        text = "···",
                        fontSize = 18.sp,
                        color = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Divider
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(1.dp)
                        .background(accentColor.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Author
                Text(
                    text = "${poem.authorEn} · ${poem.dynastyEn} Dynasty",
                    fontSize = 12.sp,
                    color = accentColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // App branding
                Text(
                    text = "300 Tang Poems",
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun sharePoemCard(context: Context, poem: Poem, style: Int, isPremium: Boolean) {
    try {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgColor = when (style) {
            0 -> android.graphics.Color.parseColor("#FAF8F5")
            1 -> android.graphics.Color.parseColor("#2C3E50")
            else -> android.graphics.Color.parseColor("#F5E6D3")
        }
        canvas.drawColor(bgColor)

        val textColor = when (style) {
            0 -> android.graphics.Color.parseColor("#2C2C2C")
            1 -> android.graphics.Color.parseColor("#ECF0F1")
            else -> android.graphics.Color.parseColor("#5D4E37")
        }
        val accentColor = when (style) {
            0 -> android.graphics.Color.parseColor("#5B7B8A")
            1 -> android.graphics.Color.parseColor("#C9A96E")
            else -> android.graphics.Color.parseColor("#C4704B")
        }

        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        var y = 200f

        // Title
        paint.apply {
            color = textColor
            textSize = 72f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText(poem.title, width / 2f, y, paint)
        y += 60f

        // English title
        paint.apply {
            color = (textColor and 0xFFFFFF) or (0x99 shl 24) // 60% alpha
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText(poem.titleEn, width / 2f, y, paint)
        y += 100f

        // Divider
        paint.color = (accentColor and 0xFFFFFF) or (0x80 shl 24) // 50% alpha
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(width / 2f - 100, y, width / 2f + 100, y, paint)
        y += 80f

        // Content
        paint.color = textColor
        paint.textSize = 48f
        paint.typeface = Typeface.DEFAULT
        paint.style = Paint.Style.FILL
        val lines = poem.content.split("\n").take(4)
        lines.forEach { line ->
            canvas.drawText(line, width / 2f, y, paint)
            y += 70f
        }

        if (poem.content.split("\n").size > 4) {
            paint.color = (textColor and 0xFFFFFF) or (0x80 shl 24) // 50% alpha
            canvas.drawText("···", width / 2f, y, paint)
            y += 80f
        }

        y += 40f

        // Divider
        paint.color = (accentColor and 0xFFFFFF) or (0x80 shl 24) // 50% alpha
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(width / 2f - 100, y, width / 2f + 100, y, paint)
        y += 60f

        // Author
        paint.color = accentColor
        paint.textSize = 30f
        paint.typeface = Typeface.DEFAULT
        paint.style = Paint.Style.FILL
        canvas.drawText("${poem.authorEn} · ${poem.dynastyEn} Dynasty", width / 2f, y, paint)
        y += 80f

        // Branding
        paint.apply {
            color = (textColor and 0xFFFFFF) or (0x4D shl 24) // 30% alpha
            textSize = 24f
        }
        canvas.drawText("300 Tang Poems", width / 2f, y, paint)

        // Watermark for free users
        if (!isPremium) {
            val wmY = height - 120f
            paint.color = (textColor and 0xFFFFFF) or (0x66 shl 24) // 40% alpha
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Get Premium — No Watermark", width / 2f, wmY, paint)
            paint.textSize = 22f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("300 Tang Poems · $0.99 one time", width / 2f, wmY + 36f, paint)
        }

        // Save and share
        val cachePath = File(context.cacheDir, "shared_poems")
        cachePath.mkdirs()
        val file = File(cachePath, "poem_${poem.id!!}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share this poem"))

    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't create card. Try again?", Toast.LENGTH_SHORT).show()
    }
}
