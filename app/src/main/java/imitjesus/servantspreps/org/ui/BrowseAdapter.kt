package imitjesus.servantspreps.org.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import imitjesus.servantspreps.org.databinding.ItemBrowseBinding

data class BrowseItem(
    val id: Int?,
    val title: String,
    val subtitle: String,
    val type: BrowseType
)

enum class BrowseType { BOOK, CHAPTER, TITLE }

class BrowseAdapter(private val onClick: (BrowseItem) -> Unit) : RecyclerView.Adapter<BrowseAdapter.ViewHolder>() {

    private var items = listOf<BrowseItem>()

    fun submitList(newItems: List<BrowseItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBrowseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.title
        holder.binding.tvSubtitle.text = item.subtitle
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(val binding: ItemBrowseBinding) : RecyclerView.ViewHolder(binding.root)
}
