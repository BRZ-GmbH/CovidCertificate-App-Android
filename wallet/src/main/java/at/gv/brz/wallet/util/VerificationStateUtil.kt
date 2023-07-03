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

import androidx.annotation.ColorRes
import at.gv.brz.sdk.data.state.*
import at.gv.brz.wallet.R

@ColorRes
fun VerificationResultStatus.getNameDobColor(): Int {
	return if (this.isInvalid()) { R.color.grey } else R.color.black
}

fun VerificationResultStatus.getQrAlpha(): Float {
	return if (this.isInvalid()) { 0.55f } else 1f
}
