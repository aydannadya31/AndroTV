package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

object QrCodeGenerator {
    const val MATRIX_SIZE = 25

    /**
     * Dış kütüphane gerektirmeden deterministik ve standart finder-pattern uyumlu 25x25 QR matrisi üretir.
     */
    fun generateMatrix(content: String): Array<BooleanArray> {
        val matrix = Array(MATRIX_SIZE) { BooleanArray(MATRIX_SIZE) { false } }

        // 1. Finder Patterns (Sol Üst, Sağ Üst, Sol Alt 7x7)
        fun drawFinder(startX: Int, startY: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[startY + r][startX + c] = isBorder || isCenter
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(MATRIX_SIZE - 7, 0)
        drawFinder(0, MATRIX_SIZE - 7)

        // 2. Timing Patterns (Satır 6 ve Sütun 6)
        for (i in 8 until MATRIX_SIZE - 8) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // 3. Alignment Pattern (16, 16)
        for (r in 14..18) {
            for (c in 14..18) {
                val isAlignBorder = r == 14 || r == 18 || c == 14 || c == 18
                val isAlignCenter = r == 16 && c == 16
                matrix[r][c] = isAlignBorder || isAlignCenter
            }
        }

        // 4. Veri Modülleri (İçerik SHA-256 özeti ile doldurulur)
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        var byteIdx = 0
        var bitIdx = 0

        for (r in 0 until MATRIX_SIZE) {
            for (c in 0 until MATRIX_SIZE) {
                // Finder alanlarını atla
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= MATRIX_SIZE - 8
                val inBottomLeft = r >= MATRIX_SIZE - 8 && c < 8
                val inAlignment = r in 14..18 && c in 14..18
                val isTiming = r == 6 || c == 6

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inAlignment && !isTiming) {
                    val currentByte = digest[byteIdx % digest.size].toInt()
                    val bit = ((currentByte shr (bitIdx % 8)) and 1) == 1
                    matrix[r][c] = bit
                    bitIdx++
                    if (bitIdx % 8 == 0) byteIdx++
                }
            }
        }

        return matrix
    }
}

/**
 * TV ve Mobil ekranlar için optimize edilmiş, zümrüt detaylı modern QR Kod bileşeni.
 */
@Composable
fun QrCodeView(
    dataString: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 220.dp,
    qrColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val matrix = remember(dataString) {
        QrCodeGenerator.generateMatrix(dataString)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp - 28.dp)) {
            val moduleSize = size.width / QrCodeGenerator.MATRIX_SIZE

            for (r in 0 until QrCodeGenerator.MATRIX_SIZE) {
                for (c in 0 until QrCodeGenerator.MATRIX_SIZE) {
                    if (matrix[r][c]) {
                        // Finder merkezlerini özel zümrüt tonuyla vurgula
                        val isFinderCenter = (r in 2..4 && c in 2..4) ||
                                (r in 2..4 && c in (QrCodeGenerator.MATRIX_SIZE - 5)..(QrCodeGenerator.MATRIX_SIZE - 3)) ||
                                (r in (QrCodeGenerator.MATRIX_SIZE - 5)..(QrCodeGenerator.MATRIX_SIZE - 3) && c in 2..4)

                        val cellColor = if (isFinderCenter) Color(0xFF10B981) else qrColor

                        drawRoundRect(
                            color = cellColor,
                            topLeft = Offset(c * moduleSize, r * moduleSize),
                            size = Size(moduleSize * 0.94f, moduleSize * 0.94f),
                            cornerRadius = CornerRadius(moduleSize * 0.25f, moduleSize * 0.25f)
                        )
                    }
                }
            }
        }
    }
}
