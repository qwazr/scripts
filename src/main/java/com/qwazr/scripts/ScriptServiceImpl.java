/*
 * Copyright 2015-2020 Emmanuel Keller / QWAZR
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

import com.qwazr.server.AbstractServiceImpl;
import com.qwazr.server.ServerException;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LoggerUtils;

import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

class ScriptServiceImpl extends AbstractServiceImpl implements ScriptServiceInterface {

	private static final Logger LOGGER = LoggerUtils.getLogger(ScriptServiceImpl.class);

	private final ScriptManager scriptManager;

	ScriptServiceImpl(final ScriptManager scriptManager) {
		this.scriptManager = scriptManager;
	}

	@Override
	public List<ScriptRunStatus<?>> runScript(final String scriptPath, final String group, final TargetRuleEnum rule) {
		return runScriptVariables(scriptPath, group, rule, null);
	}

	@Override
	public List<ScriptRunStatus<?>> runScriptVariables(final String scriptPath, final String group,
			final TargetRuleEnum rule, final Map<String, String> variables) {
		try {
			return Collections.singletonList(scriptManager.runAsync(scriptPath, variables));
		} catch (Exception e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	private RunThreadAbstract<?> getRunThread(final String runId) throws ServerException {
		final RunThreadAbstract<?> runThread = scriptManager.getRunThread(runId);
		if (runThread == null)
			throw new ServerException(Status.NOT_FOUND, "Running script not found: " + runId);
		return runThread;
	}

	@Override
	public ScriptRunStatus<?> getRunStatus(final String runId) {
		try {
			return getRunThread(runId).getStatus();
		} catch (ServerException e) {
			throw ServerException.getJsonException(LOGGER, e);
		}
	}

	@Override
	public InputStream getRunOut(final String runId) {
		try {
			return IOUtils.toInputStream(getRunThread(runId).getOut(), StandardCharsets.UTF_8);
		} catch (ServerException e) {
			throw ServerException.getTextException(LOGGER, e);
		}
	}

	@Override
	public InputStream getRunErr(final String runId) {
		try {
			return IOUtils.toInputStream(getRunThread(runId).getErr(), StandardCharsets.UTF_8);
		} catch (ServerException e) {
			throw ServerException.getTextException(LOGGER, e);
		}
	}

	@Override
	public Map<String, ScriptRunStatus<?>> getRunsStatus() {
		return scriptManager.getRunsStatus();
	}

	@Override
	public RunThreadAbstract<?> runSync(String scriptPath, Map<String, ?> objects) {
		return scriptManager.runSync(scriptPath, objects);
	}

	@Override
	public ScriptRunStatus<?> runAsync(final String scriptPath, final Map<String, ?> objects) {
		return scriptManager.runAsync(scriptPath, objects);
	}

}
