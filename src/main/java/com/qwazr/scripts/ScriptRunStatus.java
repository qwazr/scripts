/*
 * Copyright 2014-2017 Emmanuel Keller / QWAZR
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public class ScriptRunStatus {

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

	@JsonCreator
	ScriptRunStatus(@JsonProperty("node") String node, @JsonProperty("name") String name,
			@JsonProperty("_status") String statusPath, @JsonProperty("_std_out") String stdOutPath,
			@JsonProperty("_std_err") String stdErrPath, @JsonProperty("uuid") String uuid,
			@JsonProperty("state") ScriptState state, @JsonProperty("start") Date startTime,
			@JsonProperty("end") Date endTime, @JsonProperty("bindings") Map<String, Object> bindings,
			@JsonProperty("error") String error) {
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
	}

	ScriptRunStatus(String node, String name, String uuid, ScriptState state, Long startTime, Long endTime,
			Map<String, Object> bindings, Exception exception) {
		this.node = node;
		this.startTime = startTime == null ? null : new Date(startTime);
		this.endTime = endTime == null ? null : new Date(endTime);
		this.bindings = bindings;
		this.statusPath = node + "/scripts/status/" + uuid;
		this.stdOutPath = node + "/scripts/status/" + uuid + "/out";
		this.stdErrPath = node + "/scripts/status/" + uuid + "/err";
		this.uuid = uuid;
		this.name = name;
		this.state = state;
		this.error = exception == null ? null : exception.getMessage();
	}

	private ScriptRunStatus(ScriptRunStatus src, Long startTime) {
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
	}
	
	public static List<ScriptRunStatus> cloneSchedulerResultList(List<ScriptRunStatus> sources, Long startTime) {
		if (sources == null)
			return null;
		final List<ScriptRunStatus> list = new ArrayList<ScriptRunStatus>(sources.size());
		sources.forEach(scriptRunStatus -> list.add(new ScriptRunStatus(scriptRunStatus, startTime)));
		return list;
	}
}
