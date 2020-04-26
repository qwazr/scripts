/*
 * Copyright 2015-2018 Emmanuel Keller / QWAZR
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
		setterVisibility = JsonAutoDetect.Visibility.NONE,
		creatorVisibility = JsonAutoDetect.Visibility.NONE,
		isGetterVisibility = JsonAutoDetect.Visibility.NONE,
		fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class ScriptRunStatus<T> {

	public enum ScriptState {
		ready, running, terminated, error
	}

	public final String node;
	@JsonProperty("_status")
	public final String statusPath;
	@JsonProperty("_std_out")
	public final String stdOutPath;
	@JsonProperty("_std_err")
	public final String stdErrPath;
	public final String uuid;
	public final String name;
	public final ScriptState state;
	@JsonProperty("start")
	public final Date startTime;
	@JsonProperty("end")
	public final Date endTime;
	public final Map<String, Object> bindings;
	public final String error;
	public final T result;

	@JsonCreator
	ScriptRunStatus(@JsonProperty("node") String node, @JsonProperty("name") String name,
			@JsonProperty("_status") String statusPath, @JsonProperty("_std_out") String stdOutPath,
			@JsonProperty("_std_err") String stdErrPath, @JsonProperty("uuid") String uuid,
			@JsonProperty("state") ScriptState state, @JsonProperty("start") Date startTime,
			@JsonProperty("end") Date endTime, @JsonProperty("bindings") Map<String, Object> bindings,
			@JsonProperty("error") String error, @JsonProperty("result") T result) {
		this.node = node;
		this.statusPath = statusPath;
		this.stdOutPath = stdOutPath;
		this.stdErrPath = stdErrPath;
		this.uuid = uuid;
		this.name = name;
		this.state = state;
		this.startTime = startTime;
		this.endTime = endTime;
		this.bindings = bindings;
		this.error = error;
		this.result = result;
	}

	public String getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public ScriptState getState() {
		return state;
	}

	ScriptRunStatus(String node, String name, String uuid, ScriptState state, Long startTime, Long endTime,
			Map<String, Object> bindings, Exception exception, T result) {
		this(node, name, node + "/scripts/status/" + uuid, node + "/scripts/status/" + uuid + "/out",
				node + "/scripts/status/" + uuid + "/err", uuid, state, startTime == null ? null : new Date(startTime),
				endTime == null ? null : new Date(endTime), bindings, exception == null ? null : exception.getMessage(),
				result);
	}

	private ScriptRunStatus(ScriptRunStatus<?> src, Long startTime) {
		this.node = src.node;
		this.name = src.name;
		this.uuid = src.uuid;
		this.startTime = startTime == null ? null : new Date(startTime);
		this.error = src.error;
		this.statusPath = src.statusPath;
		this.stdOutPath = null;
		this.stdErrPath = null;
		this.state = null;
		this.endTime = null;
		this.bindings = null;
		this.result = null;
	}

	public static List<ScriptRunStatus<?>> cloneSchedulerResultList(List<ScriptRunStatus<?>> sources, Long startTime) {
		if (sources == null)
			return null;
		final List<ScriptRunStatus<?>> list = new ArrayList<>(sources.size());
		sources.forEach(scriptRunStatus -> list.add(new ScriptRunStatus<>(scriptRunStatus, startTime)));
		return list;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ScriptRunStatus))
			return false;
		if (other == this)
			return true;
		final ScriptRunStatus<?> o = (ScriptRunStatus<?>) other;
		return Objects.equals(node, o.node) && Objects.equals(name, o.name) && Objects.equals(uuid, o.uuid) &&
				Objects.equals(startTime, o.startTime) && Objects.equals(error, o.error) &&
				Objects.equals(statusPath, o.statusPath) && Objects.equals(stdOutPath, o.stdOutPath) &&
				Objects.equals(stdErrPath, o.stdErrPath) && Objects.equals(state, o.state) &&
				Objects.equals(endTime, o.endTime) && Objects.equals(bindings, o.bindings) &&
				Objects.equals(result, o.result);
	}

	@Override
	public int hashCode() {
		return Objects.hash(node, name, uuid, startTime, error, endTime);
	}
}
