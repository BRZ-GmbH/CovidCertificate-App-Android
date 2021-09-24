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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.File
import java.util.*
import kotlin.math.roundToInt

object QRCodeReaderHelper {

	private val hints = mapOf(
		DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE),
		DecodeHintType.TRY_HARDER to true,
	)
	private val reader = MultiFormatReader().apply { setHints(hints) }

	fun decodeQrCode(bitmap: Bitmap): String? {
		var decoded: String? = null

		val intArray = IntArray(bitmap.width * bitmap.height)
		bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
		val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
		val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

		try {
			val result: Result = reader.decodeWithState(binaryBitmap)
			decoded = result.text
		} catch (e: NotFoundException) {
			e.printStackTrace()
		} catch (e: ChecksumException) {
			e.printStackTrace()
		} catch (e: FormatException) {
			e.printStackTrace()
		}
		return decoded
	}

	private fun renderPdfPage(page: PdfRenderer.Page, scale: Float): Bitmap {
		val bitmap: Bitmap = Bitmap.createBitmap((page.width * scale).roundToInt(),
			(page.height * scale).roundToInt(), Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmap)
		canvas.drawColor(Color.WHITE)
		canvas.drawBitmap(bitmap, 0.0f, 0.0f, null)

		page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
		return bitmap
	}

	fun pdfToBitmap(context: Context, pdfFile: File): ArrayList<Bitmap> {
		val bitmaps = arrayListOf<Bitmap>()
		try {
			val renderer = PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY))
			val pageCount = renderer.pageCount

			if (pageCount <= 2) {
				for (i in 0 until pageCount) {
					val page: PdfRenderer.Page = renderer.openPage(i)

					bitmaps.add(renderPdfPage(page, 1f))
					bitmaps.add(renderPdfPage(page, 300f / 72f))

					page.close()
				}
			}
			renderer.close()

		} catch (ex: Exception) {
			ex.printStackTrace()
		}

		return bitmaps
	}
}

