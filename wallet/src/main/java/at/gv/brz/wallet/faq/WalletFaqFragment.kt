/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.faq

import androidx.fragment.app.activityViewModels
import at.gv.brz.common.faq.FaqFragment
import at.gv.brz.common.util.AssetUtil
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R


class WalletFaqFragment : FaqFragment() {

	companion object {
		fun newInstance(): WalletFaqFragment = WalletFaqFragment()
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	override fun setupFaqProvider() {
		toolbar.setTitle(R.string.wallet_faq_header)
		val languageKey = getString(R.string.language_key)
		AssetUtil.loadDefaultConfig(requireContext())?.let {
			setupFaqList(it.generateFaqItems(languageKey))
		}
		certificatesViewModel.loadConfig()
	}

}