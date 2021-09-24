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
        private const val SHARED_PREFERENCES_NOTIFICATIONS_KEY = "NotificationsStorageKey"
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
    fun getNotificationTimestampForCertificateIdentifier(identifier: String): Long? {
        return getNotificationTimestamps()[identifier]
    }

    /**
     * Set or update the notification timestamp for the given certificate identifier
     */
    fun setNotificationTimestampForCertificateIdentifier(identifier: String, timestamp: Long) {
        val timestamps = getNotificationTimestamps()
        timestamps[identifier] = timestamp
        updateNotificationTimestamps(timestamps)
    }

    /**
     * Remove the stored timestamp for the given certificate identifier
     */
    fun deleteCertificateIdentifier(identifier: String) {
        val timestamps = getNotificationTimestamps()
        timestamps.remove(identifier)
        updateNotificationTimestamps(timestamps)
    }

    private fun getNotificationTimestamps(): MutableMap<String, Long> {
        val json = prefs.getString(SHARED_PREFERENCES_NOTIFICATIONS_KEY, null)
        if (json == null || json.isEmpty()) {
            return mutableMapOf()
        }
        return notificationTimestampsAdapter.fromJson(json) ?: mutableMapOf()
    }

    private fun updateNotificationTimestamps(timestamps: MutableMap<String, Long>) {
        val bfsIdsJson = notificationTimestampsAdapter.toJson(timestamps)
        val editor = prefs.edit()
        editor.putString(SHARED_PREFERENCES_NOTIFICATIONS_KEY, bfsIdsJson)
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