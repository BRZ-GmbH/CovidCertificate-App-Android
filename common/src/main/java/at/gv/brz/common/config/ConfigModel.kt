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
import com.fasterxml.jackson.annotation.JsonFormat
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@JsonClass(generateAdapter = true)
data class ConfigModel(
	val android: String?,
	val huawei: String?,
	@Json(name = "android_force_date") val androidForceDate: String?,
	@Json(name = "huawei_force_date") val huaweiForceDate: String?,
	val infoBox: Map<String, InfoBoxModel>?,
	val questions: Map<String, FaqModel>?,
	val works: Map<String, FaqModel>?,
	@Json(name = "refresh_vaccination_campaign_start") val vaccinationRefreshCampaignStart: String?,
	val vaccinationRefreshCampaignText: Map<String, VaccinationRefreshCampaignTextModel>?
) {
	fun getInfoBox(languageKey: String?): InfoBoxModel? = infoBox?.get(languageKey)
	fun getQuestionsFaqs(languageKey: String): FaqModel? = questions?.get(languageKey)
	fun getWorksFaqs(languageKey: String): FaqModel? = works?.get(languageKey)

	val vaccinationRefreshCampaignStartDate: LocalDateTime?
		get() {
			if (vaccinationRefreshCampaignStart == null) {
				return null
			}
			try {
				return LocalDateTime.parse(vaccinationRefreshCampaignStart, DateTimeFormatter.ISO_DATE_TIME)
			} catch (e: Exception){
			}
			return null
		}

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