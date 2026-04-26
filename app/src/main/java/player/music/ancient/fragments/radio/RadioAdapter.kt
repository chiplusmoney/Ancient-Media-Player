package player.music.ancient.fragments.radio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import player.music.ancient.db.RadioCategoryEntity
import player.music.ancient.R
import player.music.ancient.databinding.ItemRadioStationBinding
import player.music.ancient.db.RadioStationEntity
import player.music.ancient.util.RadioArtworkLoader

class RadioAdapter(
    private val onClick: (RadioStationEntity) -> Unit,
    private val onLongClick: (RadioStationEntity) -> Unit
) : ListAdapter<RadioStationEntity, RadioAdapter.RadioViewHolder>(DiffCallback) {

    private var currentPlayingUri: String? = null
    private var categoryMap: Map<Long, RadioCategoryEntity> = emptyMap()

    fun setCurrentPlayingUri(uri: String?) {
        val oldUri = currentPlayingUri
        currentPlayingUri = uri
        if (oldUri == currentPlayingUri) return

        notifyPlayingStateChanged(oldUri)
        notifyPlayingStateChanged(currentPlayingUri)
    }

    fun submitCategories(categories: List<RadioCategoryEntity>) {
        categoryMap = categories.associateBy { it.id }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RadioViewHolder {
        return RadioViewHolder(
            ItemRadioStationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RadioViewHolder, position: Int) {
        val station = getItem(position)
        holder.bind(station, station.uri == currentPlayingUri)
    }

    override fun onViewRecycled(holder: RadioViewHolder) {
        holder.clearTransientUiState()
        super.onViewRecycled(holder)
    }

    inner class RadioViewHolder(private val binding: ItemRadioStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(station: RadioStationEntity, isPlaying: Boolean) {
            binding.title.text = station.name
            binding.text.text = station.uri

            RadioArtworkLoader.load(
                imageView = binding.image,
                imageUri = station.imageUri,
                placeholderRes = R.drawable.ic_radio
            )

            val category = station.categoryId?.let(categoryMap::get)
            if (category != null) {
                binding.categoryContainer.visibility = View.VISIBLE
                binding.categoryText.text = category.name
                RadioArtworkLoader.load(
                    imageView = binding.categoryImage,
                    imageUri = category.imageUri,
                    placeholderRes = R.drawable.ic_folder
                )
            } else {
                binding.categoryContainer.visibility = View.GONE
                binding.categoryImage.setImageResource(R.drawable.ic_folder)
            }

            if (isPlaying) {
                binding.liveIndicator.visibility = View.VISIBLE
                startLiveAnimation(binding.liveDot)
            } else {
                binding.liveIndicator.visibility = View.GONE
                clearTransientUiState()
            }

            binding.root.setOnClickListener {
                it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    onClick(station)
                }.start()
            }
            binding.menuButton.setOnClickListener {
                onLongClick(station)
            }
            binding.root.setOnLongClickListener {
                onLongClick(station)
                true
            }
        }

        private fun startLiveAnimation(view: View) {
            val anim = AlphaAnimation(1.0f, 0.3f)
            anim.duration = 800
            anim.repeatMode = Animation.REVERSE
            anim.repeatCount = Animation.INFINITE
            view.startAnimation(anim)
        }

        fun clearTransientUiState() {
            binding.liveDot.clearAnimation()
            binding.root.animate().cancel()
            binding.root.scaleX = 1f
            binding.root.scaleY = 1f
        }
    }

    private fun notifyPlayingStateChanged(uri: String?) {
        if (uri == null) return
        val position = currentList.indexOfFirst { it.uri == uri }
        if (position != RecyclerView.NO_POSITION && position >= 0) {
            notifyItemChanged(position)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<RadioStationEntity>() {
            override fun areItemsTheSame(
                oldItem: RadioStationEntity,
                newItem: RadioStationEntity
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: RadioStationEntity,
                newItem: RadioStationEntity
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
