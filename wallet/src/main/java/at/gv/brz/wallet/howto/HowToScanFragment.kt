/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.howto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import at.gv.brz.common.R
import at.gv.brz.common.databinding.ItemFaqQuestionBinding
import at.gv.brz.common.util.UrlUtil
import at.gv.brz.wallet.databinding.FragmentHowToScanBinding

class HowToScanFragment : Fragment() {

	companion object {
		fun newInstance(): HowToScanFragment = HowToScanFragment()
	}

	private var _binding: FragmentHowToScanBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentHowToScanBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.howToScanToolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
		setupExpandableItem(
			binding.howToScanQuestionBubble,
			R.string.wallet_scanner_howitworks_question1,
			R.string.wallet_scanner_howitworks_answer1
		)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupExpandableItem(
		view: ItemFaqQuestionBinding,
		@StringRes question: Int,
		@StringRes answer: Int,
		isSelected: Boolean = false
	) {
		view.root.setOnClickListener {
			setupExpandableItem(view, question, answer, !isSelected)
			view.root.doOnLayout {
				binding.howToScanScrollView.smoothScrollTo(0, view.root.top)
			}
		}
		view.itemFaqQuestionTitle.setText(question)
		view.itemFaqQuestionAnswer.apply {
			setText(answer)
			isVisible = !isSelected
		}

		view.itemFaqQuestionLink.isVisible = !isSelected
		view.itemFaqQuestionLink.setOnClickListener {
			val url = requireContext().getString(R.string.wallet_scanner_howitworks_external_link)
			UrlUtil.openUrl(requireContext(), url)
		}

		view.itemFaqQuestionChevron.setImageResource(if (!isSelected) R.drawable.ic_arrow_contract else R.drawable.ic_arrow_expand)
	}
}