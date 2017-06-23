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

import com.qwazr.utils.HashUtils;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LoggerUtils;
import com.qwazr.utils.StringUtils;

import java.io.Closeable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class RunThreadAbstract implements ScriptRunThread, Runnable, Closeable {

	private static final Logger logger = LoggerUtils.getLogger(RunThreadAbstract.class);

	private volatile ScriptRunStatus.ScriptState state;
	private volatile Long startTime;
	private volatile Long endTime;
	private volatile Long expirationTime;
	private volatile Exception exception;

	protected final String httpAddressKey;

	private final String scriptName;
	private final Map<String, ?> initialVariables;
	protected final String uuid;

	protected final IOUtils.CloseableList closeables;
	protected final Writer outputWriter;
	protected final Writer errorWriter;

	protected RunThreadAbstract(String httpAddressKey, String scriptName, Map<String, ?> initialVariables) {
		this.httpAddressKey = httpAddressKey;
		this.scriptName = scriptName;
		this.initialVariables = initialVariables;
		uuid = HashUtils.newTimeBasedUUID().toString();
		state = ScriptRunStatus.ScriptState.ready;
		startTime = null;
		endTime = null;
		expirationTime = null;
		this.closeables = new IOUtils.CloseableList();
		this.outputWriter = new StringWriter();
		this.errorWriter = new StringWriter();
	}

	@Override
	final public void close() {
		IOUtils.close(outputWriter, errorWriter);
	}

	@Override
	final public Exception getException() {
		return exception;
	}

	@Override
	final public String getUUID() {
		return uuid;
	}

	@Override
	final public ScriptRunStatus getStatus() {
		return new ScriptRunStatus(httpAddressKey, scriptName, uuid, state, startTime, endTime,
				initialVariables == null ? null : initialVariables.keySet(), exception);
	}

	@Override
	final public boolean hasExpired(long currentTime) {
		if (expirationTime == null)
			return false;
		return expirationTime < currentTime;
	}

	final public String getOut() {
		return outputWriter == null ? StringUtils.EMPTY : outputWriter.toString();
	}

	final public String getErr() {
		return errorWriter == null ? StringUtils.EMPTY : errorWriter.toString();
	}

	protected abstract void runner() throws Exception;

	@Override
	final public void run() {
		logger.info("Execute: " + scriptName);
		state = ScriptRunStatus.ScriptState.running;
		startTime = System.currentTimeMillis();
		try {
			runner();
			state = ScriptRunStatus.ScriptState.terminated;
		} catch (Exception e) {
			state = ScriptRunStatus.ScriptState.error;
			exception = e;
			logger.log(Level.SEVERE, e, () -> "Error on " + scriptName + " - " + e.getMessage());
		} finally {
			endTime = System.currentTimeMillis();
			expirationTime = endTime + 2 * 60 * 1000;
			closeables.close();
		}
	}
}
