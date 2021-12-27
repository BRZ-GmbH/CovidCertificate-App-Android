/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 * Copyright (c) 2021 BRZ GmbH <https://www.brz.gv.at>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.onboarding

import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * FragmentStateAdapter for the feature intro pages
 */
class FeatureSlidePageAdapter(val fragmentActivity: FragmentActivity?, private val entries: List<FeatureIntroEntryModel>) : FragmentStateAdapter(fragmentActivity!!) {

	override fun createFragment(position: Int): Fragment {
		try {
			val entry = entries[position]

			return OnboardingContentFragment.newInstance(
				entry.heading,
				entry.title,
				drawableResourceIdForName(entry.foregroundImage),
				entry.textGroups.firstOrNull()?.text,
				drawableResourceIdForName(entry.textGroups.firstOrNull()?.image),
				entry.textGroups.getOrNull(1)?.text,
				drawableResourceIdForName(entry.textGroups.getOrNull(1)?.image),
				entry.textAlignment()
			)
		} catch (e: java.io.IOException) {
			throw IllegalArgumentException("There is no fragment for view pager position $position")
		}
	}

	override fun getItemCount(): Int {
		return entries.size
	}
}

/**
 * Converts a text alignment string to a TextView Textalignment constant
 */
fun FeatureIntroEntryModel.textAlignment(): Int {
	return if (alignment == "center") TextView.TEXT_ALIGNMENT_CENTER else TextView.TEXT_ALIGNMENT_VIEW_START
}

/**
 * Resolves the given resource name to a drawable resource ID if available
 */
fun FeatureSlidePageAdapter.drawableResourceIdForName(name: String?): Int {
	return if (name != null) fragmentActivity?.resources?.getIdentifier(name, "drawable", fragmentActivity.packageName) ?: 0 else 0
}