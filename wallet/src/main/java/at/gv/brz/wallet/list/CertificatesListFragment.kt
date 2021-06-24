/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import at.gv.brz.common.views.hideAnimated
import at.gv.brz.wallet.CertificatesViewModel
import at.gv.brz.wallet.R
import at.gv.brz.wallet.databinding.FragmentCertificatesListBinding
import at.gv.brz.wallet.detail.CertificateDetailFragment

class CertificatesListFragment : Fragment() {

	companion object {
		fun newInstance(): CertificatesListFragment = CertificatesListFragment()
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private  var _binding: FragmentCertificatesListBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificatesListBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.certificatesOverviewToolbar.setNavigationOnClickListener { v: View? ->
			parentFragmentManager.popBackStack()
		}
		setupRecyclerView()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupRecyclerView() {
		val recyclerView = binding.certificatesOverviewRecyclerView
		recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
		val itemTouchHelper = CertificatesListTouchHelper()
		itemTouchHelper.attachToRecyclerView(recyclerView)
		val adapter = CertificatesListAdapter({ certificate ->
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, CertificateDetailFragment.newInstance(certificate))
				.addToBackStack(CertificateDetailFragment::class.java.canonicalName)
				.commit()
		}, { from, to ->
			certificatesViewModel.moveCertificate(from, to)
		}, { viewHolder ->
			itemTouchHelper.startDrag(viewHolder)
		})
		recyclerView.adapter = adapter

		binding.certificatesOverviewLoadingGroup.isVisible = true

		certificatesViewModel.verifiedCertificates.observe(viewLifecycleOwner) { verifiedCertificates ->
			if (verifiedCertificates.isEmpty()) {
				parentFragmentManager.popBackStack()
			}
			binding.certificatesOverviewLoadingGroup.hideAnimated()
			adapter.setItems(verifiedCertificates.map { VerifiedCeritificateItem(it) })
		}

		certificatesViewModel.loadCertificates()
	}

}