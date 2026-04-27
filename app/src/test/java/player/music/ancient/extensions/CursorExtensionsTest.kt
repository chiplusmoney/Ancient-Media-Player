package player.music.ancient.extensions

import android.database.Cursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class CursorExtensionsTest {

    private fun mockCursorThrowingExceptionOnColumnIndex(): Cursor {
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
                if (method.name == "getColumnIndexOrThrow") {
                    throw IllegalArgumentException("Mock exception")
                }
                return null
            }
        }
        return Proxy.newProxyInstance(
            Cursor::class.java.classLoader,
            arrayOf(Cursor::class.java),
            handler
        ) as Cursor
    }

    private fun mockCursorSuccess(): Cursor {
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
                when (method.name) {
                    "getColumnIndexOrThrow" -> return 1
                    "getInt" -> return 42
                    "getLong" -> return 42L
                    "getString" -> return "success"
                }
                return null
            }
        }
        return Proxy.newProxyInstance(
            Cursor::class.java.classLoader,
            arrayOf(Cursor::class.java),
            handler
        ) as Cursor
    }

    @Test
    fun getInt_throwsIllegalStateException() {
        val cursor = mockCursorThrowingExceptionOnColumnIndex()
        val exception = assertThrows(IllegalStateException::class.java) {
            cursor.getInt("invalid_column")
        }
        assertEquals("invalid column invalid_column", exception.message)
        assertEquals("Mock exception", exception.cause?.message)
    }

    @Test
    fun getInt_success() {
        val cursor = mockCursorSuccess()
        assertEquals(42, cursor.getInt("valid_column"))
    }

    @Test
    fun getLong_throwsIllegalStateException() {
        val cursor = mockCursorThrowingExceptionOnColumnIndex()
        val exception = assertThrows(IllegalStateException::class.java) {
            cursor.getLong("invalid_column")
        }
        assertEquals("invalid column invalid_column", exception.message)
        assertEquals("Mock exception", exception.cause?.message)
    }

    @Test
    fun getLong_success() {
        val cursor = mockCursorSuccess()
        assertEquals(42L, cursor.getLong("valid_column"))
    }

    @Test
    fun getString_throwsIllegalStateException() {
        val cursor = mockCursorThrowingExceptionOnColumnIndex()
        val exception = assertThrows(IllegalStateException::class.java) {
            cursor.getString("invalid_column")
        }
        assertEquals("invalid column invalid_column", exception.message)
        assertEquals("Mock exception", exception.cause?.message)
    }

    @Test
    fun getString_success() {
        val cursor = mockCursorSuccess()
        assertEquals("success", cursor.getString("valid_column"))
    }

    @Test
    fun getStringOrNull_throwsIllegalStateException() {
        val cursor = mockCursorThrowingExceptionOnColumnIndex()
        val exception = assertThrows(IllegalStateException::class.java) {
            cursor.getStringOrNull("invalid_column")
        }
        assertEquals("invalid column invalid_column", exception.message)
        assertEquals("Mock exception", exception.cause?.message)
    }

    @Test
    fun getStringOrNull_success() {
        val cursor = mockCursorSuccess()
        assertEquals("success", cursor.getStringOrNull("valid_column"))
    }
}
