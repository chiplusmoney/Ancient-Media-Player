package player.music.ancient.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object RadioImageStore {
    private const val ROOT_FOLDER = "radio_images"
    private const val STATION_FOLDER = "stations"
    private const val CATEGORY_FOLDER = "categories"
    private const val THUMBNAIL_SIZE = 640
    private const val JPEG_QUALITY = 82

    suspend fun persistStationImage(context: Context, source: Uri): String? {
        return persistImage(context, source, STATION_FOLDER)
    }

    suspend fun persistCategoryImage(context: Context, source: Uri): String? {
        return persistImage(context, source, CATEGORY_FOLDER)
    }

    fun deleteManagedImage(context: Context, imageUri: String?) {
        val file = resolveManagedFile(context, imageUri) ?: return
        if (file.exists()) {
            file.delete()
        }
    }

    private suspend fun persistImage(context: Context, source: Uri, folderName: String): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                Glide.with(context)
                    .asBitmap()
                    .load(source)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .submit()
                    .get()
            }.mapCatching { bitmap ->
                saveBitmap(context, bitmap, folderName)
            }.getOrNull()
        }
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap, folderName: String): String? {
        val directory = File(File(context.filesDir, ROOT_FOLDER), folderName)
        if (!directory.exists() && !directory.mkdirs()) {
            return null
        }

        val file = File(directory, "radio_${System.currentTimeMillis()}.jpg")
        val wasSaved = file.outputStream().buffered().use { outputStream ->
            ImageUtil.resizeBitmap(bitmap, THUMBNAIL_SIZE)
                .compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        }
        return if (wasSaved) {
            Uri.fromFile(file).toString()
        } else {
            null
        }
    }

    private fun resolveManagedFile(context: Context, imageUri: String?): File? {
        if (imageUri.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(imageUri) }.getOrNull() ?: return null
        val path = uri.path ?: return null
        val rootPath = File(context.filesDir, ROOT_FOLDER).absolutePath
        return if (path.startsWith(rootPath)) File(path) else null
    }
}
