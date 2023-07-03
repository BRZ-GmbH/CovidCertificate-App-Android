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
data class Eudgc (
	@Json(name = "ver") @get:JsonProperty("ver") val version: String,
	@Json(name = "nam") @get:JsonProperty("nam") val person: PersonName,
	@Json(name = "dob") @get:JsonProperty("dob") val dateOfBirth: String,
	@Json(name = "v") @get:JsonProperty("v") val vaccinations: List<VaccinationEntry>?,
	@Json(name = "t") @get:JsonProperty("t") val tests: List<TestEntry>?,
	@Json(name = "r") @get:JsonProperty("r") val pastInfections: List<RecoveryEntry>?,
	@Json(name = "ve") @get:JsonProperty("ve") val vaccinationExemptions: List<VaccinationExemptionEntry>?,
): Serializable
