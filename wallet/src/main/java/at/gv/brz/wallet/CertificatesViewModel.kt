/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import at.gv.brz.common.config.ConfigModel
import at.gv.brz.common.net.ConfigRepository
import at.gv.brz.common.net.ConfigSpec
import at.gv.brz.common.util.SingleLiveEvent
import at.gv.brz.sdk.CovidCertificateSdk
import at.gv.brz.sdk.data.state.DecodeState
import at.gv.brz.sdk.data.state.VerificationResultStatus
import at.gv.brz.sdk.decoder.CertificateDecoder
import at.gv.brz.sdk.models.DccHolder
import at.gv.brz.sdk.verification.CertificateVerificationTask
import at.gv.brz.wallet.data.CertificateStorage
import at.gv.brz.wallet.data.NotificationSecureStorage
import at.gv.brz.wallet.data.WalletSecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import kotlin.collections.set

class CertificatesViewModel(application: Application) : AndroidViewModel(application) {

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val verificationController = CovidCertificateSdk.getCertificateVerificationController()

	private val dccHolderCollectionMutableLiveData = MutableLiveData<List<DccHolder>>()
	val dccHolderCollectionLiveData: LiveData<List<DccHolder>> = dccHolderCollectionMutableLiveData

	private val verifiedCertificatesMutableLiveData = MutableLiveData<List<VerifiedCertificate>>()
	val verifiedCertificates: LiveData<List<VerifiedCertificate>> = verifiedCertificatesMutableLiveData
	private val verificationJobs = mutableMapOf<DccHolder, Job>()

	private val certificateStorage: CertificateStorage by lazy { CertificateStorage.getInstance(getApplication()) }
	val secureStorage by lazy { WalletSecureStorage.getInstance(getApplication()) }
	val notificationStorage by lazy { NotificationSecureStorage.getInstance(getApplication()) }

	private var forceLoadConfig = true

	val onQrCodeClickedSingleLiveEvent = SingleLiveEvent<DccHolder>()

	val certificateSchema: JsonNode by lazy {
		val objectMapper = ObjectMapper().apply { this.findAndRegisterModules()
			registerModule(JavaTimeModule())
		}

		val schemaJson = readSchema()
		objectMapper.readValue(schemaJson, JsonNode::class.java)
	}

	init {
		dccHolderCollectionLiveData.observeForever { certificates ->
			// When the stored DccHolders change, map the verified certificates with the existing verification state or LOADING
			val currentVerifiedCertificates = verifiedCertificates.value ?: emptyList()
			verifiedCertificatesMutableLiveData.value = certificates.map { certificate ->
				currentVerifiedCertificates.find { it.dccHolder == certificate } ?: VerifiedCertificate(certificate, VerificationResultStatus.LOADING)
			}

			// (Re-)Verify all certificates
			certificates.forEach { startVerification(it) }
		}
	}

	fun reverifyAllCertificates() {
		val certificates = dccHolderCollectionLiveData.value
		certificates?.forEach { startVerification(it) }
	}

	fun loadCertificates() {
		viewModelScope.launch(Dispatchers.Default) {
			dccHolderCollectionMutableLiveData.postValue(
				certificateStorage.getCertificateList().mapNotNull { (CertificateDecoder.decode(it) as? DecodeState.SUCCESS)?.dccHolder }
			)
		}
	}

	fun readSchema(): String {
		val context: Context = getApplication()
		val inputStream = context.assets.open("JSON_SCHEMA.json")
		val reader = BufferedReader(InputStreamReader(inputStream))
		val out = StringBuilder()
		var line: String?
		while (reader.readLine().also { line = it } != null) {
			out.append(line)
		}
		reader.close()
		return out.toString()
	}

	fun startVerification(dccHolder: DccHolder, delayInMillis: Long = 0L, isForceVerification: Boolean = false) {
		if (isForceVerification) {
			verificationController.refreshTrustList(viewModelScope, true)
		}

		verificationJobs[dccHolder]?.cancel()

		var overwriteTrustlistClock = false
		/**
		 * In test builds (for Q as well as P environment) we allow switching a setting for the app to either use the real time fetched from a time server (behaviour in the published app) or to use the current device time for validating the business rules.
		 */
		if (BuildConfig.FLAVOR == "abn" || BuildConfig.FLAVOR == "prodtest") {
			val context: Context = getApplication()
			val sharedPreferences =
				context.getSharedPreferences("wallet.test", Context.MODE_PRIVATE)
			overwriteTrustlistClock = sharedPreferences.getBoolean("wallet.test.useDeviceTime", false)
		}

		val task = CertificateVerificationTask(dccHolder, connectivityManager, "AT", secureStorage.getSelectedValidationRegion() ?: "W", overwriteTrustlistClock = overwriteTrustlistClock)
		val job = viewModelScope.launch {
			task.verificationStateFlow.collect { state ->
				// Replace the verified certificate in the live data
				val newVerifiedCertificates = verifiedCertificates.value?.toMutableList() ?: mutableListOf()
				val index = newVerifiedCertificates.indexOfFirst { it.dccHolder == dccHolder }
				if (index >= 0) {
					newVerifiedCertificates[index] = VerifiedCertificate(dccHolder, state)
				} else {
					newVerifiedCertificates.add(VerifiedCertificate(dccHolder, state))
				}

				// Set the livedata value on the main dispatcher to prevent multiple posts overriding each other
				withContext(Dispatchers.Main.immediate) {
					verifiedCertificatesMutableLiveData.value = newVerifiedCertificates
				}

				// Once the verification state is not loading anymore, cancel the flow collection job (otherwise the flow stays active without emitting anything)
				if (state !is VerificationResultStatus.LOADING) {
					verificationJobs[dccHolder]?.cancel()
					verificationJobs.remove(dccHolder)
				}
			}
		}
		verificationJobs[dccHolder] = job

		viewModelScope.launch {
			if (delayInMillis > 0) delay(delayInMillis)
			verificationController.enqueue(task, viewModelScope)
		}
	}

