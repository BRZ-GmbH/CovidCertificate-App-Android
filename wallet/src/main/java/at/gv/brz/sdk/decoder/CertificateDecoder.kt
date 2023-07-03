/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk.decoder

import at.gv.brz.sdk.chain.CertTypeService
import at.gv.brz.sdk.chain.LocalCwtService
import at.gv.brz.sdk.data.EvalErrorCodes
import at.gv.brz.sdk.data.state.DecodeState
import at.gv.brz.sdk.data.state.StateError
import at.gv.brz.sdk.euhealthcert.Eudgc
import at.gv.brz.sdk.models.DccHolder
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import ehn.techiop.hcert.kotlin.chain.*
import ehn.techiop.hcert.kotlin.chain.impl.*
import ehn.techiop.hcert.kotlin.trust.CoseAdapter
import kotlinx.datetime.toJavaInstant
import java.util.*

object CertificateDecoder {

	/**
	 * Decodes the string from a QR code into a DCC.
	 *
	 * Does not do any validity checks. Simply checks whether the data is decodable.
	 *
	 * @param qrCodeData content of the scanned qr code, of the format "HC1:base45(...)"
	 */
	fun decode(qrCodeData: String): DecodeState {
		val euContextService = DefaultContextIdentifierService("HC1:")
		val atContextService = DefaultContextIdentifierService("AT1:")

		val check = VerificationResult()
		var input = qrCodeData
		try {
			input = euContextService.decode(input, check)
		} catch(exception: VerificationException) {
			try {
				input = atContextService.decode(qrCodeData, check)
			} catch(exception: VerificationException) {
				return DecodeState.ERROR(StateError(EvalErrorCodes.DECODE_PREFIX))
			}
		}
		val base45Decode: ByteArray
		try {
			base45Decode = DefaultBase45Service().decode(input, check)
		} catch (exception: VerificationException) {
			return DecodeState.ERROR(StateError(EvalErrorCodes.DECODE_BASE_45))
		}
		val decompressedData: ByteArray
		try {
			decompressedData = DefaultCompressorService().decode(base45Decode, check)
		} catch (exception: VerificationException) {
			return DecodeState.ERROR(StateError(EvalErrorCodes.DECODE_Z_LIB))
		}
		val coseContent: ByteArray
		try {
			coseContent = CoseAdapter(decompressedData).getContent()
		} catch (exception: VerificationException) {
			return DecodeState.ERROR(StateError(EvalErrorCodes.DECODE_COSE))
		}

		try {
			val cborContent = LocalCwtService().decode(coseContent, check)
			val cborJSON = cborContent.toJsonString()

			val adapter: JsonAdapter<Eudgc> =
				Moshi.Builder().add(Date::class.java, Rfc3339DateJsonAdapter()).build().adapter(Eudgc::class.java)

			val eudgc = adapter.fromJson(cborJSON)

			val expirationDate = check.expirationTime
			val issueDate = check.issuedAt
			val issuer = check.issuer
			if (eudgc != null && expirationDate != null && issueDate != null && issuer != null) {
				val dccHolder = DccHolder(eudgc, qrCodeData, expirationDate.toJavaInstant(), issueDate.toJavaInstant(), issuer)
				dccHolder.certType = CertTypeService.decode(dccHolder.euDGC)
				return DecodeState.SUCCESS(dccHolder)
			} else {
				return DecodeState.ERROR(StateError(EvalErrorCodes.DECODE_CBOR))
			}
		} catch (e: VerificationException) {
			return DecodeState.ERROR(StateError(EvalErrorCodes.DECODE_CBOR))
		}
	}
}
