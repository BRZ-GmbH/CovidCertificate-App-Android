/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.html

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import at.gv.brz.common.util.UrlUtil
import at.gv.brz.common.views.hideAnimated
import at.gv.brz.common.html.BuildInfo
import at.gv.brz.wallet.databinding.FragmentHtmlBinding

class HtmlFragment : Fragment() {

	companion object {
		private const val ARG_BASE_URL = "ARG_BASE_URL"
		private const val ARG_BUILD_INFO = "ARG_BUILD_INFO"
		private const val ARG_DATA = "ARG_DATA"
		private const val ARG_TITLE = "ARG_TITLE"
		private const val ARG_FRAGMENT_LAYOUT_ID = "ARG_FRAGMENT_LAYOUT_ID"
		fun newInstance(titleRes: Int, buildInfo: BuildInfo, baseUrl: String, data: String?, fragmentLayoutId: Int): HtmlFragment {
			val args = Bundle()
			args.putString(ARG_BASE_URL, baseUrl)
			args.putSerializable(ARG_BUILD_INFO, buildInfo)
			args.putString(ARG_DATA, data)
			args.putInt(ARG_TITLE, titleRes)
			args.putInt(ARG_FRAGMENT_LAYOUT_ID, fragmentLayoutId)
			val fragment = HtmlFragment()
			fragment.arguments = args
			return fragment
		}
	}

	private var _binding: FragmentHtmlBinding? = null
	private val binding get() = _binding!!

	private lateinit var baseUrl: String
	private var data: String? = null
	private var buildInfo: BuildInfo? = null

	@StringRes
	private var titleRes = 0

	@IdRes
	private var fragmentLayoutId = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		requireArguments().apply {
			baseUrl = getString(ARG_BASE_URL) ?: throw IllegalStateException("No baseUrl specified for HtmlFragment")
			buildInfo = getSerializable(ARG_BUILD_INFO) as? BuildInfo?
			data = getString(ARG_DATA)
			titleRes = getInt(ARG_TITLE)
			fragmentLayoutId = getInt(ARG_FRAGMENT_LAYOUT_ID)
		}

	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentHtmlBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val toolbar = binding.htmlToolbar
		toolbar.setTitle(titleRes)
		toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		val web = binding.htmlWebview
		val loadingSpinner = binding.loadingSpinner

		web.webViewClient = object : WebViewClient() {
			override fun onPageFinished(view: WebView, url: String) {
				loadingSpinner.hideAnimated()
				updateCampaignOptOutElement()
				super.onPageFinished(view, url)
			}

			override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
				if (baseUrl == url) return true
				UrlUtil.openUrl(context, url)
				return true
			}
		}
		if (data != null) {
			data?.let { web.loadDataWithBaseURL(baseUrl, it, "text/html", "UTF-8", null) }
		} else {
			web.loadUrl(baseUrl)
		}
	}

	private fun updateCampaignOptOutElement() {
		val secureStorage = WalletSecureStorage.getInstance(requireContext())
		val text = if (secureStorage.getHasOptedOutOfNonImportantCampaigns()) getString(R.string.campaigns_opt_in_action) else getString(R.string.campaigns_opt_out_action)
		binding.htmlWebview.settings.javaScriptEnabled = true
		binding.htmlWebview.evaluateJavascript("document.getElementById('campaignOptOut').innerHTML = \"${text}\";", ValueCallback {
			binding.htmlWebview.settings.javaScriptEnabled = false
		})
	}

	fun getAlertDialog(
		context: Context,
		layout: Int,
		setCancellationOnTouchOutside: Boolean
	): AlertDialog {
		val builder: AlertDialog.Builder = AlertDialog.Builder(context)
		val customLayout: View =
			layoutInflater.inflate(layout, null)
		builder.setView(customLayout)
		val dialog = builder.create()
		dialog.setCanceledOnTouchOutside(setCancellationOnTouchOutside)
		return dialog
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}