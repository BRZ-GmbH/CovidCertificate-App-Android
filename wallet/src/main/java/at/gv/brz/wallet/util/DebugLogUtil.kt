package at.gv.brz.wallet.util

import android.content.Context
import at.gv.brz.wallet.BuildConfig
import java.time.Instant

/**
 * Debug Utility class that stores the last 100 log text entries and dates in sharedPreferences for debug purposes
 */
class DebugLogUtil {

    companion object {
        private fun logData(context: Context): Pair<List<String>, List<String>> {
            if (BuildConfig.FLAVOR == "abn" || BuildConfig.FLAVOR == "prodtest") {
                val sharedPreferences =
                    context.getSharedPreferences("wallet.test", Context.MODE_PRIVATE)
                val debugLogString =
                    (sharedPreferences.getString("wallet.test.debuglogs", "") ?: "")
                val debugDateString =
                    (sharedPreferences.getString("wallet.test.debugdates", "") ?: "")

                var debugLogs = mutableListOf<String>()
                var debugDates = mutableListOf<String>()

                if (debugLogString.isNotBlank() && debugDateString.isNotBlank()) {
                    debugLogs = debugLogString.split(";").toMutableList()
                    debugDates = debugDateString.split(";").toMutableList()
                }

                return Pair(debugLogs, debugDates)
            }
            return Pair(listOf(), listOf())
        }

        fun log(text: String, context: Context) {
            if (BuildConfig.FLAVOR == "abn" || BuildConfig.FLAVOR == "prodtest") {
                val logData = logData(context)
                val debugLogs = logData.first.toMutableList()
                val debugDates = logData.second.toMutableList()

                debugLogs.add(text)
                debugDates.add(Instant.now().toEpochMilli().toString())
                if (debugLogs.size > 100) {
                    debugLogs.removeFirst()
                    debugDates.removeFirst()
                }
                val editor = context.getSharedPreferences("wallet.test", Context.MODE_PRIVATE).edit()
                editor.putString("wallet.test.debuglogs", debugLogs.joinToString(";"))
                editor.putString("wallet.test.debugdates", debugDates.joinToString(";"))
                editor.apply()
            }
        }

        fun lastLogs(context: Context): List<Pair<Instant, String>> {
            if (BuildConfig.FLAVOR == "abn" || BuildConfig.FLAVOR == "prodtest") {
                val logData = logData(context)
                return logData.second.map { Instant.ofEpochMilli(it.toLong()) }.zip(logData.first)
            }
            return listOf()
        }
    }
}