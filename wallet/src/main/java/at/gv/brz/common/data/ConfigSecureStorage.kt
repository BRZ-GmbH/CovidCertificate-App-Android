/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.common.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import at.gv.brz.sdk.utils.SingletonHolder
import at.gv.brz.common.config.ConfigModel
import com.squareup.moshi.Moshi
import java.io.IOException
import java.security.GeneralSecurityException

class ConfigSecureStorage private constructor(context: Context) {

	companion object : SingletonHolder<ConfigSecureStorage, Context>(::ConfigSecureStorage) {

		private const val PREFERENCES = "ConfigSecureStorage"

		private const val KEY_CONFIG = "ConfigKey"
		private const val KEY_CONFIG_LAST_SUCCESS = "LastSuccessTimestampKey"

		private const val KEY_ZOOM_ACTIVE = "KEY_ZOOM_ACTIVE"

		private val moshi = Moshi.Builder().build()
		private val configModelAdapter = moshi.adapter(ConfigModel::class.java)
	}

	private val prefs: SharedPreferences

	init {
		prefs = initializeSharedPreferences(context)
	}

	@Synchronized
	private fun initializeSharedPreferences(context: Context): SharedPreferences {
		return try {
			createEncryptedSharedPreferences(context)
		} catch (e: GeneralSecurityException) {
			throw RuntimeException(e)
		} catch (e: IOException) {
			throw RuntimeException(e)
		}
	}

	/**
	 * Create or obtain an encrypted SharedPreferences instance. Note that this method is synchronized because the AndroidX
	 * Security
	 * library is not thread-safe.
	 * @see [https://developer.android.com/topic/security/data](https://developer.android.com/topic/security/data)
	 */
	@Synchronized
	@Throws(GeneralSecurityException::class, IOException::class)
	private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
		val masterKeys = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
		return EncryptedSharedPreferences
			.create(
				PREFERENCES, masterKeys, context, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
				EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
			)
	}

	fun updateConfigData(config: ConfigModel, timestamp: Long) {
		val editor = prefs.edit()
		editor.putLong(KEY_CONFIG_LAST_SUCCESS, timestamp)
		editor.putString(KEY_CONFIG, configModelAdapter.toJson(config))
		editor.apply()
	}

	fun getConfigLastSuccessTimestamp(): Long =
		prefs.getLong(KEY_CONFIG_LAST_SUCCESS, 0)

	fun getConfig(): ConfigModel? =
		prefs.getString(KEY_CONFIG, null)
			?.let { configModelAdapter.fromJson(it) }

	fun getZoomOn(): Boolean = prefs.getBoolean(KEY_ZOOM_ACTIVE, false)
	fun setZoomOn(isZoomActive: Boolean) = prefs.edit().putBoolean(KEY_ZOOM_ACTIVE, isZoomActive).apply()
}