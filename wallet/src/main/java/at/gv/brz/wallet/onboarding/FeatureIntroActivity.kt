/*
 * Copyright (c) 2021 BRZ GmbH <https://www.brz.gv.at>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.onboarding

import androidx.viewpager2.adapter.FragmentStateAdapter
import at.gv.brz.wallet.BuildConfig
import at.gv.brz.wallet.R
import at.gv.brz.wallet.util.WalletAssetUtil

/**
 * Activity for displaying the new feature intro for an app version
 */
class FeatureIntroActivity : OnboardingActivity() {

	override fun createAdapter(): FragmentStateAdapter {
		val featureIntros = WalletAssetUtil.loadFeatureIntrosForLanguageAndVersion(this, getString(R.string.language_key), BuildConfig.VERSION_NAME)

		return FeatureSlidePageAdapter(this, featureIntros ?: listOf())
	}
}