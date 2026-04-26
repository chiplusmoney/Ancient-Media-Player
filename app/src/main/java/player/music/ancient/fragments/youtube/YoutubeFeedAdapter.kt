package player.music.ancient.fragments.youtube

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import player.music.ancient.R
import player.music.ancient.databinding.ItemYoutubeFeedBinding
import java.util.Date
import java.time.Instant
import java.time.OffsetDateTime

class YoutubeFeedAdapter(
    private val onVideoClick: (YoutubeFeedVideo) -> Unit
) : ListAdapter<YoutubeFeedVideo, YoutubeFeedAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemYoutubeFeedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemYoutubeFeedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onVideoClick(getItem(position))
                }
            }
        }

        fun bind(item: YoutubeFeedVideo) {
            binding.videoTitle.text = item.title
            binding.channelName.text = item.channelTitle
            binding.publishedDate.text = getTimeAgo(item.publishedAt)

            Glide.with(binding.root.context)
                .load(item.thumbnailUrl)
                .placeholder(R.drawable.default_audio_art)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .format(DecodeFormat.PREFER_RGB_565)
                .centerCrop()
                .dontAnimate()
                .thumbnail(0.25f)
                .into(binding.videoThumbnail)

            // Using default channel avatar for now, real implementation would fetch it from Channels API
            Glide.with(binding.root.context)
                .load(R.drawable.ic_youtube)
                .circleCrop()
                .dontAnimate()
                .into(binding.channelAvatar)
        }
        
        private fun getTimeAgo(dateString: String): String {
            val past = parsePublishedDate(dateString) ?: return dateString
            return DateUtils.getRelativeTimeSpanString(
                past.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        }

        private fun parsePublishedDate(dateString: String): Date? {
            return runCatching { Date.from(Instant.parse(dateString)) }
                .getOrElse {
                    runCatching { Date.from(OffsetDateTime.parse(dateString).toInstant()) }
                        .getOrNull()
                }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<YoutubeFeedVideo>() {
            override fun areItemsTheSame(
                oldItem: YoutubeFeedVideo,
                newItem: YoutubeFeedVideo
            ): Boolean {
                return oldItem.videoId == newItem.videoId
            }

            override fun areContentsTheSame(
                oldItem: YoutubeFeedVideo,
                newItem: YoutubeFeedVideo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
