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
import android.app.Dialog
import android.net.Uri
import at.gv.brz.common.config.Birthdate
import at.gv.brz.common.config.DirectLinkModel
import at.gv.brz.sdk.data.state.DecodeState
import at.gv.brz.sdk.data.state.StateError
import at.gv.brz.sdk.decoder.CertificateDecoder
import at.gv.brz.wallet.add.CertificateAddFragment
import at.gv.brz.wallet.databinding.DialogDirectlinkDatepickerBinding
import java.util.*
import at.gv.brz.wallet.directlink.WalletDirectLinkViewModel
import at.gv.brz.wallet.directlink.WalletDirectLinkViewModel.DirectLinkType.*
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
import at.gv.brz.common.config.DirectLinkResult
import at.gv.brz.common.util.PlatformUtil
import at.gv.brz.common.util.UrlUtil
import at.gv.brz.common.util.setSecureFlagToBlockScreenshots
import at.gv.brz.sdk.CovidCertificateSdk
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
import at.gv.brz.wallet.notification.NotificationHelper

class MainActivity : AppCompatActivity() {

	companion object {
		private const val KEY_IS_INTENT_CONSUMED = "KEY_IS_INTENT_CONSUMED"
		private const val KEY_IS_INTENT_CONSUMED_DIRECTLINK = "KEY_IS_INTENT_CONSUMED_DIRECTLINK"
	}

	private val certificateViewModel by viewModels<CertificatesViewModel>()
	private val pdfViewModel by viewModels<PdfViewModel>()
	private val directLinkViewModel by viewModels<WalletDirectLinkViewModel>()

	private lateinit var binding: ActivityMainBinding
	val secureStorage by lazy { WalletSecureStorage.getInstance(this) }

	private var forceUpdateDialog: AlertDialog? = null
	private var isIntentConsumed = false
	private var isDirectLinkIntentConsumed = false

