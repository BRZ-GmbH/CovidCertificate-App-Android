/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.gv.brz.sdk.data.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class RawJsonString

/**
 * A Moshi json adapter that parses a JSON object as a string.
 */
class RawJsonStringAdapter {

	@ToJson
	fun toJson(writer: JsonWriter, @RawJsonString value: String?) {
		value?.let {
			writer.value(ByteArrayInputStream(value.toByteArray()).source().buffer())
		} ?: writer.jsonValue(null)
	}
	@FromJson
	@RawJsonString
	fun fromJson(reader: JsonReader): String {
		return reader.nextSource().readString(Charset.defaultCharset())
	}

}