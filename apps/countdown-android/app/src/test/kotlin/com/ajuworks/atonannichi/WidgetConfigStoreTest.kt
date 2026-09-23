package com.ajuworks.atonannichi

import androidx.test.core.app.ApplicationProvider
import com.ajuworks.atonannichi.core.Design
import com.ajuworks.atonannichi.data.WidgetConfigStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Each placed widget remembers its own event and design. Two widgets showing
 * two different events is the whole point of the app on the home screen, so it
 * is worth pinning down that one's configuration cannot overwrite another's.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetConfigStoreTest {
    private val store = WidgetConfigStore(ApplicationProvider.getApplicationContext())

    @Test
    fun `an unconfigured widget has no config`() {
        assertThat(store.get(999)).isNull()
    }

    @Test
    fun `each widget keeps its own event and design`() {
        store.put(1, WidgetConfigStore.Config(eventId = 10, design = Design.PHOTO))
        store.put(2, WidgetConfigStore.Config(eventId = 20, design = Design.DARK))

        assertThat(store.get(1)).isEqualTo(WidgetConfigStore.Config(10, Design.PHOTO))
        assertThat(store.get(2)).isEqualTo(WidgetConfigStore.Config(20, Design.DARK))
    }

    @Test
    fun `reconfiguring a widget replaces its config`() {
        store.put(3, WidgetConfigStore.Config(eventId = 10, design = Design.MINIMAL))
        store.put(3, WidgetConfigStore.Config(eventId = 11, design = Design.NUMBER))

        assertThat(store.get(3)).isEqualTo(WidgetConfigStore.Config(11, Design.NUMBER))
    }

    @Test
    fun `removing a widget forgets it`() {
        store.put(4, WidgetConfigStore.Config(eventId = 10, design = Design.SOFT))

        store.remove(4)

        assertThat(store.get(4)).isNull()
    }
}
