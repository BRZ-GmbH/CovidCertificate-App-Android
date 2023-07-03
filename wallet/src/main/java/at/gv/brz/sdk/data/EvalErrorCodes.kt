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

object EvalErrorCodes {
	/* Errors during decoding */
	const val DECODE_PREFIX = "D|PRX"
	const val DECODE_BASE_45 = "D|B45"
	const val DECODE_Z_LIB = "D|ZLB"
	const val DECODE_COSE = "D|CSE"
	const val DECODE_CBOR = "D|CBR"
}