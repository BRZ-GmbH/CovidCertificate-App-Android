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

import at.gv.brz.brvc.model.data.BusinessRuleContainer
import at.gv.brz.sdk.Eval
import at.gv.brz.sdk.data.state.*
import at.gv.brz.sdk.models.DccHolder
import at.gv.brz.sdk.models.TrustList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import ehn.techiop.hcert.kotlin.chain.impl.TrustListCertificateRepository
import java.time.ZonedDateTime

internal class CertificateVerifier() {

	suspend fun verify(dccHolder: DccHolder,
					   trustList: TrustList,
					   validationClock: ZonedDateTime?,
					   countryCode: String,
					   region: String): VerificationResultStatus = withContext(Dispatchers.Default) {
		if (validationClock == null) {
			VerificationResultStatus.TIMEMISSING
		} else {
			val checkSignatureStateDeferred = async { checkSignature(dccHolder, trustList.signatures, trustList.nationalSignatures) }

			val checkSignatureState = checkSignatureStateDeferred.await()
			if (!(checkSignatureState is VerificationResultStatus.SUCCESS)) {
				checkSignatureState
			} else {
				val checkBusinessRulesStateDeferred = async {
					checkNationalRules(
						dccHolder,
						validationClock,
						trustList.valueSets,
						countryCode,
						region,
						trustList.businessRules
					)
				}
				val businessRulesState = checkBusinessRulesStateDeferred.await()
				businessRulesState
			}
		}
	}

	private suspend fun checkSignature(dccHolder: DccHolder, signatures: TrustListCertificateRepository, nationalSignatures: TrustListCertificateRepository) = withContext(Dispatchers.Default) {
		try {
			Eval.checkSignature(dccHolder, signatures, nationalSignatures)
		} catch (e: Exception) {
			VerificationResultStatus.ERROR
		}
	}

	private suspend fun checkNationalRules(
		dccHolder: DccHolder,
		validationClock: ZonedDateTime,
		valueSets: Map<String, List<String>>,
		countryCode: String,
		region: String?,
		businessRulesContainer: BusinessRuleContainer
	) = withContext(Dispatchers.Default) {
		try {
			Eval.checkNationalRules(dccHolder, validationClock, valueSets, countryCode, region, businessRulesContainer)
		} catch (e: Exception) {
			VerificationResultStatus.SUCCESS(mapOf())
		}
	}

}