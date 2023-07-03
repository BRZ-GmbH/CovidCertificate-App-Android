/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk.euhealthcert

import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class RecoveryEntry(
	@Json(name = "tg") @get:JsonProperty("tg") val disease: String,
	@Json(name = "fr") @get:JsonProperty("fr") val dateFirstPositiveTest: String,
	@Json(name = "co") @get:JsonProperty("co") val countryOfTest: String,
	@Json(name = "is") @get:JsonProperty("is") val certificateIssuer: String,
	@Json(name = "df") @get:JsonProperty("df") val validFrom: String,
	@Json(name = "du") @get:JsonProperty("du") val validUntil: String,
	@Json(name = "ci") @get:JsonProperty("ci") val certificateIdentifier: String,
) : Serializable
