package at.gv.brz.wallet.regionlist

import android.view.View
import at.gv.brz.wallet.R
import at.gv.brz.wallet.data.Region
import at.gv.brz.wallet.databinding.ItemRegionBinding

data class RegionItem(val region: Region, val selectedRegionIdentifier: String?) {

    fun bindView(itemView: View, onRegionClickListener: ((Region) -> Unit)? = null) {
        val binding = ItemRegionBinding.bind(itemView)

        binding.itemRegionListFlag.setImageResource(region.getFlag())
        binding.itemRegionListName.setText(region.getName())
        binding.itemRegionListRadio.isChecked = region.identifier == selectedRegionIdentifier
        if (region.identifier == selectedRegionIdentifier) {
            itemView.contentDescription = "${itemView.resources.getString(region.getName())}.\n\n${itemView.resources.getString(R.string.accessibility_region_active)}"
        } else {
            itemView.contentDescription = "${itemView.resources.getString(region.getName())}.\n\n${itemView.resources.getString(R.string.accessibility_region_inactive)}"
        }

        binding.root.setOnClickListener {
            onRegionClickListener?.invoke(region)
        }
        binding.itemRegionListRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                onRegionClickListener?.invoke(region)
            }
        }
    }
}