package at.gv.brz.common.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class VaccinationRefreshCampaignTextModel(
    val title: String,
    val message: String,
    @Json(name = "remind_again_button") val remindAgainButton: String,
    @Json(name = "read_button") val readButton: String
)