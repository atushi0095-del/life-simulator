package com.ajuworks.atonannichi.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajuworks.atonannichi.core.Design
import com.ajuworks.atonannichi.data.EventEntity
import com.ajuworks.atonannichi.widget.CardRenderer
import com.ajuworks.atonannichi.widget.Labels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** アプリ内のカード。背景はウィジェットと同じ描画を使う。 */
@Composable
fun EventCard(event: EventEntity, today: LocalDate, modifier: Modifier = Modifier, design: Design = event.designEnum, onClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val eff = CardRenderer.effectiveDesign(event, design)
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.9f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(eff.bgTop), Color(eff.bgBottom))))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }.toInt().coerceAtMost(1000)
        val hPx = (wPx / 1.9f).toInt()
        val bmp by produceState<Bitmap?>(null, event.id, event.photoFile, eff, wPx) {
            value = withContext(Dispatchers.Default) { CardRenderer.render(context, event, design, wPx, hPx, 0f) }
        }
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }

        val labels = Labels.of(context, event, today)
        val shadow = if (eff == Design.PHOTO) Shadow(Color(0x99000000), Offset(0f, 2f), 8f) else null
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.icon, fontSize = 22.sp)
                Text(
                    "  " + labels.headline,
                    color = Color(eff.subText),
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, shadow = shadow),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    labels.number,
                    color = Color(eff.text),
                    style = TextStyle(
                        fontSize = (if (labels.bigWord) 44 else 64).sp * eff.numberScale.coerceAtMost(1.2f),
                        fontWeight = FontWeight.Black,
                        shadow = shadow,
                    ),
                    maxLines = 1,
                )
                if (labels.unit.isNotEmpty()) {
                    Text(
                        " " + labels.unit,
                        color = Color(eff.accent),
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, shadow = shadow),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
            Text(labels.dateLine, color = Color(eff.subText), style = TextStyle(fontSize = 14.sp, letterSpacing = 1.sp, shadow = shadow))
        }
    }
}

@Composable
fun DesignSwatch(design: Design, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(design.bgTop), Color(design.bgBottom))))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            (if (selected) "✓ " else "") + design.name.lowercase().replaceFirstChar { it.uppercase() },
            color = Color(design.text),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
