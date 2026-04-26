package player.music.ancient.fragments.tv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import player.music.ancient.R
import player.music.ancient.activities.VideoPlayerActivity
import player.music.ancient.databinding.ItemRadioStationBinding
import player.music.ancient.db.TvCategoryEntity
import player.music.ancient.db.TvChannelEntity

class TvAdapter(
    private val onClick: (TvChannelEntity, View) -> Unit,
    private val onLongClick: (TvChannelEntity) -> Unit
) : ListAdapter<TvChannelEntity, TvAdapter.TvViewHolder>(DiffCallback) {

    private var categoryMap: Map<Long, TvCategoryEntity> = emptyMap()

    fun submitCategories(categories: List<TvCategoryEntity>) {
        categoryMap = categories.associateBy { it.id }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TvViewHolder {
        return TvViewHolder(
            ItemRadioStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: TvViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TvViewHolder(private val binding: ItemRadioStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: TvChannelEntity) {
            binding.title.text = channel.name
            binding.text.text = channel.url
            binding.image.transitionName = VideoPlayerActivity.transitionNameFor(channel.id)

            Glide.with(binding.image.context)
                .load(channel.imageUri)
                .placeholder(R.drawable.ic_tv)
                .error(R.drawable.ic_tv)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.image)

            val category = channel.categoryId?.let(categoryMap::get)
            if (category != null) {
                binding.categoryContainer.visibility = View.VISIBLE
                binding.categoryText.text = category.name
                Glide.with(binding.categoryImage.context)
                    .load(category.imageUri)
                    .placeholder(R.drawable.ic_folder)
                    .error(R.drawable.ic_folder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.categoryImage)
            } else {
                binding.categoryContainer.visibility = View.GONE
            }

            binding.liveIndicator.visibility = View.VISIBLE
            binding.root.setOnClickListener { onClick(channel, binding.image) }
            binding.root.setOnLongClickListener {
                onLongClick(channel)
                true
            }
            binding.menuButton.setOnClickListener { onLongClick(channel) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TvChannelEntity>() {
            override fun areItemsTheSame(oldItem: TvChannelEntity, newItem: TvChannelEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TvChannelEntity, newItem: TvChannelEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
