/*
 * Copyright 2015-2018 Emmanuel Keller / QWAZR
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.logging.Logger;

public class ScriptManager {

	private static final Logger LOGGER = LoggerUtils.getLogger(ScriptManager.class);

	private final ReadWriteLock runsMapLock = ReadWriteLock.stamped();
	private final HashMap<String, RunThreadAbstract> runsMap;

	private final ExecutorService executorService;
	private final ScriptEngine scriptEngine;

	private final String myAddress;
	private final LibraryServiceInterface libraryService;

	private final ScriptServiceInterface service;

	private final Function<String, Path> pathResolver;

	public ScriptManager(final ExecutorService executorService, final String myAddress,
			final LibraryServiceInterface libraryService, final Path rootDirectory) {
		this.executorService = executorService;
		this.libraryService = libraryService;
		this.myAddress = myAddress;
		this.scriptEngine = Objects.requireNonNull(initScriptEngine(), "No javascript engine found");
		LOGGER.info("Init scriptEngine: " + scriptEngine);
		this.pathResolver = rootDirectory == null ? Paths::get : rootDirectory::resolve;
		this.runsMap = new HashMap<>();
		this.service = new ScriptServiceImpl(this);
	}

	private static ScriptEngine initScriptEngine() {
		final ScriptEngineManager manager = new ScriptEngineManager(Thread.currentThread().getContextClassLoader());
		ScriptEngine scriptEngine = manager.getEngineByName("graal.js");
		if (scriptEngine != null)
			return scriptEngine;
		return manager.getEngineByName("nashorn");
	}

	public ScriptManager(final ExecutorService executorService, final Path rootDirectory) {
		this(executorService, (String) null, null, rootDirectory);
	}

	public ScriptManager(final ExecutorService executorService, final ClusterManager clusterManager,
			final LibraryServiceInterface libraryService, final Path rootDirectory) {
		this(executorService, clusterManager.getService().getStatus().me, libraryService, rootDirectory);
	}

	public ScriptServiceInterface getService() {
		return service;
	}

	private Path getScriptFilePath(String scriptPath) {
		if (StringUtils.isEmpty(scriptPath))
			throw new ServerException(Status.NOT_ACCEPTABLE, "No path given");
		final Path scriptFilePath = pathResolver.apply(scriptPath);
		if (!Files.exists(scriptFilePath))
			throw new ServerException(Status.NOT_FOUND, "Script not found: " + scriptFilePath.toAbsolutePath());
		if (!Files.isRegularFile(scriptFilePath))
			throw new ServerException(Status.NOT_ACCEPTABLE,
					"Script is not a regular file: " + scriptFilePath.toAbsolutePath());
		return scriptFilePath;
	}

	private RunThreadAbstract getNewScriptRunThread(final String scriptPath, final Map<String, ?> objects) {
		final RunThreadAbstract scriptRunThread;
		if (scriptPath.endsWith(".js"))
			scriptRunThread =
					new JsRunThread(myAddress, scriptEngine, libraryService, getScriptFilePath(scriptPath), objects);
		else
			scriptRunThread = new JavaRunThread(myAddress, libraryService, scriptPath, objects);
		addScriptRunThread(scriptRunThread);
		return scriptRunThread;
	}

	RunThreadAbstract runSync(String scriptPath, Map<String, ?> objects) {
		LOGGER.info(() -> "Run sync: " + scriptPath);
		final RunThreadAbstract scriptRunThread = getNewScriptRunThread(scriptPath, objects);
		scriptRunThread.run();
		expireScriptRunThread();
		return scriptRunThread;
	}

	ScriptRunStatus runAsync(final String scriptPath, final Map<String, ?> objects) {
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
