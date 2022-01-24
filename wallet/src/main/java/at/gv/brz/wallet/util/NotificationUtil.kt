package at.gv.brz.wallet.util

import android.content.Context
import at.gv.brz.common.config.*
import at.gv.brz.eval.certificateType
import at.gv.brz.eval.euhealthcert.Eudgc
import at.gv.brz.eval.models.CertType
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.eval.utils.ValidationUtil
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.wallet.R
import at.gv.brz.wallet.data.NotificationSecureStorage
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dgca.verifier.app.engine.data.CertificateType
import java.time.*

data class QueuedCampaignNotification(val certificate: Eudgc?, val campaign: CampaignModel, val title: String?, val message: String?)

data class CampaignNotificationCheckResult(val certificateCombinationHash: Int, val queuedNotifications: List<QueuedCampaignNotification>)
/**
 * Handles determining eligibility for Vaccination booster reminders
 */
class NotificationUtil {

    companion object {
        val maximumSupportedCampaignVersion = 1
        val testCertificateExpirationPeriod: Long = 60 * 60 * 48 * 1000L
        val recurringExpiredTestCertificateNotificationPeriod: Long =
            if (BuildConfig.FLAVOR == "abn") {
                1000L * 60 * 10
            } else {
                1000L * 60 * 60 * 24 * 7
            }
    }

    fun testCertificateEligibleForAutomaticRemoval(certificates: List<DccHolder>, notificationSecureStorage: NotificationSecureStorage): List<DccHolder> {
        if (notificationSecureStorage.shouldIgnoreExpiredTestCertificates()) {
            return listOf()
        }

        if (notificationSecureStorage.getLastExpiredTestCertificateReminderDate() > 0 && (Instant.now().toEpochMilli() - notificationSecureStorage.getLastExpiredTestCertificateReminderDate()) < recurringExpiredTestCertificateNotificationPeriod) {
            return listOf()
        }
        val expiredTestCertificates = certificates.filter {
            it.certType == CertType.TEST && (it.expirationTime != null && (Instant.now().toEpochMilli() - it.expirationTime!!.toEpochMilli()) > testCertificateExpirationPeriod)
        }
        return expiredTestCertificates
    }

    fun startCertificateNotificationCheck(certificates: List<DccHolder>, valueSets: Map<String, List<String>>, validationClock: ZonedDateTime, config: ConfigModel, hasOptedOutOfNonImportantCampaigns: Boolean, certificateCampaignLastDisplayTimestamps: Map<String, Long>, context: Context): CampaignNotificationCheckResult {
        val hash = (certificates.joinToString("_") { it.qrCodeData } + "_${Instant.now().toEpochMilli()}").hashCode()
        val vaccinationCertificates = certificates.filter { it.certificateType() == CertificateType.VACCINATION /*&& it.expirationTime!!.isAfter(Instant.now())*/ }.sortedWith { o1, o2 ->
            if (o1.issuedAt!!.isAfter(o2.issuedAt!!)) {
                -1
            } else {
                1
            }
        }
        val groupedCertificates = vaccinationCertificates.groupBy {
            val name = "${it.euDGC.person.familyName} ${it.euDGC.person.givenName}"
            "${name}_${it.euDGC.dateOfBirth}"
        }

        val campaignsToDisplay: MutableList<QueuedCampaignNotification> = mutableListOf()

        val objectMapper = ObjectMapper().apply {
            this.findAndRegisterModules()
            registerModule(JavaTimeModule())
        }

        val languageKey = context.getString(R.string.language_key)
        config.campaigns?.forEach { campaign ->
            if (!campaign.hasCompatibleCampaignVersion()) {
                return@forEach
            }
            if (!campaign.isActive) {
                return@forEach
            }
            if (!campaign.important && hasOptedOutOfNonImportantCampaigns) {
                return@forEach
            }

            if (campaign.campaignType == CampaignType.SINGLE || campaign.campaignType == CampaignType.REPEATING) {
                if (campaign.shouldBeDisplayed(certificateCampaignLastDisplayTimestamps)) {
                    campaignsToDisplay.add(QueuedCampaignNotification(null, campaign, campaign.getTitle(languageKey), campaign.getMessage(languageKey)))
                }
            } else {
                var hasAddedCampaign = false

                val certificatesToCheck = vaccinationCertificates.toMutableList()
                if (campaign.applicationType == CampaignApplicationType.NEWEST_CERTIFICATE_PER_PERSON) {
                    certificatesToCheck.clear()
                    groupedCertificates.forEach { key, values ->
                        values.firstOrNull()?.let {
                            certificatesToCheck.add(it)
                        }
                    }
                }

                certificatesToCheck.forEach { certificate ->
                    val externalParameter = ValidationUtil.getExternalParameterStringForValidation(objectMapper, valueSets, validationClock, certificate.expirationTime!!.atZone(ZoneId.systemDefault()), certificate.issuedAt!!.atZone(ZoneId.systemDefault()))
                    if (!hasAddedCampaign && campaign.appliesToCertificate(certificate.euDGC, config.conditions ?: mapOf(), externalParameter)) {
                        if (campaign.shouldBeDisplayedForCertificate(certificate.euDGC, certificateCampaignLastDisplayTimestamps)) {
                            if (campaign.campaignType == CampaignType.SINGLE_ANY_CERTIFICATE || campaign.campaignType == CampaignType.REPEATING_ANY_CERTIFICATE) {
                                hasAddedCampaign = true
                            }

                            val jsonObjectForValidation = ValidationUtil.getJsonObjectForValidation(objectMapper, certificate.euDGC, externalParameter)

                            campaignsToDisplay.add(QueuedCampaignNotification(certificate.euDGC, campaign, campaign.getLocalizedTitleWithPlaceholders(languageKey, objectMapper, jsonObjectForValidation), campaign.getLocalizedMessageWithPlaceholders(languageKey, objectMapper, jsonObjectForValidation)))
                        }
                    }
                }
            }
        }
        return CampaignNotificationCheckResult(hash, campaignsToDisplay)
    }
}

