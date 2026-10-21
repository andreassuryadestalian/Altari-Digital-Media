package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.db.PlaylistEntity
import com.example.model.LyricsContent
import com.example.model.ServicePlaylist
import com.example.presentation.PresentationServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Altari Digital", appName)
    }

    @Test
    fun `test web remote pin setting and retrieval`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = PresentationServer(context)

        // Initially no PIN set
        server.setRemotePin(null)
        assertNull(server.getRemotePin())
        assertNull(server.webServer.remotePin)

        // Set 4-digit PIN
        server.setRemotePin("5678")
        assertEquals("5678", server.getRemotePin())
        assertEquals("5678", server.webServer.remotePin)

        // Recreate server to test persistence in SharedPreferences
        val server2 = PresentationServer(context)
        assertEquals("5678", server2.getRemotePin())

        // Clear PIN
        server.setRemotePin(null)
        assertNull(server.getRemotePin())
    }

    @Test
    fun `test playlist entity serialization and deserialization`() {
        val items = androidx.compose.runtime.mutableStateListOf<com.example.model.PresentationContent>()
        items.add(
            LyricsContent(
                id = "song_1",
                title = "Bapa Engkau Sungguh Baik",
                slides = listOf("Bapa Engkau sungguh baik", "Kasih-Mu melimpah di hidupku")
            )
        )
        val playlist = ServicePlaylist(
            id = "pl_test_1",
            name = "Ibadah Minggu Pagi",
            items = items
        )

        val entity = PlaylistEntity.fromModel(playlist, 0)
        assertEquals("pl_test_1", entity.id)
        assertEquals("Ibadah Minggu Pagi", entity.name)
        assertTrue(entity.itemsJson.contains("Bapa Engkau Sungguh Baik"))

        val restoredModel = entity.toModel()
        assertEquals("pl_test_1", restoredModel.id)
        assertEquals("Ibadah Minggu Pagi", restoredModel.name)
        assertEquals(1, restoredModel.items.size)
        assertEquals("Bapa Engkau Sungguh Baik", restoredModel.items[0].title)
    }
}
