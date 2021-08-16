/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.debug

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import at.gv.brz.eval.data.state.VerificationState
import at.gv.brz.eval.models.CertType
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R
import at.gv.brz.wallet.util.getQrAlpha

data class DebugCertificateItem(val verifiedCertificate: CertificatesViewModel.VerifiedCertificate) {

	fun bindView(
		itemView: View,
		onShareClickListener: ((DccHolder) -> Unit),
	) {
		val context = itemView.context
		val certificate = verifiedCertificate.dccHolder
		val state = verifiedCertificate.state

		var typeBackgroundColor = R.color.blue
		var typeTextColor = R.color.white
		var typeLabelRes = R.string.certificate_reason_vaccinated
		when (certificate.certType) {
			CertType.RECOVERY -> {
				typeLabelRes = R.string.certificate_reason_recovered
			}
			CertType.TEST -> {
				typeLabelRes = R.string.certificate_reason_tested
				typeBackgroundColor = R.color.blueish
				typeTextColor = R.color.blue
			}
		}

		if (state.isInvalid()) {
			typeBackgroundColor = R.color.greyish
			typeTextColor = R.color.grey
		}

		// Name
		val name = "${certificate.euDGC.person.familyName} ${certificate.euDGC.person.givenName}"
		val qrAlpha = state.getQrAlpha()
		itemView.findViewById<TextView>(R.id.item_certificate_list_name).apply {
			text = name
			alpha = qrAlpha
		}

		// Type
		itemView.findViewById<TextView>(R.id.item_certificate_list_type).apply {
			backgroundTintList = context.resources.getColorStateList(typeBackgroundColor, context.theme)
			setTextColor(ContextCompat.getColor(context, typeTextColor))
			setText(typeLabelRes)
			isVisible = certificate.certType != null
		}

		// Buttons
		itemView.findViewById<Button>(R.id.button_share_string).setOnClickListener {
			onShareClickListener.invoke(certificate)
		}

	}
}