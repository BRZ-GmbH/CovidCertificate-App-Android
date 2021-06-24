/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier

import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import at.gv.brz.common.config.ConfigModel
import at.gv.brz.common.util.UrlUtil
import at.gv.brz.common.util.setSecureFlagToBlockScreenshots
import at.gv.brz.eval.CovidCertificateSdk
import ch.admin.bag.covidcertificate.verifier.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

	private lateinit var binding: ActivityMainBinding

	private val verifierViewModel by viewModels<VerifierViewModel>()

	private var forceUpdateDialog: AlertDialog? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityMainBinding.inflate(layoutInflater)
		val view = binding.root
		setContentView(view)

		window.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR)

		if (savedInstanceState == null) {
			supportFragmentManager.beginTransaction()
				.add(R.id.fragment_container, HomeFragment.newInstance())
				.commit()
		}

		verifierViewModel.configLiveData.observe(this) { config -> handleConfig(config) }

		CovidCertificateSdk.registerWithLifecycle(lifecycle)
	}

	override fun onStart() {
		super.onStart()
		verifierViewModel.loadConfig()
		CovidCertificateSdk.getCertificateVerificationController().refreshTrustList(lifecycleScope)
	}

	override fun onDestroy() {
		super.onDestroy()
		CovidCertificateSdk.unregisterWithLifecycle(lifecycle)
	}

	private fun handleConfig(config: ConfigModel) {
		if (config.forceUpdate && forceUpdateDialog == null) {
			val forceUpdateDialog = AlertDialog.Builder(this, R.style.CovidCertificate_AlertDialogStyle)
				.setTitle(R.string.force_update_title)
				.setMessage(R.string.force_update_text)
				.setPositiveButton(R.string.force_update_button, null)
				.setCancelable(false)
				.create()
				.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
			forceUpdateDialog.setOnShowListener {
				forceUpdateDialog.getButton(DialogInterface.BUTTON_POSITIVE)
					.setOnClickListener {
						val packageName = packageName
						UrlUtil.openUrl(this@MainActivity, "market://details?id=$packageName")
					}
			}
			this.forceUpdateDialog = forceUpdateDialog
			forceUpdateDialog.show()
		}
	}

}