/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import at.gv.brz.common.config.ConfigModel
import at.gv.brz.common.util.PlatformUtil
import at.gv.brz.common.util.UrlUtil
import at.gv.brz.common.util.setSecureFlagToBlockScreenshots
import at.gv.brz.eval.CovidCertificateSdk
import at.gv.brz.wallet.data.WalletSecureStorage
import at.gv.brz.wallet.databinding.ActivityMainBinding
import at.gv.brz.wallet.homescreen.HomeFragment
import at.gv.brz.wallet.onboarding.FeatureIntroActivity
import at.gv.brz.wallet.onboarding.OnboardingActivity
import at.gv.brz.wallet.pdf.PdfViewModel
import at.gv.brz.wallet.util.DebugLogUtil
import at.gv.brz.wallet.util.WalletAssetUtil
import com.google.android.play.core.review.ReviewManagerFactory
import java.lang.Integer.max

class MainActivity : AppCompatActivity() {

	companion object {
		private const val KEY_IS_INTENT_CONSUMED = "KEY_IS_INTENT_CONSUMED"
	}

	private val certificateViewModel by viewModels<CertificatesViewModel>()
	private val pdfViewModel by viewModels<PdfViewModel>()

	private lateinit var binding: ActivityMainBinding
	val secureStorage by lazy { WalletSecureStorage.getInstance(this) }

	private var forceUpdateDialog: AlertDialog? = null
	private var isIntentConsumed = false

	private var hasCheckedForcedUpdate = false
	private var forceRefreshTrustlist = true
	private var ignoreForcedUpdateBecauseOfPDFImport = false

	private val onAndUpdateBoardingLauncher =
		registerForActivityResult(StartActivityForResult()) { activityResult: ActivityResult ->
			if (activityResult.resultCode == RESULT_OK) {
				secureStorage.setLastInstalledVersion(BuildConfig.VERSION_NAME)
				secureStorage.setOnboardingCompleted(true)
				showHomeFragment()
			} else {
				finish()
			}
		}

	private val onAndUpdateFeatureLauncher =
		registerForActivityResult(StartActivityForResult()) { activityResult: ActivityResult ->
			if (activityResult.resultCode == RESULT_OK) {
				secureStorage.setLastInstalledVersion(BuildConfig.VERSION_NAME)
				showHomeFragment()
			} else {
				finish()
			}
		}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityMainBinding.inflate(layoutInflater)
		val view = binding.root
		setContentView(view)

