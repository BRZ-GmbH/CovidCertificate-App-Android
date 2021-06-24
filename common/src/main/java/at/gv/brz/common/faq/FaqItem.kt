/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.common.faq

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import at.gv.brz.common.R
import at.gv.brz.common.faq.model.Header
import at.gv.brz.common.faq.model.Question

sealed class FaqItem {
	abstract fun bindView(view: View, onItemClickListener: (() -> Unit)? = null)
}

data class HeaderItem(val header: Header) : FaqItem() {
	companion object {
		val layoutResource = R.layout.item_faq_header
	}

	override fun bindView(view: View, onItemClickListener: (() -> Unit)?) {
		view.findViewById<TextView>(R.id.item_faq_header_title).text = header.title
		view.findViewById<TextView>(R.id.item_faq_header_text).apply {
			text = header.subtitle
			isVisible = header.subtitle != null
		}
		val drawableId = header.iconName?.let { iconName ->
			view.context.resources.getIdentifier(iconName, "drawable", view.context.packageName)
		} ?: 0

		view.findViewById<ImageView>(R.id.item_faq_header_illu).apply {
			setImageResource(drawableId)
			isVisible = drawableId != 0
		}
	}
}

data class QuestionItem(
	val question: Question,
	val onLinkClickListener: OnUrlClickListener? = null,
) : FaqItem() {
	companion object {
		val layoutResource = R.layout.item_faq_question
	}

	override fun bindView(view: View, onItemClickListener: (() -> Unit)?) {
		view.setOnClickListener {
			question.isSelected = !question.isSelected
			view.doOnPreDraw { onItemClickListener?.invoke() }
		}
		view.findViewById<TextView>(R.id.item_faq_question_title).text = question.question
		view.findViewById<TextView>(R.id.item_faq_question_answer).apply {
			text = question.answer
			isVisible = question.isSelected
		}
		val linkGroup = view.findViewById<View>(R.id.item_faq_question_link)
		val linkLabel = view.findViewById<TextView>(R.id.item_faq_question_link_label)
		val hasLink = !question.linkTitle.isNullOrEmpty() && !question.linkUrl.isNullOrEmpty()
		(hasLink && question.isSelected)?.let { visible ->
			linkLabel.isVisible = visible
			linkGroup.isVisible = visible
		}
		if (hasLink) {
			linkLabel.text = question.linkTitle
			linkGroup.setOnClickListener { onLinkClickListener?.onLinkClicked(question.linkUrl!!) }
		} else {
			linkGroup.setOnClickListener(null)
		}

		view.findViewById<ImageView>(R.id.item_faq_question_chevron)
			.setImageResource(if (question.isSelected) R.drawable.ic_arrow_contract else R.drawable.ic_arrow_expand)
	}
}
