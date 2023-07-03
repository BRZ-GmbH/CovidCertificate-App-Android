/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk.verification

import android.net.ConnectivityManager
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.sdk.data.state.VerificationResultStatus
import at.gv.brz.sdk.models.DccHolder
import at.gv.brz.sdk.models.TrustList
import at.gv.brz.engine.UTC_ZONE_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZonedDateTime

class CertificateVerificationTask(val dccHolder: DccHolder, val connectivityManager: ConnectivityManager, val countryCode: String, val region: String, val overwriteTrustlistClock: Boolean) {

	private val mutableVerificationStateFlow = MutableStateFlow<VerificationResultStatus>(VerificationResultStatus.LOADING)
	val verificationStateFlow = mutableVerificationStateFlow.asStateFlow()

	/**
	 * Execute this verification task with the specified verifier and trust list
	 */
	internal suspend fun execute(verifier: CertificateVerifier, trustList: TrustList?) {
		if (trustList != null) {
			var validationClock: ZonedDateTime? = null
			if (trustList.kronosClock.getCurrentNtpTimeMs() == null) {
				trustList.kronosClock.sync()
			}

			val kronosMilliseconds = trustList.kronosClock.getCurrentNtpTimeMs()
			if (kronosMilliseconds != null) {
				val instant = Instant.ofEpochMilli(kronosMilliseconds)
				validationClock = instant.atZone(UTC_ZONE_ID)
			}

			/**
			 * In test builds (for Q as well as P environment) we allow switching a setting for the app to either use the real time fetched from a time server (behaviour in the published app) or to use the current device time for validating the business rules.
			 */
			if ((BuildConfig.FLAVOR == "abn" || BuildConfig.FLAVOR == "prodtest") && overwriteTrustlistClock) {
				validationClock = ZonedDateTime.now()
			}

			val state = verifier.verify(dccHolder, trustList, validationClock, countryCode, region)
			mutableVerificationStateFlow.emit(state)
		} else {
			mutableVerificationStateFlow.emit(
				VerificationResultStatus.DATAEXPIRED
			)
		}
	}
}