fun CampaignModel.lastDisplayTimestampKeyForCertificate(certificate: Eudgc?): String? {
    when (campaignType) {
        CampaignType.SINGLE_EACH_CERTIFICATE, CampaignType.REPEATING_EACH_CERTIFICATE -> {
            val certificateIdentifier = certificate?.vaccinations?.firstOrNull()?.certificateIdentifier
                ?: return null
            return "${id}_${certificateIdentifier}"
        }
        else -> return "${id}_*"
    }
}

fun CampaignModel.shouldBeDisplayed(lastDisplayTimestamps: Map<String, Long>): Boolean {
    val key = lastDisplayTimestampKeyForCertificate(null) ?: false

    when (campaignType) {
        CampaignType.SINGLE_ANY_CERTIFICATE, CampaignType.SINGLE -> return !lastDisplayTimestamps.containsKey(key)
        CampaignType.REPEATING_ANY_CERTIFICATE, CampaignType.REPEATING -> {
            val repeat = repeatInterval ?: return false

            val lastDisplayTime = lastDisplayTimestamps[key] ?: return true

            return repeat.dateByAddingRepeatIntervalToDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(lastDisplayTime), ZoneOffset.UTC)).isBefore(LocalDateTime.now())
        }
        else -> return false
    }
}

fun CampaignModel.shouldBeDisplayedForCertificate(certificate: Eudgc, lastDisplayTimestamps: Map<String, Long>): Boolean {
    val key = lastDisplayTimestampKeyForCertificate(certificate) ?: false

    when (campaignType) {
        CampaignType.SINGLE_EACH_CERTIFICATE, CampaignType.SINGLE_ANY_CERTIFICATE -> return !lastDisplayTimestamps.containsKey(key)
        CampaignType.REPEATING_EACH_CERTIFICATE, CampaignType.REPEATING_ANY_CERTIFICATE -> {
            val repeat = repeatInterval ?: return false

            val lastDisplayTime = lastDisplayTimestamps[key] ?: return true

            return repeat.dateByAddingRepeatIntervalToDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(lastDisplayTime), ZoneOffset.UTC)).isBefore(LocalDateTime.now())
        }
        else -> return false
    }
}

fun CampaignModel.appliesToCertificate(certificate: Eudgc, conditions: Map<String, CertificateConditionModel>, externalParameterString: String): Boolean {
    val objectMapper = ObjectMapper().apply {
        this.findAndRegisterModules()
        registerModule(JavaTimeModule())
    }

    if (campaignType == CampaignType.SINGLE || campaignType == CampaignType.REPEATING) {
        return true
    }

    val groups = conditionGroups ?: return true
    if (groups.isEmpty()) {
        return true
    }

    val jsonObjectForValidation = ValidationUtil.getJsonObjectForValidation(objectMapper, certificate, externalParameterString)

    for (group in groups) {
        var isMatchingAllConditions = true

        for (conditionName in group) {
            val condition = conditions[conditionName]?.parsedLogic(objectMapper)
            if (condition != null) {
                val result = ValidationUtil.evaluateBooleanRule(condition, jsonObjectForValidation)
                if (result == null || result == false) {
                    isMatchingAllConditions = false
                }
            } else {
                isMatchingAllConditions = false
            }
        }

        if (isMatchingAllConditions) {
            return true
        }
    }

    return false
}

fun CampaignModel.hasCompatibleCampaignVersion(): Boolean {
    return version <= NotificationUtil.maximumSupportedCampaignVersion
}

fun CampaignModel.getLocalizedTitleWithPlaceholders(languageKey: String, objectMapper: ObjectMapper, validationObject: ObjectNode): String? {
    val localizedTitle = getTitle(languageKey) ?: return null

    return ValidationUtil.evaluatePlaceholdersInString(localizedTitle, objectMapper, validationObject)
}

fun CampaignModel.getLocalizedMessageWithPlaceholders(languageKey: String, objectMapper: ObjectMapper, validationObject: ObjectNode): String? {
    val localizedMessage = getMessage(languageKey) ?: return null

    return ValidationUtil.evaluatePlaceholdersInString(localizedMessage, objectMapper, validationObject)
}