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
import at.gv.brz.common.R
import at.gv.brz.common.util.AssetUtil.loadImpressumHtmlFile
import at.gv.brz.common.util.UrlUtil
import at.gv.brz.common.views.hideAnimated
import androidx.lifecycle.lifecycleScope
import at.gv.brz.common.BuildConfig
import at.gv.brz.common.html.BuildInfo
import at.gv.brz.common.util.setSecureFlagToBlockScreenshots
import at.gv.brz.eval.CovidCertificateSdk
import at.gv.brz.wallet.data.WalletSecureStorage
import at.gv.brz.wallet.databinding.FragmentHtmlBinding

class HtmlFragment : Fragment() {

	companion object {
		private const val COVID_CERT_IMPRESSUM_PREFIX = "ccert://"
		private const val DATA_UPDATE_IMPRESSUM_PREFIX = "dataupdate://"
		private const val TOGGLE_CAMPAIGN_OPT_OUT_IMPRESSUM_PREFIX = "togglecampaignoptout://"

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
				if (url.toLowerCase().startsWith(COVID_CERT_IMPRESSUM_PREFIX)) {
					val buildInfo = buildInfo ?: throw IllegalStateException("No BuildInfo supplied for imprint")
					val strippedUrl = url.substring(COVID_CERT_IMPRESSUM_PREFIX.length)
					val htmlFragment = newInstance(
						R.string.impressum_title,
						buildInfo,
						baseUrl,
						loadImpressumHtmlFile(view.context, strippedUrl, buildInfo),
						fragmentLayoutId
					)
					parentFragmentManager.beginTransaction()
						.setCustomAnimations(
							R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter,
							R.anim.slide_pop_exit
						)
						.replace(fragmentLayoutId, htmlFragment)
						.addToBackStack(HtmlFragment::class.java.canonicalName)
						.commit()
					return true
				} else if (url.toLowerCase().startsWith(DATA_UPDATE_IMPRESSUM_PREFIX)) {
					val alertDialog = getAlertDialog(requireContext(), R.layout.dialog_progress, true)
					alertDialog.show()
					alertDialog.findViewById<TextView>(R.id.text_progress_bar).setText(R.string.business_rule_update_progress)
					CovidCertificateSdk.getCertificateVerificationController().refreshTrustList(lifecycleScope, true, onCompletionCallback = {
						alertDialog.dismiss()
						val message = if (it.failed) R.string.business_rule_update_failed_message else R.string.business_rule_update_success_message
						androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CovidCertificate_AlertDialogStyle)
							.setMessage(message)
							.setPositiveButton(R.string.business_rule_update_ok_button) { dialog, _ ->
								dialog.dismiss()
							}
							.setCancelable(true)
							.create()
							.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
							.show()
					})
					return true
				} else if (url.toLowerCase().startsWith(TOGGLE_CAMPAIGN_OPT_OUT_IMPRESSUM_PREFIX)) {
					val secureStorage = WalletSecureStorage.getInstance(requireContext())
					secureStorage.setHasOptedOutOfNonImportantCampaigns(!secureStorage.getHasOptedOutOfNonImportantCampaigns())
					updateCampaignOptOutElement()
					return true
				}
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