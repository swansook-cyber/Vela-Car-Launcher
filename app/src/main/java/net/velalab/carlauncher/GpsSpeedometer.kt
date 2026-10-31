package net.velalab.carlauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import java.util.Locale

/** Visual scale only: never draw a fabricated live speed. */
@Composable
fun GpsSpeedometer(speedKmh: Float?, status: String, modifier: Modifier = Modifier, unit: String = "km/h") {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * .43f
            val center = Offset(size.width / 2, size.height / 2)
            val start = 145f
            val sweep = 250f
            drawArc(Color(0xFF29445D), start, sweep, false, topLeft = Offset(center.x-radius, center.y-radius), size = androidx.compose.ui.geometry.Size(radius*2, radius*2), style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
            if (speedKmh != null) {
                drawArc(Color(0xFF16B7FF), start, sweep * (speedKmh / 200f).coerceIn(0f,1f), false, topLeft = Offset(center.x-radius, center.y-radius), size = androidx.compose.ui.geometry.Size(radius*2, radius*2), style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
            }
            for (n in 0..20) {
                val angle = Math.toRadians((start + sweep*n/20).toDouble())
                val outer = radius - 19.dp.toPx()
                val inner = radius - (if(n%2==0) 38.dp.toPx() else 29.dp.toPx())
                val x1 = center.x + outer*cos(angle).toFloat()
                val y1 = center.y + outer*sin(angle).toFloat()
                val x2 = center.x + inner*cos(angle).toFloat()
                val y2 = center.y + inner*sin(angle).toFloat()
                drawLine(if(n%2==0) Color.White else Color(0xFF6B8EAB), Offset(x1,y1), Offset(x2,y2), if(n%2==0) 2.dp.toPx() else 1.dp.toPx())
                if (n % 2 == 0) {
                    val labelRadius = radius - 56.dp.toPx()
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 13.sp.toPx()
                    }
                    drawContext.canvas.nativeCanvas.drawText("${n * 10}", center.x + labelRadius*cos(angle).toFloat(), center.y + labelRadius*sin(angle).toFloat() + 5.dp.toPx(), paint)
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if(speedKmh==null) "—" else String.format(Locale.US,"%.0f",speedKmh), color=Color.White, fontSize=74.sp, fontWeight=FontWeight.Bold)
            Text(unit, color=Color(0xFF16B7FF), fontSize=23.sp)
            Text(status, color=Color.LightGray, fontSize=12.sp)
        }
    }
}
