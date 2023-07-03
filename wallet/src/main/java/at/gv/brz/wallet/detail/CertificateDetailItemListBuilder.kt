/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.detail

import android.content.Context
import android.content.res.Configuration
import at.gv.brz.common.util.LocaleUtil
import at.gv.brz.sdk.data.AcceptedTestProvider
import at.gv.brz.sdk.data.AcceptedVaccineProvider
import at.gv.brz.sdk.models.DccHolder
import at.gv.brz.sdk.utils.*
import at.gv.brz.wallet.R
import java.util.*
import kotlin.collections.ArrayList

class CertificateDetailItemListBuilder(val context: Context, val dccHolder: DccHolder, val showEnglishVersion: Boolean = true) {
	private val showEnglishVersionForLabels = showEnglishVersion && LocaleUtil.isSystemLangNotEnglish(context)

	fun buildAll(): List<CertificateDetailItem> {
		val detailItems = ArrayList<CertificateDetailItem>()
		detailItems.addAll(buildVaccinationEntries())
		detailItems.addAll(buildRecoveryEntries())
		detailItems.addAll(buildTestEntries())
		detailItems.addAll(buildVaccinationExemptionEntries())
		return detailItems
	}

	private fun buildVaccinationEntries(): List<CertificateDetailItem> {
		val detailItems = ArrayList<CertificateDetailItem>()
		val vaccinations = dccHolder.euDGC.vaccinations

		if (vaccinations.isNullOrEmpty()) {
			return detailItems
		}

		detailItems.add(DividerItem)
		detailItems.add(TitleItem(R.string.covid_certificate_vaccination_title, showEnglishVersionForLabels))

		for (vaccinationEntry in vaccinations) {
			detailItems.add(DividerItem)

			detailItems.add(ValueItem(R.string.wallet_certificate_impfdosis_title, vaccinationEntry.getNumberOverTotalDose(),
				showEnglishVersionForLabels))

			// Vaccine data
			if (vaccinationEntry.isTargetDiseaseCorrect()) {
				detailItems.add(
					ValueItem(R.string.wallet_certificate_target_disease_title,
						context.getString(R.string.target_disease_name),
						showEnglishVersionForLabels)
				)
			}
			val acceptedTestProvider = AcceptedVaccineProvider.getInstance(context)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccine_prophylaxis,
				acceptedTestProvider.getProphylaxis(vaccinationEntry), showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_impfstoff_product_name_title,
				acceptedTestProvider.getVaccineName(vaccinationEntry), showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_impfstoff_holder,
				acceptedTestProvider.getAuthHolder(vaccinationEntry), showEnglishVersionForLabels))

