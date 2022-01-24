package at.gv.brz.common.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Model object for a vaccination campaign
 */
@JsonClass(generateAdapter = true)
data class CampaignModel(
    val id: String,
    val version: Int,
    @Json(name = "valid_from") val validFromString: String?,
    @Json(name = "valid_until") val validUntilString: String?,
    @Json(name = "type") val campaignType: CampaignType,
    val important: Boolean,
    val title: Map<String, String>?,
    val message: Map<String, String>?,
    @Json(name = "repeat_interval") val repeatInterval: CampaignRepeatInterval?,
    @Json(name = "applies_to") val applicationType: CampaignApplicationType,
    val buttons: List<CampaignButton>?,
    @Json(name = "conditions") val conditionGroups: List<List<String>>?
) {
    fun getTitle(languageKey: String): String? = title?.get(languageKey)
    fun getMessage(languageKey: String): String? = message?.get(languageKey)

    val validFrom: LocalDateTime
        get() {
            if (validFromString == null) {
                return LocalDateTime.MIN
            }
            try {
                return LocalDateTime.parse(validFromString, DateTimeFormatter.ISO_DATE_TIME)
            } catch (e: Exception) {
                // Ignore exception when date string cannot be parsed
            }
            return LocalDateTime.MIN
        }

    val validUntil: LocalDateTime
        get() {
            if (validUntilString == null) {
                return LocalDateTime.MAX
            }
            try {
                return LocalDateTime.parse(validUntilString, DateTimeFormatter.ISO_DATE_TIME)
            } catch (e: Exception) {
                // Ignore exception when date string cannot be parsed
            }
            return LocalDateTime.MAX
        }

    val isActive: Boolean
        get() {
            return LocalDateTime.now().isAfter(validFrom) && LocalDateTime.now().isBefore(validUntil)
        }
}

/**
 * Model object for a repeat interval of a vaccination campaign
 */
@JsonClass(generateAdapter = true)
data class CampaignRepeatInterval(val type: String, val interval: Int) {
    fun dateByAddingRepeatIntervalToDate(date: LocalDateTime): LocalDateTime {
        when (type) {
            "minute" -> return date.plusMinutes(interval.toLong())
            "hour" -> return date.plusHours(interval.toLong())
            "day" -> return date.plusDays(interval.toLong())
            "month" -> return date.plusMonths(interval.toLong())
        }
        return date
    }
}

/**
 * Model object for the button within a vaccination Campaign
 */
@JsonClass(generateAdapter = true)
data class CampaignButton(
    val type: CampaignButtonType,
    val title: Map<String, String>,
    @Json(name = "url") val urlString: String?
) {
    fun getTitle(languageKey: String): String? = title.get(languageKey)
}

/**
 * Enum for the type of vaccination campaign
 */
enum class CampaignType {
    @Json(name = "single")
    SINGLE,
    @Json(name = "repeating")
    REPEATING,
    @Json(name = "single_any_certificate")
    SINGLE_ANY_CERTIFICATE,
    @Json(name = "single_each_certificate")
    SINGLE_EACH_CERTIFICATE,
    @Json(name = "repeating_any_certificate")
    REPEATING_ANY_CERTIFICATE,
    @Json(name = "repeating_each_certificate")
    REPEATING_EACH_CERTIFICATE
}

/**
 * Enum for the group of certificates a vaccination campaign applies to
 */
enum class CampaignApplicationType {
    @Json(name = "all")
    ALL,
    @Json(name = "newest_certificate_per_person")
    NEWEST_CERTIFICATE_PER_PERSON
}

/**
 * Enum for the type of CampaignButton
 */
enum class CampaignButtonType {
    @Json(name = "dismiss")
    DISMISS,
    @Json(name = "later")
    LATER,
    @Json(name = "dismiss_action")
    DISMISS_WITH_ACTION,
    @Json(name = "later_action")
    LATER_WITH_ACTION
}