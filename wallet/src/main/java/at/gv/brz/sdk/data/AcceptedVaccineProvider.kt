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
import at.gv.brz.sdk.euhealthcert.VaccinationEntry
import at.gv.brz.sdk.utils.SingletonHolder

class AcceptedVaccineProvider private constructor(context: Context) {

	companion object : SingletonHolder<AcceptedVaccineProvider, Context>(::AcceptedVaccineProvider) {
	}

	private val certificateSecureStorage: CertificateSecureStorage = CertificateSecureStorage.getInstance(context)

	private fun getValueSetDisplayValue(valueSetKey: String, valueKey: String): String {
		return certificateSecureStorage.mappedValueSetObjects?.get(valueSetKey)?.valueSetValues?.get(valueKey)?.display ?: valueKey
	}

	fun getVaccineName(vaccinationEntry: VaccinationEntry): String {
		return getValueSetDisplayValue("vaccines-covid-19-names", vaccinationEntry.medicinialProduct)
	}

	fun getProphylaxis(vaccinationEntry: VaccinationEntry): String {
		return getValueSetDisplayValue("sct-vaccines-covid-19", vaccinationEntry.vaccine)
	}

	fun getAuthHolder(vaccinationEntry: VaccinationEntry): String {
		return getValueSetDisplayValue("vaccines-covid-19-auth-holders", vaccinationEntry.marketingAuthorizationHolder)
	}
}