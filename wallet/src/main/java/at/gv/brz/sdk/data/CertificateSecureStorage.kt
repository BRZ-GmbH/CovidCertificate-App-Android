/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import at.gv.brz.brvc.model.data.BusinessRuleContainer
import at.gv.brz.sdk.data.moshi.RawJsonStringAdapter
import at.gv.brz.sdk.utils.SingletonHolder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import at.gv.brz.engine.data.ValueSetRemote
import at.gv.brz.engine.data.toValueSet
import ehn.techiop.hcert.kotlin.rules.BusinessRulesContainer
import ehn.techiop.hcert.kotlin.valueset.ValueSetContainer
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.Exception

internal class CertificateSecureStorage private constructor(private val context: Context) : TrustListStore {

	companion object : SingletonHolder<CertificateSecureStorage, Context>(::CertificateSecureStorage) {
		private const val PREFERENCES_NAME = "CertificateSecureStorage"
		private const val FILE_PATH_CERTIFICATE_SIGNATURES = "signatures.json"
		private const val FILE_PATH_TRUSTLIST_CONTENT = "trustlist_content"
		private const val FILE_PATH_TRUSTLIST_SIGNATURE = "trustlist_signature"
		private const val FILE_PATH_NATIONAL_TRUSTLIST_CONTENT = "national_trustlist_content"
		private const val FILE_PATH_NATIONAL_TRUSTLIST_SIGNATURE = "national_trustlist_signature"
		private const val FILE_PATH_VALUESETS = "valuesets.json"
		private const val FILE_PATH_RULESET = "modernbusinessrules.json"

		private const val KEY_TRUSTLIST_LAST_UPDATE = "KEY_TRUSTLIST_LAST_UPDATE"
		private const val KEY_NATIONAL_TRUSTLIST_LAST_UPDATE = "KEY_NATIONAL_TRUSTLIST_LAST_UPDATE"
		private const val KEY_VALUESETS_LAST_UPDATE = "KEY_VALUESETS_LAST_UPDATE"
		private const val KEY_RULESET_LAST_UPDATE = "KEY_MODERN_BUSINESS_RULES_LAST_UPDATE"

		private const val KEY_TRUSTLIST_CONTENT_HASH = "KEY_TRUSTLIST_CONTENT_HASH"
		private const val KEY_NATIONAL_TRUSTLIST_CONTENT_HASH = "KEY_NATIONAL_TRUSTLIST_CONTENT_HASH"
		private const val KEY_VALUESETS_CONTENT_HASH = "KEY_VALUESETS_CONTENT_HASH"
		private const val KEY_RULESET_CONTENT_HASH = "KEY_MODERN_BUSINESS_RULES_CONTENT_HASH"

		private val TRUSTLIST_UPDATE_INTERVAL = TimeUnit.HOURS.toMillis(8L)
		private val NATIONAL_TRUSTLIST_UPDATE_INTERVAL = TimeUnit.HOURS.toMillis(8L)
		private val VALUESETS_UPDATE_INTERVAL = TimeUnit.HOURS.toMillis(8L)
		private val RULESET_UPDATE_INTERVAL = TimeUnit.HOURS.toMillis(8L)

		private val TRUSTLIST_MAX_AGE = TimeUnit.HOURS.toMillis(72L)
		private val NATIONAL_TRUSTLIST_MAX_AGE = TimeUnit.HOURS.toMillis(72L)
		private val VALUESETS_MAX_AGE = TimeUnit.HOURS.toMillis(72L)
		private val RULESET_MAX_AGE = TimeUnit.HOURS.toMillis(72L)

		private val moshi = Moshi.Builder().add(RawJsonStringAdapter()).addLast(
			KotlinJsonAdapterFactory()
		).build()
		private val valueSetsAdapter = moshi.adapter(ValueSetContainer::class.java)
		private val businessRulesAdapter = moshi.adapter(BusinessRulesContainer::class.java)
	}

	private val trustlistContentFileStorage = EncryptedFileStorage(FILE_PATH_TRUSTLIST_CONTENT)
	private val trustlistSignatureFileStorage = EncryptedFileStorage(FILE_PATH_TRUSTLIST_SIGNATURE)
	private val nationalTrustlistContentFileStorage = EncryptedFileStorage(FILE_PATH_NATIONAL_TRUSTLIST_CONTENT)
	private val nationalTrustlistSignatureFileStorage = EncryptedFileStorage(FILE_PATH_NATIONAL_TRUSTLIST_SIGNATURE)
	private val valueSetsFileStorage = EncryptedFileStorage(FILE_PATH_VALUESETS)
	private val ruleSetFileStorage = EncryptedFileStorage(FILE_PATH_RULESET)

