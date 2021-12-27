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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import at.gv.brz.wallet.R

class OnboardingSlidePageAdapter(private val fragmentActivity: FragmentActivity?) : FragmentStateAdapter(fragmentActivity!!) {
	override fun createFragment(position: Int): Fragment {
		when (position) {
			0 -> return OnboardingIntroFragment.newInstance()
			1 -> return OnboardingContentFragment.newInstance(
				fragmentActivity?.getString(R.string.wallet_onboarding_store_title),
				fragmentActivity?.getString(R.string.wallet_onboarding_store_header),
				R.drawable.illu_onboarding_privacy,
				fragmentActivity?.getString(R.string.wallet_onboarding_store_text1),
				R.drawable.ic_privacy,
				null,
				0
			)
			2 -> return OnboardingContentFragment.newInstance(
				fragmentActivity?.getString(R.string.wallet_onboarding_show_title),
				fragmentActivity?.getString(R.string.wallet_onboarding_show_header),
				R.drawable.illu_onboarding_covid_certificate,
				fragmentActivity?.getString(R.string.wallet_onboarding_show_text1),
				R.drawable.ic_qr_certificate,
				fragmentActivity?.getString(R.string.wallet_onboarding_show_text2),
				R.drawable.ic_check_mark
			)
			3 -> return OnboardingAgbFragment.newInstance()
		}
		throw IllegalArgumentException("There is no fragment for view pager position $position")
	}

	override fun getItemCount(): Int {
		return 4
	}
}