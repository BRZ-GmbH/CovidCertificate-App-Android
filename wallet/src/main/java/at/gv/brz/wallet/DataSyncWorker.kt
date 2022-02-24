package at.gv.brz.wallet

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import at.gv.brz.eval.CovidCertificateSdk
import at.gv.brz.wallet.util.DebugLogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.launch

/**
 * Background Worker to periodically fetch necessary data for certificate validation
 */
class DataSyncWorker(context: Context, params: WorkerParameters): ListenableWorker(context, params) {

    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer ->
            CoroutineScope(Default).launch {
                CovidCertificateSdk.getCertificateVerificationController().refreshTrustList(this, false, onCompletionCallback = {
                    if (it.failed) {
                        DebugLogUtil.log("Background Data Update - Failed", applicationContext)
                        completer.set(Result.failure())
                    } else {
                        if (it.refreshed) {
                            DebugLogUtil.log("Background Data Update - New Data", applicationContext)
                        } else {
                            DebugLogUtil.log("Background Data Update - Unchanged", applicationContext)
                        }
                        completer.set(Result.success())
                    }
                })
            }
        }
    }
}