	private val preferences = initializeSharedPreferences(context)

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
	 * Security library is not thread-safe.
	 * @see [https://developer.android.com/topic/security/data](https://developer.android.com/topic/security/data)
	 */
	@Synchronized
	@Throws(GeneralSecurityException::class, IOException::class)
	private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
		val masterKeys = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
		return EncryptedSharedPreferences
			.create(
				PREFERENCES_NAME, masterKeys, context, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
				EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
			)
	}

	/*override var certificateSignatures: TrustListV2? = null
		get() {
			if (field == null) {
				field = certificateFileStorage.read(context)?.let { trustListAdapter.fromJson(it) }
			}
			return field
		}
		set(value) {
			certificateFileStorage.write(context, trustListAdapter.toJson(value))
			trustlistLastUpdate = Instant.now().toEpochMilli()
			field = value
		}*/

	override var trustlistContentData: ByteArray? = null
		get() {
			if (field == null) {
				field = trustlistContentFileStorage.readByteArray(context)
			}
			return field
		}
		set(value) {
			trustlistContentFileStorage.writeByteArray(context, value ?: ByteArray(0))
			trustlistLastUpdate = Instant.now().toEpochMilli()
			field = value
		}

	override var trustlistSignatureData: ByteArray? = null
		get() {
			if (field == null) {
				field = trustlistSignatureFileStorage.readByteArray(context)
			}
			return field
		}
		set(value) {
			trustlistSignatureFileStorage.writeByteArray(context, value ?: ByteArray(0))
			field = value
		}

	override var nationalTrustlistContentData: ByteArray? = null
		get() {
			if (field == null) {
				field = nationalTrustlistContentFileStorage.readByteArray(context)
			}
			return field
		}
		set(value) {
			nationalTrustlistContentFileStorage.writeByteArray(context, value ?: ByteArray(0))
			nationalTrustlistLastUpdate = Instant.now().toEpochMilli()
			field = value
		}

	override var nationalTrustlistSignatureData: ByteArray? = null
		get() {
			if (field == null) {
				field = nationalTrustlistSignatureFileStorage.readByteArray(context)
			}
			return field
		}
		set(value) {
			nationalTrustlistSignatureFileStorage.writeByteArray(context, value ?: ByteArray(0))
			field = value
		}

	override var valueSets: ValueSetContainer? = null
		get() {
			if (field == null) {
				field = valueSetsFileStorage.read(context)?.let { valueSetsAdapter.fromJson(it) }
			}
			return field
		}
		set(value) {
			valueSetsFileStorage.write(context, valueSetsAdapter.toJson(value))
			valueSetsLastUpdate = Instant.now().toEpochMilli()
			field = value
			mappedValueSets = null
			mappedValueSetObjects = null
		}

	override var mappedValueSets: Map<String, List<String>>? = null
		get() {
			if (field == null) {
				val valueSetsToParse = valueSets
				if (valueSetsToParse != null) {
					val objectMapper = ObjectMapper().apply { this.findAndRegisterModules()
						registerModule(JavaTimeModule())
					}
					field = valueSetsToParse.valueSets.map {
						val valueSet =
							objectMapper.readValue(it.valueSet, ValueSetRemote::class.java)
								.toValueSet()
						val values = valueSet.valueSetValues.fieldNames().asSequence().toList()
						it.name to values
					}.toMap()
				}
			}
			return field
		}

	var mappedValueSetObjects: Map<String, at.gv.brz.sdk.products.ValueSet>? = null
		get() {
			if (field == null) {
				val valueSetsToParse = valueSets
				if (valueSetsToParse != null) {
					val valueSetAdapter: JsonAdapter<at.gv.brz.sdk.products.ValueSet> = Moshi.Builder().build().adapter(
						at.gv.brz.sdk.products.ValueSet::class.java)

					val map = mutableMapOf<String, at.gv.brz.sdk.products.ValueSet>()
					valueSetsToParse.valueSets.forEach {
						try {
							val valueSet = valueSetAdapter.fromJson(it.valueSet)
							if (valueSet != null) {
								map.put(it.name, valueSet)
							}
						} catch (e: Exception) {}
					}
					field = map
				}
			}
			return field
		}

	override var modernBusinessRules: BusinessRulesContainer? = null
		get() {
			if (field == null) {
				field = ruleSetFileStorage.read(context)?.let { businessRulesAdapter.fromJson(it) }
			}
			return field
		}
		set(value) {
			ruleSetFileStorage.write(context, businessRulesAdapter.toJson(value))
			modernBusinessRulesLastUpdate = Instant.now().toEpochMilli()
			field = value
			mappedBusinessRules = null
		}

