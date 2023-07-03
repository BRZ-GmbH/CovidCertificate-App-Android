package at.gv.brz.sdk.utils

import at.gv.brz.sdk.euhealthcert.VaccinationExemptionEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun VaccinationExemptionEntry.isTargetDiseaseCorrect(): Boolean {
    return this.disease == AcceptanceCriteriasConstants.TARGET_DISEASE
}

fun VaccinationExemptionEntry.validUntilDate(): LocalDateTime? {
    if (this.validUntil.isEmpty()) {
        return null
    }
    val date: LocalDate?
    try {
        date = LocalDate.parse(this.validUntil, DateTimeFormatter.ISO_DATE)
    } catch (e: Exception) {
        return null
    }
    return date.atStartOfDay().plusHours(23).plusMinutes(59)
}

fun VaccinationExemptionEntry.getExemptionCountry(showEnglishVersionForLabels: Boolean): String {
    return try {
        val loc = Locale("", this.country)
        var countryString = loc.displayCountry
        if (showEnglishVersionForLabels) {
            countryString = "$countryString / ${loc.getDisplayCountry(Locale.ENGLISH)}"
        }
        return countryString
    } catch (e: Exception) {
        this.country
    }
}

fun VaccinationExemptionEntry.getIssuer(): String {
    return this.issuer
}

fun VaccinationExemptionEntry.getCertificateIdentifier(): String {
    return this.certificateIdentifier
}
