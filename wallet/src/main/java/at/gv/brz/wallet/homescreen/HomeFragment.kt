/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.homescreen

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import at.gv.brz.common.config.CampaignButtonType
import at.gv.brz.common.config.CampaignType
import at.gv.brz.common.util.HorizontalMarginItemDecoration
import at.gv.brz.common.util.UrlUtil
import at.gv.brz.common.util.setSecureFlagToBlockScreenshots
import at.gv.brz.common.views.hideAnimated
import at.gv.brz.common.views.rotate
import at.gv.brz.common.views.showAnimated
import at.gv.brz.eval.CovidCertificateSdk
import at.gv.brz.eval.data.state.DecodeState
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R
import at.gv.brz.wallet.add.CertificateAddFragment
import at.gv.brz.wallet.data.Region
import at.gv.brz.wallet.databinding.FragmentHomeBinding
import at.gv.brz.wallet.detail.CertificateDetailFragment
import at.gv.brz.wallet.faq.WalletFaqFragment
import at.gv.brz.wallet.homescreen.pager.CertificatesPagerAdapter
import at.gv.brz.wallet.list.CertificatesListFragment
import at.gv.brz.wallet.pdf.PdfImportState
import at.gv.brz.wallet.pdf.PdfViewModel
import at.gv.brz.wallet.qr.WalletQrScanFragment
import at.gv.brz.wallet.regionlist.RegionListFragment
import at.gv.brz.wallet.settings.SettingsFragment
import at.gv.brz.wallet.util.NotificationUtil
import at.gv.brz.wallet.util.QueuedCampaignNotification
import at.gv.brz.wallet.util.lastDisplayTimestampKeyForCertificate
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class HomeFragment : Fragment() {

	companion object {

		fun newInstance(): HomeFragment {
			return HomeFragment()
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()
	private val pdfViewModel by activityViewModels<PdfViewModel>()

	private var _binding: FragmentHomeBinding? = null
	private val binding get() = _binding!!

	private lateinit var certificatesAdapter: CertificatesPagerAdapter

	private var isAddOptionsShowing = false

	private var certificateBoosterNotificationDialog: AlertDialog? = null
	private var certificateCheckTimestamp: Instant? = null
	private var certificateBoosterNotificationHash: Int = 0
	private var queuedCampaignNotifications: MutableList<QueuedCampaignNotification> = mutableListOf()

	private var currentPagerIndex: Int = -1
	private var currentCertificateCount: Int = 0
	private var currentCertificateOrderString: String = ""

	private val filePickerLauncher =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult: ActivityResult ->
			if (activityResult.resultCode == AppCompatActivity.RESULT_OK) {
				activityResult.data?.data?.let { uri ->
					pdfViewModel.importPdf(uri)
				}
			}
		}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentHomeBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		setupButtons()
		setupPager()
		setupImportObservers()
		view.announceForAccessibility(getString(at.gv.brz.common.R.string.wallet_main_loaded))
	}

	override fun onResume() {
		super.onResume()
		reloadCertificates()

		val region = Region.getRegionFromIdentifier(certificatesViewModel.secureStorage.getSelectedValidationRegion())
		if (region == null) {
			showRegionSelection()
		}
	}

	override fun onPause() {
		super.onPause()
		certificateBoosterNotificationDialog?.dismiss()
		certificateBoosterNotificationDialog = null
		certificateBoosterNotificationHash = 0
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupButtons() {
		setupAddCertificateOptions()

		binding.homescreenSupportButton.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, WalletFaqFragment.newInstance())
				.addToBackStack(WalletFaqFragment::class.java.canonicalName)
				.commit()
		}
		binding.homescreenListButton.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, CertificatesListFragment.newInstance())
				.addToBackStack(CertificatesListFragment::class.java.canonicalName)
				.commit()
		}

		val impressumClickListener = View.OnClickListener {

			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(
					R.id.fragment_container, SettingsFragment.newInstance()
				)
				.addToBackStack(SettingsFragment::class.java.canonicalName)
				.commit()
		}

		val regionSelectionClickListener = View.OnClickListener {
			showRegionSelection()
		}
		binding.homescreenHeaderEmpty.headerRegion.setOnClickListener(regionSelectionClickListener)
		binding.homescreenHeaderNotEmpty.headerRegion.setOnClickListener(regionSelectionClickListener)
		binding.homescreenHeaderEmpty.headerSettings.setOnClickListener(impressumClickListener)
		binding.homescreenHeaderNotEmpty.headerSettings.setOnClickListener(impressumClickListener)

		/**
		 * In test builds (for Q as well as P environment) we support a double tap on the country flag to change the device time setting.
		 * This setting allows the app to either use the real time fetched from a time server (behaviour in the published app) or to use the current device time for validating the business rules.
		 */
		if (BuildConfig.FLAVOR == "abn" || BuildConfig.FLAVOR == "prodtest") {
			val lastClick = AtomicLong(0)
			val debugButtonClickListener = View.OnClickListener {
				val now = System.currentTimeMillis()
				if (lastClick.get() > now - 1000L) {
					lastClick.set(0)
					val useDeviceTime = certificatesViewModel.toggleDeviceTimeSetting()

					val title = if (useDeviceTime) "Using Device Time" else "Using Real Time"
					val message = if (useDeviceTime) "The app now uses the current device time for Business Rule Validation" else "The app now uses the real time (fetched from NTP-Server) for Business Rule Validation"
					AlertDialog.Builder(requireContext(), R.style.CovidCertificate_AlertDialogStyle)
						.setTitle(title)
						.setMessage(message)
						.setPositiveButton(R.string.ok_button) { dialog, _ ->
							dialog.dismiss()
						}
						.setCancelable(true)
						.create()
						.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
						.show()
				} else {
					lastClick.set(now)
				}
			}
			binding.homescreenHeaderEmpty.bundLogo.setOnClickListener(debugButtonClickListener)
			binding.homescreenHeaderNotEmpty.bundLogo.setOnClickListener(debugButtonClickListener)
		}
	}

	private fun showRegionSelection() {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, RegionListFragment.newInstance())
			.addToBackStack(RegionListFragment::class.java.canonicalName)
			.commit()
	}

	private fun setupPager() {
		val viewPager = binding.homescreenCertificatesViewPager

		val marginPagerHorizontal = resources.getDimensionPixelSize(R.dimen.certificates_padding)
		val pageTransformer = ViewPager2.PageTransformer { page: View, position: Float ->
			page.translationX = -2 * marginPagerHorizontal * position
		}
		viewPager.setPageTransformer(pageTransformer)
		viewPager.addItemDecoration(HorizontalMarginItemDecoration(requireContext(), marginPagerHorizontal))
		viewPager.apply { (getChildAt(0) as? RecyclerView)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER }

		certificatesAdapter = CertificatesPagerAdapter(this)
		viewPager.offscreenPageLimit = 1
		viewPager.adapter = certificatesAdapter
		TabLayoutMediator(binding.homescreenCertificatesTabLayout, viewPager) { tab, position -> }.attach()

		certificatesViewModel.dccHolderCollectionLiveData.observe(viewLifecycleOwner) {
			it ?: return@observe
			binding.homescreenLoadingIndicator.isVisible = false
			updateHomescreen(it)
			handleCertificateNotifications()
		}

		certificatesViewModel.dataUpdateLiveData.observe(viewLifecycleOwner) {
			handleCertificateNotifications()
		}

		certificatesViewModel.onQrCodeClickedSingleLiveEvent.observe(this) { certificate ->
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, CertificateDetailFragment.newInstance(certificate))
				.addToBackStack(CertificateDetailFragment::class.java.canonicalName)
				.commit()
		}
		certificatesViewModel.configLiveData.observe(viewLifecycleOwner) { handleCertificateNotifications() }
		viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageScrollStateChanged(state: Int) {
				currentPagerIndex = viewPager.currentItem
			}
		})

	}

	private fun setupImportObservers() {
		pdfViewModel.pdfImportLiveData.observe(viewLifecycleOwner) { importState ->
			when (importState) {
				is PdfImportState.LOADING -> {
					binding.homescreenAddCertificateOptionsEmpty.optionImportPdf.importantForAccessibility = 2
					binding.loadingSpinner.showAnimated()
				}
				is PdfImportState.DONE -> {
					binding.loadingSpinner.hideAnimated()
					when (importState.decodeState) {
						is DecodeState.SUCCESS -> {
							isAddOptionsShowing = false
							showCertificationAddFragment(importState.decodeState.dccHolder)
						}
						is DecodeState.ERROR -> {
							showImportError(importState.decodeState.error.code)
						}
					}
					binding.homescreenAddCertificateOptionsEmpty.optionImportPdf.importantForAccessibility = 0
					pdfViewModel.clearPdf()
				}
			}
		}
	}

	private fun setupAddCertificateOptions() {
		binding.homescreenScanButtonSmall.setOnClickListener {
			showAddCertificateOptionsOverlay(!isAddOptionsShowing)
		}

		binding.backgroundDimmed.setOnClickListener {
			showAddCertificateOptionsOverlay(!isAddOptionsShowing)
		}

		binding.homescreenAddCertificateOptionsEmpty.optionScanCertificate.setOnClickListener { showQrScanFragment() }
		binding.homescreenAddCertificateOptionsNotEmpty.optionScanCertificate.setOnClickListener {
			isAddOptionsShowing = false
			showQrScanFragment()
		}

		binding.homescreenAddCertificateOptionsEmpty.optionImportPdf.setOnClickListener { launchPdfFilePicker() }
		binding.homescreenAddCertificateOptionsNotEmpty.optionImportPdf.setOnClickListener {
			isAddOptionsShowing = false
			launchPdfFilePicker()
		}
	}

	private fun showQrScanFragment() {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, WalletQrScanFragment.newInstance())
			.addToBackStack(WalletQrScanFragment::class.java.canonicalName)
			.commit()
	}

	private fun launchPdfFilePicker() {
		val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
			type = "application/pdf"
		}
		try {
			filePickerLauncher.launch(intent)
		} catch (e: ActivityNotFoundException) {
			Toast.makeText(context, "No file picker found", Toast.LENGTH_LONG).show()
		}
	}

	private fun showAddCertificateOptionsOverlay(show: Boolean) {
		if (show) {
			binding.homescreenSupportButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			binding.backgroundDimmed.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			binding.homescreenHeaderNotEmpty.headerSettings.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			binding.homescreenHeaderNotEmpty.headerRegion.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
			binding.homescreenEmptyContent.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			binding.homescreenCertificatesViewPager.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
			binding.homescreenOptionsOverlay.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			binding.homescreenListButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			binding.homescreenCertificatesTabLayout.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
			binding.homescreenScanButtonSmall.rotate(45f)
			binding.homescreenScanButtonSmall.contentDescription = getString(R.string.accessibility_close_button)
			binding.backgroundDimmed.showAnimated()
			binding.homescreenOptionsOverlay.showAnimated()

			val mainHandler = Handler(Looper.getMainLooper())
			mainHandler.postDelayed({
				binding.homescreenAddCertificateOptionsNotEmpty.homescreenOptionsOverlayAddCertificateOptionsTitle.requestFocus()
				binding.homescreenAddCertificateOptionsNotEmpty.homescreenOptionsOverlayAddCertificateOptionsTitle.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
			}, 500)
		} else {
			binding.homescreenSupportButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
			binding.homescreenHeaderNotEmpty.headerSettings.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
			binding.homescreenHeaderNotEmpty.headerRegion.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
			binding.homescreenEmptyContent.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
			binding.homescreenOptionsOverlay.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
			binding.homescreenCertificatesViewPager.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
			binding.homescreenListButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
			binding.homescreenCertificatesTabLayout.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
			binding.homescreenScanButtonSmall.rotate(0f)
			binding.homescreenScanButtonSmall.contentDescription = getString(R.string.accessibility_add_button)
			binding.backgroundDimmed.hideAnimated()
			binding.homescreenOptionsOverlay.hideAnimated()
		}

		isAddOptionsShowing = show
	}

	private fun showCertificationAddFragment(dccHolder: DccHolder) {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, CertificateAddFragment.newInstance(dccHolder, false))
			.addToBackStack(CertificateAddFragment::class.java.canonicalName)
			.commit()
	}

	private fun showImportError(errorCode: String) {
		val message = getString(R.string.wallet_import_error_invalid_format) + " ($errorCode)"
		AlertDialog.Builder(requireContext(), R.style.CovidCertificate_AlertDialogStyle)
			.setTitle(R.string.error_title)
			.setMessage(message)
			.setPositiveButton(R.string.ok_button) { dialog, _ ->
				dialog.dismiss()
			}
			.setCancelable(true)
			.create()
			.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
			.show()
	}

	private fun reloadCertificates() {
		binding.homescreenLoadingIndicator.isVisible = true
		certificatesViewModel.loadCertificates()
	}

	private fun updateHomescreen(dccHolders: List<DccHolder>) {
		val hasCertificates = dccHolders.isNotEmpty()

		binding.homescreenEmptyContent.isVisible = !hasCertificates
		binding.homescreenScanButtonSmall.isVisible = hasCertificates
		binding.homescreenListButton.isVisible = hasCertificates
		binding.homescreenCertificatesViewPager.isVisible = hasCertificates
		binding.homescreenCertificatesTabLayout.isVisible = dccHolders.size > 1
		binding.homescreenHeaderEmpty.root.isVisible = !hasCertificates
		binding.homescreenHeaderNotEmpty.root.isVisible = hasCertificates
		binding.homescreenListButton.isVisible = dccHolders.size > 1
		certificatesAdapter.setData(dccHolders)
		val newCertificateOrderString = dccHolders.map { it.qrCodeData }.joinToString("__")

		if (hasCertificates) {
			if (newCertificateOrderString == currentCertificateOrderString) {

				if (currentPagerIndex != -1 && currentCertificateCount == dccHolders.count()) {
					binding.homescreenCertificatesViewPager.setCurrentItem(
						currentPagerIndex,
						false
					)
				} else {
					binding.homescreenCertificatesViewPager.postDelayed(250) {
						if (isAdded) {
							currentCertificateCount = dccHolders.count()
							binding.homescreenCertificatesViewPager.setCurrentItem(
								0,
								true
							)
						}
					}
				}
			} else {
				binding.homescreenCertificatesViewPager.postDelayed(100) {
					if (isAdded) {
						currentCertificateCount = dccHolders.count()
						currentPagerIndex = 0
						binding.homescreenCertificatesViewPager.setCurrentItem(
							currentPagerIndex,
							true
						)
					}
				}
			}
		}
		currentCertificateOrderString = newCertificateOrderString

		val selectedRegion = Region.getRegionFromIdentifier(certificatesViewModel.secureStorage.getSelectedValidationRegion())
		if (selectedRegion != null) {
			binding.homescreenHeaderEmpty.headerRegionFlag.setImageResource(selectedRegion.getFlag())
			binding.homescreenHeaderNotEmpty.headerRegionFlag.setImageResource(selectedRegion.getFlag())
			binding.homescreenHeaderEmpty.headerRegionText.setText(selectedRegion.getName())
			binding.homescreenHeaderNotEmpty.headerRegionText.setText(selectedRegion.getName())
			binding.homescreenHeaderEmpty.headerRegionText.contentDescription =
				"${getString(selectedRegion.getName())}.\n" +
						"\n${getString(R.string.accessibility_state_selector)}.\n" +
						"\n${getString(R.string.accessibility_change_selected_region)}"
			binding.homescreenHeaderNotEmpty.headerRegionText.contentDescription =
				"${getString(selectedRegion.getName())}.\n" +
						"\n${getString(R.string.accessibility_state_selector)}.\n" +
						"\n${getString(R.string.accessibility_change_selected_region)}"
		} else {
			binding.homescreenHeaderEmpty.headerRegionText.contentDescription =
						"\n${getString(R.string.accessibility_state_selector)}.\n" +
						"\n${getString(R.string.accessibility_change_selected_region)}"
			binding.homescreenHeaderNotEmpty.headerRegionText.contentDescription =
						"\n${getString(R.string.accessibility_state_selector)}.\n" +
						"\n${getString(R.string.accessibility_change_selected_region)}"
		}
	}

	private fun handleCertificateNotifications() {
		val instant = Instant.now()
		certificateCheckTimestamp = instant

		if (!certificatesViewModel.hasLoadedData()) {
			return
		}
		if (certificatesViewModel.getValidationTime() == null) {
			return
		}
		if (CovidCertificateSdk.getValueSets().isEmpty()) {
			return
		}
		if (certificatesViewModel.configLiveData.value == null) {
			return
		}

		viewLifecycleOwner.lifecycleScope.launch {
			delay(1000)
			if (certificateCheckTimestamp == instant) {
				handleAutomaticTestCertificateRemoval { hasRemovedCertificates ->
					if (!hasRemovedCertificates) {
						handleCampaignNotifications()
					}
				}
			}
		}
	}

	private fun handleCampaignNotifications() {
		val dccHolders = certificatesViewModel.dccHolderCollectionLiveData.value ?: return
		val config = certificatesViewModel.configLiveData.value ?: return
		val validationClock = certificatesViewModel.getValidationTime() ?: return
		val valueSets = CovidCertificateSdk.getValueSets()

		if (valueSets.isEmpty()) {
			return
		}

		val campaignNotificationResult = NotificationUtil().startCertificateNotificationCheck(dccHolders, valueSets, validationClock, config, certificatesViewModel.secureStorage.getHasOptedOutOfNonImportantCampaigns(), certificatesViewModel.notificationStorage.getCurrentCertificateCampaignLastDisplayTimestamps(), requireContext())
		certificateBoosterNotificationHash = campaignNotificationResult.certificateCombinationHash
		queuedCampaignNotifications = campaignNotificationResult.queuedNotifications.toMutableList()

		presentAlertForNextQueuedCampaignNotification(campaignNotificationResult.certificateCombinationHash)
	}

	private fun handleAutomaticTestCertificateRemoval(completion: (hasRemovedCertificates: Boolean) -> Unit) {
		val dccHolders = certificatesViewModel.dccHolderCollectionLiveData.value
		if (dccHolders == null) {
			completion(false)
			return
		}
		val dccHoldersToRemove = NotificationUtil().testCertificateEligibleForAutomaticRemoval(dccHolders, certificatesViewModel.notificationStorage)
		if (dccHoldersToRemove.count() >= 10) {
			viewLifecycleOwner.lifecycleScope.launch {
				val notificationDialogBuilder = AlertDialog.Builder(requireContext(), R.style.CovidCertificate_AlertDialogStyle)
					.setMessage(getString(R.string.wallet_test_certificate_cleanup_message, dccHoldersToRemove.count()))
					.setNegativeButton(R.string.wallet_test_certificate_cleanup_no_button, null)
					.setCancelable(false)

				if (certificatesViewModel.notificationStorage.getExpiredTestCertificateReminderCount() >= 3) {
					notificationDialogBuilder.setNeutralButton(R.string.wallet_test_certificate_cleanup_never_button, null)
				}
				notificationDialogBuilder.setPositiveButton(R.string.wallet_test_certificate_cleanup_cleanup_button, null)

				val notificationDialog = notificationDialogBuilder.create()
					.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }

				notificationDialog.setOnShowListener {
					notificationDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
						notificationDialog.dismiss()
						certificatesViewModel.notificationStorage.setLastExpiredTestCertificateReminderDate(0)
						certificatesViewModel.notificationStorage.setShouldIgnoreExpiredTestCertificates(false)
						certificatesViewModel.notificationStorage.setExpiredTestCertificateReminderCount(0)

						certificatesViewModel.removeCertificates(dccHoldersToRemove.map { it.qrCodeData })

						completion(true)
					}
					notificationDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
						notificationDialog.dismiss()
						certificatesViewModel.notificationStorage.setLastExpiredTestCertificateReminderDate(Instant.now().toEpochMilli())
						certificatesViewModel.notificationStorage.setExpiredTestCertificateReminderCount(certificatesViewModel.notificationStorage.getExpiredTestCertificateReminderCount() + 1)

						completion(false)
					}
					notificationDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
						notificationDialog.dismiss()
						certificatesViewModel.notificationStorage.setLastExpiredTestCertificateReminderDate(0)
						certificatesViewModel.notificationStorage.setShouldIgnoreExpiredTestCertificates(true)
						certificatesViewModel.notificationStorage.setExpiredTestCertificateReminderCount(0)

						completion(false)
					}
				}
				notificationDialog.show()
			}
		} else {
			completion(false)
		}
	}

	private fun presentAlertForNextQueuedCampaignNotification(certificateHash: Int) {
		certificateBoosterNotificationDialog?.dismiss()
		certificateBoosterNotificationDialog = null

		if (certificateHash == certificateBoosterNotificationHash) {
			val campaignNotification = queuedCampaignNotifications.firstOrNull()
			if (campaignNotification != null) {
				queuedCampaignNotifications.removeAt(0)

				val languageKey = getString(R.string.language_key)
				val notificationDialogBuilder = AlertDialog.Builder(requireContext(), R.style.CovidCertificate_AlertDialogStyle)
				campaignNotification.title.let {
					notificationDialogBuilder.setTitle(it)
				}
				campaignNotification.message?.let {
					notificationDialogBuilder.setMessage(it)
				}
				notificationDialogBuilder.setCancelable(false)

				var hasAddedNegativeButton = false
				campaignNotification.campaign.buttons?.forEach { button ->
					when (button.type) {
						CampaignButtonType.DISMISS, CampaignButtonType.DISMISS_WITH_ACTION -> {
							if (hasAddedNegativeButton) {
								notificationDialogBuilder.setNeutralButton(
									button.getTitle(
										languageKey
									)
								) { _, _ ->
									certificateBoosterNotificationDialog?.dismiss()
									certificateBoosterNotificationDialog = null

									campaignNotification.campaign.lastDisplayTimestampKeyForCertificate(campaignNotification.certificate)?.let {
										certificatesViewModel.notificationStorage.setCertificateCampaignLastDisplayTimestampForIdentifier(it, LocalDateTime.now().plusYears(100).toInstant(ZoneOffset.UTC).toEpochMilli())
									}

									if (button.type == CampaignButtonType.DISMISS_WITH_ACTION && button.urlString != null) {
										UrlUtil.openUrl(requireContext(), button.urlString)
									} else {
										presentAlertForNextQueuedCampaignNotification(
											certificateHash
										)
									}
								}
							} else {
								hasAddedNegativeButton = true
								notificationDialogBuilder.setNegativeButton(
									button.getTitle(
										languageKey
									)
								) { _, _ ->
									certificateBoosterNotificationDialog?.dismiss()
									certificateBoosterNotificationDialog = null

									campaignNotification.campaign.lastDisplayTimestampKeyForCertificate(campaignNotification.certificate)?.let {
										certificatesViewModel.notificationStorage.setCertificateCampaignLastDisplayTimestampForIdentifier(it, LocalDateTime.now().plusYears(100).toInstant(ZoneOffset.UTC).toEpochMilli())
									}

									if (button.type == CampaignButtonType.DISMISS_WITH_ACTION && button.urlString != null) {
										UrlUtil.openUrl(requireContext(), button.urlString)
									} else {
										presentAlertForNextQueuedCampaignNotification(
											certificateHash
										)
									}
								}
							}
						}
						CampaignButtonType.LATER, CampaignButtonType.LATER_WITH_ACTION -> {
							if (campaignNotification.campaign.campaignType == CampaignType.REPEATING || campaignNotification.campaign.campaignType == CampaignType.REPEATING_ANY_CERTIFICATE || campaignNotification.campaign.campaignType == CampaignType.REPEATING_EACH_CERTIFICATE) {
								notificationDialogBuilder.setPositiveButton(button.getTitle(languageKey)) { _, _ ->
									certificateBoosterNotificationDialog?.dismiss()
									certificateBoosterNotificationDialog = null

									campaignNotification.campaign.lastDisplayTimestampKeyForCertificate(campaignNotification.certificate)?.let {
										certificatesViewModel.notificationStorage.setCertificateCampaignLastDisplayTimestampForIdentifier(it, LocalDateTime.now().toInstant(
											ZoneOffset.UTC).toEpochMilli())
									}

									if (button.type == CampaignButtonType.LATER_WITH_ACTION && button.urlString != null) {
										UrlUtil.openUrl(requireContext(), button.urlString)
									} else {
										presentAlertForNextQueuedCampaignNotification(
											certificateHash
										)
									}
								}
							}
						}
					}
				}

				val notificationDialog = notificationDialogBuilder.create()
					.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }

				this.certificateBoosterNotificationDialog = notificationDialog
				notificationDialog.show()
			} else {
				certificateBoosterNotificationHash = 0
			}
		}
	}
}