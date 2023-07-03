package at.gv.brz.sdk

import at.gv.brz.brvc.BusinessRulesValidator
import at.gv.brz.brvc.model.ValidationResult
import at.gv.brz.brvc.model.data.BusinessRuleCertificateType
import at.gv.brz.brvc.model.data.ValidityTimeModificationUnit
import at.gv.brz.brvc.util.dateByAddingUnitAndValue
import at.gv.brz.sdk.euhealthcert.Eudgc
import at.gv.brz.sdk.models.DccHolder
import at.gv.brz.sdk.utils.TestType
import at.gv.brz.sdk.utils.firstPositiveResult
import at.gv.brz.sdk.utils.vaccineDate
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.time.*
import java.time.format.DateTimeFormatter

fun String.parsedCertificateType(): BusinessRuleCertificateType? {
    if (BusinessRuleCertificateType.VACCINATION.value.lowercase() == lowercase()) {
        return BusinessRuleCertificateType.VACCINATION
    } else if (BusinessRuleCertificateType.TEST.value.lowercase() == lowercase()) {
        return BusinessRuleCertificateType.TEST
    } else if (BusinessRuleCertificateType.RECOVERY.value.lowercase() == lowercase()) {
        return BusinessRuleCertificateType.RECOVERY
    } else if (BusinessRuleCertificateType.VACCINATION_EXEMPTION.value.lowercase() == lowercase()) {
        return BusinessRuleCertificateType.VACCINATION_EXEMPTION
    }
    return null
}

fun TestType.readableName(): String {
    return when (this) {
        TestType.PCR -> "PCR"
        TestType.RAT -> "RAT"
    }
}

/**
 * Verification class to evaluate external conditions that the app allows to handle
 */
