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
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.common.config.DirectLinkModel
import at.gv.brz.common.config.DirectLinkResult
import at.gv.brz.sdk.utils.SingletonHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.HttpURLConnection

/**
 * The purpose of this class is used to communicate with the direct link API services
 * Passes the DirectLinkModel and retrieve the results
 */
class DirectLinkRepository private constructor(spec: DirectLinkRepositorySpec) {

    private val directLinkService: DirectLinkService

    companion object :
        SingletonHolder<DirectLinkRepository, DirectLinkRepositorySpec>(::DirectLinkRepository);

    init {
        val okHttpBuilder = OkHttpClient.Builder()
        val cacheSize = 5 * 1024 * 1024 // 5 MB
        val cache = Cache(spec.context.cacheDir, cacheSize.toLong())
        okHttpBuilder.cache(cache)

        if (BuildConfig.DEBUG) {
            val httpInterceptor =
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            okHttpBuilder.addInterceptor(httpInterceptor)
        }

        directLinkService = Retrofit.Builder()
            .baseUrl(spec.directLinkUrl)
            .client(okHttpBuilder.build())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(DirectLinkService::class.java)
    }

    suspend fun getCertificateWithBirthdateOrBypassToken(directLinkModel: DirectLinkModel): DirectLinkResult? {
        return try {
            val response = withContext(Dispatchers.IO) {
                directLinkService.fetchCertificateWithBirthdateOrBypassToken(directLinkModel)
            }
            when {
                response.isSuccessful && response.code()==HttpURLConnection.HTTP_OK -> {
                    DirectLinkResult.Valid(response.body()?.qr.toString())
                }
                response.code()==HttpURLConnection.HTTP_BAD_REQUEST -> {
                    DirectLinkResult.InvalidRequestData
                }
                else -> {
                    DirectLinkResult.NetworkError
                }
            }
        } catch (e: Exception) {
            DirectLinkResult.NetworkError
        }
    }

}

class DirectLinkRepositorySpec(val context: Context, val directLinkUrl: String)