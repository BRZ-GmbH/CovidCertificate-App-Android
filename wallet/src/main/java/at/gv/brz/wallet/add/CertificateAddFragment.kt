/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.eval.utils.DEFAULT_DISPLAY_DATE_FORMATTER
import at.gv.brz.eval.utils.prettyPrintIsoDateTime
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R
import at.gv.brz.wallet.databinding.FragmentCertificateAddBinding
import at.gv.brz.wallet.detail.CertificateDetailAdapter
import at.gv.brz.wallet.detail.CertificateDetailItemListBuilder

class CertificateAddFragment : Fragment() {

	companion object {
		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificate: DccHolder): CertificateAddFragment = CertificateAddFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificate)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private var _binding: FragmentCertificateAddBinding? = null
	private val binding get() = _binding!!

	private lateinit var dccHolder: DccHolder
	private var isAlreadyAdded = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		dccHolder = (arguments?.getSerializable(ARG_CERTIFICATE) as? DccHolder)
			?: throw IllegalStateException("Certificate detail fragment created without Certificate!")
		isAlreadyAdded = certificatesViewModel.containsCertificate(dccHolder.qrCodeData)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificateAddBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		setupCertificateDetails()

		binding.certificateAddToolbar.setNavigationOnClickListener {
			parentFragmentManager.popBackStack()
		}
		binding.certificateAlreadyExistsInfo.isVisible = isAlreadyAdded
		binding.certificateAddButton.apply {
			if (isAlreadyAdded) {
				text = context.getString(R.string.ok_button)
				setOnClickListener {
					parentFragmentManager.popBackStack()
					parentFragmentManager.popBackStack()
				}
			} else {
				text = context.getString(R.string.wallet_add_certificate)
				setOnClickListener {
					certificatesViewModel.addCertificate(dccHolder.qrCodeData)
					parentFragmentManager.popBackStack()
					parentFragmentManager.popBackStack()
				}
			}
		}
		binding.certificateAddRetry.setOnClickListener {
			parentFragmentManager.popBackStack()
		}

	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupCertificateDetails() {
		val recyclerView = binding.certificateAddDataRecyclerView
		val layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
		recyclerView.layoutManager = layoutManager
		val adapter = CertificateDetailAdapter()
		recyclerView.adapter = adapter

		val name = "${dccHolder.euDGC.person.familyName} ${dccHolder.euDGC.person.givenName}"
		binding.certificateAddName.text = name
		val dateOfBirth = dccHolder.euDGC.dateOfBirth.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER)
		binding.certificateAddBirthdate.text = dateOfBirth

		val detailItems = CertificateDetailItemListBuilder(recyclerView.context, dccHolder, showEnglishVersion = false).buildAll()
		adapter.setItems(detailItems)
	}

}