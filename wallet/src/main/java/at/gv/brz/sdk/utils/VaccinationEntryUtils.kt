/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk.utils

import at.gv.brz.sdk.euhealthcert.VaccinationEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun VaccinationEntry.doseNumber(): Int = this.doseNumber

fun VaccinationEntry.totalDoses(): Int = this.totalDoses

fun VaccinationEntry.getNumberOverTotalDose(): String {
	return " ${this.doseNumber()}/${this.totalDoses()}"
}

fun VaccinationEntry.isTargetDiseaseCorrect(): Boolean {
	return this.disease == AcceptanceCriteriasConstants.TARGET_DISEASE
}

fun VaccinationEntry.vaccineDate(): LocalDateTime? {
	if (this.vaccinationDate.isEmpty()) {
		return null
	}
	val date: LocalDate?
	try {
		date = LocalDate.parse(this.vaccinationDate, DateTimeFormatter.ISO_DATE)
	} catch (e: Exception) {
		return null
	}
	return date.atStartOfDay()
}

fun VaccinationEntry.getVaccinationCountry(showEnglishVersionForLabels: Boolean): String {
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

fun VaccinationEntry.getIssuer(): String {
	return this.certificateIssuer
}

fun VaccinationEntry.getCertificateIdentifier(): String {
	return this.certificateIdentifier
}
