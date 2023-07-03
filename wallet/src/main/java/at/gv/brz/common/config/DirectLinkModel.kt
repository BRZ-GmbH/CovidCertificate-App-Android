package at.gv.brz.common.config

import com.squareup.moshi.JsonClass

/**
 * The purpose of this data class is for direct link delivery
 * Input comes from the direct link (includ. secret+signature) (z.B SMS/Email Link).
 * Output is the qr data based on date of birth.
 */
@JsonClass(generateAdapter = true)
data class DirectLinkModel(
    val secret: String,
    val secretSignature: String,
    val clientId: String? = null,
    val clientIdSignature: String? = null,
    val birthdate: Birthdate? = null,
    val bypassToken: String? = null,
    val request: List<String> = listOf()
)

@JsonClass(generateAdapter = true)
data class Birthdate(
    val day: Int,
    val month: Int,
    val year: Int
)

@JsonClass(generateAdapter = true)
data class DirectLinkResponse(
    val qr: String?,
    val error: Boolean
)

sealed class DirectLinkResult {
     data class Valid(val qr: String): DirectLinkResult()
     object InvalidRequestData: DirectLinkResult()
     object MissingQrData: DirectLinkResult()
     object NetworkError: DirectLinkResult()
}