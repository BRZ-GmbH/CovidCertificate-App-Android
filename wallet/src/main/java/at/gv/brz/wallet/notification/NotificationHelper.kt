/*
 * Copyright (c) 2022 BRZ Wien <https://www.brz.gv.at>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import at.gv.brz.common.net.ConfigRepository
import at.gv.brz.common.net.ConfigSpec
import at.gv.brz.sdk.CovidCertificateSdk
import at.gv.brz.sdk.data.state.DecodeState
import at.gv.brz.sdk.decoder.CertificateDecoder
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.wallet.MainActivity
import at.gv.brz.wallet.R
import at.gv.brz.wallet.data.CertificateStorage
import at.gv.brz.wallet.data.NotificationSecureStorage
import at.gv.brz.wallet.data.WalletSecureStorage
import at.gv.brz.wallet.util.CampaignNotificationCheckResult
import at.gv.brz.wallet.util.NotificationUtil
import at.gv.brz.wallet.util.QueuedCampaignNotification
import at.gv.brz.wallet.util.lastDisplayTimestampKeyForCertificate

/**
 * This is a helper class to handle the local notifications.
 * Such as creating channel, sending and updating notifications
 */
class NotificationHelper {

    companion object {
        const val CHANNEL_ID = "WALLET_APP_NOTIFICATION_CHANNEL_ID"
        const val CHANNEL_NAME = "WALLET_APP_CAMPAIGN_NOTIFICATION"
        const val CHANNEL_DESCRIPTION = "TO_SHOW_CAMPAIGN_NOTIFICATIONS"
        const val KEY_NOTIFICATION_EXTRAS = "NOTIFICATION_EXTRA"
        const val KEY_CAMPAIGN_ID = "campaignId"
        const val KEY_LAST_DISPLAY_TIMESTAMP = "lastDisplayTimeStamp"
        const val KEY_CAMPAIGN_TITLE = "campaignTitle"
        const val KEY_CAMPAIGN_MESSAGE = "campaignMessage"
    }

    fun init(application: Context) {
        createNotificationChannel(application)
    }

