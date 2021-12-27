/*
 * Copyright (c) 2021 BRZ GmbH <https://www.brz.gv.at>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.wallet.onboarding

import com.squareup.moshi.JsonClass

/**
 * Data Model for Feature Intro Text Group
 */
@JsonClass(generateAdapter = true)
data class FeatureIntroTextGroupModel(
    val image: String?,
    val imageColor: String?,
    val imageAlt: String?,
    val text: String?
)