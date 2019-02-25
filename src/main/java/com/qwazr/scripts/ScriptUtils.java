/*
 * Copyright 2015-2019 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.scripts;

import com.qwazr.utils.ObjectMappers;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScriptUtils {

	public static <T> T fromJson(final Value value, final Class<T> valueClass) throws IOException {
		if (value == null)
			return null;
		final Object object = buildJson(value);
		return ObjectMappers.JSON.readValue(ObjectMappers.JSON.writeValueAsString(object), valueClass);
	}

	private static Object buildJson(final Value value) {
		if (value.isBoolean())
			return value.asBoolean();
		if (value.isNull())
			return null;
		if (value.isNumber()) {
			if (value.fitsInByte())
				return value.asByte();
			if (value.fitsInShort())
				return value.asShort();
			if (value.fitsInInt())
				return value.asInt();
			if (value.fitsInFloat())
				return value.asFloat();
			if (value.fitsInLong())
				return value.asLong();
			if (value.fitsInDouble())
				return value.asDouble();
			return value.asDouble();
		}
		if (value.isString())
			return value.asString();
		if (value.hasArrayElements()) {
			final List<Object> list = new ArrayList<>();
			for (final String key : value.getMemberKeys()) {
				list.add(buildJson(value.getMember(key)));
			}
			return list;
		}
		if (value.hasMembers()) {
			final Map<String, Object> map = new LinkedHashMap<>();
			for (final String key : value.getMemberKeys()) {
				map.put(key, buildJson(value.getMember(key)));
			}
			return map;
		}
		return null;
	}
}
