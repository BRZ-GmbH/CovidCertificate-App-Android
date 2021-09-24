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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import at.gv.brz.common.config.InfoBoxModel
import at.gv.brz.common.data.ConfigSecureStorage
import at.gv.brz.common.dialog.InfoDialogFragment
import at.gv.brz.common.html.BuildInfo
import at.gv.brz.common.html.HtmlFragment
import at.gv.brz.common.util.AssetUtil
import at.gv.brz.common.util.HorizontalMarginItemDecoration
import at.gv.brz.common.util.setSecureFlagToBlockScreenshots
import at.gv.brz.common.views.hideAnimated
import at.gv.brz.common.views.rotate
import at.gv.brz.common.views.showAnimated
import at.gv.brz.eval.data.state.DecodeState
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R
import at.gv.brz.wallet.add.CertificateAddFragment
import at.gv.brz.wallet.data.Region
import at.gv.brz.wallet.databinding.FragmentHomeBinding
import at.gv.brz.wallet.debug.DebugFragment
import at.gv.brz.wallet.detail.CertificateDetailFragment
import at.gv.brz.wallet.faq.WalletFaqFragment
import at.gv.brz.wallet.homescreen.pager.CertificatesPagerAdapter
import at.gv.brz.wallet.list.CertificatesListFragment
import at.gv.brz.wallet.pdf.PdfImportState
import at.gv.brz.wallet.pdf.PdfViewModel
import at.gv.brz.wallet.qr.WalletQrScanFragment
import at.gv.brz.wallet.regionlist.RegionListFragment
import at.gv.brz.wallet.util.NotificationUtil
import at.gv.brz.wallet.util.neverAgainNotificationTimestamp
import at.gv.brz.wallet.util.nextNotificationTimestamp
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
	private var certificateBoosterNotificationHash: Int = 0

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
		setupInfoBox()
		setupImportObservers()
	}

	override fun onResume() {
		super.onResume()
		reloadCertificates()
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
			val buildInfo =
				BuildInfo(
					getString(R.string.wallet_onboarding_app_title),
					BuildConfig.VERSION_NAME,
					BuildConfig.BUILD_TIME,
					BuildConfig.FLAVOR,
					getString(R.string.wallet_terms_privacy_link)
				)
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(
					R.id.fragment_container, HtmlFragment.newInstance(
						R.string.impressum_title,
						buildInfo,
						AssetUtil.getImpressumBaseUrl(it.context),
						AssetUtil.getImpressumHtml(it.context, buildInfo),
						R.id.fragment_container
					)
				)
				.addToBackStack(HtmlFragment::class.java.canonicalName)
				.commit()
		}

		val regionSelectionClickListener = View.OnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, RegionListFragment.newInstance())
				.addToBackStack(RegionListFragment::class.java.canonicalName)
				.commit()
		}
		binding.homescreenHeaderEmpty.headerRegion.setOnClickListener(regionSelectionClickListener)
		binding.homescreenHeaderNotEmpty.headerRegion.setOnClickListener(regionSelectionClickListener)
		binding.homescreenHeaderEmpty.headerImpressum.setOnClickListener(impressumClickListener)
		binding.homescreenHeaderNotEmpty.headerImpressum.setOnClickListener(impressumClickListener)

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
			showCertificateNotifications(it)
		}

		certificatesViewModel.onQrCodeClickedSingleLiveEvent.observe(this) { certificate ->
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, CertificateDetailFragment.newInstance(certificate))
				.addToBackStack(CertificateDetailFragment::class.java.canonicalName)
				.commit()
		}
	}

	private fun setupImportObservers() {
		pdfViewModel.pdfImportLiveData.observe(viewLifecycleOwner) { importState ->
			when (importState) {
				is PdfImportState.LOADING -> {
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
			binding.homescreenScanButtonSmall.rotate(45f)
			binding.backgroundDimmed.showAnimated()
			binding.homescreenOptionsOverlay.showAnimated()
		} else {
			binding.homescreenScanButtonSmall.rotate(0f)
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
		val message = getString(R.string.verifier_error_invalid_format) + " ($errorCode)"
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
		if (hasCertificates) {
			binding.homescreenCertificatesViewPager.postDelayed(250) {
				if (isAdded) {
					binding.homescreenCertificatesViewPager.setCurrentItem(0, true)
				}
			}
		}
		val selectedRegion = Region.values().first { it.identifier == certificatesViewModel.secureStorage.getSelectedValidationRegion() }
		binding.homescreenHeaderEmpty.headerRegionFlag.setImageResource(selectedRegion.getFlag())
		binding.homescreenHeaderNotEmpty.headerRegionFlag.setImageResource(selectedRegion.getFlag())
		binding.homescreenHeaderEmpty.headerRegionText.setText(selectedRegion.getName())
		binding.homescreenHeaderNotEmpty.headerRegionText.setText(selectedRegion.getName())
	}

	private fun showCertificateNotifications(dccHolders: List<DccHolder>) {
		/*
		Disable for release 2.1.0 - EPIEMSCO-1527 will be launched with release 2.2.0
		certificateBoosterNotificationHash = dccHolders.joinToString("_") { it.qrCodeData }.hashCode()
		viewLifecycleOwner.lifecycleScope.launch {
			delay(1000)
			val notifyableCertificates = NotificationUtil().certificatesForBoosterNotification(
				dccHolders,
				certificatesViewModel.notificationStorage
			)

			showNotificationAlert(notifyableCertificates, certificateBoosterNotificationHash)
		}*/
	}

	private fun showNotificationAlert(certificates: List<DccHolder>, certificateHash: Int) {
		certificateBoosterNotificationDialog?.dismiss()
		certificateBoosterNotificationDialog = null
		if (certificateHash == certificateBoosterNotificationHash) {
			val certificate = certificates.firstOrNull()
			val certificateIdentifier = certificate?.euDGC?.vaccinations?.firstOrNull()?.certificateIdentifier
			if (certificate != null && certificateIdentifier != null) {
				val remainingCertificates = certificates.drop(1)

				val notificationDialog = AlertDialog.Builder(requireContext(), R.style.CovidCertificate_AlertDialogStyle)
					.setTitle(R.string.vaccination_booster_notification_title)
					.setMessage(R.string.vaccination_booster_notification_message)
					.setPositiveButton(R.string.vaccination_booster_notification_later, null)
					.setNegativeButton(R.string.vaccination_booster_notification_read, null)
					.setCancelable(false)
					.create()
					.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
				notificationDialog.setOnShowListener {
					notificationDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
						certificatesViewModel.notificationStorage.setNotificationTimestampForCertificateIdentifier(certificateIdentifier, certificate.nextNotificationTimestamp())
						showNotificationAlert(remainingCertificates, certificateHash)
					}
					notificationDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
						certificatesViewModel.notificationStorage.setNotificationTimestampForCertificateIdentifier(certificateIdentifier, certificate.neverAgainNotificationTimestamp())
						showNotificationAlert(remainingCertificates, certificateHash)
					}
				}

				this.certificateBoosterNotificationDialog = notificationDialog
				notificationDialog.show()
			}
		}
	}

	private fun setupInfoBox() {
		certificatesViewModel.configLiveData.observe(viewLifecycleOwner) { config ->
			val buttonHeaderEmpty = binding.homescreenHeaderEmpty.headerNotification
			val buttonHeaderNotEmpty = binding.homescreenHeaderNotEmpty.headerNotification
			val localizedInfo = config.getInfoBox(getString(R.string.language_key))
			val hasInfoBox = localizedInfo != null

			val onClickListener = localizedInfo?.let { infoBox ->
				val secureStorage = ConfigSecureStorage.getInstance(buttonHeaderEmpty.context)
				if (secureStorage.getLastShownInfoBoxId() != infoBox.infoId) {
					closeCurrentInfoDialog()
					showInfoDialog(infoBox)
					secureStorage.setLastShownInfoBoxId(infoBox.infoId)
				}

				return@let View.OnClickListener { view ->
					closeCurrentInfoDialog()
					showInfoDialog(infoBox)
					secureStorage.setLastShownInfoBoxId(infoBox.infoId)
				}

			}

			buttonHeaderEmpty.isVisible = hasInfoBox
			buttonHeaderEmpty.setOnClickListener(onClickListener)
			buttonHeaderNotEmpty.isVisible = hasInfoBox
			buttonHeaderNotEmpty.setOnClickListener(onClickListener)
		}
	}

	private fun closeCurrentInfoDialog() {
		(childFragmentManager.findFragmentByTag(InfoDialogFragment::class.java.canonicalName) as? InfoDialogFragment)?.dismiss()
	}

	private fun showInfoDialog(infoBox: InfoBoxModel) {
		InfoDialogFragment.newInstance(infoBox).show(childFragmentManager, InfoDialogFragment::class.java.canonicalName)
	}
}