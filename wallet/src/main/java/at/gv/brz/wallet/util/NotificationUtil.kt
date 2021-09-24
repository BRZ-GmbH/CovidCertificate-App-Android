package at.gv.brz.wallet.util

import at.gv.brz.eval.euhealthcert.Eudgc
import at.gv.brz.eval.euhealthcert.VaccinationEntry
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.eval.utils.vaccineDate
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.wallet.data.NotificationSecureStorage
import java.lang.Exception
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Handles determining eligibility for Vaccination booster reminders
 */
class NotificationUtil {

    /**
     * Returns a filtered list of DccHolder objects that represent certificates that are eligible for showing a notification about a vaccination booster
     */
    fun certificatesEligibleForNotification(certificates: List<DccHolder>): List<DccHolder> {
        return certificates.filter { certificate ->
            // Only notify about vaccinations
            val vaccination = certificate.euDGC.vaccinations?.firstOrNull() ?: return@filter false

            // Only notify about certificates from Austria
            vaccination.country.also {
                if (it != "AT") {
                    return@filter false
                }
            }

            // Only notify for certificates that have not expired yet
            certificate.expirationTime?.also {
                if (it.isBefore(Instant.now())) {
                    return@filter false
                }
            }

            // Only notify fully vaccinated
            if (vaccination.doseNumber < vaccination.totalDoses) {
                return@filter false
            }

            // People over 65 should get booster after 6 months
            if (certificate.euDGC.isOver65() && vaccination.atLeastSixMonthsAgo()) {
                return@filter true
            }

            // People with AstraZeneca or Janssen should get booster after 6 months
            if ((vaccination.isJanssenVaccination() || vaccination.isVaxzevriaVaccination()) && vaccination.atLeastSixMonthsAgo()) {
                return@filter true
            }

            // People over 18 with mRNA should get booster after 9 months
            if (certificate.euDGC.isOver18() && !vaccination.isJanssenVaccination() && !vaccination.isVaxzevriaVaccination() && vaccination.atLeastNineMonthsAgo()) {
                return@filter true
            }

            return@filter false
        }
    }

    /**
     * Returns a filtered list of DccHolder objects that represent certificates that are eligible for showing a notification about a vaccination booster and
     * for which a notification should actually be presented (has either never been presented before or is already eligible for being shown again)
     */
    fun certificatesForBoosterNotification(certificates: List<DccHolder>, notificationSecureStorage: NotificationSecureStorage): List<DccHolder> {
        val eligibleCertificates = certificatesEligibleForNotification(certificates)

        return eligibleCertificates.filter { certificate ->
            val identifier = certificate.euDGC.vaccinations?.firstOrNull()?.certificateIdentifier ?: return@filter false

            val notificationTimestamp = notificationSecureStorage.getNotificationTimestampForCertificateIdentifier(identifier)

            return@filter notificationTimestamp == null || Instant.ofEpochMilli(notificationTimestamp).isBefore(Instant.now())
        }
    }
}

/**
 * Returns whether the person is over the passed number of years
 */
fun Eudgc.isOverAge(age: Long): Boolean {
    try {
        val dateOfBirth = LocalDate.parse(dateOfBirth, DateTimeFormatter.ISO_DATE)
        return dateOfBirth.plusYears(age).isBefore(LocalDate.now())
    } catch (ignored: Exception) {
    }
    return false
}

/**
 * Returns whether the person is at least 65 years old
 */
fun Eudgc.isOver65(): Boolean {
    return isOverAge(65)
}

/**
 * Returns whether the person is at least 18 years old
 */
fun Eudgc.isOver18(): Boolean {
    return isOverAge(18)
}

/**
 * Returns whether the vaccination date is at least the passed number of months in the past
 */
fun VaccinationEntry.isOlderThan(months: Long): Boolean {
    val vaccinationDate = vaccineDate() ?: return false
    return vaccinationDate.plusMonths(months).isBefore(LocalDateTime.now())
}

/**
 * Returns whether the vaccination date is at least 6 months ago
 */
fun VaccinationEntry.atLeastSixMonthsAgo(): Boolean {
    return isOlderThan(6)
}

/**
 * Returns whether the vaccination date is at least 9 months ago
 */
fun VaccinationEntry.atLeastNineMonthsAgo(): Boolean {
    return isOlderThan(9)
}

/**
 * Returns whether the vaccination was performed with a Janssen vaccine (by comparing the medicinalProduct (mp) field
 */
fun VaccinationEntry.isJanssenVaccination(): Boolean {
    return medicinialProduct == "EU/1/20/1525"
}

/**
 * Returns whether the vaccination was performed with a Vaxzevria (Astra Zeneca) vaccine (by comparing the medicinalProduct (mp) field
 */
fun VaccinationEntry.isVaxzevriaVaccination(): Boolean {
    return medicinialProduct == "EU/1/21/1529"
}

/**
 * Returns the next date for notifying about the vaccination booster for this certificate - either one week before expiration or one month from now
 */
fun DccHolder.nextNotificationTimestamp(): Long {
    val oneWeekBeforeExpiration = Instant.from(Period.ofWeeks(1).subtractFrom(expirationTime ?: Instant.now()))

    if (BuildConfig.FLAVOR == "abn") {
        val tenMinutesFromNow = Instant.now().plus(10, ChronoUnit.MINUTES)
        return if (oneWeekBeforeExpiration.isBefore(tenMinutesFromNow)) {
            oneWeekBeforeExpiration.toEpochMilli()
        } else {
            tenMinutesFromNow.toEpochMilli()
        }
    }

    val oneMonthFromNow = LocalDateTime.now().plusMonths(1).toInstant(OffsetDateTime.now().offset)
    return if (oneWeekBeforeExpiration.isBefore(oneMonthFromNow)) {
        oneWeekBeforeExpiration.toEpochMilli()
    } else {
        oneMonthFromNow.toEpochMilli()
    }
}

/**
 * Returns a distance timestamp to never notify about the certificate again
 */
fun DccHolder.neverAgainNotificationTimestamp(): Long {
    return LocalDateTime.now().plusYears(100).toInstant(OffsetDateTime.now().offset).toEpochMilli()
}