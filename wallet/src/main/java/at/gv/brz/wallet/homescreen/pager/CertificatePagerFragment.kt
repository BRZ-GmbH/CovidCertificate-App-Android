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
import android.view.ViewTreeObserver
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import at.gv.brz.brvc.model.ValidationResult
import at.gv.brz.brvc.model.data.BusinessRuleCertificateType
import at.gv.brz.common.util.makeSubStringBold
import at.gv.brz.sdk.businessRuleCertificateType
import at.gv.brz.sdk.data.state.VerificationResultStatus
import at.gv.brz.sdk.models.DccHolder
import at.gv.brz.sdk.models.ValidationProfile
import at.gv.brz.sdk.utils.DEFAULT_DISPLAY_DATE_FORMATTER
import at.gv.brz.sdk.utils.prettyPrintIsoDateTime
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R
import at.gv.brz.wallet.databinding.FragmentCertificatePagerBinding
import at.gv.brz.wallet.util.QrCode
import at.gv.brz.wallet.util.getNameDobColor
import at.gv.brz.wallet.util.getQrAlpha

class CertificatePagerFragment : Fragment() {

	companion object {
		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificate: DccHolder) = CertificatePagerFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificate)
		}

		var maximumQRHeight: Int? = null
		var maximumQRWidth: Int? = null
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private var _binding: FragmentCertificatePagerBinding? = null
	private val binding get() = _binding!!

	private lateinit var dccHolder: DccHolder

	private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

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
		val qrCodeBitmap = QrCode.renderToBitmap(dccHolder.qrCodeData)
		val qrCodeDrawable = BitmapDrawable(resources, qrCodeBitmap).apply { isFilterBitmap = false }
		binding.certificatePageQrCode.setImageDrawable(qrCodeDrawable)

		val name = "${dccHolder.euDGC.person.familyName} ${dccHolder.euDGC.person.givenName}"
		binding.certificatePageName.text = name
		val dateOfBirth = dccHolder.euDGC.dateOfBirth.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER)
		binding.certificatePageBirthdate.text = dateOfBirth

		if (dccHolder.businessRuleCertificateType() == BusinessRuleCertificateType.VACCINATION_EXEMPTION) {
			binding.certificatePageTitle.setText(R.string.wallet_certificate_vaccination_exemption)
		} else {
			binding.certificatePageTitle.setText(R.string.wallet_certificate)
		}

		setupStatusInfo()

		binding.certificatePageMainGroup.setOnClickListener { certificatesViewModel.onQrCodeClicked(dccHolder) }
		binding.certificateContentScrollviewLayout.setOnClickListener { certificatesViewModel.onQrCodeClicked(dccHolder) }

		if (maximumQRHeight != null) {
			binding.certificatePageQrCode.updateLayoutParams {
				this.height = CertificatePagerFragment.maximumQRHeight!!
				this.width = CertificatePagerFragment.maximumQRHeight!!
			}
		} else if (maximumQRWidth != null) {
			binding.certificatePageQrCode.updateLayoutParams {
				this.width = CertificatePagerFragment.maximumQRWidth!!
				this.height = CertificatePagerFragment.maximumQRWidth!!
			}
		} else {
			globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {

				val maximumHeight = binding.certificateContentScrollview.measuredHeight - resources.getDimensionPixelSize(R.dimen.spacing_large)
				val qrHeight = binding.certificatePageQrCode.measuredHeight
				if (qrHeight > maximumHeight) {
					CertificatePagerFragment.maximumQRHeight = maximumHeight
					binding.certificatePageQrCode.updateLayoutParams {
						this.height = maximumHeight
						this.width = maximumHeight
					}
				} else {
					binding.certificatePageQrCode.updateLayoutParams {
						CertificatePagerFragment.maximumQRWidth = binding.certificateContentScrollview.measuredWidth - resources.getDimensionPixelSize(R.dimen.certificate_details_side_padding) * 2
						this.width = binding.certificateContentScrollview.measuredWidth - resources.getDimensionPixelSize(R.dimen.certificate_details_side_padding) * 2
						this.height = binding.certificateContentScrollview.measuredWidth - resources.getDimensionPixelSize(R.dimen.certificate_details_side_padding) * 2
					}
				}
				if (globalLayoutListener != null) {
					binding.certificatePageMainGroup.viewTreeObserver.removeOnGlobalLayoutListener(
						globalLayoutListener
					)
					globalLayoutListener = null
				}
			}
			binding.certificatePageMainGroup.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		if (globalLayoutListener != null) {
			binding.certificatePageMainGroup.viewTreeObserver.removeOnGlobalLayoutListener(
				globalLayoutListener
			)
			globalLayoutListener = null
		}
		_binding = null
	}

	private fun setupStatusInfo() {
		certificatesViewModel.verifiedCertificates.observe(viewLifecycleOwner) { certificates ->
			certificates.find { it.dccHolder == dccHolder }?.let {
				updateStatusInfo(it.state)
			}
		}
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

		binding.certificatePageInfo.updateLayoutParams {
			(this as? ViewGroup.MarginLayoutParams)?.topMargin = resources.getDimensionPixelSize(R.dimen.home_certificate_info_top_padding_without_validity_hint)
		}
		binding.certificatePageStatusIcon.visibility = View.VISIBLE
		binding.certificatePageInfo.visibility = View.VISIBLE
		binding.certificatePageInfoCircle.visibility = View.VISIBLE
		binding.certificatePageRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificatePageExemptionValidityContainer.visibility = View.INVISIBLE
		binding.certificatePageValidityHintEt.visibility = View.GONE
	}

	private fun displaySuccessState(state: VerificationResultStatus.SUCCESS) {
		showLoadingIndicator(false)
		setInfoBubbleBackground(R.color.green_valid)

		binding.certificatePageStatusIcon.visibility = View.INVISIBLE
		binding.certificatePageInfo.visibility = View.INVISIBLE
		binding.certificatePageInfoCircle.visibility = View.INVISIBLE
		binding.certificatePageValidityHintEt.visibility = View.VISIBLE

		binding.certificatePageInfo.updateLayoutParams {
			(this as? ViewGroup.MarginLayoutParams)?.topMargin = resources.getDimensionPixelSize(R.dimen.home_certificate_info_top_padding_with_validity_hint)
		}

		if (dccHolder.businessRuleCertificateType() == BusinessRuleCertificateType.VACCINATION_EXEMPTION) {
			binding.certificatePageExemptionValidityContainer.visibility = View.VISIBLE
			if (state.results[ValidationProfile.ENTRY.profileName] is ValidationResult.Valid) {
				binding.certificatePageExemptionContainer.setBackgroundResource(R.drawable.bg_certificate_bubble_valid)
				binding.certificatePageInfoVeIcon.setImageResource(R.drawable.ic_check_circle)
				binding.certificatePageExemptionContainer.contentDescription = getString(R.string.region_type_valid_vaccination_exemption)
			} else {
				binding.certificatePageExemptionContainer.setBackgroundResource(R.drawable.bg_certificate_bubble_invalid)
				binding.certificatePageInfoVeIcon.setImageResource(R.drawable.ic_minus_circle)
				binding.certificatePageExemptionContainer.contentDescription = getString(R.string.region_type_invalid_vaccination_exemption)
			}
		} else {
			binding.certificatePageRegionValidityContainer.visibility = View.VISIBLE

			binding.certificatePageRegionEtContainer.setBackgroundResource(
				if (state.results[ValidationProfile.ENTRY.profileName] is ValidationResult.Valid) {
					R.drawable.bg_certificate_bubble_valid
				} else {
					R.drawable.bg_certificate_bubble_invalid
				}
			)
			binding.certificatePageRegionNgContainer.setBackgroundResource(
				if (state.results[ValidationProfile.NIGHT_CLUB.profileName] is ValidationResult.Valid) {
					R.drawable.bg_certificate_bubble_valid
				} else {
					R.drawable.bg_certificate_bubble_invalid
				}
			)

			binding.certificatePageInfoEtIcon.setImageResource(
				if (state.results[ValidationProfile.ENTRY.profileName] is ValidationResult.Valid) {
					R.drawable.ic_check_circle
				} else {
					R.drawable.ic_minus_circle
				}
			)
			binding.certificatePageInfoNgIcon.setImageResource(
				if (state.results[ValidationProfile.NIGHT_CLUB.profileName] is ValidationResult.Valid) {
					R.drawable.ic_check_circle
				} else {
					R.drawable.ic_minus_circle
				}
			)

			binding.certificatePageRegionEtContainer.contentDescription =
				if (state.results[ValidationProfile.ENTRY.profileName] is ValidationResult.Valid) getString(R.string.accessibility_valid_text) + getString(
					R.string.region_type_ET
				) else getString(R.string.accessibility_invalid_text) + getString(R.string.region_type_ET)
			binding.certificatePageRegionNgContainer.contentDescription =
				if (state.results[ValidationProfile.NIGHT_CLUB.profileName] is ValidationResult.Valid) getString(R.string.accessibility_valid_text) + getString(
					R.string.region_type_NG
				) else getString(R.string.accessibility_invalid_text) + getString(R.string.region_type_NG)
		}
	}

	private fun displayInvalidState(state: VerificationResultStatus.SIGNATURE_INVALID) {
		val context = context ?: return
		showLoadingIndicator(false)

		val infoBubbleColorId: Int = R.color.red_invalid
		val statusIconId: Int = R.drawable.ic_error
		setInfoBubbleBackground(infoBubbleColorId)

		binding.certificatePageStatusIcon.visibility = View.VISIBLE
		binding.certificatePageInfo.visibility = View.VISIBLE
		binding.certificatePageInfoCircle.visibility = View.VISIBLE
		binding.certificatePageInfo.updateLayoutParams {
			(this as? ViewGroup.MarginLayoutParams)?.topMargin = resources.getDimensionPixelSize(R.dimen.home_certificate_info_top_padding_without_validity_hint)
		}
		binding.certificatePageValidityHintEt.visibility = View.GONE
		binding.certificatePageRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificatePageExemptionValidityContainer.visibility = View.INVISIBLE

		binding.certificatePageStatusIcon.setImageResource(statusIconId)
		binding.certificatePageInfo.text = context.getString(R.string.wallet_error_invalid_signature)
			.makeSubStringBold(context.getString(R.string.wallet_error_invalid_signature_bold))
	}

	private fun displayErrorState(state: VerificationResultStatus.ERROR) {
		val context = context ?: return
		showLoadingIndicator(false)
		setInfoBubbleBackground(R.color.red_invalid)

		binding.certificatePageStatusIcon.visibility = View.VISIBLE
		binding.certificatePageInfo.visibility = View.VISIBLE
		binding.certificatePageInfoCircle.visibility = View.VISIBLE
		binding.certificatePageInfo.updateLayoutParams {
			(this as? ViewGroup.MarginLayoutParams)?.topMargin = resources.getDimensionPixelSize(R.dimen.home_certificate_info_top_padding_without_validity_hint)
		}
		binding.certificatePageRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificatePageExemptionValidityContainer.visibility = View.INVISIBLE
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
		binding.certificatePageInfo.updateLayoutParams {
			(this as? ViewGroup.MarginLayoutParams)?.topMargin = resources.getDimensionPixelSize(R.dimen.home_certificate_info_top_padding_without_validity_hint)
		}
		binding.certificatePageRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificatePageExemptionValidityContainer.visibility = View.INVISIBLE
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
		binding.certificatePageInfo.updateLayoutParams {
			(this as? ViewGroup.MarginLayoutParams)?.topMargin = resources.getDimensionPixelSize(R.dimen.home_certificate_info_top_padding_without_validity_hint)
		}
		binding.certificatePageRegionValidityContainer.visibility = View.INVISIBLE
		binding.certificatePageExemptionValidityContainer.visibility = View.INVISIBLE
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