	fun onQrCodeClicked(dccHolder: DccHolder) {
		onQrCodeClickedSingleLiveEvent.postValue(dccHolder)
	}

	fun containsCertificate(certificate: String): Boolean {
		return certificateStorage.containsCertificate(certificate)
	}

	fun addCertificate(certificate: String) {
		certificateStorage.saveCertificate(certificate)
		secureStorage.setHasModifiedCertificatesInSession(true)
	}

	fun moveCertificate(from: Int, to: Int) {
		certificateStorage.changeCertificatePosition(from, to)
	}

	fun removeCertificate(certificate: String) {
		certificateStorage.deleteCertificate(certificate)
		(CertificateDecoder.decode(certificate) as? DecodeState.SUCCESS)?.dccHolder?.euDGC?.vaccinations?.firstOrNull()?.certificateIdentifier?.let {
			notificationStorage.deleteCertificateIdentifier(it)
		}
		loadCertificates()
		secureStorage.setHasModifiedCertificatesInSession(true)
	}

	fun removeCertificates(certificates: List<String>) {
		certificates.forEach {
			certificateStorage.deleteCertificate(it)
			(CertificateDecoder.decode(it) as? DecodeState.SUCCESS)?.dccHolder?.euDGC?.vaccinations?.firstOrNull()?.certificateIdentifier?.let {
				notificationStorage.deleteCertificateIdentifier(it)
			}
		}
		loadCertificates()
		secureStorage.setHasModifiedCertificatesInSession(true)
	}

	fun toggleDeviceTimeSetting(): Boolean {
		/**
		 * In test builds (for Q as well as P environment) we allow switching a setting for the app to either use the real time fetched from a time server (behaviour in the published app) or to use the current device time for validating the business rules.
		 */
		if (BuildConfig.FLAVOR == "abn" || BuildConfig.FLAVOR == "prodtest") {
			val context: Context = getApplication()
			val sharedPreferences = context.getSharedPreferences("wallet.test", Context.MODE_PRIVATE)
			val newValue = !sharedPreferences.getBoolean("wallet.test.useDeviceTime", false)
			sharedPreferences.edit().putBoolean("wallet.test.useDeviceTime", newValue).apply()
			dccHolderCollectionLiveData.value?.forEach {
				startVerification(it)
			}
			return newValue
		}
		return false
	}

	fun getValidationTime(): ZonedDateTime? {
		if (BuildConfig.FLAVOR == "abn" || BuildConfig.FLAVOR == "prodtest") {
			val context: Context = getApplication()
			val sharedPreferences =
				context.getSharedPreferences("wallet.test", Context.MODE_PRIVATE)
			if (sharedPreferences.getBoolean("wallet.test.useDeviceTime", false)) {
				return ZonedDateTime.now()
			}
		}
		return CovidCertificateSdk.getValidationClock()
	}

	private val configMutableLiveData = MutableLiveData<ConfigModel>()
	val configLiveData: LiveData<ConfigModel> = configMutableLiveData

	private var hasLoadedConfig = false
	private var hasLoadedTrustlistData = false

	private val dataUpdateMutableLiveData = MutableLiveData<Unit>()
	val dataUpdateLiveData: LiveData<Unit> = dataUpdateMutableLiveData

	fun setHasUpdatedTrustlistData() {
		hasLoadedTrustlistData = true
		dataUpdateMutableLiveData.postValue(Unit)
	}

	fun hasLoadedData(): Boolean {
		return hasLoadedConfig && hasLoadedTrustlistData
	}

	fun resetLoadingFlags() {
		hasLoadedConfig = false
		hasLoadedTrustlistData = false
	}

	fun loadConfig() {
		val configRepository = ConfigRepository.getInstance(ConfigSpec(getApplication(),
			BuildConfig.BASE_URL))
		viewModelScope.launch {
			configRepository.loadConfig(forceLoadConfig, getApplication())?.let { config ->
				hasLoadedConfig = true
				configMutableLiveData.postValue(config)
				dataUpdateMutableLiveData.postValue(Unit)
			}
			forceLoadConfig = false
		}
	}

	data class VerifiedCertificate(val dccHolder: DccHolder, val state: VerificationResultStatus)
}