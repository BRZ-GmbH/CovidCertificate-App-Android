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
 * Data Model for Feature Intro Page
 */
@JsonClass(generateAdapter = true)
data class FeatureIntroEntryModel(
    val heading: String?,
    val foregroundImage: String?,
    val title: String?,
    val alignment: String?,
    val textGroups: List<FeatureIntroTextGroupModel>
)