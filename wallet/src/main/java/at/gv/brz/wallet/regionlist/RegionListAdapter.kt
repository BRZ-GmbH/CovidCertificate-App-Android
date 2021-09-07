package at.gv.brz.wallet.regionlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.gv.brz.wallet.data.Region

class RegionListAdapter(private val onRegionClickListener: ((Region) -> Unit)? = null): RecyclerView.Adapter<RegionListViewHolder>() {
    private val items: MutableList<RegionItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionListViewHolder {
        return RegionListViewHolder.inflate(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: RegionListViewHolder, position: Int) {
        holder.bindItem(items[position], onRegionClickListener)
    }

    override fun getItemCount(): Int = items.size

    fun setItems(items: List<RegionItem>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }
}