package at.gv.brz.wallet.util

import android.content.Context
import at.gv.brz.common.util.AssetUtil
import at.gv.brz.wallet.onboarding.FeatureIntroEntryModel
import at.gv.brz.wallet.onboarding.FeatureIntroModel
import com.squareup.moshi.Moshi

object WalletAssetUtil {

    private const val ASSET_FILENAME_DEFAULT_CONFIG = "intros.json"

    fun loadFeatureIntrosForLanguageAndVersion(
        context: Context,
        languageKey: String,
        version: String): List<FeatureIntroEntryModel>? = AssetUtil.loadAssetJson(
        context,
        ASSET_FILENAME_DEFAULT_CONFIG
    )?.let {
        Moshi.Builder().build().adapter(FeatureIntroModel::class.java).fromJson(it)
    }?.intros?.get(languageKey)?.get(version)

    fun hasFeatureIntrosForLanguageAndVersion(
        context: Context,
        languageKey: String,
        version: String): Boolean = (loadFeatureIntrosForLanguageAndVersion(context, languageKey, version) ?: listOf()).isNotEmpty()
}