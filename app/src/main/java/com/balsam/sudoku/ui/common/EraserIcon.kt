package com.balsam.sudoku.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 橡皮擦图标（描边风格，随 Icon tint 着色）。 */
val EraserIcon: ImageVector = ImageVector.Builder(
    name = "Eraser",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(11.58f, 4.88f)
        lineTo(17.52f, 10.82f)
        lineTo(17.52f, 13.37f)
        lineTo(14.97f, 15.92f)
        lineTo(12.42f, 15.92f)
        lineTo(6.48f, 9.98f)
        lineTo(6.48f, 7.43f)
        lineTo(9.03f, 4.88f)
        close()
        moveTo(14.76f, 8.49f)
        lineTo(10.09f, 13.16f)
        moveTo(4.8f, 11.96f)
        lineTo(9.74f, 16.9f)
    }
}.build()
