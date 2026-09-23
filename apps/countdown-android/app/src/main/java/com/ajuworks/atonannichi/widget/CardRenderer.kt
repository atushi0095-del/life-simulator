package com.ajuworks.atonannichi.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.ajuworks.atonannichi.core.Design
import com.ajuworks.atonannichi.core.Readability
import com.ajuworks.atonannichi.data.EventEntity
import com.ajuworks.atonannichi.data.Photos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale

/**
 * カードの背景を描く（ウィジェットとアプリ内で共通）。文字は重ねない。
 *
 * 写真の場合の読みやすさ対策:
 * - 下側ほどぼかした写真に切り替える（文字の背後の細かい模様を消す）
 * - 写真の明るさに応じた暗幕（明るい写真ほど濃く）と、下へ向かって濃くなるグラデーション
 * - 文字側では影を付ける（レイアウトで指定）
 */
object CardRenderer {
    /** ウィジェット1枚あたりの画素数の上限。RemoteViews のプロセス間転送（約1MB）に収める。 */
    const val MAX_WIDGET_PIXELS = 180_000

    fun effectiveDesign(event: EventEntity?, design: Design): Design =
        if (design == Design.PHOTO && event?.photoFile == null) Design.SOFT else design

    fun render(context: Context, event: EventEntity?, design: Design, width: Int, height: Int, cornerPx: Float): Bitmap {
        val w = width.coerceAtLeast(8)
        val h = height.coerceAtLeast(8)
        val out = createBitmap(w, h)
        val canvas = Canvas(out)
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerPx, cornerPx, Path.Direction.CW) }
        canvas.clipPath(clip)

        val d = effectiveDesign(event, design)
        val photo = if (d == Design.PHOTO) event?.photoFile?.let { Photos.load(context, it, max(w, h)) } else null
        if (photo != null) {
            drawPhoto(canvas, photo, w, h)
            photo.recycle()
        } else {
            drawGradient(canvas, d, w, h)
        }
        return out
    }

    private fun drawGradient(canvas: Canvas, d: Design, w: Int, h: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, w * 0.3f, h.toFloat(), d.bgTop, d.bgBottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        // 控えめな飾り（右上の円）。単色より奥行きが出る
        if (d != Design.MINIMAL) {
            val deco = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(28, 255, 255, 255) }
            val r = max(w, h) * 0.55f
            canvas.drawCircle(w * 0.95f, h * -0.05f, r, deco)
        }
    }

    private fun drawPhoto(canvas: Canvas, photo: Bitmap, w: Int, h: Int) {
        val sharp = centerCrop(photo, w, h)
        // 縮小→拡大でぼかした版
        val small = sharp.scale((w / 12).coerceAtLeast(2), (h / 12).coerceAtLeast(2))
        val blurred = small.scale(w, h)
        small.recycle()
        canvas.drawBitmap(blurred, 0f, 0f, null)
        blurred.recycle()

        // 上はくっきり、下へ行くほどぼかし版が見える
        val layer = canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), null)
        canvas.drawBitmap(sharp, 0f, 0f, null)
        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, h * 0.35f, 0f, h.toFloat(), Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), mask)
        canvas.restoreToCount(layer)

        val alpha = Readability.scrimAlpha(meanLuminance(sharp))
        sharp.recycle()
        val scrim = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                Color.argb((alpha * 0.55f * 255).roundToInt(), 0, 0, 0),
                Color.argb((alpha * 255).roundToInt(), 0, 0, 0),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), scrim)
    }

    private fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        val scale = max(w.toFloat() / src.width, h.toFloat() / src.height)
        val cw = (w / scale).roundToInt().coerceIn(1, src.width)
        val ch = (h / scale).roundToInt().coerceIn(1, src.height)
        val left = (src.width - cw) / 2
        val top = (src.height - ch) / 2
        val out = createBitmap(w, h)
        Canvas(out).drawBitmap(src, Rect(left, top, left + cw, top + ch), Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private fun meanLuminance(b: Bitmap): Double {
        val s = b.scale(16, 16)
        var sum = 0.0
        for (y in 0 until 16) for (x in 0 until 16) {
            val c = s[x, y]
            sum += Readability.luminance(Color.red(c), Color.green(c), Color.blue(c))
        }
        s.recycle()
        return sum / 256
    }

    /** ウィジェットの dp サイズから、上限内に収まる画素数を決める。 */
    fun widgetPixels(widthDp: Int, heightDp: Int, density: Float): Pair<Int, Int> {
        var w = (widthDp * density).coerceAtLeast(40f)
        var h = (heightDp * density).coerceAtLeast(40f)
        val px = w * h
        if (px > MAX_WIDGET_PIXELS) {
            val k = sqrt(MAX_WIDGET_PIXELS / px)
            w *= k
            h *= k
        }
        return w.roundToInt() to h.roundToInt()
    }
}
