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

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_DATE
import java.time.format.DateTimeFormatter.ISO_DATE_TIME


val DEFAULT_DISPLAY_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val DEFAULT_DISPLAY_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")


fun Instant.prettyPrint(dateFormatter: DateTimeFormatter): String {
	return try {
		this.atZone(ZoneId.systemDefault()).format(dateFormatter)
	} catch (e: Throwable) {
		this.toString()
	}
}

fun String.prettyPrintIsoDateTime(dateTimeFormatter: DateTimeFormatter): String {
	return try {
		// Dates may be either given as 1970-01-01T00:00:00Z ...
		LocalDate.parse(this, ISO_DATE_TIME).format(dateTimeFormatter)
	} catch (e: java.lang.Exception) {
		try {
			// ... or as 1970-01-01
			LocalDate.parse(this, ISO_DATE).format(dateTimeFormatter)
		} catch (e: java.lang.Exception) {
			// Fall back to the original string if all parsing + formatting attempts fail
			this
		}
	}
}
