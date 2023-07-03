package at.gv.brz.sdk

/**
 * Supported external condition parameters
 */
enum class ExternalConditionParameter(val parameterName: String) {
    TYPE("type"),
    TEST_TYPE("testType"),
    AGE_IN_DAYS_LESS_THAN("ageInDaysLessThan"),
    AGE_IN_DAYS_MORE_THAN("ageInDaysMoreThan"),
    AGE_IN_HOURS_LESS_THAN("ageInHoursLessThan"),
    AGE_IN_HOURS_MORE_THAN("ageInHoursMoreThan"),
    VACCINE_TYPE("vaccineType"),
    VACCINE_TYPE_NOT_EQUAL("vaccineTypeNotEqual"),
    DOSE_LESS_THAN("doseLessThan"),
    DOSE_LESS_THAN_OR_EQUAL("doseLessThanOrEqual"),
    DOSE_GREATER_THAN("doseGreaterThan"),
    DOSE_GREATER_THAN_OR_EQUAL("doseGreaterThanOrEqual"),
    DOSE_EQUAL("doseEqual"),
    DOES_NOT_EQUAL("doseNotEqual"),
    PERSON_AGE_IN_YEARS_LESS_THAN("personAgeInYearsLessThan"),
    PERSON_AGE_IN_YEARS_MORE_THAN("personAgeInYearsMoreThan"),
    PERSON_AGE_IN_MONTHS_LESS_THAN("personAgeInMonthsLessThan"),
    PERSON_AGE_IN_MONTHS_MORE_THAN("personAgeInMonthsMoreThan"),
    IS_ISSUED_BEFORE_CURRENT_CERTIFICATE("isIssuedBeforeCurrentCertificate"),
    IS_ISSUED_AFTER_CURRENT_CERTIFICATE("isIssuedAfterCurrentCertificate");

    companion object {
        fun fromParameterName(name: String): ExternalConditionParameter? {
            return values().firstOrNull { it.parameterName == name }
        }
    }
}