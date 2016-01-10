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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.connectors.ConnectorManager;
import com.qwazr.scripts.ScriptRunStatus.ScriptState;
import com.qwazr.semaphores.SemaphoresManager;
import com.qwazr.tools.ToolsManager;
import com.qwazr.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonInclude(Include.NON_EMPTY)
public class ScriptRunThread extends SimpleScriptContext implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ScriptRunThread.class);

	private final String uuid;
	private volatile ScriptState state;
	private volatile Long startTime;
	private volatile Long endTime;
	private volatile Long expirationTime;
	private volatile Exception exception;

	private final Set<String> semaphores;
	private final IOUtils.CloseableList closeables;

	private final Map<String, ?> bindings;
	private final ScriptEngine scriptEngine;
	private final File scriptFile;

	ScriptRunThread(ScriptEngine scriptEngine, File scriptFile, Map<String, ?> bindings, ConnectorManager connectors,
			ToolsManager tools) {
		uuid = UUIDs.timeBased().toString();
		state = ScriptState.ready;
		startTime = null;
		endTime = null;
		expirationTime = null;
		this.globalScope = new GlobalBindings();
		this.bindings = bindings;
		this.scriptEngine = scriptEngine;
		this.semaphores = new HashSet<String>();
		this.closeables = new IOUtils.CloseableList();
		if (bindings != null)
			engineScope.putAll(bindings);
		if (connectors != null)
			engineScope.put("connectors", connectors);
		if (tools != null)
			engineScope.put("tools", tools);
		engineScope.put("closeable", closeables);
		this.scriptFile = scriptFile;
		this.setWriter(new StringWriter());
		this.setErrorWriter(new StringWriter());
		removeAttributeIfAny("quit", "exit");
	}

	private void removeAttributeIfAny(String... names) {
		if (names == null)
			return;
		for (String name : names) {
			int scope = getAttributesScope(name);
			if (scope != -1)
				removeAttribute(name, scope);
		}
	}

	@Override
	public void run() {
		logger.info("Execute: " + scriptFile.getName());
		state = ScriptState.running;
		startTime = System.currentTimeMillis();
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(scriptFile);
			scriptEngine.eval(fileReader, this);
			state = ScriptState.terminated;
		} catch (Exception e) {
			state = ScriptState.error;
			exception = e;
			logger.error("Error on " + scriptFile.getName() + " - " + e.getMessage(), e);
		} finally {
			endTime = System.currentTimeMillis();
			expirationTime = endTime + 2 * 60 * 1000;
			closeables.close();
			if (fileReader != null)
				IOUtils.closeQuietly(fileReader);
			for (String semaphore : semaphores)
				SemaphoresManager.getInstance().unregister(semaphore, uuid);
		}
	}

	public Exception getException() {
		return exception;
	}

	public String getUUID() {
		return uuid;
	}

	public String getOut() {
		return getWriter().toString();
	}

	public String getErr() {
		return getErrorWriter().toString();
	}

	ScriptState getState() {
		return state;
	}

	public ScriptRunStatus getStatus() {
		return new ScriptRunStatus(ClusterManager.INSTANCE.myAddress, scriptFile.getName(), uuid, state, startTime,
				endTime, bindings == null ? null : bindings.keySet(), exception);
	}

	boolean hasExpired(long currentTime) {
		if (expirationTime == null)
			return false;
		return expirationTime < currentTime;
	}

	public class GlobalBindings extends HashMap<String, Object> implements Bindings {

		/**
		 *
		 */
		private final long serialVersionUID = -7250097260119419346L;

		private GlobalBindings() {
			this.put("console", new ScriptConsole());
			this.put("semaphore", new ScriptSemaphore());
		}

		public void sleep(int msTimeout) throws InterruptedException {
			Thread.sleep(msTimeout);
		}
	}

	public class ScriptSemaphore {

		public Set<String> owners(String semaphore_id, Boolean local, Integer timeOut) {
			return SemaphoresManager.getService().getSemaphoreOwners(semaphore_id);
		}

		public void register(String semaphore_id) {
			synchronized (semaphores) {
				SemaphoresManager.getInstance().register(semaphore_id, uuid);
				semaphores.add(semaphore_id);
			}
		}

		public void unregister(String semaphore_id) {
			synchronized (semaphores) {
				SemaphoresManager.getInstance().unregister(semaphore_id, uuid);
				semaphores.remove(semaphore_id);
			}
		}
	}
}
