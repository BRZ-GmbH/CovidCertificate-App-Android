package at.gv.brz.wallet

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import at.gv.brz.sdk.CovidCertificateSdk
import at.gv.brz.wallet.notification.NotificationHelper
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Background Worker to periodically fetch necessary data for certificate validation
 */
class DataSyncWorker(context: Context, params: WorkerParameters): ListenableWorker(context, params) {

    private var refreshTrustListJob: Job? = null
    private var updateConfigJob: Job? = null

    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer ->

            CoroutineScope(Default).launch {
                refreshTrustListJob=launch {
                    CovidCertificateSdk.getCertificateVerificationController().refreshTrustList(this, false)
                }
                updateConfigJob = launch {
                    NotificationHelper().updateConfigForLocalNotification(applicationContext)
                }
                updateDataSyncWorkerStatus(completer)
            }

        }
    }

    private suspend fun updateDataSyncWorkerStatus(
        completer: CallbackToFutureAdapter.Completer<Result>
    ) {
        this.refreshTrustListJob?.join()
        this.updateConfigJob?.join()

        if(this.refreshTrustListJob?.isCompleted == true && this.updateConfigJob?.isCompleted == true) {
            completer.set(Result.success())
        }else {
            completer.set(Result.failure())
        }

    }
}