		window.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR)

		if (savedInstanceState == null) {
			val onboardingCompleted: Boolean = secureStorage.getOnboardingCompleted()
			val sameVersion: Boolean = secureStorage.getLastInstalledVersion() == BuildConfig.VERSION_NAME

			if (!onboardingCompleted) {
				onAndUpdateBoardingLauncher.launch(Intent(this, OnboardingActivity::class.java))
			} else if (!sameVersion && WalletAssetUtil.hasFeatureIntrosForLanguageAndVersion(this, getString(R.string.language_key), BuildConfig.VERSION_NAME)) {
				onAndUpdateFeatureLauncher.launch(Intent(this, FeatureIntroActivity::class.java))
			} else {
				if (!sameVersion && secureStorage.getHasAskedForInAppReview() == false) {
					secureStorage.setHasAskedForInAppReview(true)

					if (PlatformUtil.getPlatformType(applicationContext) == PlatformUtil.PlatformType.GOOGLE_PLAY) {
						val mainHandler = Handler(Looper.getMainLooper())
						mainHandler.postDelayed({
							val manager = ReviewManagerFactory.create(applicationContext)

							val request = manager.requestReviewFlow()
							request.addOnCompleteListener { task ->
								if (task.isSuccessful) {
									val reviewInfo = task.result
									val flow = manager.launchReviewFlow(this, reviewInfo)
									flow.addOnCompleteListener { _ ->
									}
								}
							}
						}, 500)
					}
				}
				secureStorage.setLastInstalledVersion(BuildConfig.VERSION_NAME)
				showHomeFragment()
			}
		}

		certificateViewModel.configLiveData.observe(this) { config -> handleConfig(config) }

		CovidCertificateSdk.registerWithLifecycle(lifecycle)

		if (savedInstanceState != null) {
			isIntentConsumed = savedInstanceState.getBoolean(KEY_IS_INTENT_CONSUMED)
		}
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		setIntent(intent)
		isIntentConsumed = false
	}

	override fun onResume() {
		super.onResume()
		checkIntentForActions()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(KEY_IS_INTENT_CONSUMED, isIntentConsumed)
	}

	private fun checkIntentForActions() {
		val launchedFromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
		if (!launchedFromHistory && !isIntentConsumed) {
			isIntentConsumed = true
			when (intent.action) {
				Intent.ACTION_SEND -> {
					if ("application/pdf" == intent.type) {
						handleCertificatePDF(intent)
					}
				}
			}
		}
	}

	private fun handleCertificatePDF(intent: Intent) {
		if (secureStorage.getOnboardingCompleted()) {
			intent.clipData?.let {
				ignoreForcedUpdateBecauseOfPDFImport = true
				pdfViewModel.importPdf(clipData = it)
			}
		}
	}

	override fun onStart() {
		super.onStart()
		hasCheckedForcedUpdate = false
		certificateViewModel.loadConfig()
		CovidCertificateSdk.getCertificateVerificationController().refreshTrustList(lifecycleScope, forceRefreshTrustlist, onCompletionCallback = {
			if (it.failed) {
				DebugLogUtil.log("${if (forceRefreshTrustlist) "Forced " else ""}Data Update - Failed", this)
			} else {
				if (it.refreshed) {
					DebugLogUtil.log("${if (forceRefreshTrustlist) "Forced " else ""}Data Update - New Data", this)
				} else {
					DebugLogUtil.log("${if (forceRefreshTrustlist) "Forced " else ""}Data Update - Unchanged", this)
				}
			}
			forceRefreshTrustlist = false
			certificateViewModel.setHasUpdatedTrustlistData()
			if (it.refreshed) {
				certificateViewModel.reverifyAllCertificates()
			}
		})
	}

	override fun onStop() {
		super.onStop()

		certificateViewModel.resetLoadingFlags()
	}

	override fun onPause() {
		super.onPause()
		forceUpdateDialog?.dismiss()
		forceUpdateDialog = null
		ignoreForcedUpdateBecauseOfPDFImport = false
	}

	override fun onDestroy() {
		super.onDestroy()
		CovidCertificateSdk.unregisterWithLifecycle(lifecycle)
	}

	private fun showHomeFragment() {
		supportFragmentManager.beginTransaction()
			.add(R.id.fragment_container, HomeFragment.newInstance())
			.commit()
	}

	private fun handleConfig(config: ConfigModel) {
		if (hasCheckedForcedUpdate || ignoreForcedUpdateBecauseOfPDFImport) {
			return
		}
		hasCheckedForcedUpdate = true
		val platformType = PlatformUtil.getPlatformType(this)
		val configVersion = if (platformType == PlatformUtil.PlatformType.HUAWEI) config.huawei else config.android
		if (configVersion != null && Version(configVersion).compareTo(Version(BuildConfig.VERSION_NAME)) == 1) {
			if (forceUpdateDialog != null) {
				forceUpdateDialog?.dismiss()
				forceUpdateDialog = null
			}

			val shouldForceUpdate = config.shouldForceUpdate(platformType)

			val forceUpdateDialogBuilder = AlertDialog.Builder(this, R.style.CovidCertificate_AlertDialogStyle)
				.setCancelable(false)

			if (shouldForceUpdate) {
				forceUpdateDialogBuilder
					.setTitle(R.string.force_update_title)
					.setMessage(R.string.force_update_text)
					.setPositiveButton(R.string.force_update_button, null)
			} else {
				val date = config.formattedForceDate(platformType)
				forceUpdateDialogBuilder
					.setTitle(R.string.force_update_grace_period_title)
					.setMessage(getString(R.string.force_update_grace_period_text, date))
					.setPositiveButton(R.string.force_update_grace_period_update_button, null)
					.setNegativeButton(R.string.force_update_grace_period_skip_button, null)
			}

			val forceUpdateDialog = forceUpdateDialogBuilder
				.create()
				.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
			forceUpdateDialog.setOnShowListener {
				forceUpdateDialog.getButton(DialogInterface.BUTTON_POSITIVE)
					.setOnClickListener {
						UrlUtil.openStoreUrl(this@MainActivity, "market://details?id=at.gv.brz.wallet", "appmarket://details?id=at.gv.brz.wallet")
					}
				forceUpdateDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
					forceUpdateDialog.dismiss()
					this.forceUpdateDialog = null
				}
			}
			this.forceUpdateDialog = forceUpdateDialog
			forceUpdateDialog.show()
		}
	}
}

/**
 * Represents a version string (= version name on Android) that can be logically compared to other versions.
 */
class Version(inputVersion: String) : Comparable<Version> {

	var version: String
		private set

	override fun compareTo(other: Version) =
		(split() to other.split()).let {(thisParts, thatParts)->
			val length = max(thisParts.size, thatParts.size)
			for (i in 0 until length) {
				val thisPart = if (i < thisParts.size) thisParts[i].toInt() else 0
				val thatPart = if (i < thatParts.size) thatParts[i].toInt() else 0
				if (thisPart < thatPart) return -1
				if (thisPart > thatPart) return 1
			}
			0
		}

	init {
		require(inputVersion.matches("[0-9]+(\\.[0-9]+)*".toRegex())) { "Invalid version format" }
		version = inputVersion
	}
}

fun Version.split() = version.split(".").toTypedArray()