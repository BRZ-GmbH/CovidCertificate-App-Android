/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package at.gv.brz.wallet.directlink

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.gv.brz.common.config.DirectLinkModel
import at.gv.brz.common.config.DirectLinkResult
import at.gv.brz.common.net.DirectLinkRepository
import at.gv.brz.common.net.DirectLinkRepositorySpec
import at.gv.brz.common.util.SingleLiveEvent
import at.gv.brz.wallet.BuildConfig
import kotlinx.coroutines.launch

/**
 * The purpose of this class is to serve as a ViewModel for the WalletDirectLink
 * Flow from MainActivity to WalletDirectLinkViewModel
 */
class WalletDirectLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val bypassTokenQueryKey = "bpt"
    private val pathParameter = "result"
    sealed class DirectLinkType {
        data class SmsLink(val secret: String, val signature: String, val clientId: String?=null, val clientIdSignature: String?=null): DirectLinkType()
        data class WebLink(val base64EncodedQRCodeData: String): DirectLinkType()
        data class BypassTokenLink(val secret: String, val signature: String, val bypassToken:String, val clientId: String?=null, val clientIdSignature: String?=null): DirectLinkType()
    }

    private val directLinkResponseMutableLiveData = SingleLiveEvent<DirectLinkResult>()
    val directLinkResponseLiveData: SingleLiveEvent<DirectLinkResult> = directLinkResponseMutableLiveData

    fun loadCertificateWithBirthdateOrBypassToken(directLinkModel: DirectLinkModel, smsImportLinkHostName: String) {
        val directLinkRepository =
            DirectLinkRepository.getInstance(
                DirectLinkRepositorySpec(
                    getApplication(),
                    smsImportLinkHostName
                )
            )
        viewModelScope.launch {
            directLinkResponseMutableLiveData.value = (
                directLinkRepository.getCertificateWithBirthdateOrBypassToken(
                    directLinkModel
                )
            )
        }
    }

    fun checkDirectLinkType(appLinkData: Uri): DirectLinkType? {
        when ("${appLinkData.scheme}://${appLinkData.host}") {
            BuildConfig.smsImportLinkHost -> {
                if (appLinkData.pathSegments.size == 3 && appLinkData.pathSegments[0].equals(
                        pathParameter)
                ) {
                    return if (appLinkData.query != null)
                        DirectLinkType.BypassTokenLink(appLinkData.pathSegments[1],
                            appLinkData.pathSegments[2],
                            appLinkData.getQueryParameter(bypassTokenQueryKey)!!)
                    else
                        DirectLinkType.SmsLink(appLinkData.pathSegments[1],
                            appLinkData.pathSegments[2])
                } else if (appLinkData.pathSegments.size == 5 && appLinkData.pathSegments[0].equals(
                        pathParameter)
                ) {
                    return if (appLinkData.query != null)
                        DirectLinkType.BypassTokenLink(appLinkData.pathSegments[1],
                            appLinkData.pathSegments[2],
                            appLinkData.getQueryParameter(bypassTokenQueryKey)!!,
                            appLinkData.pathSegments[3],
                            appLinkData.pathSegments[4])
                    else
                        DirectLinkType.SmsLink(appLinkData.pathSegments[1],
                            appLinkData.pathSegments[2],
                            appLinkData.pathSegments[3],
                            appLinkData.pathSegments[4])
                }
            }
            BuildConfig.websiteImportLinkHost -> {
                if (appLinkData.pathSegments.size != 5) {
                    return null
                } else if (appLinkData.pathSegments[0].equals("gruenerpass") && appLinkData.pathSegments[1].equals("download") && appLinkData.pathSegments[2].equals("qr-code") && appLinkData.pathSegments[3].equals("wallet")) {
                    return DirectLinkType.WebLink(appLinkData.pathSegments[4])
                }
            }
        }
        return null
    }

}