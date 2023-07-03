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

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable
import java.util.*

@JsonClass(generateAdapter = true)
data class TestEntry(
	@Json(name = "tg") @get:JsonProperty("tg") val disease: String,
	@Json(name = "tt") @get:JsonProperty("tt") val type: String,
	@Json(name = "nm") @get:JsonProperty("nm") val naaTestName: String?,
	@Json(name = "ma") @get:JsonProperty("ma") val ratTestNameAndManufacturer: String?,
	@Json(name = "sc") @get:JsonProperty("sc") @get:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZZ", timezone = "Europe/Vienna") val timestampSample: Date,
	@Json(name = "dr") @get:JsonProperty("dr") @get:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZZ", timezone = "Europe/Vienna") val timestampResult: Date?,
	@Json(name = "tr") @get:JsonProperty("tr") val result: String,
	@Json(name = "tc") @get:JsonProperty("tc") val testCenter: String?,
	@Json(name = "co") @get:JsonProperty("co") val country: String,
	@Json(name = "is") @get:JsonProperty("is") val certificateIssuer: String,
	@Json(name = "ci") @get:JsonProperty("ci") val certificateIdentifier: String,
) : Serializable
