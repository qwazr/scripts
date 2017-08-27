/*
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
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

import com.qwazr.server.RemoteService;
import com.qwazr.server.client.JsonClient;
import org.apache.commons.io.input.AutoCloseInputStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ScriptSingleClient extends JsonClient implements ScriptServiceInterface {

	private final WebTarget scriptsTarget;
	private final WebTarget runTarget;
	private final WebTarget statusTarget;

	public ScriptSingleClient(final RemoteService remote) {
		super(remote);
		final WebTarget rootTarget = client.target(remote.serviceAddress);
		scriptsTarget = rootTarget.path("scripts");
		runTarget = scriptsTarget.path("run");
		statusTarget = scriptsTarget.path("status");
	}

	private final static GenericType<List<ScriptRunStatus>> listRunStatusType =
			new GenericType<List<ScriptRunStatus>>() {
			};

	private WebTarget getRunTarget(final String scriptPath, final String group, final TargetRuleEnum rule) {
		WebTarget target = runTarget.path(scriptPath);
		if (group != null)
			target = target.queryParam("group", group);
		if (rule != null)
			target = target.queryParam("rule", rule.name());
		return target;
	}

	@Override
	public List<ScriptRunStatus> runScript(final String scriptPath, final String group, final TargetRuleEnum rule) {
		return getRunTarget(scriptPath, group, rule).request(MediaType.APPLICATION_JSON).get(listRunStatusType);
	}

	@Override
	public List<ScriptRunStatus> runScriptVariables(final String scriptPath, final String group,
			final TargetRuleEnum rule, final Map<String, String> variables) {
		if (variables == null || variables.isEmpty())
			return runScript(scriptPath, group, rule);
		return getRunTarget(scriptPath, group, rule).request(MediaType.APPLICATION_JSON)
				.post(Entity.json(variables), listRunStatusType);
	}

	@Override
	public ScriptRunStatus getRunStatus(final String runId) {
		return statusTarget.path(runId).request(MediaType.APPLICATION_JSON).get(ScriptRunStatus.class);
	}

	private final static GenericType<TreeMap<String, ScriptRunStatus>> mapRunStatusType =
			new GenericType<TreeMap<String, ScriptRunStatus>>() {
			};

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus() {
		return statusTarget.request(MediaType.APPLICATION_JSON).get(mapRunStatusType);
	}

	@Override
	public InputStream getRunOut(final String runId) {
		return new AutoCloseInputStream(
				statusTarget.path(runId).path("out").request(MediaType.TEXT_PLAIN).get(InputStream.class));
	}

	@Override
	public InputStream getRunErr(final String runId) {
		return new AutoCloseInputStream(
				statusTarget.path(runId).path("err").request(MediaType.TEXT_PLAIN).get(InputStream.class));
	}
}
