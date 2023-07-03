/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk

import at.gv.brz.brvc.BusinessRulesValidator
import at.gv.brz.brvc.ExternalConditionEvaluationStrategy
import at.gv.brz.brvc.model.data.BusinessRuleCertificateType
import at.gv.brz.brvc.model.data.BusinessRuleContainer
import at.gv.brz.sdk.chain.LocalCwtService
import at.gv.brz.sdk.data.state.*
import at.gv.brz.sdk.models.CertType
import at.gv.brz.sdk.models.DccHolder
import at.gv.brz.sdk.models.ValidationProfile
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import ehn.techiop.hcert.kotlin.chain.Chain
import ehn.techiop.hcert.kotlin.chain.DelegatingChain
import ehn.techiop.hcert.kotlin.chain.impl.*
import kotlinx.datetime.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

internal object Eval {
	private val TAG = Eval::class.java.simpleName

	/**
	 * Checks whether the DCC has a valid signature.
	 *
	 * A signature is only valid if it is signed by a trusted key, but also only if other attributes are valid
	 * (e.g. the signature is not expired - which may be different from the legal national rules).
	 */
	fun checkSignature(dccHolder: DccHolder, signatures: TrustListCertificateRepository, nationalSignatures: TrustListCertificateRepository): VerificationResultStatus {
		try {
			val euContextService = DefaultContextIdentifierService("HC1:")
			val euChain = Chain(
				DefaultHigherOrderValidationService(),
				DefaultSchemaValidationService(),
				DefaultCborService(),
				LocalCwtService(clock = Clock.System),
				DefaultCoseService(signatures),
				DefaultCompressorService(),
				DefaultBase45Service(),
				euContextService
			)

			val atContextService = DefaultContextIdentifierService("AT1:")
			val atChain = Chain(
				DefaultHigherOrderValidationService(),
				DefaultSchemaValidationService(false, arrayOf("AT-1.0.0")),
				DefaultCborService(),
				LocalCwtService(clock = Clock.System),
				DefaultCoseService(nationalSignatures),
				DefaultCompressorService(),
				DefaultBase45Service(),
				atContextService
			)

			val chain = DelegatingChain(euChain, euContextService, atChain, atContextService)

			val result = chain.decode(dccHolder.qrCodeData)
			if (result.verificationResult.error == null) {
				return VerificationResultStatus.SUCCESS(mapOf())
			} else {
				return VerificationResultStatus.SIGNATURE_INVALID
			}
		} catch(e: Throwable) {}
		return VerificationResultStatus.SIGNATURE_INVALID
	}

	/**
	 * @param dccHolder Object which was returned from the decode function
	 * @return State for the Signaturecheck
	 */
	fun checkNationalRules(
		dccHolder: DccHolder,
		validationClock: ZonedDateTime,
		valueSets: Map<String, List<String>>,
		countryCode: String,
		region: String?,
		businessRulesContainer: BusinessRuleContainer
	): VerificationResultStatus {
		val core = BusinessRulesValidator(
			businessRules = businessRulesContainer,
			valueSets = valueSets,
			validationClock = validationClock,
			externalConditionEvaluator = null,
			externalConditionEvaluationStrategy = ExternalConditionEvaluationStrategy.DEFAULT_TO_FALSE
		)
		val objectMapper = ObjectMapper().apply {
			this.findAndRegisterModules()
			registerModule(JavaTimeModule())
		}
		val certificatePayload = objectMapper.writeValueAsString(dccHolder.euDGC)

		val validationResult = core.evaluateCertificate(
			certificate = certificatePayload,
			certificateType = dccHolder.businessRuleCertificateType(),
			expiration = dccHolder.expirationTime!!.atZone(ZoneId.systemDefault()),
			issue = dccHolder.issuedAt!!.atZone(ZoneId.systemDefault()),
			country = countryCode,
			region = region ?: "W",
			listOf(ValidationProfile.ENTRY.profileName, ValidationProfile.NIGHT_CLUB.profileName),
			dccHolder
		)

		return VerificationResultStatus.SUCCESS(validationResult)
	}
}

fun DccHolder.businessRuleCertificateType(): BusinessRuleCertificateType {
	when (this.certType) {
		CertType.VACCINATION -> return BusinessRuleCertificateType.VACCINATION
		CertType.RECOVERY -> return BusinessRuleCertificateType.RECOVERY
		CertType.VACCINATION_EXEMPTION -> return BusinessRuleCertificateType.VACCINATION_EXEMPTION
		else -> return BusinessRuleCertificateType.TEST
	}
}