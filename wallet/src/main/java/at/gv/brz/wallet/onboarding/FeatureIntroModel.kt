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
 * Data Model for the Feature Intros (intros.json)
 */
@JsonClass(generateAdapter = true)
data class FeatureIntroModel(val intros: Map<String, Map<String, List<FeatureIntroEntryModel>>>)