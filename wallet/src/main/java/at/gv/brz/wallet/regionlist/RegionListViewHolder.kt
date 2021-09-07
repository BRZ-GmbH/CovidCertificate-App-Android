package at.gv.brz.wallet.regionlist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.gv.brz.wallet.R
import at.gv.brz.wallet.data.Region

class RegionListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    companion object {
        @SuppressLint("ClickableViewAccessibility")
        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup
        ): RegionListViewHolder {
            val itemView = inflater.inflate(R.layout.item_region, parent, false)
            val viewHolder = RegionListViewHolder(itemView)
            return viewHolder
        }
    }

    fun bindItem(region: RegionItem, onRegionClickListener: ((Region) -> Unit)? = null) =
        region.bindView(itemView, onRegionClickListener)
}