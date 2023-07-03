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
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import at.gv.brz.common.util.UrlUtil
import at.gv.brz.wallet.R

class OnboardingContentFragment : Fragment(R.layout.fragment_onboarding_content) {

	companion object {
		private const val ARG_RES_TITLE = "RES_TITLE"
		private const val ARG_RES_SUBTITLE = "RES_SUBTITLE"
		private const val ARG_RES_DESCRIPTION_1 = "RES_DESCRIPTION_1"
		private const val ARG_RES_DESCRIPTION_2 = "RES_DESCRIPTION_2"
		private const val ARG_RES_DESCR_ICON_1 = "ARG_RES_DESCR_ICON_1"
		private const val ARG_RES_DESCR_ICON_2 = "ARG_RES_DESCR_ICON_2"
		private const val ARG_RES_ILLUSTRATION = "RES_ILLUSTRATION"
		private const val ARG_RES_TEXT_ALIGNMENT = "ARG_RES_TEXT_ALIGNMENT"

		fun newInstance(
			title: String?, subtitle: String?,
			@DrawableRes illustration: Int, description1: String?, @DrawableRes iconDescription1: Int,
			description2: String?, @DrawableRes iconDescription2: Int,
			textAlignment: Int = TextView.TEXT_ALIGNMENT_VIEW_START,
		): OnboardingContentFragment {
			val args = Bundle()
			args.putString(ARG_RES_TITLE, title)
			args.putString(ARG_RES_SUBTITLE, subtitle)
			args.putInt(ARG_RES_ILLUSTRATION, illustration)
			args.putInt(ARG_RES_DESCR_ICON_1, iconDescription1)
			args.putString(ARG_RES_DESCRIPTION_1, description1)
			args.putInt(ARG_RES_DESCR_ICON_2, iconDescription2)
			args.putString(ARG_RES_DESCRIPTION_2, description2)
			args.putInt(ARG_RES_TEXT_ALIGNMENT, textAlignment)
			val fragment = OnboardingContentFragment()
			fragment.arguments = args
			return fragment
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val args = requireArguments()
		val title = view.findViewById<TextView>(R.id.onboarding_title)
		title.text = args.getString(ARG_RES_TITLE)
		title.textAlignment = args.getInt(ARG_RES_TEXT_ALIGNMENT)

		val subtitle = view.findViewById<TextView>(R.id.onboarding_subtitle)
		subtitle.text = args.getString(ARG_RES_SUBTITLE)

		(view.findViewById<View>(R.id.onboarding_illustration) as ImageView).setImageResource(args.getInt(ARG_RES_ILLUSTRATION))
		val icon1 = view.findViewById<ImageView>(R.id.onboarding_description_1_icon)
		icon1.setImageResource(args.getInt(ARG_RES_DESCR_ICON_1))

		val description1 = view.findViewById<TextView>(R.id.onboarding_description_1)
		description1.text = args.getString(ARG_RES_DESCRIPTION_1)
		description1.textAlignment = args.getInt(
			ARG_RES_TEXT_ALIGNMENT)

		val icon2 = view.findViewById<ImageView>(R.id.onboarding_description_2_icon)
		args.getInt(ARG_RES_DESCR_ICON_2).let {
			if (it != 0) {
				icon2.setImageResource(it)
			} else {
				icon2.visibility = View.GONE
			}
		}

		val description2 = view.findViewById<TextView>(R.id.onboarding_description_2)
		args.getString(ARG_RES_DESCRIPTION_2).let {
			if (it != null) {
				description2.text = args.getString(ARG_RES_DESCRIPTION_2)
			} else {
				description2.visibility = View.GONE
			}
		}
		description2.textAlignment = args.getInt(ARG_RES_TEXT_ALIGNMENT)
		val continueButton = view.findViewById<Button>(R.id.onboarding_continue_button)
		continueButton.setOnClickListener { v: View? -> (activity as OnboardingActivity?)!!.continueToNextPage() }


	}
}