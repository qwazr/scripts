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

import com.datastax.driver.core.utils.UUIDs;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.FileReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

abstract class RunThreadAbstract implements ScriptRunThread, Runnable, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(RunThreadAbstract.class);

	private volatile ScriptRunStatus.ScriptState state;
	private volatile Long startTime;
	private volatile Long endTime;
	private volatile Long expirationTime;
	private volatile Exception exception;

	private final String scriptName;
	private final Map<String, ?> initialVariables;
	protected final String uuid;

	protected final IOUtils.CloseableList closeables;
	protected final Writer outWriter;
	protected final Writer errorWriter;

	protected RunThreadAbstract(String scriptName, Map<String, ?> initialVariables) {
		this.scriptName = scriptName;
		this.initialVariables = initialVariables;
		uuid = UUIDs.timeBased().toString();
		state = ScriptRunStatus.ScriptState.ready;
		startTime = null;
		endTime = null;
		expirationTime = null;
		this.closeables = new IOUtils.CloseableList();
		this.outWriter = new StringWriter();
		this.errorWriter = new StringWriter();
	}

	final public void close() {
		IOUtils.close(outWriter, errorWriter);
	}

	@Override
	final public Exception getException() {
		return exception;
	}

	@Override
	final public String getUUID() {
		return uuid;
	}

	final public ScriptRunStatus.ScriptState getState() {
		return state;
	}

	@Override
	final public ScriptRunStatus getStatus() {
		return new ScriptRunStatus(ClusterManager.INSTANCE.myAddress, scriptName, uuid, state, startTime, endTime,
				initialVariables == null ? null : initialVariables.keySet(), exception);
	}

	@Override
	final public boolean hasExpired(long currentTime) {
		if (expirationTime == null)
			return false;
		return expirationTime < currentTime;
	}

	final public String getOut() {
		return outWriter == null ? StringUtils.EMPTY : outWriter.toString();
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
		FileReader fileReader = null;
		try {
			runner();
			state = ScriptRunStatus.ScriptState.terminated;
		} catch (Exception e) {
			state = ScriptRunStatus.ScriptState.error;
			exception = e;
			logger.error("Error on " + scriptName + " - " + e.getMessage(), e);
		} finally {
			endTime = System.currentTimeMillis();
			expirationTime = endTime + 2 * 60 * 1000;
			closeables.close();
		}
	}
}
