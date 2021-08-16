/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.homescreen.pager

import android.content.res.ColorStateList
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import at.gv.brz.common.util.getStatusInformationString
import at.gv.brz.common.util.makeBold
import at.gv.brz.common.util.makeSubStringBold
import at.gv.brz.eval.data.EvalErrorCodes
import at.gv.brz.eval.data.state.CheckNationalRulesState
import at.gv.brz.eval.data.state.CheckSignatureState
import at.gv.brz.eval.data.state.VerificationResultStatus
import at.gv.brz.eval.data.state.VerificationState
import at.gv.brz.eval.models.CertType
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.eval.utils.DEFAULT_DISPLAY_DATE_FORMATTER
import at.gv.brz.eval.utils.prettyPrintIsoDateTime
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R
import at.gv.brz.wallet.databinding.FragmentCertificatePagerBinding
import at.gv.brz.wallet.util.QrCode
import at.gv.brz.wallet.util.getNameDobColor
import at.gv.brz.wallet.util.getQrAlpha
import at.gv.brz.wallet.util.getValidationStatusString
import at.gv.brz.wallet.util.isOfflineMode

class CertificatePagerFragment : Fragment() {

	companion object {
		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificate: DccHolder) = CertificatePagerFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificate)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private var _binding: FragmentCertificatePagerBinding? = null
	private val binding get() = _binding!!

	private lateinit var dccHolder: DccHolder

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		dccHolder = arguments?.getSerializable(ARG_CERTIFICATE) as? DccHolder
			?: throw IllegalStateException("Certificate pager fragment created without QrCode!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificatePagerBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		when (dccHolder.certType) {
			CertType.TEST -> binding.certificatePageType.text = SpannableString(context?.getString(R.string.covid_certificate_test_title))
			CertType.VACCINATION -> binding.certificatePageType.text = SpannableString(context?.getString(R.string.covid_certificate_vaccination_title))
			CertType.RECOVERY -> binding.certificatePageType.text = SpannableString(context?.getString(R.string.covid_certificate_recovery_title))
		}

		val qrCodeBitmap = QrCode.renderToBitmap(dccHolder.qrCodeData)
		val qrCodeDrawable = BitmapDrawable(resources, qrCodeBitmap).apply { isFilterBitmap = false }
		binding.certificatePageQrCode.setImageDrawable(qrCodeDrawable)

		val name = "${dccHolder.euDGC.person.familyName} ${dccHolder.euDGC.person.givenName}"
		binding.certificatePageName.text = name
		val dateOfBirth = dccHolder.euDGC.dateOfBirth.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER)
		binding.certificatePageBirthdate.text = dateOfBirth

		setupStatusInfo()

		binding.certificatePageMainGroup.setOnClickListener { certificatesViewModel.onQrCodeClicked(dccHolder) }
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupStatusInfo() {
		certificatesViewModel.verifiedCertificates.observe(viewLifecycleOwner) { certificates ->
			certificates.find { it.dccHolder == dccHolder }?.let {
				updateStatusInfo(it.state)
			}
		}

		certificatesViewModel.startVerification(dccHolder)
	}

	private fun updateStatusInfo(verificationState: VerificationResultStatus?) {
		val state = verificationState ?: return

		when (state) {
			is VerificationResultStatus.LOADING -> displayLoadingState()
			is VerificationResultStatus.SUCCESS -> displaySuccessState(state)
			is VerificationResultStatus.ERROR -> displayErrorState(state)
			is VerificationResultStatus.SIGNATURE_INVALID -> displayInvalidState(state)
			is VerificationResultStatus.TIMEMISSING -> displayTimeMissingState()
			is VerificationResultStatus.DATAEXPIRED -> displayDataExpiredState()
		}

		setCertificateDetailTextColor(state.getNameDobColor())
		binding.certificatePageQrCode.alpha = state.getQrAlpha()
	}

	private fun displayLoadingState() {
		val context = context ?: return
		showLoadingIndicator(true)
		setInfoBubbleBackground(R.color.greyish)
		binding.certificatePageStatusIcon.setImageResource(0)
		binding.certificatePageInfo.text = SpannableString(context.getString(R.string.wallet_certificate_verifying))

		binding.certificatePageStatusIcon.visibility = View.VISIBLE
		binding.certificatePageInfo.visibility = View.VISIBLE
		binding.certificatePageInfoCircle.visibility = View.VISIBLE
		binding.certificatePageRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificatePageValidityHintEt.visibility = View.GONE
	}

	private fun displaySuccessState(state: VerificationResultStatus.SUCCESS) {
		showLoadingIndicator(false)
		setInfoBubbleBackground(R.color.green_light)

		binding.certificatePageStatusIcon.visibility = View.INVISIBLE
		binding.certificatePageInfo.visibility = View.INVISIBLE
		binding.certificatePageInfoCircle.visibility = View.INVISIBLE
		binding.certificatePageRegionValidityContainer.visibility = View.VISIBLE
		binding.certificatePageRegionValidityContainer.clipToOutline = true
		binding.certificatePageValidityHintEt.visibility = View.VISIBLE

		binding.certificatePageInfoEt.setBackgroundResource(if (state.results.first { it.region == "ET" }.valid) { R.color.green_light} else { R.color.red})
		binding.certificatePageInfoNg.setBackgroundResource(if (state.results.first { it.region == "NG" }.valid) { R.color.green_light} else { R.color.red})
	}

	private fun displayInvalidState(state: VerificationResultStatus.SIGNATURE_INVALID) {
		val context = context ?: return
		showLoadingIndicator(false)

		val infoBubbleColorId: Int
		val statusIconId: Int
		when (state.nationalRulesState) {
			is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
				infoBubbleColorId = R.color.blueish
				statusIconId = R.drawable.ic_invalid_grey
			}
			is CheckNationalRulesState.NOT_YET_VALID -> {
				infoBubbleColorId = R.color.blueish
				statusIconId = R.drawable.ic_timelapse
			}
			else -> {
				infoBubbleColorId = R.color.greyish
				statusIconId = R.drawable.ic_error_grey
			}
		}

		binding.certificatePageMainGroup.alpha = 0.25f

		setInfoBubbleBackground(infoBubbleColorId)

		binding.certificatePageStatusIcon.visibility = View.VISIBLE
		binding.certificatePageInfo.visibility = View.VISIBLE
		binding.certificatePageInfoCircle.visibility = View.VISIBLE
		binding.certificatePageValidityHintEt.visibility = View.GONE
		binding.certificatePageRegionValidityContainer.visibility = View.INVISIBLE

		binding.certificatePageStatusIcon.setImageResource(statusIconId)
		binding.certificatePageInfo.text = context.getString(R.string.wallet_error_invalid_signature)
			.makeSubStringBold(context.getString(R.string.wallet_error_invalid_signature_bold))
	}

	private fun displayErrorState(state: VerificationResultStatus.ERROR) {
		val context = context ?: return
		showLoadingIndicator(false)
		setInfoBubbleBackground(R.color.red)

		binding.certificatePageStatusIcon.visibility = View.VISIBLE
		binding.certificatePageInfo.visibility = View.VISIBLE
		binding.certificatePageInfoCircle.visibility = View.VISIBLE
		binding.certificatePageRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificatePageValidityHintEt.visibility = View.GONE

		binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_error)
		binding.certificatePageInfo.text = context.getString(R.string.wallet_error_invalid_format)
			.makeSubStringBold(context.getString(R.string.wallet_error_invalid_format_bold))
	}

	private fun displayTimeMissingState() {
		val context = context ?: return
		showLoadingIndicator(false)
		setInfoBubbleBackground(R.color.orange)

		binding.certificatePageStatusIcon.visibility = View.VISIBLE
		binding.certificatePageInfo.visibility = View.VISIBLE
		binding.certificatePageInfoCircle.visibility = View.VISIBLE
		binding.certificatePageRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificatePageValidityHintEt.visibility = View.GONE

		binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_info_orange)
		binding.certificatePageInfo.text = context.getString(R.string.wallet_time_missing)
	}

	private fun displayDataExpiredState() {
		val context = context ?: return
		showLoadingIndicator(false)
		setInfoBubbleBackground(R.color.orange)

		binding.certificatePageStatusIcon.visibility = View.VISIBLE
		binding.certificatePageInfo.visibility = View.VISIBLE
		binding.certificatePageInfoCircle.visibility = View.VISIBLE
		binding.certificatePageRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificatePageValidityHintEt.visibility = View.GONE

		binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_no_connection)
		binding.certificatePageInfo.text = context.getString(R.string.wallet_validation_data_expired)
	}

	private fun showLoadingIndicator(isLoading: Boolean) {
		binding.certificatePageStatusLoading.isVisible = isLoading
		binding.certificatePageStatusIcon.isVisible = !isLoading
	}

	private fun setInfoBubbleBackground(@ColorRes infoBubbleColorId: Int) {
		val infoBubbleColor = ContextCompat.getColor(requireContext(), infoBubbleColorId)
		binding.certificatePageInfo.backgroundTintList = ColorStateList.valueOf(infoBubbleColor)
	}

	private fun setCertificateDetailTextColor(@ColorRes colorId: Int) {
		val textColor = ContextCompat.getColor(requireContext(), colorId)
		binding.certificatePageName.setTextColor(textColor)
		binding.certificatePageBirthdate.setTextColor(textColor)
	}

}
