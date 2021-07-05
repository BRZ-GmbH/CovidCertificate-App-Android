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
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import at.gv.brz.common.R

class CameraPermissionExplanationDialog(context: Context) : AlertDialog(context) {

	private var grantCameraAccessClickListener: View.OnClickListener? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.dialog_camera_permission_explanation)
		window?.apply {
			setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			setBackgroundDrawableResource(R.drawable.bg_dialog)
		}
		findViewById<TextView>(R.id.camera_permission_dialog_ok_button)?.apply {
			paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
			setOnClickListener { v: View? ->
				dismiss()
				grantCameraAccessClickListener?.onClick(v)
			}
		}
		findViewById<View>(R.id.camera_permission_dialog_close_button)?.setOnClickListener { _ -> cancel() }
	}

	fun setGrantCameraAccessClickListener(listener: View.OnClickListener?) {
		grantCameraAccessClickListener = listener
	}

}