    private fun createNotificationChannel(context: Context) {
        context.apply {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                channel.description= CHANNEL_DESCRIPTION
                getNotificationManager(context).createNotificationChannel(channel)
            }
        }
    }

    private fun getNotificationManager(context: Context): NotificationManager {
       return context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun createNotification(
        queuedNotifications: QueuedCampaignNotification,
        context: Context
    ) {

        val campaignId = queuedNotifications.campaign.id
        val lastDisplayTimeStampKey = queuedNotifications.campaign.lastDisplayTimestampKeyForCertificate(queuedNotifications.certificate)
        val notificationTitle:String = queuedNotifications.title!!
        val notificationMessage:String = queuedNotifications.message!!

        val bundle=Bundle()
        bundle.putBoolean(KEY_NOTIFICATION_EXTRAS, true)
        bundle.putString(KEY_CAMPAIGN_ID, campaignId)
        bundle.putString(KEY_LAST_DISPLAY_TIMESTAMP, lastDisplayTimeStampKey)
        bundle.putString(KEY_CAMPAIGN_TITLE, notificationTitle)
        bundle.putString(KEY_CAMPAIGN_MESSAGE, notificationMessage)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val notifyPendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setColor(ContextCompat.getColor(context, R.color.green_dark))
            .setContentTitle(notificationTitle)
            .setContentText(notificationMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(notifyPendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(lastDisplayTimeStampKey, System.currentTimeMillis().toInt(), notificationBuilder.build())
        }
    }

    private fun removeNotification(notificationIdentifiersToRemove: List<Map.Entry<Int, String>>, context:Context) {
        for (notificationIdToRemove in notificationIdentifiersToRemove) {
            NotificationManagerCompat.from(context).cancel(notificationIdToRemove.value, notificationIdToRemove.key)
        }
    }

    private fun getCampaignTimestampKeysFromDeliveredLocalNotifications(context:Context):Map<Int, String> {
        val visibleCampaignIds: MutableMap<Int, String> = mutableMapOf()
        for (activeNotification in getNotificationManager(context).activeNotifications) {
            val notificationUniqueId = activeNotification.id
            val notificationTimeStampKey = activeNotification.tag
            visibleCampaignIds.put(notificationUniqueId, notificationTimeStampKey)
        }
        return visibleCampaignIds
    }

    fun updateLocalNotificationsForCampaignCheckResult(checkResult: CampaignNotificationCheckResult, context: Context) {

        val visibleCampaignTimestampKeys = getCampaignTimestampKeysFromDeliveredLocalNotifications(context)

        val timestampKeysToShow = checkResult.queuedNotifications.map {
            it.campaign.lastDisplayTimestampKeyForCertificate(it.certificate)
        }

        val campaignsToAdd = checkResult.queuedNotifications.filter {
            it.campaign.lastDisplayTimestampKeyForCertificate(it.certificate).let { timestampKey ->
                !visibleCampaignTimestampKeys.containsValue(timestampKey)
            }
        }

        val removeNotApplicableNotifications = visibleCampaignTimestampKeys.filter { visibleNotification ->
            !timestampKeysToShow.contains(visibleNotification.value)
        }.map { it }

        if (removeNotApplicableNotifications.isNotEmpty()) {
            removeNotification(removeNotApplicableNotifications, context)
        }
        /**
         * Create notifications for new campaigns
         */
        campaignsToAdd.map { queuedCampaignNotification -> createNotification(queuedCampaignNotification, context) }

    }

    suspend fun updateLocalNotificationAfterCertificateModification(context:Context){
        val campaignNotificationCheckResult = campaignNotificationCheckResult(context) ?: return
        val visibleCampaignTimestampKeys = getCampaignTimestampKeysFromDeliveredLocalNotifications(context)
        val timestampKeysToShow = campaignNotificationCheckResult.queuedNotifications.map {
            it.campaign.lastDisplayTimestampKeyForCertificate(it.certificate)
        }

        val removeNotApplicableNotifications = visibleCampaignTimestampKeys.filter { visibleNotification ->
            !timestampKeysToShow.contains(visibleNotification.value)
        }.map { it }

        if (removeNotApplicableNotifications.isNotEmpty()) {
            removeNotification(removeNotApplicableNotifications, context)
        }
    }

    suspend fun updateConfigForLocalNotification(context: Context) {
        val campaignNotificationCheckResult = campaignNotificationCheckResult(context) ?: return
        if(campaignNotificationCheckResult.queuedNotifications.isNotEmpty()) {
                NotificationHelper().updateLocalNotificationsForCampaignCheckResult(campaignNotificationCheckResult, context)
        }
    }
    
    private suspend fun campaignNotificationCheckResult(context:Context): CampaignNotificationCheckResult? {
        val configRepository = ConfigRepository.getInstance(ConfigSpec(context, BuildConfig.BASE_URL))
        configRepository.loadConfig(false, context)?.let { config ->
            val dccHolders = CertificateStorage.getInstance(context).getCertificateList()
                .mapNotNull { (CertificateDecoder.decode(it) as? DecodeState.SUCCESS)?.dccHolder }
            val validationClock = CovidCertificateSdk.getValidationClock()?:return null
            val valueSets = CovidCertificateSdk.getValueSets()
            if (valueSets.isEmpty()) {
                return null
            }

            NotificationHelper().init(context)
            return NotificationUtil().startCertificateNotificationCheck(
                dccHolders,
                valueSets,
                validationClock,
                config,
                WalletSecureStorage.getInstance(context)
                    .getHasOptedOutOfNonImportantCampaigns(),
                NotificationSecureStorage.getInstance(context)
                    .getCurrentCertificateCampaignLastDisplayTimestamps(),
                context)
        }
        return null
    }

}