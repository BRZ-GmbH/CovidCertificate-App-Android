/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk.data.state

import at.gv.brz.brvc.model.ValidationResult
import at.gv.brz.sdk.models.DccHolder

sealed class DecodeState {
	data class SUCCESS(val dccHolder: DccHolder) : DecodeState()
	data class ERROR(val error: StateError) : DecodeState()
}

/**
 * Verification Result for a single certificate. If verification was successful, it
 * contains the verification results for all regions
 */
sealed class VerificationResultStatus {
	object LOADING: VerificationResultStatus()
	object SIGNATURE_INVALID: VerificationResultStatus()
	object ERROR: VerificationResultStatus()
	object TIMEMISSING: VerificationResultStatus()
	object DATAEXPIRED: VerificationResultStatus()
	data class SUCCESS(val results: Map<String,ValidationResult>): VerificationResultStatus()

	fun isInvalid(): Boolean {
		if (this is SIGNATURE_INVALID || this is ERROR || this.containsOnlyInvalidVerification()) {
			return true
		}
		return false
	}

	fun containsOnlyInvalidVerification(): Boolean {
		if (this is SUCCESS) {
			return results.values.filter { it is ValidationResult.Valid }.count() == 0
		}
		return false
	}
}

data class StateError(val code: String, val message: String? = null, val dccHolder: DccHolder? = null)
