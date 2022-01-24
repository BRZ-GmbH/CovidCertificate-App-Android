package at.gv.brz.wallet.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import at.gv.brz.eval.utils.SingletonHolder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Secure storage (EncryptedSharedPreferences) for storing information about notification timestamps for certificates
 */
class NotificationSecureStorage  private constructor(context: Context) {

    companion object :
        SingletonHolder<NotificationSecureStorage, Context>(::NotificationSecureStorage) {
        private const val SHARED_PREFERENCES_CERTIFICATE_CAMPAIGN_LAST_DISPLAY_TIMESTAMPS_KEY = "CertificateCampaignLastDisplayTimestampsKey"

        private const val SHARED_PREFERENCES_LAST_EXPIRED_TEST_CERTIFICATE_REMINDER_DATE_KEY = "LastExpiredTestCertificateReminderDateKey"
        private const val SHARED_PREFERENCES_EXPIRED_TEST_CERTIFICATE_REMINDER_COUNT_KEY = "ExpiredTestCertificateReminderCountKey"
        private const val SHARED_PREFERENCES_IGNORE_EXPIRED_TEST_CERTIFICATES_KEY = "IgnoreExpiredTestCertificatesKey"

        private const val SHARED_PREFERENCES_JOHNSON_BOOSTER_NOTIFICATION_SHOWN_KEY = "JohnsonBoosterNotificationShownStorageKey"
        const val SHARED_PREFERENCES_NAME: String = "NotificationsStorageName"

        private val moshi = Moshi.Builder().build()
        private val notificationTimestampsAdapter =
            moshi.adapter<MutableMap<String, Long>>(Types.newParameterizedType(MutableMap::class.java, String::class.java, Long::class.javaObjectType))
    }

    private val prefs: SharedPreferences

    init {
        prefs = initializeSharedPreferences(context)
    }

    /**
     * Get the notification timestamp for the given certificate identifier
     */
    fun getCertificateCampaignLastDisplayTimestampForIdentifier(identifier: String): Long? {
        return getModifiableCertificateCampaignLastDisplayTimestamps()[identifier]
    }

    fun getCurrentCertificateCampaignLastDisplayTimestamps(): Map<String, Long> {
        val json = prefs.getString(SHARED_PREFERENCES_CERTIFICATE_CAMPAIGN_LAST_DISPLAY_TIMESTAMPS_KEY, null)
        if (json == null || json.isEmpty()) {
            return mapOf()
        }
        return notificationTimestampsAdapter.fromJson(json) ?: mapOf()
    }

    /**
     * Set or update the notification timestamp for the given certificate identifier
     */
    fun setCertificateCampaignLastDisplayTimestampForIdentifier(identifier: String, timestamp: Long) {
        val timestamps = getModifiableCertificateCampaignLastDisplayTimestamps()
        timestamps[identifier] = timestamp
        updateCertificateCampaignLastDisplayTimestamps(timestamps)
    }

    /**
     * Remove the stored timestamp for the given certificate identifier
     */
    fun deleteCertificateIdentifier(identifier: String) {
        val timestamps = getModifiableCertificateCampaignLastDisplayTimestamps()
        val keysToRemove = timestamps.keys.filter { it.endsWith("_${identifier}") }
        keysToRemove.forEach { timestamps.remove(it) }
        updateCertificateCampaignLastDisplayTimestamps(timestamps)
    }

    private fun getModifiableCertificateCampaignLastDisplayTimestamps(): MutableMap<String, Long> {
        val json = prefs.getString(SHARED_PREFERENCES_CERTIFICATE_CAMPAIGN_LAST_DISPLAY_TIMESTAMPS_KEY, null)
        if (json == null || json.isEmpty()) {
            return mutableMapOf()
        }
        return notificationTimestampsAdapter.fromJson(json) ?: mutableMapOf()
    }

    private fun updateCertificateCampaignLastDisplayTimestamps(timestamps: MutableMap<String, Long>) {
        val bfsIdsJson = notificationTimestampsAdapter.toJson(timestamps)
        val editor = prefs.edit()
        editor.putString(SHARED_PREFERENCES_CERTIFICATE_CAMPAIGN_LAST_DISPLAY_TIMESTAMPS_KEY, bfsIdsJson)
        editor.apply()
    }

    fun getLastExpiredTestCertificateReminderDate(): Long {
        return prefs.getLong(SHARED_PREFERENCES_LAST_EXPIRED_TEST_CERTIFICATE_REMINDER_DATE_KEY, 0)
    }

    fun setLastExpiredTestCertificateReminderDate(value: Long) {
        val editor = prefs.edit()
        editor.putLong(SHARED_PREFERENCES_LAST_EXPIRED_TEST_CERTIFICATE_REMINDER_DATE_KEY, value)
        editor.apply()
    }

    fun getExpiredTestCertificateReminderCount(): Int {
        return prefs.getInt(SHARED_PREFERENCES_EXPIRED_TEST_CERTIFICATE_REMINDER_COUNT_KEY, 0)
    }

    fun setExpiredTestCertificateReminderCount(value: Int) {
        val editor = prefs.edit()
        editor.putInt(SHARED_PREFERENCES_EXPIRED_TEST_CERTIFICATE_REMINDER_COUNT_KEY, value)
        editor.apply()
    }

    fun shouldIgnoreExpiredTestCertificates(): Boolean {
        return prefs.getBoolean(SHARED_PREFERENCES_IGNORE_EXPIRED_TEST_CERTIFICATES_KEY, false)
    }

    fun setShouldIgnoreExpiredTestCertificates(value: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(SHARED_PREFERENCES_IGNORE_EXPIRED_TEST_CERTIFICATES_KEY, value)
        editor.apply()
    }

    fun getJohnsonBoosterNotificationShown(): Boolean {
        return prefs.getBoolean(SHARED_PREFERENCES_JOHNSON_BOOSTER_NOTIFICATION_SHOWN_KEY, false)
    }

    fun setJohnsonBoosterNotificationShown(value: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(SHARED_PREFERENCES_JOHNSON_BOOSTER_NOTIFICATION_SHOWN_KEY, value)
        editor.apply()
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

    @Synchronized
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            SHARED_PREFERENCES_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}