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
import at.gv.brz.common.util.makeBold
import at.gv.brz.eval.data.state.CheckNationalRulesState
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

	private fun updateStatusInfo(verificationState: VerificationState?) {
		val state = verificationState ?: return

		when (state) {
			is VerificationState.LOADING -> displayLoadingState()
			is VerificationState.SUCCESS -> displaySuccessState()
			is VerificationState.INVALID -> displayInvalidState(state)
			is VerificationState.ERROR -> displayErrorState(state)
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
	}

	private fun displaySuccessState() {
		val context = context ?: return
		showLoadingIndicator(false)
		setInfoBubbleBackground(R.color.greenish)
		binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_info_blue)
		binding.certificatePageInfo.text = SpannableString(context.getString(R.string.verifier_verify_success_info))

	}

	private fun displayInvalidState(state: VerificationState.INVALID) {
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

		setInfoBubbleBackground(infoBubbleColorId)
		binding.certificatePageStatusIcon.setImageResource(statusIconId)
		binding.certificatePageInfo.text = state.getValidationStatusString(context)
	}

	private fun displayErrorState(state: VerificationState.ERROR) {
		val context = context ?: return
		showLoadingIndicator(false)
		setInfoBubbleBackground(R.color.greyish)

		val statusIconId = if (state.isOfflineMode()) R.drawable.ic_offline else R.drawable.ic_process_error_grey
		binding.certificatePageStatusIcon.setImageResource(statusIconId)

		binding.certificatePageInfo.text = if (state.isOfflineMode()) {
			context.getString(R.string.wallet_homescreen_offline).makeBold()
		} else {
			SpannableString(context.getString(R.string.wallet_homescreen_network_error))
		}
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