/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.list

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import at.gv.brz.eval.data.state.VerificationResultStatus
import at.gv.brz.eval.models.CertType
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R
import at.gv.brz.wallet.databinding.ItemCertificateListBinding
import at.gv.brz.wallet.util.getQrAlpha

data class VerifiedCeritificateItem(val verifiedCertificate: CertificatesViewModel.VerifiedCertificate) {

	fun bindView(itemView: View, onCertificateClickListener: ((DccHolder) -> Unit)? = null) {
		val binding = ItemCertificateListBinding.bind(itemView)
		val state = verifiedCertificate.state
		val certificate = verifiedCertificate.dccHolder
		val certType = certificate.certType

		val name = "${certificate.euDGC.person.familyName} ${certificate.euDGC.person.givenName}"
		val qrAlpha = state.getQrAlpha()
		binding.itemCertificateListName.text = name
		binding.itemCertificateListName.alpha = qrAlpha
		binding.itemCertificateListIconQr.alpha = qrAlpha
		binding.itemCertificateListIconStatus.alpha = qrAlpha

		setCertificateType(binding.itemCertificateListType, state, certificate.certType)
		binding.itemCertificateListType.isVisible = certType != null

		var validityAccessibilityString = binding.root.resources.getString(R.string.accessibility_certificate_list_invalid)
		when (state) {
			is VerificationResultStatus.LOADING -> {
				binding.itemCertificateListIconLoadingView.isVisible = true
				binding.itemCertificateListIconStatusGroup.isVisible = true
				binding.itemCertificateListIconStatus.isVisible = false
				binding.itemCertificateListIconStatus.setImageResource(0)
			}
			is VerificationResultStatus.SUCCESS -> {
				binding.itemCertificateListIconLoadingView.isVisible = false
				binding.itemCertificateListIconStatusGroup.isVisible = false
				binding.itemCertificateListIconStatus.isVisible = true
				binding.itemCertificateListIconStatus.setImageResource(R.drawable.ic_info_blue)
				validityAccessibilityString = binding.root.resources.getString(R.string.accessibility_certificate_list_valid)
			}
			is VerificationResultStatus.SIGNATURE_INVALID -> {
				binding.itemCertificateListIconLoadingView.isVisible = false
				binding.itemCertificateListIconStatusGroup.isVisible = true
				binding.itemCertificateListIconStatus.isVisible = true

				binding.itemCertificateListIconStatus.setImageResource(R.drawable.ic_error_grey)
			}
			is VerificationResultStatus.ERROR -> {
				binding.itemCertificateListIconLoadingView.isVisible = false
				binding.itemCertificateListIconStatusGroup.isVisible = true
				binding.itemCertificateListIconStatus.isVisible = true

				binding.itemCertificateListIconStatus.setImageResource(R.drawable.ic_process_error_grey)
			}
			is VerificationResultStatus.TIMEMISSING -> {
				binding.itemCertificateListIconLoadingView.isVisible = false
				binding.itemCertificateListIconStatusGroup.isVisible = false
				binding.itemCertificateListIconStatus.isVisible = true
				binding.itemCertificateListIconStatus.setImageResource(R.drawable.ic_info_blue)
			}
			is VerificationResultStatus.DATAEXPIRED -> {
				binding.itemCertificateListIconLoadingView.isVisible = false
				binding.itemCertificateListIconStatusGroup.isVisible = false
				binding.itemCertificateListIconStatus.isVisible = true
				binding.itemCertificateListIconStatus.setImageResource(R.drawable.ic_no_connection)
			}
		}

		binding.root.contentDescription = "${name}. ${binding.itemCertificateListType.text}. ${validityAccessibilityString}"

		binding.root.setOnClickListener {
			onCertificateClickListener?.invoke(certificate)
		}
	}

	/**
	 * Set the text, text color and background of the certificate type, depending on the verification state and the certificate type
	 */
	private fun setCertificateType(
		view: TextView,
		state: VerificationResultStatus,
		certType: CertType?
	) {
		val backgroundColorId: Int
		val textColorId: Int
		when {
			state.isInvalid() -> {
				backgroundColorId = R.color.greyish
				textColorId = R.color.grey
			}
			certType == CertType.TEST -> {
				backgroundColorId = R.color.test
				textColorId = R.color.test_contrast
			}
			certType == CertType.VACCINATION -> {
				backgroundColorId = R.color.vaccination
				textColorId = R.color.vaccination_contrast
			}
			certType == CertType.RECOVERY -> {
				backgroundColorId = R.color.recovery
				textColorId = R.color.recovery_contrast
			}
			else -> {
				backgroundColorId = R.color.blue
				textColorId = R.color.white
			}
		}

		val context = view.context
		val colorStateList = context.resources.getColorStateList(backgroundColorId, context.theme)
		view.backgroundTintList = colorStateList
		view.setTextColor(ContextCompat.getColor(context, textColorId))

		val typeLabelRes: Int = when (certType) {
			CertType.RECOVERY -> R.string.certificate_reason_recovered
			CertType.TEST -> R.string.certificate_reason_tested
			else -> R.string.certificate_reason_vaccinated
		}
		view.setText(typeLabelRes)
	}
}