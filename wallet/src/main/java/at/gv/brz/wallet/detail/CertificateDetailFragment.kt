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
import androidx.recyclerview.widget.LinearLayoutManager
import at.gv.brz.brvc.model.BusinessRuleValidationResult
import at.gv.brz.brvc.model.ValidationResult
import at.gv.brz.brvc.model.ValidityTimeFormat
import at.gv.brz.brvc.model.ValidityTimeResult
import at.gv.brz.brvc.model.data.BusinessRuleCertificateType
import at.gv.brz.common.util.setSecureFlagToBlockScreenshots
import at.gv.brz.sdk.models.CertType
import at.gv.brz.sdk.models.DccHolder
import at.gv.brz.sdk.utils.*
import at.gv.brz.common.util.makeSubStringBold
import at.gv.brz.sdk.businessRuleCertificateType
import at.gv.brz.sdk.data.state.*
import at.gv.brz.sdk.models.ValidationProfile
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R
import at.gv.brz.wallet.data.Region
import at.gv.brz.wallet.databinding.FragmentCertificateDetailBinding
import at.gv.brz.wallet.util.*
import java.time.OffsetDateTime

class CertificateDetailFragment : Fragment() {

	companion object {
		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificate: DccHolder): CertificateDetailFragment = CertificateDetailFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificate)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private var _binding: FragmentCertificateDetailBinding? = null
	private val binding get() = _binding!!

	private lateinit var dccHolder: DccHolder

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
		setupCertificateDetails(view)
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

	private fun setupCertificateDetails(view:View) {
		val recyclerView = binding.certificateDetailDataRecyclerView
		val layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
		recyclerView.layoutManager = layoutManager
		val adapter = CertificateDetailAdapter()
		recyclerView.adapter = adapter

		val name = "${dccHolder.euDGC.person.familyName} ${dccHolder.euDGC.person.givenName}"
		binding.certificateDetailName.text = name
		val dateOfBirth = dccHolder.euDGC.dateOfBirth.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER)
		binding.certificateDetailBirthdate.text = dateOfBirth

		binding.certificateDetailInfo.setText(R.string.wallet_verify_success_info)

		if (dccHolder.businessRuleCertificateType() == BusinessRuleCertificateType.VACCINATION_EXEMPTION) {
			binding.certificateDetailToolbar.setTitle(R.string.wallet_certificate_vaccination_exemption)
			view.announceForAccessibility(getString(R.string.wallet_certificate_vaccination_exemption_loaded))
		} else {
			binding.certificateDetailToolbar.setTitle(R.string.wallet_certificate)
			view.announceForAccessibility(getString(R.string.wallet_certificate_loaded))
		}

		if (dccHolder.businessRuleCertificateType() == BusinessRuleCertificateType.VACCINATION_EXEMPTION) {
			binding.certificateDetailNote.setText(R.string.wallet_certificate_vaccination_exemption_detail_note)
		} else {
			binding.certificateDetailNote.setText(R.string.wallet_certificate_detail_note)
		}

		val detailItems = CertificateDetailItemListBuilder(recyclerView.context, dccHolder).buildAll()
		adapter.setItems(detailItems)
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
		binding.certificateDetailExemptionValidityContainer.visibility = View.INVISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.GONE
		binding.certificateDetailInfoExemptionValidityGroup.isVisible = false

		val info = SpannableString(context.getString(R.string.wallet_certificate_verifying))
		showStatusInfoAndDescription(null, info, 0)
	}

	private fun displaySuccessState(state: VerificationResultStatus.SUCCESS) {
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false
		binding.certificateDetailErrorCode.isVisible = false
		setInfoBubbleBackgrounds(R.color.green_valid, R.color.green_valid)

		binding.certificateDetailStatusIcon.visibility = View.INVISIBLE
		binding.certificateDetailInfo.visibility = View.INVISIBLE
		binding.certificateDetailInfoCircle.visibility = View.INVISIBLE

		if (dccHolder.businessRuleCertificateType() == BusinessRuleCertificateType.VACCINATION_EXEMPTION) {
			binding.certificateDetailExemptionValidityContainer.visibility = View.VISIBLE
			if (state.results[ValidationProfile.ENTRY.profileName] is ValidationResult.Valid) {
				binding.certificateDetailExemptionContainer.setBackgroundResource(R.drawable.bg_certificate_bubble_valid)
				binding.certificateDetailInfoVeIcon.setImageResource(R.drawable.ic_check_circle)
				binding.certificateDetailExemptionContainer.contentDescription = getString(R.string.region_type_valid_vaccination_exemption)

				binding.certificateDetailInfoExemptionValidityGroup.isVisible = true
				binding.certificateDetailInfoExemptionValidityDate.text =
					getValidityDate(dccHolder.euDGC.vaccinationExemptions?.first()?.validUntilDate()?.atOffset(OffsetDateTime.now().offset), dccHolder.certType)
			} else {
				binding.certificateDetailExemptionContainer.setBackgroundResource(R.drawable.bg_certificate_bubble_invalid)
				binding.certificateDetailInfoVeIcon.setImageResource(R.drawable.ic_minus_circle)
				binding.certificateDetailExemptionContainer.contentDescription = getString(R.string.region_type_invalid_vaccination_exemption)
				binding.certificateDetailInfoExemptionValidityGroup.isVisible = false
			}
		} else {
			binding.certificateDetailRegionValidityContainer.visibility = View.VISIBLE

			val etResult = state.results[ValidationProfile.ENTRY.profileName]
			val ngResult = state.results[ValidationProfile.NIGHT_CLUB.profileName]

			binding.certificateDetailRegionEtContainer.setBackgroundResource(if (etResult is ValidationResult.Valid) { R.drawable.bg_certificate_bubble_valid} else { R.drawable.bg_certificate_bubble_invalid})
			binding.certificateDetailRegionNgContainer.setBackgroundResource(if (ngResult is ValidationResult.Valid) { R.drawable.bg_certificate_bubble_valid} else { R.drawable.bg_certificate_bubble_invalid})

			binding.certificateDetailInfoEtIcon.setImageResource(if (etResult is ValidationResult.Valid) { R.drawable.ic_check_circle} else { R.drawable.ic_minus_circle})
			binding.certificateDetailInfoNgIcon.setImageResource(if (ngResult is ValidationResult.Valid) { R.drawable.ic_check_circle} else { R.drawable.ic_minus_circle})

			binding.certificateDetailRegionEtContainer.importantForAccessibility = 1
			binding.certificateDetailRegionEtContainer.contentDescription = if (etResult is ValidationResult.Valid) getString(R.string.accessibility_valid_text) + getString(R.string.region_type_ET)  else getString(R.string.accessibility_invalid_text) + getString(R.string.region_type_ET)
			binding.certificateDetailRegionNgContainer.importantForAccessibility = 1
			binding.certificateDetailRegionNgContainer.contentDescription = if (ngResult is ValidationResult.Valid) getString(R.string.accessibility_valid_text) + getString(R.string.region_type_NG) else getString(R.string.accessibility_invalid_text) + getString(R.string.region_type_NG)

			binding.certificateDetailInfoEtValidityGroup.isVisible = etResult is ValidationResult.Valid
			binding.certificateDetailInfoNgValidityGroup.isVisible = ngResult is ValidationResult.Valid
			binding.certificateDetailInfoValidityHeadline.isVisible = etResult is ValidationResult.Valid || ngResult is ValidationResult.Valid

			val selectedRegion = Region.getRegionFromIdentifier(certificatesViewModel.secureStorage.getSelectedValidationRegion())

			if (selectedRegion != null) {
				if (etResult is ValidationResult.Valid) {
					binding.certificateDetailInfoEtValidityDateDisclaimer.text = getString(
						R.string.wallet_certificate_validity,
						getString(R.string.region_type_ET_validity),
						getString(selectedRegion.getName())
					)
					binding.certificateDetailInfoEtValidityDate.text = etResult.result.formattedValidUntil()
				}
				if (ngResult is ValidationResult.Valid) {
					binding.certificateDetailInfoNgValidityDateDisclaimer.text = getString(
						R.string.wallet_certificate_validity,
						getString(R.string.region_type_NG_validity),
						getString(selectedRegion.getName())
					)
					binding.certificateDetailInfoNgValidityDate.text = ngResult.result.formattedValidUntil()
				}
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
		binding.certificateDetailExemptionValidityContainer.visibility = View.INVISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.GONE
		binding.certificateDetailInfoExemptionValidityGroup.isVisible = false

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
		binding.certificateDetailExemptionValidityContainer.visibility = View.INVISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.GONE
		binding.certificateDetailInfoExemptionValidityGroup.isVisible = false

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
		binding.certificateDetailExemptionValidityContainer.visibility = View.INVISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.GONE
		binding.certificateDetailInfoExemptionValidityGroup.isVisible = false

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
		binding.certificateDetailExemptionValidityContainer.visibility = View.INVISIBLE
		binding.certificateDetailValidityHintEt.visibility = View.GONE
		binding.certificateDetailInfoExemptionValidityGroup.isVisible = false

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

		return validUntil?.let { formatter.format(it) } ?: "-"
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


		val infoBubbleColorTintList = ColorStateList.valueOf(infoBubbleColor)
		binding.certificateDetailInfo.backgroundTintList = infoBubbleColorTintList
		binding.certificateDetailInfoVerificationStatus.backgroundTintList = ColorStateList.valueOf(infoBubbleValidationColor)
		binding.certificateDetailInfoDescriptionGroup.backgroundTintList = infoBubbleColorTintList
	}

	/**
	 * Display the verification status info and description
	 */
	private fun showStatusInfoAndDescription(description: SpannableString?, info: SpannableString?, @DrawableRes iconId: Int) {
		binding.certificateDetailInfoDescription.text = description
		binding.certificateDetailInfo.text = info
		binding.certificateDetailStatusIcon.setImageResource(iconId)
	}


}

fun BusinessRuleValidationResult.formattedValidFrom(): String? {
	val validFrom = validFrom.firstOrNull() ?: return null

	when (validFrom.format) {
		ValidityTimeFormat.DATE_TIME -> return DEFAULT_DISPLAY_DATE_TIME_FORMATTER.format(validFrom.time)
		ValidityTimeFormat.DATE -> return DEFAULT_DISPLAY_DATE_FORMATTER.format(validFrom.time)
	}
}

fun ValidityTimeResult.formattedString(): String {
	when (format) {
		ValidityTimeFormat.DATE_TIME -> return DEFAULT_DISPLAY_DATE_TIME_FORMATTER.format(time)
		ValidityTimeFormat.DATE -> return DEFAULT_DISPLAY_DATE_FORMATTER.format(time)
	}
}

fun BusinessRuleValidationResult.formattedValidUntil(): String? {
	return validUntil.firstOrNull()?.formattedString()
}