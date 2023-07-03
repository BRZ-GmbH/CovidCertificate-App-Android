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

import at.gv.brz.sdk.data.Config
import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor : Interceptor {

	companion object {
		private const val HEADER_TOKEN = "X-Token"
	}

	override fun intercept(chain: Interceptor.Chain): Response {
		val newRequest = chain.request()
			.newBuilder()
			.addHeader(HEADER_TOKEN, Config.apiToken)
			.build()

		return chain.proceed(newRequest)
	}

}