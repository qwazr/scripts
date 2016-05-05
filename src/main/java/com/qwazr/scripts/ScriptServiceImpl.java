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
import com.qwazr.utils.server.ServerException;

import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ScriptServiceImpl implements ScriptServiceInterface {

	@Override
	public List<ScriptRunStatus> runScript(final String scriptPath, final String group, final TargetRuleEnum rule) {
		return runScriptVariables(scriptPath, group, rule, null);
	}

	@Override
	public List<ScriptRunStatus> runScriptVariables(final String scriptPath, final String group,
			final TargetRuleEnum rule, final Map<String, String> variables) {
		try {
			return Arrays.asList(ScriptManager.INSTANCE.runAsync(scriptPath, variables));
		} catch (Exception e) {
			throw ServerException.getJsonException(e);
		}
	}

	private RunThreadAbstract getRunThread(final String run_id) throws ServerException {
		final RunThreadAbstract runThread = ScriptManager.INSTANCE.getRunThread(run_id);
		if (runThread == null)
			throw new ServerException(Status.NOT_FOUND, "No status found");
		return runThread;
	}

	@Override
	public ScriptRunStatus getRunStatus(final String run_id) {
		try {
			return getRunThread(run_id).getStatus();
		} catch (ServerException e) {
			throw e.getTextException();
		}
	}

	@Override
	public String getRunOut(final String run_id) {
		try {
			return getRunThread(run_id).getOut();
		} catch (ServerException e) {
			throw e.getTextException();
		}
	}

	@Override
	public String getRunErr(final String run_id) {
		try {
			return getRunThread(run_id).getErr();
		} catch (ServerException e) {
			throw e.getTextException();
		}
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus() {
		return ScriptManager.INSTANCE.getRunsStatus();
	}
}
