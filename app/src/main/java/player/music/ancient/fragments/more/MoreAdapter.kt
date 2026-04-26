package player.music.ancient.fragments.more

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import player.music.ancient.databinding.ItemMoreEntryBinding
import player.music.ancient.model.CategoryInfo

class MoreAdapter(
    private val entries: List<CategoryInfo>,
    private val onClick: (CategoryInfo) -> Unit
) : RecyclerView.Adapter<MoreAdapter.MoreViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoreViewHolder {
        return MoreViewHolder(
            ItemMoreEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int = entries.size

    override fun onBindViewHolder(holder: MoreViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    inner class MoreViewHolder(private val binding: ItemMoreEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryInfo) {
            binding.icon.setImageResource(item.category.icon)
            binding.title.setText(item.category.stringRes)
            binding.subtitle.text = "Open the ${binding.root.context.getString(item.category.stringRes)} section"
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
