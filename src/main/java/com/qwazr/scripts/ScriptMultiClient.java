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

import com.qwazr.cluster.service.TargetRuleEnum;
import com.qwazr.utils.ExceptionUtils;
import com.qwazr.utils.json.AbstractStreamingOutput;
import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.RemoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.util.*;

public class ScriptMultiClient extends JsonMultiClientAbstract<ScriptSingleClient> implements ScriptServiceInterface {

	private static final Logger logger = LoggerFactory.getLogger(ScriptMultiClient.class);

	public ScriptMultiClient(RemoteService... remotes) {
		super(new ScriptSingleClient[remotes.length], remotes);
	}

	@Override
	protected ScriptSingleClient newClient(RemoteService remote) {
		return new ScriptSingleClient(remote);
	}

	private List<ScriptRunStatus> runScriptRuleAll(final ExceptionUtils.Holder exceptionHolder, final String scriptPath,
			final String group, final TargetRuleEnum rule, final Map<String, String> variables) {
		final List<ScriptRunStatus> statusList = new ArrayList<>(size());
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
		final ExceptionUtils.Holder exceptionHolder = new ExceptionUtils.Holder(logger);
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

	public List<ScriptRunStatus> runScript(final String scriptPath, final String group, final TargetRuleEnum rule,
			final String... variables) {
		if (variables == null || variables.length == 0)
			return runScript(scriptPath, group, rule);
		HashMap<String, String> variablesMap = new HashMap<>();
		int l = variables.length / 2;
		for (int i = 0; i < l; i++)
			variablesMap.put(variables[i * 2], variables[i * 2 + 1]);
		return runScriptVariables(scriptPath, group, rule, variablesMap);
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus() {
		TreeMap<String, ScriptRunStatus> results = new TreeMap<>();
		for (ScriptSingleClient client : this) {
			try {
				results.putAll(client.getRunsStatus());
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() != 404)
					throw e;
			}
		}
		return results;
	}

	@Override
	public AbstractStreamingOutput getRunOut(final String run_id) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunOut(run_id);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

	@Override
	public AbstractStreamingOutput getRunErr(final String run_id) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunErr(run_id);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

	@Override
	public ScriptRunStatus getRunStatus(String run_id) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunStatus(run_id);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

}
