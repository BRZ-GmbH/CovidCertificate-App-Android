/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import at.gv.brz.sdk.utils.SingletonHolder
import at.gv.brz.wallet.BuildConfig
import java.io.IOException
import java.security.GeneralSecurityException

class WalletSecureStorage private constructor(context: Context) {

	companion object : SingletonHolder<WalletSecureStorage, Context>(::WalletSecureStorage) {

		private const val KEY_LAST_INSTALLED_VERSION = "last_installed_version"
		private const val PREFERENCES = "SecureStorage"
		private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
		private const val KEY_SELECTED_VALIDATION_REGION = "selected_validation_region"
		private const val KEY_HAS_ASKED_FOR_INAPP_REVIEW = "has_asked_for_inapp_review"
		private const val KEY_HAS_OPTED_OUT_OF_NON_IMPORTANT_CAMPAIGNS = "has_opted_out_of_non_important_campaigns"
		private const val KEY_NOTIFICATION_CAMPAIGN_ID = "notification_campaign_id"
		private const val KEY_NOTIFICATION_CAMPAIGN_TITLE = "notification_campaign_title"
		private const val KEY_NOTIFICATION_CAMPAIGN_MESSAGE = "notification_campaign_message"
		private const val KEY_NOTIFICATION_CAMPAIGN_LAST_TIMESTAMP = "notification_campaign_last_timestamp_key"
		private const val KEY_HAS_MODIFIED_CERTIFICATES_IN_SESSION = "has_modified_certificates_in_session"
		private const val KEY_IS_NOTIFICATION_PERMISSION_LAUNCHED = "is_notification_permission_launched"
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
	fun getLastInstalledVersion(): String = prefs.getString(KEY_LAST_INSTALLED_VERSION, BuildConfig.VERSION_NAME)!!

	fun setLastInstalledVersion(version: String) = prefs.edit().putString(KEY_LAST_INSTALLED_VERSION, version).apply()

	fun getOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

	fun setOnboardingCompleted(completed: Boolean) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()

	fun getSelectedValidationRegion(): String? = prefs.getString(KEY_SELECTED_VALIDATION_REGION, null)

	fun setSelectedValidationRegion(region: String) = prefs.edit().putString(KEY_SELECTED_VALIDATION_REGION, region).apply()

	fun getHasAskedForInAppReview(): Boolean = prefs.getBoolean(KEY_HAS_ASKED_FOR_INAPP_REVIEW, false)

	fun setHasAskedForInAppReview(asked: Boolean) = prefs.edit().putBoolean(
		KEY_HAS_ASKED_FOR_INAPP_REVIEW, asked).apply()

	fun getHasOptedOutOfNonImportantCampaigns(): Boolean = prefs.getBoolean(
		KEY_HAS_OPTED_OUT_OF_NON_IMPORTANT_CAMPAIGNS, false)

	fun setHasOptedOutOfNonImportantCampaigns(optedOut: Boolean) = prefs.edit().putBoolean(
		KEY_HAS_OPTED_OUT_OF_NON_IMPORTANT_CAMPAIGNS, optedOut).apply()

	fun getNotificationCampaignID(): String? = prefs.getString(KEY_NOTIFICATION_CAMPAIGN_ID, null)

	fun setNotificationCampaignID(campaignId: String?) = prefs.edit().putString(KEY_NOTIFICATION_CAMPAIGN_ID, campaignId).apply()

	fun getNotificationCampaignLastTimeStampKey(): String? = prefs.getString(KEY_NOTIFICATION_CAMPAIGN_LAST_TIMESTAMP, null)

	fun setNotificationCampaignLastTimeStampKey(lastTimeStampKey:String?) = prefs.edit().putString(KEY_NOTIFICATION_CAMPAIGN_LAST_TIMESTAMP, lastTimeStampKey).apply()

	fun getHasModifiedCertificatesInSession(): Boolean = prefs.getBoolean(KEY_HAS_MODIFIED_CERTIFICATES_IN_SESSION, false)

	fun setHasModifiedCertificatesInSession(status: Boolean) = prefs.edit().putBoolean(KEY_HAS_MODIFIED_CERTIFICATES_IN_SESSION, status).apply()

	fun getNotificationCampaignTitle(): String? = prefs.getString(KEY_NOTIFICATION_CAMPAIGN_TITLE, null)

	fun setNotificationCampaignTitle(campaignTitle: String?) = prefs.edit().putString(KEY_NOTIFICATION_CAMPAIGN_TITLE, campaignTitle).apply()

	fun getNotificationCampaignMessage(): String? = prefs.getString(KEY_NOTIFICATION_CAMPAIGN_MESSAGE, null)

	fun setNotificationCampaignMessage(campaignMessage: String?) = prefs.edit().putString(KEY_NOTIFICATION_CAMPAIGN_MESSAGE, campaignMessage).apply()

	fun getIsNotificationPermissionLaunched(): Boolean = prefs.getBoolean(
		KEY_IS_NOTIFICATION_PERMISSION_LAUNCHED, false)

	fun setIsNotificationPermissionLaunched(status: Boolean) =
		prefs.edit().putBoolean(KEY_IS_NOTIFICATION_PERMISSION_LAUNCHED, status).apply()

}