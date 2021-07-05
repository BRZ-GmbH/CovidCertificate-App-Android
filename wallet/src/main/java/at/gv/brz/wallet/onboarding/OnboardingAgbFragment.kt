/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import at.gv.brz.common.R
import at.gv.brz.common.util.UrlUtil
import at.gv.brz.wallet.databinding.FragmentHomeBinding
import at.gv.brz.wallet.databinding.FragmentOnboardingAgbBinding

class OnboardingAgbFragment : Fragment() {

	companion object {
		fun newInstance(): OnboardingAgbFragment {
			return OnboardingAgbFragment()
		}
	}

	private var _binding: FragmentOnboardingAgbBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentOnboardingAgbBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.onboardingContinueButton.setOnClickListener {
			(requireActivity() as OnboardingActivity).continueToNextPage()
		}

		binding.itemAgbLink.setOnClickListener { v ->
			val url = v.context.getString(R.string.wallet_terms_privacy_link)
			UrlUtil.openUrl(v.context, url)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}
