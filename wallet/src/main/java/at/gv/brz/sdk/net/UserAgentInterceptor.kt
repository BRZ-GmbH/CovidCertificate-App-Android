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

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class UserAgentInterceptor(private val userAgentGenerator: UserAgentGenerator) : Interceptor {
	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val request: Request = chain.request()
			.newBuilder()
			.header("User-Agent", userAgentGenerator.userAgent())
			.build()
		return chain.proceed(request)
	}

	fun interface UserAgentGenerator {
		fun userAgent(): String
	}
}