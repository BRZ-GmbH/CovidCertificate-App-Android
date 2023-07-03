package at.gv.brz.common.util

import android.content.Context
import at.gv.brz.wallet.R

object LocaleUtil {
	fun isSystemLangNotEnglish(context: Context): Boolean {
		return context.getString(R.string.language_key) != "en"
	}
}