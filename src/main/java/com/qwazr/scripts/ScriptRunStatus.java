/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
 **/
package com.qwazr.scripts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Date;
import java.util.Set;

@JsonInclude(Include.NON_NULL)
public class ScriptRunStatus {

	public enum ScriptState {
		ready, running, terminated, error
	}

	public final String node;
	public final String _status;
	public final String _std_out;
	public final String _std_err;
	public final String uuid;
	public final String name;
	public final ScriptState state;
	public final Date start;
	public final Date end;
	public final Set<String> bindings;
	public final String error;

	public ScriptRunStatus() {
		node = null;
		_status = null;
		_std_out = null;
		_std_err = null;
		uuid = null;
		name = null;
		state = null;
		start = null;
		end = null;
		bindings = null;
		error = null;
	}

	ScriptRunStatus(String node, String name, String uuid, ScriptState state, Long startTime, Long endTime,
			Set<String> bindings, Exception exception) {
		this.node = node;
		this.start = startTime == null ? null : new Date(startTime);
		this.end = endTime == null ? null : new Date(endTime);
		this.bindings = bindings;
		this._status = node + "/scripts/status/" + uuid;
		this._std_out = node + "/scripts/status/" + uuid + "/out";
		this._std_err = node + "/scripts/status/" + uuid + "/err";
		this.uuid = uuid;
		this.name = name;
		this.state = state;
		this.error = exception == null ? null : exception.getMessage();
	}
}
