/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.common.net

import android.content.Context
import at.gv.brz.common.BuildConfig
import at.gv.brz.common.config.ConfigModel
import at.gv.brz.common.data.ConfigSecureStorage
import at.gv.brz.common.util.AssetUtil
import at.gv.brz.eval.utils.SingletonHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ConfigRepository private constructor(private val configSpec: ConfigSpec) {

	companion object : SingletonHolder<ConfigRepository, ConfigSpec>(::ConfigRepository) {
		private const val MIN_LOAD_WAIT_TIME = 8 * 60 * 60 * 8000L // 8h
		private const val MAX_AGE_STATUS_VALID_CACHED_CONFIG = 48 * 60 * 60 * 1000L // 48h
	}

	private val configService: ConfigService
	private val storage = ConfigSecureStorage.getInstance(configSpec.context)

	private var lastConfigLoadTimestamp: Long? = null

	init {
		val okHttpBuilder = OkHttpClient.Builder()

		val cacheSize = 5 * 1024 * 1024 // 5 MB
		val cache = Cache(configSpec.context.cacheDir, cacheSize.toLong())
		okHttpBuilder.cache(cache)

		if (BuildConfig.DEBUG) {
			val httpInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
			okHttpBuilder.addInterceptor(httpInterceptor)
		}

		configService = Retrofit.Builder()
			.baseUrl(configSpec.baseUrl)
			.client(okHttpBuilder.build())
			.addConverterFactory(MoshiConverterFactory.create())
			.build()
			.create(ConfigService::class.java)
	}

	suspend fun loadConfig(force: Boolean, context: Context): ConfigModel? {
		var config =
			if (force || (storage.getConfigLastSuccessTimestamp() + MIN_LOAD_WAIT_TIME <= System.currentTimeMillis())) {
				try {
					val response = withContext(Dispatchers.IO) { configService.getConfig() }
					if (!response.isSuccessful) throw HttpException(response)
					response.body()?.let { storage.updateConfigData(it, System.currentTimeMillis()) }
					response.body()
				} catch (e: Exception) {
					null
				}
			} else null

		if (config == null) {
			config = storage.getConfig()
		}
		if (config == null) {
			config = AssetUtil.loadDefaultConfig(context)
		}
		return config
	}
}

class ConfigSpec(val context: Context, val baseUrl: String)