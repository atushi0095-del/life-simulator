package com.ajuworks.atonannichi.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max

/**
 * 写真はアプリ専用領域へ縮小コピーして使う（元の写真へのアクセス権が切れても表示が崩れない）。
 * 外部へは送らない。
 */
object Photos {
    private const val MAX_EDGE = 1440

    fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(context: Context, name: String) = File(dir(context), name)

    /** 取り込んで保存したファイル名を返す。失敗したら null。 */
    fun import(context: Context, uri: Uri): String? = runCatching {
        val r = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        r.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val bmp = r.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }!!
        val orientation = runCatching {
            r.openInputStream(uri)!!.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val deg = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val upright = if (deg != 0f) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true) else bmp
        val scale = MAX_EDGE.toFloat() / max(upright.width, upright.height)
        val out = if (scale < 1f) Bitmap.createScaledBitmap(upright, (upright.width * scale).toInt(), (upright.height * scale).toInt(), true) else upright
        val name = "p_${System.currentTimeMillis()}.jpg"
        file(context, name).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        name
    }.getOrNull()

    fun delete(context: Context, name: String?) {
        if (name != null) file(context, name).delete()
    }

    /** 表示用に、目的の大きさ程度まで間引いて読む。 */
    fun load(context: Context, name: String, targetLongEdge: Int): Bitmap? = runCatching {
        val f = file(context, name)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetLongEdge) sample *= 2
        BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
}
