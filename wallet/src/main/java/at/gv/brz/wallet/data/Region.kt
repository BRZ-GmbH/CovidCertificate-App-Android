package at.gv.brz.wallet.data

import at.gv.brz.wallet.R

enum class Region(val identifier: String) {
    VIENNA("W") {
        override fun getName(): Int {
            return R.string.region_wien
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_w
        }
    },
    NIEDEROESTERREICH("NOE") {
        override fun getName(): Int {
            return R.string.region_niederoesterreich
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_noe
        }
    },
    OBEROESTERREICH("OOE") {
        override fun getName(): Int {
            return R.string.region_oberoesterreich
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_ooe
        }
    },
    KAERNTEN("KTN") {
        override fun getName(): Int {
            return R.string.region_kaernten
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_ktn
        }
    },
    BURGENLAND("BGLD") {
        override fun getName(): Int {
            return R.string.region_burgenland
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_bgld
        }
    },
    SALZBURG("SBG") {
        override fun getName(): Int {
            return R.string.region_salzburg
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_sbg
        }
    },
    STEIERMARK("STMK") {
        override fun getName(): Int {
            return R.string.region_steiermark
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_stmk
        }
    },
    TIROL("T") {
        override fun getName(): Int {
            return R.string.region_tirol
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_t
        }
    },
    VORARLBERG("VBG") {
        override fun getName(): Int {
            return R.string.region_vorarlberg
        }

        override fun getFlag(): Int {
            return R.drawable.ic_flag_vbg
        }
    };

    abstract fun getName(): Int
    abstract fun getFlag(): Int

    companion object {
        fun getRegionFromIdentifier(identifier: String?): Region? {
            if (identifier == null || identifier.isEmpty()) {
                return null
            }
            return values().firstOrNull { it.identifier == identifier }
        }
    }
}

fun String.regionModifiedProfile(selectedRegionIdentifier: String?): String {
    if (selectedRegionIdentifier == null || selectedRegionIdentifier.isEmpty()) {
        return this;
    } else {
        return "${this}-${selectedRegionIdentifier}"
    }
}