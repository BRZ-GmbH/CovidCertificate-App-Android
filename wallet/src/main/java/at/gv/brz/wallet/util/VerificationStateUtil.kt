/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.util

import android.content.Context
import android.text.SpannableString
import androidx.annotation.ColorRes
import at.gv.brz.common.util.addBoldDate
import at.gv.brz.common.util.makeSubStringBold
import at.gv.brz.eval.data.EvalErrorCodes
import at.gv.brz.eval.data.state.*
import at.gv.brz.wallet.R

const val DATE_REPLACEMENT_STRING = "{DATE}"

/**
 * The verification state indicates an offline mode if it is an ERROR and the error code is set to GENERAL_OFFLINE (G|OFF)
 */
fun VerificationState.isOfflineMode() = this is VerificationState.ERROR && this.error.code == EvalErrorCodes.GENERAL_OFFLINE

fun VerificationState.INVALID.getValidationStatusString(context: Context) = when {
	signatureState is CheckSignatureState.INVALID -> {
		val invalidSignatureState = signatureState as CheckSignatureState.INVALID
		if (invalidSignatureState.signatureErrorCode == EvalErrorCodes.SIGNATURE_TYPE_INVALID) {
			context.getString(R.string.wallet_error_invalid_format)
				.makeSubStringBold(context.getString(R.string.wallet_error_invalid_format_bold))
		} else if (invalidSignatureState.signatureErrorCode == EvalErrorCodes.SIGNATURE_TIMESTAMP_EXPIRED) {
			context.getString(R.string.wallet_error_expired)
				.makeSubStringBold(context.getString(R.string.wallet_error_expired_bold))
		} else if (invalidSignatureState.signatureErrorCode == EvalErrorCodes.SIGNATURE_TIMESTAMP_NOT_YET_VALID) {
			SpannableString(context.getString(R.string.wallet_error_national_rules))
		} else {
			context.getString(R.string.wallet_error_invalid_signature)
				.makeSubStringBold(context.getString(R.string.wallet_error_invalid_signature_bold))
		}
	}
	revocationState == CheckRevocationState.INVALID -> {
		context.getString(R.string.wallet_error_revocation)
			.makeSubStringBold(context.getString(R.string.wallet_error_revocation_bold))
	}
	nationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
		context.getString(R.string.wallet_error_expired)
			.makeSubStringBold(context.getString(R.string.wallet_error_expired_bold))
	}
	nationalRulesState is CheckNationalRulesState.NOT_YET_VALID -> {
		context.getString(R.string.wallet_error_valid_from).addBoldDate(
			DATE_REPLACEMENT_STRING,
			(nationalRulesState as CheckNationalRulesState.NOT_YET_VALID).validityRange.validFrom!!
		)
	}
	nationalRulesState is CheckNationalRulesState.INVALID -> {
		SpannableString(context.getString(R.string.wallet_error_national_rules))
	}
	else -> SpannableString(context.getString(R.string.unknown_error))
}

@ColorRes
fun VerificationResultStatus.getNameDobColor(): Int {
	return if (this.isInvalid()) { R.color.grey } else R.color.black
}

fun VerificationResultStatus.getQrAlpha(): Float {
	return if (this.isInvalid()) { 0.55f } else 1f
}
