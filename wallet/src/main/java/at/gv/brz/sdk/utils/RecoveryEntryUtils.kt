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

import at.gv.brz.sdk.euhealthcert.RecoveryEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.*


fun RecoveryEntry.isTargetDiseaseCorrect(): Boolean {
	return this.disease == AcceptanceCriteriasConstants.TARGET_DISEASE
}

fun RecoveryEntry.getRecoveryCountry(showEnglishVersionForLabels: Boolean): String {
	return try {
		val loc = Locale("", this.countryOfTest)
		var countryString = loc.displayCountry
		if (showEnglishVersionForLabels) {
			countryString = "$countryString / ${loc.getDisplayCountry(Locale.ENGLISH)}"
		}
		return countryString
	} catch (e: Exception) {
		this.countryOfTest
	}
}

fun RecoveryEntry.getIssuer(): String {
	return this.certificateIssuer
}

fun RecoveryEntry.getCertificateIdentifier(): String {
	return this.certificateIdentifier
}

fun RecoveryEntry.firstPositiveResult(): LocalDateTime? {
	if (this.dateFirstPositiveTest.isEmpty()) {
		return null
	}
	val date: LocalDate?
	try {
		date = LocalDate.parse(this.dateFirstPositiveTest, ISO_DATE)
	} catch (e: Exception) {
		return null
	}
	return date.atStartOfDay()
}



