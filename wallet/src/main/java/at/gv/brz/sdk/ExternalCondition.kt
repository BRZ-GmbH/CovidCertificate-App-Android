package at.gv.brz.sdk

/**
 * Supported External Conditions
 */
enum class ExternalCondition(val conditionName: String) {
    HAS_VALID_CERTIFICATE_FOR_SAME_PERSON("hasValidCertificateForSamePerson"),
    GET_VALID_CERTIFICATE_FOR_SAME_PERSON("getValidCertificateForSamePerson"),
    HAS_NO_VALID_CERTIFICATE_FOR_SAME_PERSON("hasNoValidCertificateForSamePerson"),
    HAS_NO_CERTIFICATE_FOR_SAME_PERSON("hasNoCertificateForSamePerson"),
    HAS_CERTIFICATE_FOR_SAME_PERSON("hasCertificateForSamePerson"),
    HAS_NO_CERTIFICATE("hasNoCertificate"),
    HAS_CERTIFICATE("hasCertificate");

    companion object {
        fun fromConditionName(name: String): ExternalCondition? {
            return values().firstOrNull { it.conditionName == name }
        }
    }
}