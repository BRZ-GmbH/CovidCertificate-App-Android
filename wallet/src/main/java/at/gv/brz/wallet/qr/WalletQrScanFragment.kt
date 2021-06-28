/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.qr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import at.gv.brz.common.qr.QrScanFragment
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.wallet.R
import at.gv.brz.wallet.add.CertificateAddFragment
import at.gv.brz.wallet.databinding.FragmentQrScanBinding
import at.gv.brz.wallet.howto.HowToScanFragment


class WalletQrScanFragment : QrScanFragment() {

	companion object {
		val TAG = WalletQrScanFragment::class.java.canonicalName

		fun newInstance(): WalletQrScanFragment {
			return WalletQrScanFragment()
		}
	}

	private var _binding: FragmentQrScanBinding? = null
	private val binding get() = _binding!!

	override val viewFinderErrorColor: Int = R.color.red_error_qr_wallet
	override val viewFinderColor: Int = R.color.green_dark
	override val torchOnDrawable: Int = R.drawable.ic_light_on
	override val torchOffDrawable: Int = R.drawable.ic_light_off_blue

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)

		_binding = FragmentQrScanBinding.inflate(inflater, container, false)

		toolbar = binding.fragmentQrScannerToolbar
		barcodeScanner = binding.barcodeScanner
		flashButton = binding.fragmentQrScannerFlashButton
		errorView = binding.fragmentQrScannerErrorView

		invalidCodeText = binding.qrCodeScannerInvalidCodeText
		viewFinderTopLeftIndicator = binding.qrCodeScannerTopLeftIndicator
		viewFinderTopRightIndicator = binding.qrCodeScannerTopRightIndicator
		viewFinderBottomLeftIndicator = binding.qrCodeScannerBottomLeftIndicator
		viewFinderBottomRightIndicator = binding.qrCodeScannerBottomRightIndicator

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.qrCodeScannerButtonHow.setOnClickListener { showHowToScanFragment() }
	}

	override fun onDecodeSuccess(dccHolder: DccHolder) = showCertificationAddFragment(dccHolder)

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun showCertificationAddFragment(dccHolder: DccHolder) {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, CertificateAddFragment.newInstance(dccHolder))
			.addToBackStack(CertificateAddFragment::class.java.canonicalName)
			.commit()
	}

	private fun showHowToScanFragment() {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, HowToScanFragment.newInstance())
			.addToBackStack(HowToScanFragment::class.java.canonicalName)
			.commit()
	}

}