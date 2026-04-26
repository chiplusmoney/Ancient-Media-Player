package player.music.ancient.util

import android.content.Context
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy

object RadioArtworkLoader {
    private const val MAX_PRELOAD_KEYS = 96
    private val recentPreloads = LinkedHashSet<String>()

    fun load(
        imageView: ImageView,
        imageUri: String?,
        @DrawableRes placeholderRes: Int
    ) {
        val model = imageUri?.takeIf { it.isNotBlank() }
        Glide.with(imageView)
            .load(model)
            .placeholder(placeholderRes)
            .error(placeholderRes)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .format(DecodeFormat.PREFER_RGB_565)
            .dontAnimate()
            .thumbnail(
                Glide.with(imageView)
                    .load(model)
                    .sizeMultiplier(0.25f)
            )
            .centerCrop()
            .into(imageView)
    }

    fun preload(
        context: Context,
        imageUri: String?,
        width: Int = 512,
        height: Int = 512
    ) {
        if (imageUri.isNullOrBlank()) return

        val normalizedUri = imageUri.trim()
        if (shouldSkipPreload(normalizedUri, width, height)) return

        Glide.with(context)
            .load(normalizedUri)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .format(DecodeFormat.PREFER_RGB_565)
            .dontAnimate()
            .centerCrop()
            .preload(width, height)
    }

    private fun shouldSkipPreload(
        imageUri: String,
        width: Int,
        height: Int
    ): Boolean = synchronized(recentPreloads) {
        val key = "$imageUri@${width}x$height"
        if (recentPreloads.contains(key)) {
            true
        } else {
            recentPreloads += key
            while (recentPreloads.size > MAX_PRELOAD_KEYS) {
                recentPreloads.remove(recentPreloads.first())
            }
            false
        }
    }
}
