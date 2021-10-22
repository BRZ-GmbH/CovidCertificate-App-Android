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

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import at.gv.brz.common.util.LocaleUtil
import at.gv.brz.wallet.R
import java.util.*

sealed class CertificateDetailItem() {
	abstract fun bindView(view: View)
}

data class TitleItem(@StringRes val titleResource: Int, val showEnglishVersionForLabels: Boolean) : CertificateDetailItem() {
	companion object {
		const val layoutResource = R.layout.item_detail_title
	}

	override fun bindView(view: View) {
		view.findViewById<TextView>(R.id.item_title).setText(titleResource)
		val englishLabel = view.findViewById<TextView>(R.id.item_title_english)
		englishLabel.isVisible = showEnglishVersionForLabels
		englishLabel.text = getEnglishTranslation(view.context, titleResource)
	}
}

data class ValueItem(@StringRes val labelResource: Int, val value: String, val showEnglishVersionForLabels: Boolean) :
	CertificateDetailItem() {
	companion object {
		const val layoutResource = R.layout.item_detail_value
	}

	override fun bindView(view: View) {
		view.findViewById<TextView>(R.id.item_value_label).setText(labelResource)
		view.findViewById<TextView>(R.id.item_value_value).text = value
		val englishLabel = view.findViewById<TextView>(R.id.item_value_label_english)
		englishLabel.isVisible = showEnglishVersionForLabels
		englishLabel.text = getEnglishTranslation(view.context, labelResource)

		//Accessibilty issue concerning 1/2 2/2 vaccines not being read correctly
		if (labelResource === R.string.wallet_certificate_impfdosis_title) {
			var first = value.get(1)
			var second = value.get(3)
			if (showEnglishVersionForLabels) {
				view.contentDescription = view.context.getString(labelResource) + ", " + first + " von " + second
			} else {
				view.contentDescription = view.context.getString(labelResource) + ", " + first + " of " + second
			}
		} else {
			view.contentDescription = view.context.getString(labelResource) + ", " + value
		}
	}
}

data class ValueItemWithoutLabel(val value: String, val isGrey: Boolean = false) : CertificateDetailItem() {
	companion object {
		const val layoutResource = R.layout.item_detail_value_without_label
	}

	override fun bindView(view: View) {
		val valueText = view.findViewById<TextView>(R.id.item_value_text)
		if (isGrey) {
			valueText.setTextColor(view.context.getColor(R.color.grey))
		} else {
			valueText.setTextColor(view.context.getColor(R.color.black))
		}
		valueText.text = value
	}
}

object DividerItem : CertificateDetailItem() {
	const val layoutResource = R.layout.item_detail_divider
	override fun bindView(view: View) {}
}

fun getEnglishTranslation(context: Context, res: Int): String {
	val config = Configuration(context.resources.configuration)
	config.setLocale(Locale.ENGLISH)
	return context.createConfigurationContext(config).getText(res).toString()
}
