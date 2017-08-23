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

import com.qwazr.cluster.TargetRuleEnum;
import com.qwazr.server.AbstractStreamingOutput;
import com.qwazr.server.RemoteService;
import com.qwazr.server.client.MultiClient;
import com.qwazr.utils.ExceptionUtils;
import com.qwazr.utils.LoggerUtils;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ScriptMultiClient extends MultiClient<ScriptSingleClient> implements ScriptServiceInterface {

	private static final Logger LOGGER = LoggerUtils.getLogger(ScriptMultiClient.class);
	private final ExecutorService executorService;

	ScriptMultiClient(ExecutorService executorService, RemoteService... remotes) {
		super(getClients(remotes));
		this.executorService = executorService;
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

	private List<ScriptRunStatus> runScriptRuleAll(final ExceptionUtils.Holder exceptionHolder, final String scriptPath,
			final String group, final TargetRuleEnum rule, final Map<String, String> variables) {
		final List<ScriptRunStatus> statusList = new ArrayList<>();
		for (ScriptSingleClient client : this) {
			try {
				if (variables == null)
					statusList.addAll(client.runScript(scriptPath, group, rule));
				else
					statusList.addAll(client.runScriptVariables(scriptPath, group, rule, variables));
			} catch (WebApplicationException e) {
				exceptionHolder.switchAndWarn(e);
			}
		}
		exceptionHolder.thrownIfAny();
		return statusList;
	}

	private List<ScriptRunStatus> runScriptRuleOne(final ExceptionUtils.Holder exceptionHolder, final String scriptPath,
			final String group, final TargetRuleEnum rule, final Map<String, String> variables) {
		for (ScriptSingleClient client : this) {
			try {
				if (variables == null)
					return client.runScript(scriptPath, group, rule);
				else
					return client.runScriptVariables(scriptPath, group, rule, variables);
			} catch (WebApplicationException e) {
				exceptionHolder.switchAndWarn(e);
			}
		}
		exceptionHolder.thrownIfAny();
		return Collections.emptyList();
	}

	@Override
	public List<ScriptRunStatus> runScript(final String scriptPath, final String group, final TargetRuleEnum rule) {
		return runScriptVariables(scriptPath, group, rule, null);
	}

	@Override
	public List<ScriptRunStatus> runScriptVariables(final String scriptPath, final String group, TargetRuleEnum rule,
			Map<String, String> variables) {
		final ExceptionUtils.Holder exceptionHolder = new ExceptionUtils.Holder(LOGGER);
		if (rule == null)
			rule = TargetRuleEnum.one;
		switch (rule) {
		case all:
			return runScriptRuleAll(exceptionHolder, scriptPath, group, rule, variables);
		default:
		case one:
			return runScriptRuleOne(exceptionHolder, scriptPath, group, rule, variables);
		}
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus() {
		final Map<String, ScriptRunStatus> results = new ConcurrentHashMap<>();
		forEachParallel(executorService, 1, TimeUnit.MINUTES, client -> {
			results.putAll(client.getRunsStatus());
			return null;
		});
		return results;
	}

	private <T> T checkNotNull(final String runId, final T value) {
		if (value == null)
			throw new NotFoundException("Running script not found: " + runId);
		return value;
	}

	@Override
	public AbstractStreamingOutput getRunOut(final String runId) {
		return checkNotNull(runId, firstRandomSuccess(client -> client.getRunOut(runId))).result;
	}

	@Override
	public AbstractStreamingOutput getRunErr(final String runId) {
		return checkNotNull(runId, firstRandomSuccess(client -> client.getRunErr(runId))).result;
	}

	@Override
	public ScriptRunStatus getRunStatus(final String runId) {
		return checkNotNull(runId, firstRandomSuccess(client -> client.getRunStatus(runId))).result;
	}

}
