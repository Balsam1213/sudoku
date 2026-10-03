package com.balsam.sudoku.ui.common

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

/** 分享用的盘面数据。 */
data class ShareBoard(
    val title: String,
    val subtitle: String?,
    val values: List<Int>,
    val givenMask: List<Boolean>,
)

/**
 * 把数独盘面渲染成图片并通过系统分享面板发出。
 * 图片使用固定浅色配色，不随应用主题变化。
 */
object SudokuShare {

    private const val CELL = 120
    private const val GRID = CELL * 9
    private const val PAD = 24
    private const val HEADER = 130

    private val COLOR_BG = Color.WHITE
    private val COLOR_TEXT = Color.parseColor("#17251D")
    private val COLOR_USER = Color.parseColor("#2E9C67")
    private val COLOR_SUBTLE = Color.parseColor("#66766C")
    private val COLOR_LINE = Color.parseColor("#C9D2CC")
    private val COLOR_BOX_LINE = Color.parseColor("#5F6B64")

    fun buildBitmap(data: ShareBoard): Bitmap {
        val width = GRID + PAD * 2
        val height = PAD + HEADER + GRID + PAD
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(COLOR_BG)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SUBTLE
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        val gridLeft = PAD.toFloat()
        val gridTop = (PAD + HEADER).toFloat()

        canvas.drawText(data.title, width / 2f, PAD + 62f, titlePaint)
        data.subtitle?.let {
            canvas.drawText(it, width / 2f, PAD + 112f, subtitlePaint)
        }

        // 格子底色：提示格白，玩家填入格淡绿
        val givenBg = Paint().apply { color = COLOR_BG }
        val userBg = Paint().apply { color = Color.parseColor("#E7F3EC") }
        for (i in 0..80) {
            val r = i / 9
            val c = i % 9
            canvas.drawRect(
                gridLeft + c * CELL,
                gridTop + r * CELL,
                gridLeft + (c + 1) * CELL,
                gridTop + (r + 1) * CELL,
                if (data.givenMask[i]) givenBg else userBg,
            )
        }

        // 网格线
        val thin = Paint().apply {
            color = COLOR_LINE
            strokeWidth = 3f
        }
        val thick = Paint().apply {
            color = COLOR_BOX_LINE
            strokeWidth = 8f
        }
        for (i in 0..9) {
            val offset = i * CELL.toFloat()
            val line = if (i % 3 == 0) thick else thin
            canvas.drawLine(gridLeft, gridTop + offset, gridLeft + GRID, gridTop + offset, line)
            canvas.drawLine(gridLeft + offset, gridTop, gridLeft + offset, gridTop + GRID, line)
        }

        // 数字
        val givenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 68f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val userPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_USER
            textSize = 64f
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = givenPaint.fontMetrics
        val textOffset = -(fontMetrics.ascent + fontMetrics.descent) / 2f
        for (i in 0..80) {
            val v = data.values[i]
            if (v !in 1..9) continue
            val cx = gridLeft + (i % 9) * CELL + CELL / 2f
            val cy = gridTop + (i / 9) * CELL + CELL / 2f + textOffset
            canvas.drawText(v.toString(), cx, cy, if (data.givenMask[i]) givenPaint else userPaint)
        }
        return bitmap
    }

    /** 渲染并调起系统分享面板。 */
    fun share(context: Context, data: ShareBoard) {
        val bitmap = buildBitmap(data)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "sudoku_share.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享数独"))
    }
}
