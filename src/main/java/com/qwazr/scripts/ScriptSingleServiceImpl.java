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

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.TargetRuleEnum;
import com.qwazr.utils.server.ServerException;

import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScriptSingleServiceImpl implements ScriptServiceInterface {

	@Override
	public List<ScriptRunStatus> runScript(String scriptPath, Boolean local, String group, Integer msTimeout,
			TargetRuleEnum rule) {
		return runScriptVariables(scriptPath, local, group, msTimeout, rule, null);
	}

	@Override
	public List<ScriptRunStatus> runScriptVariables(String scriptPath, Boolean local, String group, Integer msTimeout,
			TargetRuleEnum rule, Map<String, String> variables) {
		try {
			if (!ClusterManager.INSTANCE.isGroup(group))
				throw new ServerException(Status.NOT_FOUND, "Wrong group: " + group);
			return Arrays.asList(ScriptManager.INSTANCE.runAsync(scriptPath, variables));
		} catch (Exception e) {
			throw ServerException.getJsonException(e);
		}
	}

	private ScriptRunThread getRunThread(String run_id) throws ServerException {
		ScriptRunThread runThread = ScriptManager.INSTANCE.getRunThread(run_id);
		if (runThread == null)
			throw new ServerException(Status.NOT_FOUND, "No status found");
		return runThread;
	}

	@Override
	public ScriptRunStatus getRunStatus(String run_id, Boolean local, String group, Integer msTimeout) {
		try {
			if (!ClusterManager.INSTANCE.isGroup(group))
				throw new ServerException(Status.NOT_FOUND, "Wrong group: " + group);
			return getRunThread(run_id).getStatus();
		} catch (ServerException e) {
			throw e.getTextException();
		}
	}

	@Override
	public String getRunOut(String run_id, Boolean local, String group, Integer msTimeout) {
		try {
			if (!ClusterManager.INSTANCE.isGroup(group))
				throw new ServerException(Status.NOT_FOUND, "Wrong group: " + group);
			return getRunThread(run_id).getOut();
		} catch (ServerException e) {
			throw e.getTextException();
		}
	}

	@Override
	public String getRunErr(String run_id, Boolean local, String group, Integer msTimeout) {
		try {
			if (!ClusterManager.INSTANCE.isGroup(group))
				throw new ServerException(Status.NOT_FOUND, "Wrong group: " + group);
			return getRunThread(run_id).getErr();
		} catch (ServerException e) {
			throw e.getTextException();
		}
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus(Boolean local, String group, Integer msTimeout) {
		if (!ClusterManager.INSTANCE.isGroup(group))
			return Collections.emptyMap();
		return ScriptManager.INSTANCE.getRunsStatus();
	}
}
