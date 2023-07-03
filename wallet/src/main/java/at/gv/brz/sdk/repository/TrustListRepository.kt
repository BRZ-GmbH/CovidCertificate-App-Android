/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk.repository

import at.gv.brz.sdk.data.TrustListStore
import at.gv.brz.sdk.models.*
import at.gv.brz.sdk.net.BusinessRulesService
import at.gv.brz.sdk.net.NationalTrustlistService
import at.gv.brz.sdk.net.TrustlistService
import at.gv.brz.sdk.net.ValueSetsService
import com.lyft.kronos.KronosClock
import ehn.techiop.hcert.kotlin.chain.CertificateRepository
import ehn.techiop.hcert.kotlin.chain.impl.PrefilledCertificateRepository
import ehn.techiop.hcert.kotlin.chain.impl.TrustListCertificateRepository
import ehn.techiop.hcert.kotlin.rules.BusinessRulesDecodeService
import ehn.techiop.hcert.kotlin.trust.SignedData
import ehn.techiop.hcert.kotlin.trust.TrustListDecodeService
import ehn.techiop.hcert.kotlin.valueset.ValueSetDecodeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

internal class TrustListRepository(
	private val trustlistService: TrustlistService,
	private val nationalTrustlistService: NationalTrustlistService,
	private val valueSetsService: ValueSetsService,
	private val businessRulesService: BusinessRulesService,
	private val store: TrustListStore,
	private val trustAnchor: String,
	private val alternativeTrustAnchor: String,
	private val kronosClock: KronosClock,
) {

	/**
	 * Refresh the trust list if necessary. This will check for the presence and validity of the certificate signatures,
	 * revoked certificates and rule set and load them from the backend if necessary. Set the [forceRefresh] flag to always load
	 * the data from the server.
	 *
	 * @param forceRefresh False to only load data from the server if it is missing or outdated, true to always load from the server
	 */
	suspend fun refreshTrustList(forceRefresh: Boolean): RefreshResult = withContext(Dispatchers.IO) {
		var refreshedSignatures = RefreshResult(false, false)
		var refreshedNationalSignatures = RefreshResult(false, false)
		var refreshedValueSets = RefreshResult(false, false)
		var refreshedRules = RefreshResult(false, false)
		listOf(
			launch { refreshedSignatures = refreshCertificateSignatures(forceRefresh) },
			launch { refreshedNationalSignatures = refreshNationalCertificateSignatures(forceRefresh) },
			launch { refreshedRules = refreshBusinessRules(forceRefresh) },
			launch { refreshedValueSets = refreshValueSets(forceRefresh) }
		).joinAll()

		RefreshResult(refreshedSignatures.refreshed || refreshedNationalSignatures.refreshed || refreshedValueSets.refreshed || refreshedRules.refreshed, refreshedSignatures.failed || refreshedNationalSignatures.failed || refreshedValueSets.failed || refreshedRules.failed)
	}

	/**
	 * Get the trust list from the provider or null if at least one of the values is not set
	 */
	fun getTrustList(): TrustList? {
		return if (store.areTrustlistCertificatesValid() && store.areValueSetsValid() && store.areBusinessRulesValid() && !store.dataExpired()) {
			val trustlistContentData = store.trustlistContentData
			val trustlistSignatureData = store.trustlistSignatureData

			val nationalTrustlistContentData = store.nationalTrustlistContentData
			val nationalTrustlistSignatureData = store.nationalTrustlistSignatureData

			val valueSets = store.mappedValueSets
			val businessRules = store.mappedBusinessRules

			if (trustlistContentData != null && trustlistSignatureData != null && nationalTrustlistContentData != null && nationalTrustlistSignatureData != null&& valueSets != null && businessRules != null) {
				val signedData = SignedData(trustlistContentData, trustlistSignatureData)
				val trustAnchorRepository: CertificateRepository =
					PrefilledCertificateRepository(trustAnchor, alternativeTrustAnchor)

				val nationalSignedData = SignedData(nationalTrustlistContentData, nationalTrustlistSignatureData)

				try {
					TrustList(
						TrustListCertificateRepository(
							signedData,
							trustAnchorRepository,
							KronosTimeClock(kronosClock)
						),
						TrustListCertificateRepository(
							nationalSignedData,
							trustAnchorRepository,
							KronosTimeClock(kronosClock)
						),
						valueSets,
						businessRules,
						kronosClock
					)
				} catch (e: Throwable) {
					null
				}
			} else {
				null
			}
		} else {
			null
		}
	}

	private suspend fun refreshCertificateSignatures(forceRefresh: Boolean): RefreshResult =
		withContext(Dispatchers.IO) {
			val shouldLoadSignatures =
				forceRefresh || !store.areTrustlistCertificatesValid() || store.shouldUpdateTrustListCertificates()
			if (shouldLoadSignatures) {
				val trustListSignatureResponse = trustlistService.getTrustlistSignature()
				val trustlistSignatureBody = trustListSignatureResponse.body()

				if (trustListSignatureResponse.isSuccessful && trustlistSignatureBody != null) {
					val signatureBytes = trustlistSignatureBody.bytes()
					val contentHash = signatureBytes.contentHashCode()
					if (contentHash != store.trustlistContentHash || forceRefresh) {
						try {
							val trustlistResponse = trustlistService.getTrustlist()
							val trustlistBody = trustlistResponse.body()
							if (trustlistResponse.isSuccessful && trustlistBody != null) {
								val trustlistBodyBytes = trustlistBody.bytes()
								val signedData =
									SignedData(trustlistBodyBytes, signatureBytes)
								val trustAnchorRepository: CertificateRepository =
									PrefilledCertificateRepository(trustAnchor, alternativeTrustAnchor)
								val service = TrustListDecodeService(
									trustAnchorRepository,
									KronosTimeClock(kronosClock)
								)
								val result = service.decode(signedData)
								if (!result.second.certificates.isEmpty()) {
									store.trustlistContentData = trustlistBodyBytes
									store.trustlistSignatureData = signatureBytes
									//store.certificateSignatures = result.second
									store.trustlistContentHash = contentHash
								}
							}
							RefreshResult(true, !trustlistResponse.isSuccessful)
						} catch (e: Throwable) {
							RefreshResult(false, true)
						}
					} else {
						store.trustlistLastUpdate = Instant.now().toEpochMilli()
						// Return true if trust list needs to be forced to update (either invalid or not present)
						RefreshResult(!store.areTrustlistCertificatesValid() || store.shouldUpdateTrustListCertificates(), false)
					}
				} else {
					RefreshResult(false, true)
				}
			} else {
				RefreshResult(false, false)
			}
		}

	private suspend fun refreshNationalCertificateSignatures(forceRefresh: Boolean): RefreshResult =
		withContext(Dispatchers.IO) {
			val shouldLoadSignatures =
				forceRefresh || !store.areNationalTrustlistCertificatesValid() || store.shouldUpdateNationalTrustListCertificates()
			if (shouldLoadSignatures) {
				val trustListSignatureResponse = nationalTrustlistService.getTrustlistSignature()
				val trustlistSignatureBody = trustListSignatureResponse.body()

				if (trustListSignatureResponse.isSuccessful && trustlistSignatureBody != null) {
					val signatureBytes = trustlistSignatureBody.bytes()
					val contentHash = signatureBytes.contentHashCode()
					if (contentHash != store.nationalTrustlistContentHash || forceRefresh) {
						try {
							val trustlistResponse = nationalTrustlistService.getTrustlist()
							val trustlistBody = trustlistResponse.body()
							if (trustlistResponse.isSuccessful && trustlistBody != null) {
								val trustlistBodyBytes = trustlistBody.bytes()
								val signedData =
									SignedData(trustlistBodyBytes, signatureBytes)
								val trustAnchorRepository: CertificateRepository =
									PrefilledCertificateRepository(trustAnchor, alternativeTrustAnchor)
								val service = TrustListDecodeService(
									trustAnchorRepository,
									KronosTimeClock(kronosClock)
								)
								val result = service.decode(signedData)
								if (!result.second.certificates.isEmpty()) {
									store.nationalTrustlistContentData = trustlistBodyBytes
									store.nationalTrustlistSignatureData = signatureBytes
									//store.certificateSignatures = result.second
									store.nationalTrustlistContentHash = contentHash
								}
							}
							RefreshResult(true, !trustlistResponse.isSuccessful)
						} catch (e: Throwable) {
							RefreshResult(false, true)
						}
					} else {
						store.nationalTrustlistLastUpdate = Instant.now().toEpochMilli()
						// Return true if trust list needs to be forced to update (either invalid or not present)
						RefreshResult(!store.areNationalTrustlistCertificatesValid() || store.shouldUpdateNationalTrustListCertificates(), false)
					}
				} else {
					RefreshResult(false, true)
				}
			} else {
				RefreshResult(false, false)
			}
		}

	private suspend fun refreshValueSets(forceRefresh: Boolean): RefreshResult = withContext(Dispatchers.IO) {
		val shouldLoadValueSets = forceRefresh || !store.areValueSetsValid() || store.shouldUpdateValueSets()
		if (shouldLoadValueSets) {
			val signatureResponse = valueSetsService.getValueSetsSignature()
			val signatureBody = signatureResponse.body()
			if (signatureResponse.isSuccessful && signatureBody != null) {
				val signatureBytes = signatureBody.bytes()
				val contentHash = signatureBytes.contentHashCode()
				if (contentHash != store.valueSetsContentHash || forceRefresh) {
					try {
						val valueSetsResponse = valueSetsService.getValueSets()
						val valueSetsBody = valueSetsResponse.body()
						if (valueSetsResponse.isSuccessful && valueSetsBody != null) {
							val signedData = SignedData(valueSetsBody.bytes(), signatureBytes)
							val trustAnchorRepository: CertificateRepository =
								PrefilledCertificateRepository(trustAnchor, alternativeTrustAnchor)

							val service = ValueSetDecodeService(
								trustAnchorRepository,
								KronosTimeClock(kronosClock)
							)
							val result = service.decode(signedData)
							if (!result.second.valueSets.isEmpty()) {
								store.valueSets = result.second
								store.valueSetsContentHash = contentHash
							}
						}
						RefreshResult(true, !valueSetsResponse.isSuccessful)
					} catch (e: Throwable) {
						RefreshResult(false, true)
					}
				} else {
					store.valueSetsLastUpdate = Instant.now().toEpochMilli()
					// Return true if value sets needs to be forced to update (either invalid or not present)
					RefreshResult(!store.areValueSetsValid() || store.shouldUpdateValueSets(), false)
				}
			} else {
				RefreshResult(false, true)
			}
		} else {
			RefreshResult(false, false)
		}
	}

	private suspend fun refreshBusinessRules(forceRefresh: Boolean): RefreshResult = withContext(Dispatchers.IO) {
		val shouldLoadBusinessRules = forceRefresh || !store.areBusinessRulesValid() || store.shouldUpdateBusinessRules()
		if (shouldLoadBusinessRules) {
			val signatureResponse = businessRulesService.getBusinessRulesSignature()
			val signatureBody = signatureResponse.body()
			if (signatureResponse.isSuccessful && signatureBody != null) {
				val signatureBytes = signatureBody.bytes()
				val contentHash = signatureBytes.contentHashCode()
				if (contentHash != store.modernBusinessRulesContentHash || forceRefresh) {
					try {
						val businessRulesResponse = businessRulesService.getBusinessRules()
						val businessRulesBody = businessRulesResponse.body()
						if (businessRulesResponse.isSuccessful && businessRulesBody != null) {

							val signedData =
								SignedData(businessRulesBody.bytes(), signatureBytes)
							val trustAnchorRepository: CertificateRepository =
								PrefilledCertificateRepository(trustAnchor, alternativeTrustAnchor)

							val service = BusinessRulesDecodeService(
								trustAnchorRepository,
								KronosTimeClock(kronosClock)
							)
							val result = service.decode(signedData)
							if (!result.second.rules.isEmpty()) {
								store.modernBusinessRules = result.second
								store.modernBusinessRulesContentHash = contentHash
							}
						}
						RefreshResult(true, !businessRulesResponse.isSuccessful)
					} catch (e: Throwable) {
						RefreshResult(false, true)
					}
				} else {
					store.modernBusinessRulesLastUpdate = Instant.now().toEpochMilli()
					// Return true if value sets needs to be forced to update (either invalid or not present)
					RefreshResult(!store.areBusinessRulesValid() || store.shouldUpdateBusinessRules(), false)
				}
			} else {
				RefreshResult(false, true)
			}
		} else {
			RefreshResult(false, false)
		}
	}
}