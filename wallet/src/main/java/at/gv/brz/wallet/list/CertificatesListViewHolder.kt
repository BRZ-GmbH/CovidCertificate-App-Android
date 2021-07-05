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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.gv.brz.eval.models.DccHolder
import at.gv.brz.wallet.R

class CertificatesListViewHolder(
	itemView: View,
	private val onStartDragListener: ((CertificatesListViewHolder) -> Unit)?
) :
	RecyclerView.ViewHolder(itemView) {

	companion object {
		@SuppressLint("ClickableViewAccessibility")
		fun inflate(
			inflater: LayoutInflater,
			parent: ViewGroup,
			onStartDragListener: ((CertificatesListViewHolder) -> Unit)? = null
		): CertificatesListViewHolder {
			val itemView = inflater.inflate(R.layout.item_certificate_list, parent, false)
			val viewHolder = CertificatesListViewHolder(itemView, onStartDragListener)
			itemView.findViewById<View>(R.id.item_certificate_list_drag_handle).setOnTouchListener { v, event ->
				if (event.actionMasked == MotionEvent.ACTION_DOWN) {
					onStartDragListener?.invoke(viewHolder);
				}
				return@setOnTouchListener false
			}
			return viewHolder
		}
	}

	fun bindItem(verifiedCertificate: VerifiedCeritificateItem, onCertificateClickListener: ((DccHolder) -> Unit)? = null) =
		verifiedCertificate.bindView(itemView, onCertificateClickListener)
}