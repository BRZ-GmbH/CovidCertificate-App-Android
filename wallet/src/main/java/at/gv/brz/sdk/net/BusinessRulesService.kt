/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk.net

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface BusinessRulesService {

	@Headers("Accept: application/octet-stream")
	@GET("extendedrulesbin")
	suspend fun getBusinessRules(): Response<ResponseBody>

	@Headers("Accept: application/octet-stream")
	@GET("extendedrulessig")
	suspend fun getBusinessRulesSignature(): Response<ResponseBody>

}