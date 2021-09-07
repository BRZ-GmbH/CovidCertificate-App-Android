package at.gv.brz.wallet.regionlist

import android.view.View
import at.gv.brz.wallet.data.Region
import at.gv.brz.wallet.databinding.ItemRegionBinding

data class RegionItem(val region: Region, val selectedRegionIdentifier: String) {

    fun bindView(itemView: View, onRegionClickListener: ((Region) -> Unit)? = null) {
        val binding = ItemRegionBinding.bind(itemView)

        binding.itemRegionListFlag.setImageResource(region.getFlag())
        binding.itemRegionListName.setText(region.getName())
        binding.itemRegionListRadio.isChecked = region.identifier == selectedRegionIdentifier

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