			// Vaccination date + country
			detailItems.add(DividerItem)
			detailItems.add(
				ValueItem(
					R.string.wallet_certificate_vaccination_date_title,
					vaccinationEntry.vaccinationDate.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER),
					showEnglishVersionForLabels
				)
			)

			detailItems.add(
				ValueItem(R.string.wallet_certificate_vaccination_country_title,
					vaccinationEntry.getVaccinationCountry(showEnglishVersionForLabels),
					showEnglishVersionForLabels
				))

			// Issuer
			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_issuer_title,
				vaccinationEntry.getIssuer(), showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_identifier,
				vaccinationEntry.getCertificateIdentifier(),
				false))
			dccHolder.issuedAt?.prettyPrint(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { dateString ->
				val dateText = context.getString(R.string.wallet_certificate_date).replace("{DATE}", dateString)
				detailItems.add(ValueItemWithoutLabel(dateText))
				if (showEnglishVersionForLabels) {
					val dateTextEnglish =
						getEnglishTranslation(context, R.string.wallet_certificate_date).replace("{DATE}", dateString)
					detailItems.add(ValueItemWithoutLabel(dateTextEnglish, true))
				}

			}
		}
		return detailItems
	}

	private fun buildRecoveryEntries(): List<CertificateDetailItem> {
		val detailItems = ArrayList<CertificateDetailItem>()
		val recoveries = dccHolder.euDGC.pastInfections

		if (recoveries.isNullOrEmpty()) {
			return detailItems
		}

		detailItems.add(DividerItem)
		detailItems.add(TitleItem(R.string.covid_certificate_recovery_title, showEnglishVersionForLabels))

		for (recoveryEntry in recoveries) {
			detailItems.add(DividerItem)
			if (recoveryEntry.isTargetDiseaseCorrect()) {
				detailItems.add(
					ValueItem(R.string.wallet_certificate_target_disease_title,
						context.getString(R.string.target_disease_name),
						showEnglishVersionForLabels)
				)
			}

			// Recovery dates + country
			detailItems.add(
				ValueItem(
					R.string.wallet_certificate_recovery_first_positiv_result,
					recoveryEntry.dateFirstPositiveTest.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER),
					showEnglishVersionForLabels
				)
			)

			detailItems.add(ValueItem(R.string.wallet_certificate_test_land,
				recoveryEntry.getRecoveryCountry(showEnglishVersionForLabels), showEnglishVersionForLabels))

			// Issuer
			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_issuer_title,
				recoveryEntry.getIssuer(),
				showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_identifier, recoveryEntry.getCertificateIdentifier(), false))

			dccHolder.issuedAt?.prettyPrint(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { dateString ->
				val dateText = context.getString(R.string.wallet_certificate_date).replace("{DATE}", dateString)
				detailItems.add(ValueItemWithoutLabel(dateText))
				if (showEnglishVersionForLabels) {
					val dateTextEnglish =
						getEnglishTranslation(context, R.string.wallet_certificate_date).replace("{DATE}", dateString)
					detailItems.add(ValueItemWithoutLabel(dateTextEnglish, true))
				}
			}
		}
		return detailItems
	}

	private fun buildTestEntries(): List<CertificateDetailItem> {
		val detailItems = ArrayList<CertificateDetailItem>()
		val tests = dccHolder.euDGC.tests

		if (tests.isNullOrEmpty()) {
			return detailItems
		}

		detailItems.add(DividerItem)
		detailItems.add(TitleItem(R.string.covid_certificate_test_title, showEnglishVersionForLabels))

		for (testEntry in tests) {
			detailItems.add(DividerItem)

			// Test result
			if (testEntry.isTargetDiseaseCorrect()) {
				detailItems.add(
					ValueItem(R.string.wallet_certificate_target_disease_title,
						context.getString(R.string.target_disease_name),
						showEnglishVersionForLabels)
				)
			}

			val resultStringId =
				if (testEntry.isNegative()) R.string.wallet_certificate_test_result_negativ else R.string.wallet_certificate_test_result_positiv
			var value = context.getString(resultStringId)
			if (showEnglishVersionForLabels) {
				value = "$value\n${getEnglishTranslation(context, resultStringId)}"
			}
			detailItems.add(ValueItem(R.string.wallet_certificate_test_result_title, value, showEnglishVersionForLabels))

			// Test details
			val acceptedTestProvider = AcceptedTestProvider.getInstance(context)
			detailItems.add(ValueItem(R.string.wallet_certificate_test_type,
				acceptedTestProvider.getTestType(testEntry),
				showEnglishVersionForLabels))
			acceptedTestProvider.getTestName(testEntry)?.let {
				detailItems.add(ValueItem(R.string.wallet_certificate_test_name, it, showEnglishVersionForLabels))
			}
			acceptedTestProvider.getManufacturesIfExists(testEntry)?.let {
				detailItems.add(ValueItem(R.string.wallet_certificate_test_holder, it, showEnglishVersionForLabels))
			}

			// Test dates + country
			detailItems.add(DividerItem)
			testEntry.getFormattedSampleDate(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { sampleDate ->
				detailItems.add(
					ValueItem(
						R.string.wallet_certificate_test_sample_date_title,
						sampleDate,
						showEnglishVersionForLabels
					)
				)
			}
			testEntry.getFormattedResultDate(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { resultDate ->
				detailItems.add(
					ValueItem(
						R.string.wallet_certificate_test_result_date_title,
						resultDate,
						showEnglishVersionForLabels
					)
				)
			}

			testEntry.getTestCenter()?.let { testCenter ->
				detailItems.add(ValueItem(R.string.wallet_certificate_test_done_by, testCenter, showEnglishVersionForLabels))
			}
			detailItems.add(ValueItem(R.string.wallet_certificate_test_land,
				testEntry.getTestCountry(showEnglishVersionForLabels), showEnglishVersionForLabels))

			// Issuer
			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_issuer_title,
				testEntry.getIssuer(),
				showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_identifier, testEntry.getCertificateIdentifier(), false))

			dccHolder.issuedAt?.prettyPrint(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { dateString ->
				val dateText = context.getString(R.string.wallet_certificate_date).replace("{DATE}", dateString)
				detailItems.add(ValueItemWithoutLabel(dateText))
				if (showEnglishVersionForLabels) {
					val dateTextEnglish =
						getEnglishTranslation(context, R.string.wallet_certificate_date).replace("{DATE}", dateString)
					detailItems.add(ValueItemWithoutLabel(dateTextEnglish, true))
				}
			}
		}
		return detailItems
	}

	private fun buildVaccinationExemptionEntries(): List<CertificateDetailItem> {
		val detailItems = ArrayList<CertificateDetailItem>()
		val vaccinationExemptions = dccHolder.euDGC.vaccinationExemptions

		if (vaccinationExemptions.isNullOrEmpty()) {
			return detailItems
		}

		detailItems.add(DividerItem)
		detailItems.add(TitleItem(R.string.covid_certificate_vaccination_exemption_title, showEnglishVersionForLabels))

		for (exemptionEntry in vaccinationExemptions) {
			detailItems.add(DividerItem)

			var exemptionValue = context.getString(R.string.wallet_certificate_exemption_reason_value)
			if (showEnglishVersionForLabels) {
				val config = Configuration(context.resources.configuration)
				config.setLocale(Locale.ENGLISH)
				exemptionValue = "${exemptionValue} /\n${context.createConfigurationContext(config).getText(R.string.wallet_certificate_exemption_reason_value).toString()}"
			}
			detailItems.add(
				ValueItem(R.string.wallet_certificate_exemption_reason_title,
					exemptionValue,
					showEnglishVersionForLabels)
			)

			// Test result
			if (exemptionEntry.isTargetDiseaseCorrect()) {
				detailItems.add(
					ValueItem(R.string.wallet_certificate_target_disease_title,
						context.getString(R.string.target_disease_name),
						showEnglishVersionForLabels)
				)
			}

			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_exemption_country_title,
				exemptionEntry.getExemptionCountry(showEnglishVersionForLabels), showEnglishVersionForLabels))

			// Issuer
			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_issuer_title,
				exemptionEntry.getIssuer(),
				showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_identifier, exemptionEntry.getCertificateIdentifier(), false))

			dccHolder.issuedAt?.prettyPrint(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { dateString ->
				val dateText = context.getString(R.string.wallet_certificate_date).replace("{DATE}", dateString)
				detailItems.add(ValueItemWithoutLabel(dateText))
				if (showEnglishVersionForLabels) {
					val dateTextEnglish =
						getEnglishTranslation(context, R.string.wallet_certificate_date).replace("{DATE}", dateString)
					detailItems.add(ValueItemWithoutLabel(dateTextEnglish, true))
				}
			}
		}

		return detailItems
	}
}