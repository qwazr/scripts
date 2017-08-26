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

import com.qwazr.server.AbstractStreamingOutput;
import com.qwazr.server.RemoteService;
import com.qwazr.server.client.MultiClient;
import com.qwazr.server.client.MultiWebApplicationException;
import com.qwazr.utils.FunctionUtils;
import com.qwazr.utils.LoggerUtils;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class ScriptMultiClient extends MultiClient<ScriptSingleClient> implements ScriptServiceInterface {

	private static final Logger LOGGER = LoggerUtils.getLogger(ScriptMultiClient.class);

	ScriptMultiClient(ExecutorService executorService, RemoteService... remotes) {
		super(getClients(remotes), executorService);
	}

	private static ScriptSingleClient[] getClients(final RemoteService... remotes) {
		if (remotes == null)
			return null;
		final ScriptSingleClient[] clients = new ScriptSingleClient[remotes.length];
		int i = 0;
		for (RemoteService remote : remotes)
			clients[i++] = new ScriptSingleClient(remote);
		return clients;
	}

	private FunctionUtils.FunctionEx<ScriptSingleClient, List<ScriptRunStatus>, Exception> getRunScriptAction(
			final String scriptPath, final String group, final TargetRuleEnum rule,
			final Map<String, String> variables) {
		if (variables == null)
			return c -> c.runScript(scriptPath, group, rule);
		else
			return c -> c.runScriptVariables(scriptPath, group, rule, variables);

	}

	private List<ScriptRunStatus> runScriptRuleAll(final String scriptPath, final String group,
			final TargetRuleEnum rule, final Map<String, String> variables) {

		final List<List<ScriptRunStatus>> statusList =
				forEachParallel(getRunScriptAction(scriptPath, group, rule, variables), LOGGER);

		final List<ScriptRunStatus> results = new ArrayList<>();
		statusList.forEach(results::addAll);
		return results;
	}

	private List<ScriptRunStatus> runScriptRuleOne(final String scriptPath, final String group,
			final TargetRuleEnum rule, final Map<String, String> variables) {

		final MultiWebApplicationException.Builder exceptions = MultiWebApplicationException.of(LOGGER);
		final List<ScriptRunStatus> result =
				firstRandomSuccess(getRunScriptAction(scriptPath, group, rule, variables), exceptions::add);
		if (result != null)
			return result;
		if (exceptions.isEmpty())
			return Collections.emptyList();
		throw exceptions.build();
	}

	@Override
	public List<ScriptRunStatus> runScript(final String scriptPath, final String group, final TargetRuleEnum rule) {
		return runScriptVariables(scriptPath, group, rule, null);
	}

	@Override
	public List<ScriptRunStatus> runScriptVariables(final String scriptPath, final String group, TargetRuleEnum rule,
			Map<String, String> variables) {
		if (rule == null)
			rule = TargetRuleEnum.one;
		switch (rule) {
		case all:
			return runScriptRuleAll(scriptPath, group, rule, variables);
		default:
		case one:
			return runScriptRuleOne(scriptPath, group, rule, variables);
		}
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus() {
		final Map<String, ScriptRunStatus> finalResult = new TreeMap<>();
		final MultiWebApplicationException.Builder exceptions = MultiWebApplicationException.of(LOGGER);
		final List<Map<String, ScriptRunStatus>> results =
				forEachParallel(ScriptSingleClient::getRunsStatus, exceptions::add);
		results.forEach(finalResult::putAll);
		return finalResult;
	}

	private <T> T checkEmptyResult(final String runId, T result,
			final MultiWebApplicationException.Builder exceptions) {
		if (result != null)
			return result;
		throw exceptions.isEmpty() ? new NotFoundException("Running script not found: " + runId) : exceptions.build();
	}

	@Override
	public AbstractStreamingOutput getRunOut(final String runId) {
		final MultiWebApplicationException.Builder exceptions = MultiWebApplicationException.of(LOGGER);
		return checkEmptyResult(runId, firstRandomSuccess(client -> client.getRunOut(runId), exceptions::add),
				exceptions);
	}

	@Override
	public AbstractStreamingOutput getRunErr(final String runId) {
		final MultiWebApplicationException.Builder exceptions = MultiWebApplicationException.of(LOGGER);
		return checkEmptyResult(runId, firstRandomSuccess(client -> client.getRunErr(runId), exceptions::add),
				exceptions);
	}

	@Override
	public ScriptRunStatus getRunStatus(final String runId) {
		final MultiWebApplicationException.Builder exceptions = MultiWebApplicationException.of(LOGGER);
		return checkEmptyResult(runId, firstRandomSuccess(client -> client.getRunStatus(runId), exceptions::add),
				exceptions);
	}

}
