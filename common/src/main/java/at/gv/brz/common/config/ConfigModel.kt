/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.common.config

import at.gv.brz.common.faq.model.Faq
import at.gv.brz.common.faq.model.Header
import at.gv.brz.common.faq.model.Question
import at.gv.brz.common.util.PlatformUtil
import at.gv.brz.eval.utils.DEFAULT_DISPLAY_DATE_FORMATTER
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@JsonClass(generateAdapter = true)
data class ConfigModel(
	val android: String?,
	val huawei: String?,
	@Json(name = "android_force_date") val androidForceDate: String?,
	@Json(name = "huawei_force_date") val huaweiForceDate: String?,
	val questions: Map<String, FaqModel>?,
	val works: Map<String, FaqModel>?,
	val campaigns: List<CampaignModel>?,
	val conditions: Map<String, CertificateConditionModel>?
) {
	fun getQuestionsFaqs(languageKey: String): FaqModel? = questions?.get(languageKey)
	fun getWorksFaqs(languageKey: String): FaqModel? = works?.get(languageKey)

	fun generateFaqItems(languageKey: String) : List<Faq> {
		val itemsList = mutableListOf<Faq>()
		getQuestionsFaqs(languageKey)?.let { questionModel ->
			val questionItems = questionModel.faqEntries
			itemsList.add(Header(questionModel.faqIconAndroid, questionModel.faqTitle, questionModel.faqSubTitle))
			questionItems?.let {
				itemsList.addAll(it.map { faqEntry ->
					Question(
						faqEntry.title,
						faqEntry.text,
						linkTitle = faqEntry.linkTitle,
						linkUrl = faqEntry.linkUrl
					)
				})
			}
		}
		getWorksFaqs(languageKey)?.let { worksModel ->
			val questionItems = worksModel.faqEntries
			itemsList.add(Header(worksModel.faqIconAndroid, worksModel.faqTitle, worksModel.faqSubTitle))
			questionItems?.let {
				itemsList.addAll(it.map { faqEntry ->
					Question(
						faqEntry.title,
						faqEntry.text,
						linkTitle = faqEntry.linkTitle,
						linkUrl = faqEntry.linkUrl
					)
				})
			}
		}
		return itemsList
	}

	fun shouldForceUpdate(type: PlatformUtil.PlatformType): Boolean {
		val field = (if (type == PlatformUtil.PlatformType.GOOGLE_PLAY) androidForceDate else huaweiForceDate)
			?: return false

		try {
			val forceDate = LocalDate.parse(field, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
			return forceDate.isBefore(LocalDate.now())
		} catch (exception: Exception) {
		}
		return false
	}

	fun formattedForceDate(type: PlatformUtil.PlatformType): String {
		val field = (if (type == PlatformUtil.PlatformType.GOOGLE_PLAY) androidForceDate else huaweiForceDate)
			?: return ""

		try {
			val forceDate = LocalDate.parse(field, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
			return DEFAULT_DISPLAY_DATE_FORMATTER.format(forceDate)
		} catch (exception: Exception) {
		}
		return ""
	}

}