	override var mappedBusinessRules: BusinessRuleContainer? = null
		get() {
			if (field == null) {
				val businessRulesToParse = modernBusinessRules
				if (businessRulesToParse != null) {
					businessRulesToParse.rules.firstOrNull()?.rule?.let {
						return BusinessRuleContainer.fromData(it)
					}
				}
			}
			return field
		}


	override var trustlistLastUpdate: Long
		get() = preferences.getLong(KEY_TRUSTLIST_LAST_UPDATE, 0L)
		set(value) {
			preferences.edit().putLong(KEY_TRUSTLIST_LAST_UPDATE, value).apply()
		}

	override var nationalTrustlistLastUpdate: Long
		get() = preferences.getLong(KEY_NATIONAL_TRUSTLIST_LAST_UPDATE, 0L)
		set(value) {
			preferences.edit().putLong(KEY_NATIONAL_TRUSTLIST_LAST_UPDATE, value).apply()
		}

	override var valueSetsLastUpdate: Long
		get() = preferences.getLong(KEY_VALUESETS_LAST_UPDATE, 0L)
		set(value) {
			preferences.edit().putLong(KEY_VALUESETS_LAST_UPDATE, value).apply()
		}

	override var modernBusinessRulesLastUpdate: Long
		get() = preferences.getLong(KEY_RULESET_LAST_UPDATE, 0L)
		set(value) {
			preferences.edit().putLong(KEY_RULESET_LAST_UPDATE, value).apply()
		}

	override var trustlistContentHash: Int
		get() = preferences.getInt(KEY_TRUSTLIST_CONTENT_HASH, 0)
		set(value) {
			preferences.edit().putInt(KEY_TRUSTLIST_CONTENT_HASH, value).apply()
		}

	override var nationalTrustlistContentHash: Int
		get() = preferences.getInt(KEY_NATIONAL_TRUSTLIST_CONTENT_HASH, 0)
		set(value) {
			preferences.edit().putInt(KEY_NATIONAL_TRUSTLIST_CONTENT_HASH, value).apply()
		}

	override var valueSetsContentHash: Int
		get() = preferences.getInt(KEY_VALUESETS_CONTENT_HASH, 0)
		set(value) {
			preferences.edit().putInt(KEY_VALUESETS_CONTENT_HASH, value).apply()
		}

	override var modernBusinessRulesContentHash: Int
		get() = preferences.getInt(KEY_RULESET_CONTENT_HASH, 0)
		set(value) {
			preferences.edit().putInt(KEY_RULESET_CONTENT_HASH, value).apply()
		}

	override fun shouldUpdateTrustListCertificates(): Boolean {
		return trustlistLastUpdate == 0L || (Instant.now().toEpochMilli() - trustlistLastUpdate) > TRUSTLIST_UPDATE_INTERVAL
	}

	override fun shouldUpdateNationalTrustListCertificates(): Boolean {
		return nationalTrustlistLastUpdate == 0L || (Instant.now().toEpochMilli() - nationalTrustlistLastUpdate) > NATIONAL_TRUSTLIST_UPDATE_INTERVAL
	}

	override fun shouldUpdateValueSets(): Boolean {
		return valueSetsLastUpdate == 0L || (Instant.now().toEpochMilli() - valueSetsLastUpdate) > VALUESETS_UPDATE_INTERVAL
	}

	override fun shouldUpdateBusinessRules(): Boolean {
		return modernBusinessRulesLastUpdate == 0L || (Instant.now().toEpochMilli() - modernBusinessRulesLastUpdate) > RULESET_UPDATE_INTERVAL
	}

	override fun areTrustlistCertificatesValid(): Boolean {
		return trustlistContentData != null && trustlistSignatureData != null
	}

	override fun areNationalTrustlistCertificatesValid(): Boolean {
		return nationalTrustlistContentData != null && nationalTrustlistSignatureData != null
	}

	override fun areValueSetsValid(): Boolean {
		return valueSets != null
	}

	override fun areBusinessRulesValid(): Boolean {
		return modernBusinessRules != null
	}

	override fun dataExpired(): Boolean {
		if (trustlistLastUpdate == 0L || nationalTrustlistLastUpdate == 0L || valueSetsLastUpdate == 0L || modernBusinessRulesLastUpdate == 0L) {
			return true
		}

		if ((Instant.now().toEpochMilli() - trustlistLastUpdate) > TRUSTLIST_MAX_AGE) {
			return true
		}
		if ((Instant.now().toEpochMilli() - nationalTrustlistLastUpdate) > NATIONAL_TRUSTLIST_MAX_AGE) {
			return true
		}
		if ((Instant.now().toEpochMilli() - valueSetsLastUpdate) > VALUESETS_MAX_AGE) {
			return true
		}
		if ((Instant.now().toEpochMilli() - modernBusinessRulesLastUpdate) > RULESET_MAX_AGE) {
			return true
		}
		return false
	}
}