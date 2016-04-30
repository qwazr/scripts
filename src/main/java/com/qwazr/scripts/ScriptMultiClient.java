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
import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.RemoteService;
import com.qwazr.utils.server.WebAppExceptionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class ScriptMultiClient extends JsonMultiClientAbstract<ScriptSingleClient> implements ScriptServiceInterface {

	private static final Logger logger = LoggerFactory.getLogger(ScriptMultiClient.class);

	public ScriptMultiClient(ExecutorService executor, RemoteService... remotes) {
		super(executor, new ScriptSingleClient[remotes.length], remotes);
	}

	@Override
	protected ScriptSingleClient newClient(RemoteService remote) {
		return new ScriptSingleClient(remote);
	}

	private List<ScriptRunStatus> runScriptRuleAll(WebAppExceptionHolder exceptionHolder, String scriptPath,
			Boolean local, String group, Integer msTimeout, TargetRuleEnum rule, Map<String, String> variables) {
		final List<ScriptRunStatus> statusList = new ArrayList<ScriptRunStatus>(size());
		for (ScriptSingleClient client : this) {
			try {
				if (variables == null)
					statusList.addAll(client.runScript(scriptPath, true, group, msTimeout, rule));
				else
					statusList.addAll(client.runScriptVariables(scriptPath, true, group, msTimeout, rule, variables));
			} catch (WebApplicationException e) {
				exceptionHolder.switchAndWarn(e);
			}
		}
		if (exceptionHolder.getException() != null)
			throw exceptionHolder.getException();
		return statusList;
	}

	private List<ScriptRunStatus> runScriptRuleOne(WebAppExceptionHolder exceptionHolder, String scriptPath,
			Boolean local, String group, Integer msTimeout, TargetRuleEnum rule, Map<String, String> variables) {
		for (ScriptSingleClient client : this) {
			try {
				if (variables == null)
					return client.runScript(scriptPath, true, group, msTimeout, rule);
				else
					return client.runScriptVariables(scriptPath, true, group, msTimeout, rule, variables);
			} catch (WebApplicationException e) {
				exceptionHolder.switchAndWarn(e);
			}
		}
		if (exceptionHolder.getException() != null)
			throw exceptionHolder.getException();
		return Collections.emptyList();
	}

	@Override
	public List<ScriptRunStatus> runScript(String scriptPath, Boolean local, String group, Integer msTimeout,
			TargetRuleEnum rule) {
		return runScriptVariables(scriptPath, local, group, msTimeout, rule, null);
	}

	@Override
	public List<ScriptRunStatus> runScriptVariables(String scriptPath, Boolean local, String group, Integer msTimeout,
			TargetRuleEnum rule, Map<String, String> variables) {
		final WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(logger);
		if (rule == null)
			rule = TargetRuleEnum.one;
		switch (rule) {
		case all:
			return runScriptRuleAll(exceptionHolder, scriptPath, local, group, msTimeout, rule, variables);
		default:
		case one:
			return runScriptRuleOne(exceptionHolder, scriptPath, local, group, msTimeout, rule, variables);
		}
	}

	public List<ScriptRunStatus> runScript(String scriptPath, Boolean local, String group, Integer msTimeout,
			TargetRuleEnum rule, String... variables) {
		if (variables == null || variables.length == 0)
			return runScript(scriptPath, local, group, msTimeout, rule);
		HashMap<String, String> variablesMap = new HashMap<String, String>();
		int l = variables.length / 2;
		for (int i = 0; i < l; i++)
			variablesMap.put(variables[i * 2], variables[i * 2 + 1]);
		return runScriptVariables(scriptPath, local, group, msTimeout, null, variablesMap);
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus(Boolean local, String group, Integer msTimeout) {
		TreeMap<String, ScriptRunStatus> results = new TreeMap<String, ScriptRunStatus>();
		for (ScriptSingleClient client : this) {
			try {
				results.putAll(client.getRunsStatus(true, group, msTimeout));
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() != 404)
					throw e;
			}
		}
		return results;
	}

	@Override
	public ScriptRunStatus getRunStatus(String run_id, Boolean local, String group, Integer msTimeout) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunStatus(run_id, true, group, msTimeout);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

	@Override
	public String getRunOut(String run_id, Boolean local, String group, Integer msTimeout) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunOut(run_id, true, group, msTimeout);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

	@Override
	public String getRunErr(String run_id, Boolean local, String group, Integer msTimeout) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunErr(run_id, true, group, msTimeout);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

}
