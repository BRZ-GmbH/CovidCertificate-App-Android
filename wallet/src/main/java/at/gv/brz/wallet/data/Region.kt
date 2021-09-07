package at.gv.brz.wallet.data

import at.gv.brz.wallet.R

enum class Region(val identifier: String) {
    NATIONWIDE("") {
        override fun getName(): Int {
            return R.string.region_nationwide
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_at
        }
    },
    VIENNA("W") {
        override fun getName(): Int {
            return R.string.region_wien
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_w
        }
    };

    abstract fun getName(): Int
    abstract fun getFlag(): Int
}

fun String.regionModifiedProfile(selectedRegionIdentifier: String): String {
    if (selectedRegionIdentifier.isEmpty()) {
        return this;
    } else {
        return "${this}-${selectedRegionIdentifier}"
    }
}