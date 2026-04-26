package player.music.ancient.fragments.youtube

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import player.music.ancient.R
import player.music.ancient.databinding.ItemRadioStationBinding
import player.music.ancient.db.YoutubeChannelEntity

class YoutubeAdapter(
    private val onClick: (YoutubeChannelEntity) -> Unit,
    private val onLongClick: (YoutubeChannelEntity) -> Unit
) : ListAdapter<YoutubeChannelEntity, YoutubeAdapter.YoutubeViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YoutubeViewHolder {
        return YoutubeViewHolder(
            ItemRadioStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: YoutubeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class YoutubeViewHolder(private val binding: ItemRadioStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: YoutubeChannelEntity) {
            binding.title.text = channel.name
            binding.text.text = channel.url
            binding.categoryContainer.visibility = View.GONE
            binding.liveIndicator.visibility = View.GONE

            Glide.with(binding.image.context)
                .load(channel.imageUri)
                .placeholder(R.drawable.ic_youtube)
                .error(R.drawable.ic_youtube)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.image)

            binding.root.setOnClickListener { onClick(channel) }
            binding.root.setOnLongClickListener {
                onLongClick(channel)
                true
            }
            binding.menuButton.setOnClickListener { onLongClick(channel) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<YoutubeChannelEntity>() {
            override fun areItemsTheSame(
                oldItem: YoutubeChannelEntity,
                newItem: YoutubeChannelEntity
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: YoutubeChannelEntity,
                newItem: YoutubeChannelEntity
            ): Boolean = oldItem == newItem
        }
    }
}
