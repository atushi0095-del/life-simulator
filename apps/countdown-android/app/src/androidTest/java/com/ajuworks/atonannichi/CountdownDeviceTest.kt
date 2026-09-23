package com.ajuworks.atonannichi

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ajuworks.atonannichi.core.Design
import com.ajuworks.atonannichi.core.NotifyRules
import com.ajuworks.atonannichi.data.EventDatabase
import com.ajuworks.atonannichi.data.EventEntity
import com.ajuworks.atonannichi.data.Photos
import com.ajuworks.atonannichi.data.WidgetConfigStore
import com.ajuworks.atonannichi.widget.CardRenderer
import com.ajuworks.atonannichi.widget.Labels
import com.ajuworks.atonannichi.widget.Tick
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CountdownDeviceTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        EventDatabase.get(context).clearAllTables()
    }

    private fun photo(): String {
        val bmp = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(250, 240, 200)) }
        val f = File(context.cacheDir, "src.jpg")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Photos.import(context, Uri.fromFile(f))!!
    }

    @Test
    fun crudAndLabels() = runBlocking {
        val dao = EventDatabase.get(context).dao()
        val today = LocalDate.now()
        val id = dao.upsert(EventEntity(title = "沖縄旅行", dateEpochDay = today.plusDays(42).toEpochDay()))
        val e = dao.byId(id)!!
        val l = Labels.of(context, e, today)
        assertEquals("42", l.number)
        assertTrue(l.headline.contains("沖縄旅行"))
        dao.upsert(e.copy(dateEpochDay = today.toEpochDay()))
        assertTrue(Labels.of(context, dao.byId(id)!!, today).bigWord)
        dao.delete(id)
        assertNull(dao.byId(id))
    }

    @Test
    fun rendererAllDesignsWithAndWithoutPhoto() {
        val name = photo()
        val withPhoto = EventEntity(title = "a", dateEpochDay = 0, photoFile = name)
        val noPhoto = EventEntity(title = "a", dateEpochDay = 0)
        val density = context.resources.displayMetrics.density
        for ((wDp, hDp) in listOf(150 to 70, 150 to 150, 320 to 150)) {
            val (w, h) = CardRenderer.widgetPixels(wDp, hDp, density)
            assertTrue("too many pixels: ${w * h}", w * h <= CardRenderer.MAX_WIDGET_PIXELS * 1.02)
            for (d in Design.entries) {
                for (e in listOf(withPhoto, noPhoto)) {
                    val b = CardRenderer.render(context, e, d, w, h, 20f)
                    assertEquals(w, b.width)
                    assertEquals(h, b.height)
                    // 角は透明（角丸）
                    assertEquals(0, Color.alpha(b.getPixel(0, 0)))
                }
            }
        }
        // 明るい写真には暗幕が掛かる（中央が元の明るさより暗い）
        val b = CardRenderer.render(context, withPhoto, Design.PHOTO, 300, 150, 0f)
        val c = b.getPixel(150, 120)
        assertTrue("scrim expected: ${Color.red(c)}", Color.red(c) < 230)
        Photos.delete(context, name)
        assertNull(Photos.load(context, name, 100))
    }

    @Test
    fun multipleWidgetsKeepSeparateConfig() {
        val store = WidgetConfigStore(context)
        store.put(101, WidgetConfigStore.Config(1, Design.DARK))
        store.put(102, WidgetConfigStore.Config(2, Design.NUMBER))
        assertEquals(Design.DARK, store.get(101)!!.design)
        assertEquals(2L, store.get(102)!!.eventId)
        store.remove(101)
        assertNull(store.get(101))
        assertNotNull(store.get(102))
        store.remove(102)
    }

    @Test
    fun notifiesOncePerOffset() = runBlocking {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val dao = EventDatabase.get(context).dao()
        val id = dao.upsert(EventEntity(title = "ライブ", dateEpochDay = LocalDate.now().plusDays(7).toEpochDay(), notifyMask = NotifyRules.DEFAULT))
        Tick.notifyDue(context)
        assertTrue(NotifyRules.isOn(dao.byId(id)!!.sentMask, 7))
        // 2回目は送らない（印が変わらない）
        val before = dao.byId(id)!!.sentMask
        Tick.notifyDue(context)
        assertEquals(before, dao.byId(id)!!.sentMask)
    }
}