	private var hasCheckedForcedUpdate = false
	private var forceRefreshTrustlist = true
	private var ignoreForcedUpdateBecauseOfPDFImport = false

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
			isDirectLinkIntentConsumed = savedInstanceState.getBoolean(
				KEY_IS_INTENT_CONSUMED_DIRECTLINK)
		}

	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		setIntent(intent)
		isIntentConsumed = false
		isDirectLinkIntentConsumed = false
	}



	private fun handleDirectLink(intent: Intent?) {
		val appLinkAction: String? = intent?.action
		val appLinkData: Uri? = intent?.data
		if (Intent.ACTION_VIEW == appLinkAction && appLinkData != null) {
			isDirectLinkIntentConsumed = true
			when (val it=directLinkViewModel.checkDirectLinkType(appLinkData)){
				is SmsLink -> {
					showDatePicker(it.secret, it.signature)
				}
				/*is WebLink -> {
					decodeQrCodeData(String().decodeBase64QrData(it.base64EncodedQRCodeData), onDecodeSuccess = {}, onDecodeError = {showInvalidDirectLink()})
				}*/
				is BypassTokenLink -> {
					addCertificateFromByPassToken(it.secret, it.signature, it.bypassToken)
				}
				else -> {
					showInvalidDirectLink()
				}
			}
		}
	}


	private fun addCertificateFromByPassToken(secret:String, signature:String, bypassToken: String?){
		val directLinkData = DirectLinkModel(
			secret,
			signature,
			bypassToken = bypassToken,
			request = arrayListOf("qr")
		)
		directLinkViewModel.loadCertificateWithBirthdateOrBypassToken(
			directLinkData,
			BuildConfig.smsImportLinkHost
		)

		directLinkViewModel.directLinkResponseLiveData.observe(this@MainActivity) {
			when (it){
				is DirectLinkResult.Valid -> {
					decodeQrCodeData(it.qr, onDecodeSuccess = {}, onDecodeError = {showErrorDialog(secret, signature = signature, bypassToken = bypassToken, title = getString(R.string.directlink_error_title), message = getString(R.string.directlink_error_message_forbypasstoken))})
				}
				is DirectLinkResult.InvalidRequestData -> {
					showErrorDialog(secret, signature, bypassToken = bypassToken, title = getString(R.string.directlink_error_title), message = getString(R.string.directlink_error_message_forbypasstoken))
				}
				is DirectLinkResult.NetworkError -> {
					showErrorDialog(secret, signature, bypassToken = bypassToken, title = getString(R.string.error_network_title), message = getString(R.string.error_network_text))
				}
			}
			directLinkViewModel.directLinkResponseLiveData.removeObservers(this@MainActivity)
		}

	}

	private fun showDatePicker(secret:String, signature:String) {

		val currentCalendar = Calendar.getInstance()
		val dialog = Dialog(this)
		val bindingDialog: DialogDirectlinkDatepickerBinding = DialogDirectlinkDatepickerBinding.inflate(layoutInflater)
		dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
		dialog.setCancelable(false)
		dialog.setContentView(bindingDialog.root)

		bindingDialog.apply {
			val datePicker = directlinkDatepicker
			val buttonCancel = dialogButtonCancel
			val buttonRetrieve = dialogButtonRetrieve
			datePicker.maxDate = currentCalendar.timeInMillis

			buttonRetrieve.setOnClickListener {
				val directLinkData = DirectLinkModel(
					secret,
					signature,
					birthdate = Birthdate(datePicker.dayOfMonth, datePicker.month + 1, datePicker.year),
					request = arrayListOf("qr")
				)

				directLinkViewModel.loadCertificateWithBirthdateOrBypassToken(
					directLinkData,
					BuildConfig.smsImportLinkHost
				)

				directLinkViewModel.directLinkResponseLiveData.observe(this@MainActivity) {
					when (it){
						is DirectLinkResult.Valid -> {
							decodeQrCodeData(it.qr, onDecodeSuccess = {}, onDecodeError = {showErrorDialog(secret,  signature, title = getString(R.string.directlink_error_title), message = getString(R.string.directlink_error_message))})
						}
						is DirectLinkResult.InvalidRequestData -> {
							showErrorDialog(secret, signature, title = getString(R.string.directlink_error_title), message = getString(R.string.directlink_error_message))
						}
						is DirectLinkResult.NetworkError -> {
							showErrorDialog(secret, signature, title = getString(R.string.error_network_title), message = getString(R.string.error_network_text))
						}
					}
					dialog.dismiss()
					directLinkViewModel.directLinkResponseLiveData.removeObservers(this@MainActivity)
				}
			}
			buttonCancel.setOnClickListener {
				dialog.dismiss()
			}
		}
		dialog.show()
	}

	private fun decodeQrCodeData(qrCodeData: String, onDecodeSuccess: () -> Unit, onDecodeError: (StateError) -> Unit) {
		when (val decodeState = CertificateDecoder.decode(qrCodeData)) {
			is DecodeState.SUCCESS -> {
				onDecodeSuccess.invoke()
				supportFragmentManager.beginTransaction()
					.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
					.replace(R.id.fragment_container, CertificateAddFragment.newInstance(decodeState.dccHolder, false))
					.addToBackStack(CertificateAddFragment::class.java.canonicalName)
					.commit()
			}
			is DecodeState.ERROR -> onDecodeError.invoke(decodeState.error)
		}
	}

	private fun showInvalidDirectLink(){
		AlertDialog.Builder(this, R.style.CovidCertificate_AlertDialogStyle)
			.setTitle(R.string.directlink_invalidlink_title)
			.setMessage(R.string.directlink_invalidlink_message)
			.setPositiveButton(R.string.directlink_invalidlink_button_ok) { dialog, _ ->
				dialog.dismiss()
			}
			.setCancelable(true)
			.create()
			.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
			.show()
	}

	private fun showErrorDialog(secret:String, signature:String, bypassToken: String?=null, title:String, message:String) {
		AlertDialog.Builder(this, R.style.CovidCertificate_AlertDialogStyle)
			.setTitle(title)
			.setMessage(message)
			.setPositiveButton(R.string.directlink_error_button_tryagain) { dialog, _ ->
				if(bypassToken==null){
					showDatePicker(secret, signature)
				} else {
					addCertificateFromByPassToken(secret, signature, bypassToken)
				}
				dialog.dismiss()
			}
			.setNegativeButton(R.string.directlink_error_button_cancel)	{ dialog, _ ->
				dialog.dismiss()
			}
			.setCancelable(true)
			.create()
			.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
			.show()
	}

	override fun onResume() {
		super.onResume()
		checkIntentForActions()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(KEY_IS_INTENT_CONSUMED, isIntentConsumed)
		outState.putBoolean(KEY_IS_INTENT_CONSUMED_DIRECTLINK, isDirectLinkIntentConsumed)
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
		if(!launchedFromHistory && !isDirectLinkIntentConsumed){
			handleDirectLink(intent)
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
		intent?.let {
			if(it.hasExtra(NotificationHelper.KEY_NOTIFICATION_EXTRAS)) {
				secureStorage.setNotificationCampaignID(it.getStringExtra(NotificationHelper.KEY_CAMPAIGN_ID))
				secureStorage.setNotificationCampaignLastTimeStampKey(it.getStringExtra(NotificationHelper.KEY_LAST_DISPLAY_TIMESTAMP))
				secureStorage.setNotificationCampaignTitle(it.getStringExtra(NotificationHelper.KEY_CAMPAIGN_TITLE))
				secureStorage.setNotificationCampaignMessage(it.getStringExtra(NotificationHelper.KEY_CAMPAIGN_MESSAGE))
			}
		}
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