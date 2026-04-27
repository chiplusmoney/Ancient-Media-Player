package player.music.ancient.activities

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {

    class MockIntent : Intent() {
        private val longExtras = mutableMapOf<String, Long>()
        private val stringExtras = mutableMapOf<String, String?>()

        override fun putExtra(name: String, value: Long): Intent {
            longExtras[name] = value
            return this
        }

        override fun putExtra(name: String, value: String?): Intent {
            stringExtras[name] = value
            return this
        }

        override fun getLongExtra(name: String, defaultValue: Long): Long {
            return longExtras[name] ?: defaultValue
        }

        override fun getStringExtra(name: String): String? {
            return stringExtras[name]
        }
    }

    @Test
    fun parseLongFromIntent_validLongExtra_returnsLong() {
        val intent = MockIntent()
        intent.putExtra("longKey", 123L)

        val result = MainActivity.parseLongFromIntent(intent, "longKey", "stringKey")
        assertEquals(123L, result)
    }

    @Test
    fun parseLongFromIntent_invalidLongExtra_validStringExtra_returnsParsedLong() {
        val intent = MockIntent()
        intent.putExtra("stringKey", "456")

        val result = MainActivity.parseLongFromIntent(intent, "longKey", "stringKey")
        assertEquals(456L, result)
    }

    @Test
    fun parseLongFromIntent_invalidLongExtra_invalidStringExtra_returnsNegativeOne() {
        val intent = MockIntent()
        intent.putExtra("stringKey", "not_a_number")

        val result = MainActivity.parseLongFromIntent(intent, "longKey", "stringKey")
        assertEquals(-1L, result)
    }

    @Test
    fun parseLongFromIntent_missingExtras_returnsNegativeOne() {
        val intent = MockIntent()

        val result = MainActivity.parseLongFromIntent(intent, "longKey", "stringKey")
        assertEquals(-1L, result)
    }
}
