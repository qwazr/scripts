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
import com.qwazr.utils.CharsetUtils;
import com.qwazr.utils.json.AbstractStreamingOutput;
import com.qwazr.utils.server.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ScriptServiceImpl implements ScriptServiceInterface {

	private static final Logger logger = LoggerFactory.getLogger(ScriptServiceImpl.class);

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
			throw ServerException.getJsonException(logger, e);
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
			throw ServerException.getTextException(logger, e);
		}
	}

	@Override
	public StreamingOutput getRunOut(final String run_id) {
		try {
			return AbstractStreamingOutput.with(new StringReader(getRunThread(run_id).getOut()),
					CharsetUtils.CharsetUTF8);
		} catch (ServerException e) {
			throw ServerException.getTextException(logger, e);
		}
	}

	@Override
	public StreamingOutput getRunErr(final String run_id) {
		try {
			return AbstractStreamingOutput.with(new StringReader(getRunThread(run_id).getErr()),
					CharsetUtils.CharsetUTF8);
		} catch (ServerException e) {
			throw ServerException.getTextException(logger, e);
		}
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus() {
		return ScriptManager.INSTANCE.getRunsStatus();
	}
}
