/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.common.qr

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import at.gv.brz.eval.data.state.StateError
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Mix approach for QR Code Analyzer.
 * Every first frame decoded with GlobalHistogramBinarizer, each second with the HybridBinarizer
 * */
class QRCodeMixedZXingAnalyzer(
    private val onDecodeCertificate: (decodeCertificateState: DecodeCertificateState) -> Unit
) : ImageAnalysis.Analyzer {
    companion object {
        private const val QR_CODE_ERROR_WRONG_FORMAT = "Q|YWF"
    }

    private val isGlobalHistogramBinarizer: AtomicBoolean = AtomicBoolean(true)

    private val yuvFormats = listOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)
    private val reader = MultiFormatReader().apply {
        val map = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true,
        )
        setHints(map)
    }

    override fun analyze(imageProxy: ImageProxy) {
        decodeFrame(
            imageProxy,
            binarizerFactory = { luminanceSource ->
                if (isGlobalHistogramBinarizer.getAndSet(!isGlobalHistogramBinarizer.get())) {
                    GlobalHistogramBinarizer(luminanceSource)
                } else {
                    HybridBinarizer(luminanceSource)
                }
            })
    }


    private fun decodeFrame(imageProxy: ImageProxy, binarizerFactory: (LuminanceSource) -> Binarizer) {
        try {
            if (imageProxy.format in yuvFormats && imageProxy.planes.size == 3) {
                val data = imageProxy.planes[0].buffer.toByteArray()
                val source = PlanarYUVLuminanceSource(
                    data,
                    imageProxy.planes[0].rowStride,
                    imageProxy.height,
                    0,
                    0,
                    imageProxy.width,
                    imageProxy.height,
                    false
                )
                val binaryBitmap = BinaryBitmap(binarizerFactory.invoke(source))
                try {
                    val result: Result = reader.decodeWithState(binaryBitmap)
                    onDecodeCertificate(DecodeCertificateState.SUCCESS(result.text))
                } catch (e: NotFoundException) {
                    onDecodeCertificate(DecodeCertificateState.SCANNING)
                    e.printStackTrace()
                } catch (e: ChecksumException) {
                    onDecodeCertificate(DecodeCertificateState.SCANNING)
                    e.printStackTrace()
                } catch (e: FormatException) {
                    onDecodeCertificate(DecodeCertificateState.SCANNING)
                    e.printStackTrace()
                }
            } else {
                onDecodeCertificate(DecodeCertificateState.ERROR(StateError(QR_CODE_ERROR_WRONG_FORMAT)))
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }
}