class ExternalConditionVerifier(
    private val originalCertificate: DccHolder?,
    private val otherCertificates: List<DccHolder>,
    private val otherCertificatesForSamePerson: List<DccHolder>,
    private val condition: String,
    private val parameters: Map<String,String>,
    private val region: String,
    private val profile: String,
    private val validationTime: ZonedDateTime,
    private val validationCore: BusinessRulesValidator?
) {

    fun evaluateCondition(): Any? {
        val externalCondition = ExternalCondition.fromConditionName(condition)?: return null
        when (externalCondition) {
            ExternalCondition.HAS_VALID_CERTIFICATE_FOR_SAME_PERSON -> {
                val type = parameters[ExternalConditionParameter.TYPE.parameterName]?.parsedCertificateType() ?: return null
                return hasValidCertificateForPerson(type, parameters)
            }
            ExternalCondition.GET_VALID_CERTIFICATE_FOR_SAME_PERSON -> {
                val type = parameters[ExternalConditionParameter.TYPE.parameterName]?.parsedCertificateType() ?: return null
                return getValidCertificateForPerson(type, parameters)
            }
            ExternalCondition.HAS_NO_VALID_CERTIFICATE_FOR_SAME_PERSON -> {
                val type = parameters[ExternalConditionParameter.TYPE.parameterName]?.parsedCertificateType() ?: return null
                return !hasValidCertificateForPerson(type, parameters)
            }
            ExternalCondition.HAS_NO_CERTIFICATE_FOR_SAME_PERSON -> {
                val type = parameters[ExternalConditionParameter.TYPE.parameterName]?.parsedCertificateType() ?: return null
                return !hasCertificateForPerson(type, parameters)
            }
            ExternalCondition.HAS_CERTIFICATE_FOR_SAME_PERSON -> {
                val type = parameters[ExternalConditionParameter.TYPE.parameterName]?.parsedCertificateType() ?: return null
                return hasCertificateForPerson(type, parameters)
            }
            ExternalCondition.HAS_NO_CERTIFICATE -> {
                val type = parameters[ExternalConditionParameter.TYPE.parameterName]?.parsedCertificateType() ?: return null
                return !hasCertificate(type, parameters)
            }
            ExternalCondition.HAS_CERTIFICATE -> {
                val type = parameters[ExternalConditionParameter.TYPE.parameterName]?.parsedCertificateType() ?: return null
                return hasCertificate(type, parameters)
            }
        }
    }


    private fun hasCertificateForPerson(type: BusinessRuleCertificateType, parameters: Map<String, String>): Boolean {
        return getCertificate(type, parameters, true) != null
    }

    private fun hasCertificate(type: BusinessRuleCertificateType, parameters: Map<String, String>): Boolean {
        return getCertificate(type, parameters, false) != null
    }

    private fun hasValidCertificateForPerson(type: BusinessRuleCertificateType, parameters: Map<String, String>): Boolean {
        return getValidCertificateForPerson(type, parameters) != null
    }

    private fun getCertificate(type: BusinessRuleCertificateType, parameters: Map<String, String>, forSamePerson: Boolean): DccHolder? {
        val certificates = (if (forSamePerson) otherCertificatesForSamePerson else otherCertificates).filter { it.businessRuleCertificateType() == type }

        for (cert in certificates) {
            if (certificateMatchesAdditionalParameters(type, cert, parameters)) {
                return cert
            }
        }
        return null
    }

    private fun getValidCertificateForPerson(type: BusinessRuleCertificateType, parameters: Map<String, String>): DccHolder? {
        val certificates = otherCertificatesForSamePerson.filter { it.businessRuleCertificateType() == type }

        val objectMapper = ObjectMapper().apply {
            this.findAndRegisterModules()
            registerModule(JavaTimeModule())
        }

        for (cert in certificates) {
            val certificatePayload = objectMapper.writeValueAsString(cert.euDGC)
            val validationResult = validationCore?.evaluateCertificate(
                certificate = certificatePayload,
                certificateType = cert.businessRuleCertificateType(),
                expiration = cert.expirationTime!!.atZone(ZoneId.systemDefault()),
                issue = cert.issuedAt!!.atZone(ZoneId.systemDefault()),
                country = "AT",
                region = region,
                profile = profile,
                originalCertificateObject = cert)
            if (validationResult is ValidationResult.Valid && certificateMatchesAdditionalParameters(type, cert, parameters)) {
                return cert
            }
        }
        return null
    }

    private fun certificateMatchesAdditionalParameters(type: BusinessRuleCertificateType, certificate: DccHolder, parameters: Map<String, String>): Boolean {
        for (entry in parameters) {
            val externalConditionParameter = ExternalConditionParameter.fromParameterName(entry.key) ?: continue
            when (externalConditionParameter) {
                ExternalConditionParameter.TYPE -> { continue }
                ExternalConditionParameter.TEST_TYPE -> {
                    if (entry.value == TestType.PCR.readableName() && certificate.euDGC.tests?.firstOrNull()?.type != TestType.PCR.code) {
                        return false
                    }
                    if (entry.value == TestType.RAT.readableName() && certificate.euDGC.tests?.firstOrNull()?.type != TestType.RAT.code) {
                        return false
                    }
                }
                ExternalConditionParameter.AGE_IN_DAYS_LESS_THAN -> {
                    entry.value.toIntOrNull()?.let { age ->
                        var date: LocalDateTime? = null
                        certificate.euDGC.tests?.firstOrNull()?.let {
                            date = LocalDateTime.ofInstant(it.timestampSample.toInstant(), ZoneOffset.UTC)
                        }
                        certificate.euDGC.pastInfections?.firstOrNull()?.let {
                            date = it.firstPositiveResult()
                        }
                        certificate.euDGC.vaccinations?.firstOrNull()?.let {
                            date = it.vaccineDate()
                        }
                        date?.let {
                            if (!it.dateByAddingUnitAndValue(ValidityTimeModificationUnit.DAY, age).isAfter(validationTime.toLocalDateTime())) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.AGE_IN_DAYS_MORE_THAN -> {
                    entry.value.toIntOrNull()?.let { age ->
                        var date: LocalDateTime? = null
                        certificate.euDGC.tests?.firstOrNull()?.let {
                            date = LocalDateTime.ofInstant(it.timestampSample.toInstant(), ZoneOffset.UTC)
                        }
                        certificate.euDGC.pastInfections?.firstOrNull()?.let {
                            date = it.firstPositiveResult()
                        }
                        certificate.euDGC.vaccinations?.firstOrNull()?.let {
                            date = it.vaccineDate()
                        }
                        date?.let {
                            if (!it.dateByAddingUnitAndValue(ValidityTimeModificationUnit.DAY, age).isBefore(validationTime.toLocalDateTime())) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.AGE_IN_HOURS_LESS_THAN -> {
                    entry.value.toIntOrNull()?.let { age ->
                        var date: LocalDateTime? = null
                        certificate.euDGC.tests?.firstOrNull()?.let {
                            date = LocalDateTime.ofInstant(it.timestampSample.toInstant(), ZoneOffset.UTC)
                        }
                        certificate.euDGC.pastInfections?.firstOrNull()?.let {
                            date = it.firstPositiveResult()
                        }
                        certificate.euDGC.vaccinations?.firstOrNull()?.let {
                            date = it.vaccineDate()
                        }
                        date?.let {
                            if (!it.dateByAddingUnitAndValue(ValidityTimeModificationUnit.HOUR, age).isAfter(validationTime.toLocalDateTime())) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.AGE_IN_HOURS_MORE_THAN -> {
                    entry.value.toIntOrNull()?.let { age ->
                        var date: LocalDateTime? = null
                        certificate.euDGC.tests?.firstOrNull()?.let {
                            date = LocalDateTime.ofInstant(it.timestampSample.toInstant(), ZoneOffset.UTC)
                        }
                        certificate.euDGC.pastInfections?.firstOrNull()?.let {
                            date = it.firstPositiveResult()
                        }
                        certificate.euDGC.vaccinations?.firstOrNull()?.let {
                            date = it.vaccineDate()
                        }
                        date?.let {
                            if (!it.dateByAddingUnitAndValue(ValidityTimeModificationUnit.HOUR, age).isBefore(validationTime.toLocalDateTime())) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.VACCINE_TYPE -> {
                    certificate.euDGC.vaccinations?.firstOrNull()?.let {
                        if (it.medicinialProduct != entry.value) {
                            return false
                        }
                    }
                }
                ExternalConditionParameter.VACCINE_TYPE_NOT_EQUAL -> {
                    certificate.euDGC.vaccinations?.firstOrNull()?.let {
                        if (it.medicinialProduct == entry.value) {
                            return false
                        }
                    }
                }
                ExternalConditionParameter.DOSE_LESS_THAN -> {
                    entry.value.toIntOrNull()?.let { dose ->
                        certificate.euDGC.vaccinations?.firstOrNull()?.let {
                            if (!(it.doseNumber < dose)) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.DOSE_LESS_THAN_OR_EQUAL -> {
                    entry.value.toIntOrNull()?.let { dose ->
                        certificate.euDGC.vaccinations?.firstOrNull()?.let {
                            if (!(it.doseNumber <= dose)) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.DOSE_GREATER_THAN -> {
                    entry.value.toIntOrNull()?.let { dose ->
                        certificate.euDGC.vaccinations?.firstOrNull()?.let {
                            if (!(it.doseNumber > dose)) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.DOSE_GREATER_THAN_OR_EQUAL -> {
                    entry.value.toIntOrNull()?.let { dose ->
                        certificate.euDGC.vaccinations?.firstOrNull()?.let {
                            if (!(it.doseNumber >= dose)) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.DOSE_EQUAL -> {
                    entry.value.toIntOrNull()?.let { dose ->
                        certificate.euDGC.vaccinations?.firstOrNull()?.let {
                            if (it.doseNumber != dose) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.DOES_NOT_EQUAL -> {
                    entry.value.toIntOrNull()?.let { dose ->
                        certificate.euDGC.vaccinations?.firstOrNull()?.let {
                            if (it.doseNumber == dose) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.PERSON_AGE_IN_YEARS_LESS_THAN -> {
                    certificate.euDGC.birthDate()?.let { birthdate ->
                        entry.value.toIntOrNull()?.let { years ->
                            if (!birthdate.atStartOfDay().plusYears(years.toLong()).isAfter(validationTime.toLocalDateTime())) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.PERSON_AGE_IN_YEARS_MORE_THAN -> {
                    certificate.euDGC.birthDate()?.let { birthdate ->
                        entry.value.toIntOrNull()?.let { years ->
                            if (!birthdate.atStartOfDay().plusYears(years.toLong()).isBefore(validationTime.toLocalDateTime())) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.PERSON_AGE_IN_MONTHS_LESS_THAN -> {
                    certificate.euDGC.birthDate()?.let { birthdate ->
                        entry.value.toIntOrNull()?.let { months ->
                            if (!birthdate.atStartOfDay().plusMonths(months.toLong()).isAfter(validationTime.toLocalDateTime())) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.PERSON_AGE_IN_MONTHS_MORE_THAN -> {
                    certificate.euDGC.birthDate()?.let { birthdate ->
                        entry.value.toIntOrNull()?.let { months ->
                            if (!birthdate.atStartOfDay().plusMonths(months.toLong()).isBefore(validationTime.toLocalDateTime())) {
                                return false
                            }
                        }
                    }
                }
                ExternalConditionParameter.IS_ISSUED_BEFORE_CURRENT_CERTIFICATE -> {
                    certificate.issuedAt?.let { currentCertificateIssueDate ->
                        originalCertificate?.issuedAt?.let { certificateIssueDate ->
                            if (entry.value == "true") {
                                if (!currentCertificateIssueDate.isBefore(certificateIssueDate)) {
                                    return false
                                }
                            } else {
                                if (!currentCertificateIssueDate.isAfter(certificateIssueDate)) {
                                    return false
                                }
                            }
                        }
                    }
                }
                ExternalConditionParameter.IS_ISSUED_AFTER_CURRENT_CERTIFICATE -> {
                    certificate.issuedAt?.let { currentCertificateIssueDate ->
                        originalCertificate?.issuedAt?.let { certificateIssueDate ->
                            if (entry.value == "true") {
                                if (!currentCertificateIssueDate.isAfter(certificateIssueDate)) {
                                    return false
                                }
                            } else {
                                if (!currentCertificateIssueDate.isBefore(certificateIssueDate)) {
                                    return false
                                }
                            }
                        }
                    }
                }
            }
        }
        return true
    }
}

fun Eudgc.birthDate(): LocalDate? {
    return try {
        LocalDate.parse(dateOfBirth, DateTimeFormatter.ISO_DATE)
    } catch (e: java.lang.Exception) {
        null
    }
}