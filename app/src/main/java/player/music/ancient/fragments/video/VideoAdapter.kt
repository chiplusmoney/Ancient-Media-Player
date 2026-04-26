package player.music.ancient.fragments.video

import android.net.Uri
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import player.music.ancient.R
import player.music.ancient.activities.VideoPlayerActivity
import player.music.ancient.databinding.ItemVideoEntryBinding
import player.music.ancient.model.LocalVideoItem

class VideoAdapter(
    private val onClick: (LocalVideoItem, View) -> Unit
) : ListAdapter<LocalVideoItem, VideoAdapter.VideoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(
            ItemVideoEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(private val binding: ItemVideoEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(video: LocalVideoItem) {
            binding.title.text = video.title
            binding.subtitle.text = video.folderName
            binding.meta.text = formatDuration(video.durationMs)
            ViewCompat.setTransitionName(
                binding.image,
                VideoPlayerActivity.transitionNameFor(video.id)
            )
            Glide.with(binding.image.context)
                .load(Uri.parse(video.uri))
                .placeholder(R.drawable.ic_video)
                .error(R.drawable.ic_video)
                .centerCrop()
                .into(binding.image)
            binding.root.setOnClickListener { onClick(video, binding.image) }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<LocalVideoItem>() {
            override fun areItemsTheSame(oldItem: LocalVideoItem, newItem: LocalVideoItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: LocalVideoItem, newItem: LocalVideoItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
