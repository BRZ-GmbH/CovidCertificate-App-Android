package at.gv.brz.wallet

import android.app.Application
import android.os.Build
import androidx.work.*
import at.gv.brz.eval.data.Config
import at.gv.brz.eval.net.UserAgentInterceptor
import at.gv.brz.eval.CovidCertificateSdk
import java.util.concurrent.TimeUnit

class MainApplication : Application() {

	companion object {
		private const val TAG_DATA_SYNC = "WALLET_DATA_SYNC_TAG"
	}

	override fun onCreate() {
		super.onCreate()
		Config.apiToken = BuildConfig.SDK_API_TOKEN
		Config.userAgent =
			UserAgentInterceptor.UserAgentGenerator { "${this.packageName};${BuildConfig.VERSION_NAME};${BuildConfig.BUILD_TIME};Android;${Build.VERSION.SDK_INT}" }

		CovidCertificateSdk.init(this)

		val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()
		val workRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(8, TimeUnit.HOURS).setConstraints(constraints).addTag(
			TAG_DATA_SYNC
		).build()
		WorkManager.getInstance(this).enqueueUniquePeriodicWork(TAG_DATA_SYNC, ExistingPeriodicWorkPolicy.REPLACE, workRequest)
	}
}