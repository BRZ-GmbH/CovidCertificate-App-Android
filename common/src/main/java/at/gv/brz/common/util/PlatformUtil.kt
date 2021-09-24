package at.gv.brz.common.util

import android.content.Context

class PlatformUtil {
    enum class PlatformType {
        GOOGLE_PLAY,
        HUAWEI
    }

    companion object {
        fun getPlatformType(context: Context): PlatformType {
            try {
                val installerPackage = (context.packageManager.getInstallerPackageName(context.packageName)) ?: return PlatformType.GOOGLE_PLAY
                return if (installerPackage.contains("huawei", ignoreCase = true)) PlatformType.HUAWEI else PlatformType.GOOGLE_PLAY
            } catch (e: Exception) {
            }
            return PlatformType.GOOGLE_PLAY
        }
    }

}