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

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import at.gv.brz.sdk.data.CertificateSecureStorage
import at.gv.brz.sdk.net.*
import at.gv.brz.sdk.repository.TrustListRepository
import at.gv.brz.sdk.verification.CertificateVerificationController
import at.gv.brz.sdk.verification.CertificateVerifier
import com.lyft.kronos.AndroidClockFactory
import com.lyft.kronos.KronosClock
import at.gv.brz.engine.UTC_ZONE_ID
import at.gv.brz.wallet.BuildConfig
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.time.Instant
import java.time.ZonedDateTime

object CovidCertificateSdk {

	private lateinit var certificateStorage: CertificateSecureStorage
	private lateinit var certificateVerificationController: CertificateVerificationController
	private var isInitialized = false
	private var trustListLifecycleObserver: TrustListLifecycleObserver? = null
	private lateinit var kronosClock: KronosClock

	fun init(context: Context) {
		val retrofit = createRetrofit(context)
		val trustlistService = retrofit.create(TrustlistService::class.java)
		val nationalTrustlistService = retrofit.create(NationalTrustlistService::class.java)
		val valueSetsService = retrofit.create(ValueSetsService::class.java)
		val businessRulesService = retrofit.create(BusinessRulesService::class.java)

		kronosClock = AndroidClockFactory.createKronosClock(context, syncListener = null, ntpHosts = listOf("ts1.univie.ac.at", "ts2.univie.ac.at"))

		certificateStorage = CertificateSecureStorage.getInstance(context)
		val trustListRepository = TrustListRepository(trustlistService, nationalTrustlistService, valueSetsService, businessRulesService, certificateStorage, BuildConfig.TRUST_ANCHOR_CERTIFICATE, BuildConfig.ALT_TRUST_ANCHOR_CERTIFICATE, kronosClock)
		val certificateVerifier = CertificateVerifier()
		certificateVerificationController = CertificateVerificationController(trustListRepository, certificateVerifier)

		isInitialized = true
	}

	fun registerWithLifecycle(lifecycle: Lifecycle) {
		requireInitialized()

		trustListLifecycleObserver = TrustListLifecycleObserver(lifecycle)
		trustListLifecycleObserver?.register(lifecycle)
	}

	fun unregisterWithLifecycle(lifecycle: Lifecycle) {
		requireInitialized()

		trustListLifecycleObserver?.unregister(lifecycle)
		trustListLifecycleObserver = null
	}

	fun getCertificateVerificationController(): CertificateVerificationController {
		requireInitialized()
		return certificateVerificationController
	}

	fun getValueSets(): Map<String, List<String>> {
		requireInitialized()

		return certificateStorage.mappedValueSets ?: mapOf()
	}

	fun getValidationClock(): ZonedDateTime? {
		val kronosMilliseconds = kronosClock.getCurrentNtpTimeMs()
		if (kronosMilliseconds != null) {
			val instant = Instant.ofEpochMilli(kronosMilliseconds)
			return instant.atZone(UTC_ZONE_ID)
		}
		return null
	}

	private fun requireInitialized() {
		if (!isInitialized) {
			throw IllegalStateException("CovidCertificateSdk must be initialized by calling init(context)")
		}
	}

	private fun createRetrofit(context: Context): Retrofit {
		val okHttpBuilder = OkHttpClient.Builder()
			.addInterceptor(ApiKeyInterceptor())

		val cacheSize = 5 * 1024 * 1024 // 5 MB
		val cache = Cache(context.cacheDir, cacheSize.toLong())
		okHttpBuilder.cache(cache)

		if (BuildConfig.DEBUG) {
			val httpInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
			okHttpBuilder.addInterceptor(httpInterceptor)
		}

		return Retrofit.Builder()
			.baseUrl(BuildConfig.BASE_URL_TRUST_LIST)
			.client(okHttpBuilder.build())
			.addConverterFactory(ToByteConvertFactory())
			.build()
	}

	internal class TrustListLifecycleObserver(private val lifecycle: Lifecycle) : LifecycleObserver {
		fun register(lifecycle: Lifecycle) {
			lifecycle.addObserver(this)
		}

		fun unregister(lifecycle: Lifecycle) {
			lifecycle.removeObserver(this)
		}

		@OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
		fun onCreate() {
			kronosClock.syncInBackground()
		}

		@OnLifecycleEvent(Lifecycle.Event.ON_START)
		fun onStart() {
			kronosClock.syncInBackground()
		}

		@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
		fun onStop() {
		}
	}

}