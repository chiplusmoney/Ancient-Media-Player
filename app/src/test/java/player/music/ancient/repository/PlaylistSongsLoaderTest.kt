package player.music.ancient.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import java.lang.reflect.Method

class PlaylistSongsLoaderTest {

    @Test
    fun testMakePlaylistSongCursor_SecurityException_ReturnsNull() {
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)

        `when`(context.contentResolver).thenReturn(contentResolver)

        // Mock query to throw SecurityException
        whenever(contentResolver.query(
            anyOrNull<Uri>(),
            anyOrNull<Array<String>>(),
            anyOrNull<String>(),
            anyOrNull<Array<String>>(),
            anyOrNull<String>()
        )).thenThrow(SecurityException("Permission denied"))

        // Use reflection to access the private method
        val method: Method = PlaylistSongsLoader::class.java.getDeclaredMethod(
            "makePlaylistSongCursor",
            Context::class.java,
            Long::class.java
        )
        method.isAccessible = true

        val result = method.invoke(PlaylistSongsLoader, context, 1L)
        assertNull("Cursor should be null when SecurityException is thrown", result)
    }
}
