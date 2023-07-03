/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk.data

import android.content.Context
import at.gv.brz.sdk.euhealthcert.TestEntry
import at.gv.brz.sdk.utils.SingletonHolder
import at.gv.brz.sdk.utils.TestType

class AcceptedTestProvider private constructor(context: Context) {

	companion object : SingletonHolder<AcceptedTestProvider, Context>(::AcceptedTestProvider) {
	}
	private val certificateSecureStorage: CertificateSecureStorage = CertificateSecureStorage.getInstance(context)

	private fun getValueSetDisplayValue(valueSetKey: String, valueKey: String): String {
		return certificateSecureStorage.mappedValueSetObjects?.get(valueSetKey)?.valueSetValues?.get(valueKey)?.display ?: valueKey
	}

	fun getTestType(testEntry: TestEntry): String {
		return getValueSetDisplayValue("covid-19-lab-test-type", testEntry.type)
	}

	fun getTestName(testEntry: TestEntry): String? {
		if (testEntry.type.equals(TestType.PCR.code)) {
			return testEntry.naaTestName ?: "PCR"
		} else if (testEntry.type.equals(TestType.RAT.code)) {
			return testEntry.naaTestName
		}
		return null
	}

	fun getManufacturesIfExists(testEntry: TestEntry): String? {
		if (testEntry.ratTestNameAndManufacturer != null) {
			return getValueSetDisplayValue(
				"covid-19-lab-test-manufacturer-and-name",
				testEntry.ratTestNameAndManufacturer
			)
		}
		return null
	}
}