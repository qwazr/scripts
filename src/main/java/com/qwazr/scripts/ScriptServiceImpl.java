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

import com.qwazr.cluster.TargetRuleEnum;
import com.qwazr.server.AbstractServiceImpl;
import com.qwazr.server.AbstractStreamingOutput;
import com.qwazr.server.ServerException;
import com.qwazr.utils.CharsetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class ScriptServiceImpl extends AbstractServiceImpl implements ScriptServiceInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptServiceImpl.class);

	private volatile ScriptManager scriptManager;

	ScriptServiceImpl(final ScriptManager scriptManager) {
		this.scriptManager = scriptManager;
	}

	public ScriptServiceImpl() {
		this(null);
	}

	@PostConstruct
	public void init() {
		scriptManager = getContextAttribute(ScriptManager.class);
	}

	@Override
	public List<ScriptRunStatus> runScript(final String scriptPath, final String group, final TargetRuleEnum rule) {
		return runScriptVariables(scriptPath, group, rule, null);
	}

	@Override
	public List<ScriptRunStatus> runScriptVariables(final String scriptPath, final String group,
			final TargetRuleEnum rule, final Map<String, String> variables) {
		try {
			return Arrays.asList(scriptManager.runAsync(scriptPath, variables));
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	private RunThreadAbstract getRunThread(final String runId) throws ServerException {
		final RunThreadAbstract runThread = scriptManager.getRunThread(runId);
		if (runThread == null)
			throw new ServerException(Status.NOT_FOUND, "No status found");
		return runThread;
	}

	@Override
	public ScriptRunStatus getRunStatus(final String runId) {
		try {
			return getRunThread(runId).getStatus();
		} catch (ServerException e) {
			throw ServerException.getTextException(LOGGER, e);
		}
	}

	@Override
	public StreamingOutput getRunOut(final String runId) {
		try {
			return AbstractStreamingOutput.with(new StringReader(getRunThread(runId).getOut()),
					CharsetUtils.CharsetUTF8);
		} catch (ServerException e) {
			throw ServerException.getTextException(LOGGER, e);
		}
	}

	@Override
	public StreamingOutput getRunErr(final String runId) {
		try {
			return AbstractStreamingOutput.with(new StringReader(getRunThread(runId).getErr()),
					CharsetUtils.CharsetUTF8);
		} catch (ServerException e) {
			throw ServerException.getTextException(LOGGER, e);
		}
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus() {
		return scriptManager.getRunsStatus();
	}

}
