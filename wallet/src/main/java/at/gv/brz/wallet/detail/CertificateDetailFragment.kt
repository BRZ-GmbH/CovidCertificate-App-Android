/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.detail

import android.content.res.ColorStateList
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import at.gv.brz.common.util.setSecureFlagToBlockScreenshots
import at.gv.brz.common.views.animateBackgroundTintColor
import at.gv.brz.common.views.hideAnimated
import at.gv.brz.common.views.showAnimated
import at.gv.brz.eval.models.CertType
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.eval.utils.*
import at.gv.brz.common.util.makeSubStringBold
import at.gv.brz.eval.data.state.*
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R
import at.gv.brz.wallet.data.Region
import at.gv.brz.wallet.databinding.FragmentCertificateDetailBinding
import at.gv.brz.wallet.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

class CertificateDetailFragment : Fragment() {

	companion object {
		private const val STATUS_HIDE_DELAY = 2000L
		private const val STATUS_LOAD_DELAY = 1000L

		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificate: DccHolder): CertificateDetailFragment = CertificateDetailFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificate)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private var _binding: FragmentCertificateDetailBinding? = null
	private val binding get() = _binding!!

	private lateinit var dccHolder: DccHolder

	private var hideDelayedJob: Job? = null

	private var isForceValidate = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		dccHolder = (arguments?.getSerializable(ARG_CERTIFICATE) as? DccHolder)
			?: throw IllegalStateException("Certificate detail fragment created without Certificate!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificateDetailBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		displayQrCode()
		setupCertificateDetails()
		setupStatusInfo()

		binding.certificateDetailToolbar.setNavigationOnClickListener { v: View? ->
			parentFragmentManager.popBackStack()
		}

		binding.certificateDetailButtonDelete.setOnClickListener { view ->
			AlertDialog.Builder(view.context, R.style.CovidCertificate_AlertDialogStyle)
				.setTitle(R.string.delete_button)
				.setMessage(R.string.wallet_certificate_delete_confirm_text)
				.setPositiveButton(R.string.delete_button) { dialog, which ->
					certificatesViewModel.removeCertificate(dccHolder.qrCodeData)
					parentFragmentManager.popBackStack()
				}
				.setNegativeButton(R.string.cancel_button) { dialog, which ->
					dialog.dismiss()
				}
				.setCancelable(true)
				.create()
				.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
				.show()
		}

		binding.certificateDetailButtonReverify.setOnClickListener {
			binding.certificateDetailButtonReverify.hideAnimated()
			binding.scrollview.smoothScrollTo(0, 0)
			isForceValidate = true
			hideDelayedJob?.cancel()
			certificatesViewModel.startVerification(dccHolder, delayInMillis = STATUS_LOAD_DELAY, isForceVerification = true)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun displayQrCode() {
		val qrCodeBitmap = QrCode.renderToBitmap(dccHolder.qrCodeData)
		val qrCodeDrawable = BitmapDrawable(resources, qrCodeBitmap).apply { isFilterBitmap = false }
		binding.certificateDetailQrCode.setImageDrawable(qrCodeDrawable)
	}

	private fun setupCertificateDetails() {
		val recyclerView = binding.certificateDetailDataRecyclerView
		val layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
		recyclerView.layoutManager = layoutManager
		val adapter = CertificateDetailAdapter()
		recyclerView.adapter = adapter

		val name = "${dccHolder.euDGC.person.familyName} ${dccHolder.euDGC.person.givenName}"
		binding.certificateDetailName.text = name
		val dateOfBirth = dccHolder.euDGC.dateOfBirth.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER)
		binding.certificateDetailBirthdate.text = dateOfBirth

		binding.certificateDetailInfo.setText(R.string.verifier_verify_success_info)

		val detailItems = CertificateDetailItemListBuilder(recyclerView.context, dccHolder).buildAll()
		adapter.setItems(detailItems)
	}

	private fun setupStatusInfo() {
		certificatesViewModel.verifiedCertificates.observe(viewLifecycleOwner) { certificates ->
			certificates.find { it.dccHolder == dccHolder }?.let {
				//binding.certificateDetailButtonReverify.showAnimated()
				updateStatusInfo(it.state)
			}
		}

		certificatesViewModel.startVerification(dccHolder)
	}

	private fun updateStatusInfo(verificationState: VerificationResultStatus?) {
		val state = verificationState ?: return

		changeAlpha(state.getQrAlpha())
		setCertificateDetailTextColor(state.getNameDobColor())

		when (state) {
			is VerificationResultStatus.LOADING -> displayLoadingState()
			is VerificationResultStatus.SUCCESS -> displaySuccessState(state)
			is VerificationResultStatus.SIGNATURE_INVALID -> displayInvalidState(state)
			is VerificationResultStatus.ERROR -> displayErrorState(state)
			is VerificationResultStatus.TIMEMISSING -> displayTimeMissingState()
			is VerificationResultStatus.DATAEXPIRED -> displayDataExpiredState()
		}
	}

	private fun displayLoadingState() {
		val context = context ?: return
		showLoadingIndicator(true)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false
		binding.certificateDetailInfoEtValidityGroup.isVisible = false
		binding.certificateDetailInfoNgValidityGroup.isVisible = false
		binding.certificateDetailInfoValidityHeadline.isVisible = false
		binding.certificateDetailErrorCode.isVisible = false
		setInfoBubbleBackgrounds(R.color.greyish, R.color.greyish)

		binding.certificateDetailStatusIcon.visibility = View.VISIBLE
		binding.certificateDetailInfo.visibility = View.VISIBLE
		binding.certificateDetailInfoCircle.visibility = View.VISIBLE
		binding.certificateDetailRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.GONE

		val info = SpannableString(context.getString(R.string.wallet_certificate_verifying))
		if (isForceValidate) {
			showStatusInfoAndDescription(null, info, 0)
			showForceValidation(R.color.grey, 0, 0, info)
		} else {
			showStatusInfoAndDescription(null, info, 0)
		}
	}

	private fun displaySuccessState(state: VerificationResultStatus.SUCCESS) {
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false
		binding.certificateDetailErrorCode.isVisible = false
		setInfoBubbleBackgrounds(R.color.green_valid, R.color.green_valid)

		binding.certificateDetailStatusIcon.visibility = View.INVISIBLE
		binding.certificateDetailInfo.visibility = View.INVISIBLE
		binding.certificateDetailInfoCircle.visibility = View.INVISIBLE
		binding.certificateDetailRegionValidityContainer.visibility = View.VISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.VISIBLE

		val etResult = state.results.first { it.region?.startsWith("ET") == true }
		val ngResult = state.results.first { it.region?.startsWith("NG") == true }

		binding.certificateDetailRegionEtContainer.setBackgroundResource(if (etResult.valid) { R.drawable.bg_certificate_bubble_valid} else { R.drawable.bg_certificate_bubble_invalid})
		binding.certificateDetailRegionNgContainer.setBackgroundResource(if (ngResult.valid) { R.drawable.bg_certificate_bubble_valid} else { R.drawable.bg_certificate_bubble_invalid})

		binding.certificateDetailInfoEtIcon.setImageResource(if (etResult.valid) { R.drawable.ic_check_circle} else { R.drawable.ic_minus_circle})
		binding.certificateDetailInfoNgIcon.setImageResource(if (ngResult.valid) { R.drawable.ic_check_circle} else { R.drawable.ic_minus_circle})

		binding.certificateDetailRegionEtContainer.importantForAccessibility = 1
		binding.certificateDetailRegionEtContainer.contentDescription = if (etResult.valid) getString(R.string.accessibility_valid_text) + getString(R.string.region_type_ET)  else getString(R.string.accessibility_invalid_text) + getString(R.string.region_type_ET)
		binding.certificateDetailRegionNgContainer.importantForAccessibility = 1
		binding.certificateDetailRegionNgContainer.contentDescription = if (ngResult.valid) getString(R.string.accessibility_valid_text) + getString(R.string.region_type_NG) else getString(R.string.accessibility_invalid_text) + getString(R.string.region_type_NG)

		binding.certificateDetailInfoEtValidityGroup.isVisible = etResult.valid
		binding.certificateDetailInfoNgValidityGroup.isVisible = ngResult.valid
		binding.certificateDetailInfoValidityHeadline.isVisible = etResult.valid || ngResult.valid

		val selectedRegion = Region.getRegionFromIdentifier(certificatesViewModel.secureStorage.getSelectedValidationRegion())

		if (selectedRegion != null) {
			if (etResult.valid) {
				binding.certificateDetailInfoEtValidityDateDisclaimer.text = getString(
					R.string.wallet_certificate_validity,
					getString(R.string.region_type_ET_validity),
					getString(selectedRegion.getName())
				)
				binding.certificateDetailInfoEtValidityDate.text =
					getValidityDate(etResult.validUntil, dccHolder.certType)
			}
			if (ngResult.valid) {
				binding.certificateDetailInfoNgValidityDateDisclaimer.text = getString(
					R.string.wallet_certificate_validity,
					getString(R.string.region_type_NG_validity),
					getString(selectedRegion.getName())
				)
				binding.certificateDetailInfoNgValidityDate.text =
					getValidityDate(ngResult.validUntil, dccHolder.certType)
			}
		}
	}

	private fun displayInvalidState(state: VerificationResultStatus.SIGNATURE_INVALID) {
		val context = context ?: return
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false
		binding.certificateDetailInfoEtValidityGroup.isVisible = false
		binding.certificateDetailInfoNgValidityGroup.isVisible = false
		binding.certificateDetailInfoValidityHeadline.isVisible = false

		showStatusInfoAndDescription(null, context.getString(R.string.wallet_error_invalid_signature)
			.makeSubStringBold(context.getString(R.string.wallet_error_invalid_signature_bold)),R.drawable.ic_error)

		val infoBubbleColorId: Int = R.color.red_invalid

		setInfoBubbleBackgrounds(infoBubbleColorId, infoBubbleColorId)

		binding.certificateDetailStatusIcon.visibility = View.VISIBLE
		binding.certificateDetailInfo.visibility = View.VISIBLE
		binding.certificateDetailInfoCircle.visibility = View.VISIBLE
		binding.certificateDetailRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.GONE

		binding.certificateDetailErrorCode.apply {
			isVisible = false
		}
	}

	private fun displayErrorState(state: VerificationResultStatus.ERROR) {
		val context = context ?: return
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false
		binding.certificateDetailInfoEtValidityGroup.isVisible = false
		binding.certificateDetailInfoNgValidityGroup.isVisible = false
		binding.certificateDetailInfoValidityHeadline.isVisible = false
		setInfoBubbleBackgrounds(R.color.red_invalid, R.color.red_invalid)

		binding.certificateDetailInfo.text = context.getString(R.string.wallet_error_invalid_format)
			.makeSubStringBold(context.getString(R.string.wallet_error_invalid_format_bold))
		binding.certificateDetailStatusIcon.setImageResource(R.drawable.ic_error)

		binding.certificateDetailStatusIcon.visibility = View.VISIBLE
		binding.certificateDetailInfo.visibility = View.VISIBLE
		binding.certificateDetailInfoCircle.visibility = View.VISIBLE
		binding.certificateDetailRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.GONE

		binding.certificateDetailErrorCode.apply {
			isVisible = false
		}
	}

	private fun displayTimeMissingState() {
		val context = context ?: return
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false
		binding.certificateDetailInfoEtValidityGroup.isVisible = false
		binding.certificateDetailInfoNgValidityGroup.isVisible = false
		binding.certificateDetailInfoValidityHeadline.isVisible = false
		setInfoBubbleBackgrounds(R.color.orange, R.color.orange)

		binding.certificateDetailInfo.text = context.getString(R.string.wallet_time_missing)
		binding.certificateDetailStatusIcon.setImageResource(R.drawable.ic_info_orange)

		binding.certificateDetailStatusIcon.visibility = View.VISIBLE
		binding.certificateDetailInfo.visibility = View.VISIBLE
		binding.certificateDetailInfoCircle.visibility = View.VISIBLE
		binding.certificateDetailRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.GONE

		binding.certificateDetailErrorCode.apply {
			isVisible = false
		}
	}

	private fun displayDataExpiredState() {
		val context = context ?: return
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false
		binding.certificateDetailInfoEtValidityGroup.isVisible = false
		binding.certificateDetailInfoNgValidityGroup.isVisible = false
		binding.certificateDetailInfoValidityHeadline.isVisible = false
		setInfoBubbleBackgrounds(R.color.orange, R.color.orange)

		binding.certificateDetailInfo.text = context.getString(R.string.wallet_validation_data_expired)
		binding.certificateDetailStatusIcon.setImageResource(R.drawable.ic_no_connection)

		binding.certificateDetailStatusIcon.visibility = View.VISIBLE
		binding.certificateDetailInfo.visibility = View.VISIBLE
		binding.certificateDetailInfoCircle.visibility = View.VISIBLE
		binding.certificateDetailRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.GONE

		binding.certificateDetailErrorCode.apply {
			isVisible = false
		}
	}

	/**
	 * Show or hide the loading indicators and status icons in the QR code and the info bubble
	 */
	private fun showLoadingIndicator(isLoading: Boolean) {
		binding.certificateDetailStatusLoading.isVisible = isLoading
		binding.certificateDetailStatusIcon.isVisible = !isLoading

		binding.certificateDetailQrCodeLoading.isVisible = isLoading
		binding.certificateDetailQrCodeStatusIcon.isVisible = !isLoading
	}

	/**
	 * Change the alpha value of the QR code, validity date and certificate content
	 */
	private fun changeAlpha(alpha: Float) {
		binding.certificateDetailQrCode.alpha = alpha
		binding.certificateDetailDataRecyclerView.alpha = alpha
	}

	/**
	 * Display the formatted validity date of the vaccine or test
	 */
	private fun getValidityDate(validUntil: OffsetDateTime?, certificateType: CertType?): String {
		val formatter = when (certificateType) {
			CertType.TEST -> DEFAULT_DISPLAY_DATE_TIME_FORMATTER
			else -> DEFAULT_DISPLAY_DATE_FORMATTER
		}

		return validUntil?.let { formatter?.format(it) } ?: "-"
	}

	/**
	 * Set the text color of the certificate details (person name and date of birth)
	 */
	private fun setCertificateDetailTextColor(@ColorRes colorId: Int) {
		val textColor = ContextCompat.getColor(requireContext(), colorId)
		binding.certificateDetailName.setTextColor(textColor)
		binding.certificateDetailBirthdate.setTextColor(textColor)
	}

	/**
	 * Set the info bubble backgrounds, depending if a force validation is running or not
	 */
	private fun setInfoBubbleBackgrounds(@ColorRes infoBubbleColorId: Int, @ColorRes infoBubbleValidationColorId: Int) {
		val infoBubbleColor = ContextCompat.getColor(requireContext(), infoBubbleColorId)
		val infoBubbleValidationColor = ContextCompat.getColor(requireContext(), infoBubbleValidationColorId)

		if (isForceValidate) {
			binding.certificateDetailInfo.animateBackgroundTintColor(infoBubbleColor)
			binding.certificateDetailInfoVerificationStatus.animateBackgroundTintColor(infoBubbleValidationColor)
			binding.certificateDetailInfoDescriptionGroup.animateBackgroundTintColor(infoBubbleValidationColor)
		} else {
			val infoBubbleColorTintList = ColorStateList.valueOf(infoBubbleColor)
			binding.certificateDetailInfo.backgroundTintList = infoBubbleColorTintList
			binding.certificateDetailInfoVerificationStatus.backgroundTintList = ColorStateList.valueOf(infoBubbleValidationColor)
			binding.certificateDetailInfoDescriptionGroup.backgroundTintList = infoBubbleColorTintList
		}
	}

	/**
	 * Display the correct QR code background, icons and text when a force validation is running
	 */
	private fun showForceValidation(
		@ColorRes solidValidationColorId: Int,
		@DrawableRes validationIconId: Int,
		@DrawableRes validationIconLargeId: Int,
		info: SpannableString?,
	) {
		binding.certificateDetailQrCodeColor.animateBackgroundTintColor(
			ContextCompat.getColor(
				requireContext(),
				solidValidationColorId
			)
		)
		binding.certificateDetailQrCodeStatusIcon.setImageResource(validationIconLargeId)
		binding.certificateDetailStatusIcon.setImageResource(validationIconId)

		if (!binding.certificateDetailQrCodeStatusGroup.isVisible) binding.certificateDetailQrCodeStatusGroup.showAnimated()

		binding.certificateDetailInfoVerificationStatus.apply {
			text = info
			if (!isVisible) showAnimated()
		}
	}

	/**
	 * Display the verification status info and description
	 */
	private fun showStatusInfoAndDescription(description: SpannableString?, info: SpannableString?, @DrawableRes iconId: Int) {
		binding.certificateDetailInfoDescription.text = description
		binding.certificateDetailInfo.text = info
		binding.certificateDetailStatusIcon.setImageResource(iconId)
	}

	/**
	 * Reset the view after a delay from the force validation verification state to the regular verification state
	 */
	private fun readjustStatusDelayed(
		@ColorRes infoBubbleColorId: Int,
		@DrawableRes statusIconId: Int,
		info: SpannableString?,
	) {
		hideDelayedJob?.cancel()
		hideDelayedJob = viewLifecycleOwner.lifecycleScope.launch {
			delay(STATUS_HIDE_DELAY)
			if (!isActive || !isVisible) return@launch

			val context = binding.root.context

			binding.certificateDetailQrCodeStatusGroup.hideAnimated()
			binding.certificateDetailQrCodeColor.animateBackgroundTintColor(
				ContextCompat.getColor(context, android.R.color.transparent)
			)

			binding.certificateDetailInfo.text = info
			binding.certificateDetailInfoDescriptionGroup.animateBackgroundTintColor(
				ContextCompat.getColor(context, infoBubbleColorId)
			)

			binding.certificateDetailInfoVerificationStatus.hideAnimated()

			binding.certificateDetailStatusIcon.setImageResource(statusIconId)

			//binding.certificateDetailButtonReverify.showAnimated()
			isForceValidate = false
		}
	}

}