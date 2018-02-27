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

import com.qwazr.cluster.ClusterManager;
import com.qwazr.library.LibraryServiceInterface;
import com.qwazr.server.ServerException;
import com.qwazr.utils.LoggerUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.concurrent.ReadWriteLock;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class ScriptManager {

	private static final Logger LOGGER = LoggerUtils.getLogger(ScriptManager.class);

	private final ReadWriteLock runsMapLock = ReadWriteLock.stamped();
	private final HashMap<String, RunThreadAbstract> runsMap;

	private final ExecutorService executorService;
	private final ScriptEngine scriptEngine;

	final String myAddress;
	final LibraryServiceInterface libraryService;

	private final ScriptServiceInterface service;

	private final File dataDir;

	public ScriptManager(final ExecutorService executorService, final String myAddress,
			final LibraryServiceInterface libraryService, final File rootDirectory) {
		this.executorService = executorService;
		this.libraryService = libraryService;
		this.myAddress = myAddress;

		final ScriptEngineManager manager = new ScriptEngineManager(Thread.currentThread().getContextClassLoader());
		scriptEngine = manager.getEngineByName("nashorn");

		dataDir = rootDirectory;
		runsMap = new HashMap<>();
		service = new ScriptServiceImpl(this);
	}

	public ScriptManager(final ExecutorService executorService, final File rootDirectory) {
		this(executorService, (String) null, null, rootDirectory);
	}

	public ScriptManager(final ExecutorService executorService, final ClusterManager clusterManager,
			final LibraryServiceInterface libraryService, final File rootDirectory) {
		this(executorService, clusterManager.getService().getStatus().me, libraryService, rootDirectory);
	}

	ScriptEngine getScriptEngine() {
		return scriptEngine;
	}

	public ScriptServiceInterface getService() {
		return service;
	}

	private File getScriptFile(String scriptPath) {
		if (StringUtils.isEmpty(scriptPath))
			throw new ServerException(Status.NOT_ACCEPTABLE, "No path given");
		final File scriptFile = new File(dataDir, scriptPath);
		if (!scriptFile.exists())
			throw new ServerException(Status.NOT_FOUND, "Script not found: " + scriptPath);
		if (!scriptFile.isFile())
			throw new ServerException(Status.NOT_ACCEPTABLE, "Script is not a file: " + scriptPath);
		return scriptFile;
	}

	private RunThreadAbstract getNewScriptRunThread(final String scriptPath, final Map<String, ?> objects) {
		final RunThreadAbstract scriptRunThread;
		if (scriptPath.endsWith(".js"))
			scriptRunThread = new JsRunThread(this, getScriptFile(scriptPath), objects);
		else
			scriptRunThread = new JavaRunThread(this, libraryService, scriptPath, objects);
		addScriptRunThread(scriptRunThread);
		return scriptRunThread;
	}

	RunThreadAbstract runSync(String scriptPath, Map<String, ?> objects) throws IOException, ClassNotFoundException {
		LOGGER.info(() -> "Run sync: " + scriptPath);
		final RunThreadAbstract scriptRunThread = getNewScriptRunThread(scriptPath, objects);
		scriptRunThread.run();
		expireScriptRunThread();
		return scriptRunThread;
	}

	ScriptRunStatus runAsync(final String scriptPath, final Map<String, ?> objects)
			throws IOException, ClassNotFoundException {
		LOGGER.info(() -> "Run async: " + scriptPath);
		final RunThreadAbstract scriptRunThread = getNewScriptRunThread(scriptPath, objects);
		executorService.execute(scriptRunThread);
		expireScriptRunThread();
		return scriptRunThread.getStatus();
	}

	private void addScriptRunThread(final RunThreadAbstract scriptRunThread) {
		if (scriptRunThread == null)
			return;
		runsMapLock.write(() -> runsMap.put(scriptRunThread.getUUID(), scriptRunThread));
	}

	private void expireScriptRunThread() {
		runsMapLock.write(() -> {
			final List<String> uuidsToDelete = new ArrayList<>();
			final long currentTime = System.currentTimeMillis();
			runsMap.forEach((s, scriptRunThread) -> {
				if (scriptRunThread.hasExpired(currentTime))
					uuidsToDelete.add(scriptRunThread.getUUID());
			});
			uuidsToDelete.forEach(runsMap::remove);
			LOGGER.info(() -> "Expire " + uuidsToDelete.size() + " jobs");
		});
	}

	Map<String, ScriptRunStatus> getRunsStatus() {
		return runsMapLock.read(() -> {
			final LinkedHashMap<String, ScriptRunStatus> runStatusMap = new LinkedHashMap<>();
			runsMap.forEach((key, runThreadAbstract) -> runStatusMap.put(key, runThreadAbstract.getStatus()));
			return runStatusMap;
		});
	}

	RunThreadAbstract getRunThread(final String uuid) {
		return runsMapLock.read(() -> runsMap.get(uuid));
	}

}
