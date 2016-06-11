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

import com.qwazr.utils.LockUtils.ReadWriteLock;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.server.ServerBuilder;
import com.qwazr.utils.server.ServerException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class ScriptManager {

	public static final String SERVICE_NAME_SCRIPT = "scripts";

	private static final Logger logger = LoggerFactory.getLogger(ScriptManager.class);

	static ScriptManager INSTANCE = null;

	public synchronized static void load(final ServerBuilder serverBuilder) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		try {
			INSTANCE = new ScriptManager(serverBuilder.getExecutorService(),
					serverBuilder.getServerConfiguration().dataDirectory);
			if (serverBuilder != null)
				serverBuilder.registerWebService(ScriptServiceImpl.class);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	public static ScriptManager getInstance() {
		if (INSTANCE == null)
			throw new RuntimeException("The scripts service is not enabled");
		return INSTANCE;
	}

	private final ScriptEngine scriptEngine;

	private final ReadWriteLock runsMapLock = new ReadWriteLock();
	private final HashMap<String, RunThreadAbstract> runsMap;

	final ExecutorService executorService;
	final File dataDir;

	private ScriptManager(ExecutorService executorService, File rootDirectory) throws IOException, URISyntaxException {

		dataDir = rootDirectory;
		// Load Nashorn
		ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");
		runsMap = new HashMap<>();
		this.executorService = executorService;
	}

	private File getScriptFile(String scriptPath) throws ServerException {
		if (StringUtils.isEmpty(scriptPath))
			throw new ServerException(Status.NOT_ACCEPTABLE, "No path given");
		final File scriptFile = new File(dataDir, scriptPath);
		if (!scriptFile.exists())
			throw new ServerException(Status.NOT_FOUND, "Script not found: " + scriptPath);
		if (!scriptFile.isFile())
			throw new ServerException(Status.NOT_ACCEPTABLE, "Script is not a file: " + scriptPath);
		return scriptFile;
	}

	public String getScript(String scriptPath) throws IOException, ServerException {
		File scriptFile = getScriptFile(scriptPath);
		FileReader fileReader = new FileReader(scriptFile);
		try {
			return IOUtils.toString(fileReader);
		} finally {
			IOUtils.closeQuietly(fileReader);
		}
	}

	private RunThreadAbstract getNewScriptRunThread(final String scriptPath, final Map<String, ?> objects)
			throws ServerException, IOException, ClassNotFoundException {
		final RunThreadAbstract scriptRunThread;
		if (scriptPath.endsWith(".js"))
			scriptRunThread = new JsRunThread(scriptEngine, getScriptFile(scriptPath), objects);
		else
			scriptRunThread = new JavaRunThread(scriptPath, objects);
		addScriptRunThread(scriptRunThread);
		return scriptRunThread;
	}

	public RunThreadAbstract runSync(String scriptPath, Map<String, ?> objects)
			throws ServerException, IOException, ClassNotFoundException {
		if (logger.isInfoEnabled())
			logger.info("Run sync: " + scriptPath);
		RunThreadAbstract scriptRunThread = getNewScriptRunThread(scriptPath, objects);
		scriptRunThread.run();
		expireScriptRunThread();
		return scriptRunThread;
	}

	public ScriptRunStatus runAsync(final String scriptPath, final Map<String, ?> objects)
			throws ServerException, IOException, ClassNotFoundException {
		if (logger.isInfoEnabled())
			logger.info("Run async: " + scriptPath);
		RunThreadAbstract scriptRunThread = getNewScriptRunThread(scriptPath, objects);
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
			if (logger.isInfoEnabled())
				logger.info("Expire " + uuidsToDelete.size() + " jobs");
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
