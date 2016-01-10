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
import com.qwazr.connectors.ConnectorManagerImpl;
import com.qwazr.store.data.StoreDataManager;
import com.qwazr.tools.ToolsManagerImpl;
import com.qwazr.utils.LockUtils.ReadWriteLock;
import com.qwazr.utils.StringUtils;
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
import java.util.concurrent.Executors;

public class ScriptManager {

	private static final Logger logger = LoggerFactory.getLogger(ScriptManager.class);

	public static volatile ScriptManager INSTANCE = null;

	public static void load(File directory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		try {
			INSTANCE = new ScriptManager(directory);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	private final ScriptEngine scriptEngine;

	private final ReadWriteLock runsMapLock = new ReadWriteLock();
	private final HashMap<String, ScriptRunThread> runsMap;

	private final ReadWriteLock semaphoreMapLock = new ReadWriteLock();
	private final HashMap<String, Set<String>> semaphoreMap;

	private final ExecutorService scriptExecutorService;
	private final ExecutorService clientExecutorService;

	private ScriptManager(File rootDirectory) throws IOException, URISyntaxException {

		// Load Nashorn
		ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");

		runsMap = new HashMap<String, ScriptRunThread>();
		semaphoreMap = new HashMap<String, Set<String>>();
		scriptExecutorService = Executors.newFixedThreadPool(100);
		clientExecutorService = Executors.newFixedThreadPool(8);
	}

	private File getScriptFile(String scriptPath) throws ServerException {
		if (StringUtils.isEmpty(scriptPath))
			throw new ServerException(Status.NOT_ACCEPTABLE, "No path given");
		final File scriptFile;
		if (scriptPath.startsWith("/")) {
			if (StoreDataManager.INSTANCE == null)
				throw new ServerException(Status.INTERNAL_SERVER_ERROR, "No store available: " + scriptPath);
			scriptFile = StoreDataManager.INSTANCE.getFile(scriptPath);
		} else
			scriptFile = new File(scriptPath);
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

	private ScriptRunThread getNewScriptRunThread(String scriptPath, Map<String, ?> objects)
			throws ServerException, IOException {
		ScriptRunThread scriptRunThread = new ScriptRunThread(scriptEngine, getScriptFile(scriptPath), objects,
				ConnectorManagerImpl.getInstance(), ToolsManagerImpl.getInstance());
		addScriptRunThread(scriptPath, scriptRunThread);
		return scriptRunThread;
	}

	public ScriptRunThread runSync(String scriptPath, Map<String, ?> objects) throws ServerException, IOException {
		if (logger.isInfoEnabled())
			logger.info("Run sync: " + scriptPath);
		ScriptRunThread scriptRunThread = getNewScriptRunThread(scriptPath, objects);
		scriptRunThread.run();
		expireScriptRunThread();
		return scriptRunThread;
	}

	public ScriptRunStatus runAsync(String scriptPath, Map<String, ?> objects) throws ServerException, IOException {
		if (logger.isInfoEnabled())
			logger.info("Run async: " + scriptPath);
		ScriptRunThread scriptRunThread = getNewScriptRunThread(scriptPath, objects);
		scriptExecutorService.execute(scriptRunThread);
		expireScriptRunThread();
		return scriptRunThread.getStatus();
	}

	private void addScriptRunThread(String scriptPath, ScriptRunThread scriptRunThread) {
		if (scriptRunThread == null)
			return;
		runsMapLock.w.lock();
		try {
			runsMap.put(scriptRunThread.getUUID(), scriptRunThread);
		} finally {
			runsMapLock.w.unlock();
		}
	}

	private void expireScriptRunThread() {
		runsMapLock.w.lock();
		try {
			List<String> uuidsToDelete = new ArrayList<String>();
			long currentTime = System.currentTimeMillis();
			for (ScriptRunThread scriptRunThread : runsMap.values())
				if (scriptRunThread.hasExpired(currentTime))
					uuidsToDelete.add(scriptRunThread.getUUID());
			for (String uuid : uuidsToDelete)
				runsMap.remove(uuid);
			if (logger.isInfoEnabled())
				logger.info("Expire " + uuidsToDelete.size() + " jobs");
		} finally {
			runsMapLock.w.unlock();
		}
	}

	Map<String, ScriptRunStatus> getRunsStatus() {
		runsMapLock.r.lock();
		try {
			LinkedHashMap<String, ScriptRunStatus> runStatusMap = new LinkedHashMap<String, ScriptRunStatus>();
			for (Map.Entry<String, ScriptRunThread> entry : runsMap.entrySet())
				runStatusMap.put(entry.getKey(), entry.getValue().getStatus());
			return runStatusMap;
		} finally {
			runsMapLock.r.unlock();
		}
	}

	ScriptRunThread getRunThread(String uuid) {
		runsMapLock.r.lock();
		try {
			return runsMap.get(uuid);
		} finally {
			runsMapLock.r.unlock();
		}
	}

	public ScriptServiceInterface getNewClient(Integer msTimeout) throws URISyntaxException {
		if (!ClusterManager.INSTANCE.isCluster())
			return new ScriptServiceImpl();
		return new ScriptMultiClient(clientExecutorService,
				ClusterManager.INSTANCE.getClusterClient().getActiveNodesByService(ScriptsServer.SERVICE_NAME_SCRIPT),
				msTimeout);
	}

	void getSemaphores(Collection<String> semaphores) {
		semaphoreMapLock.r.lock();
		try {
			semaphores.addAll(semaphoreMap.keySet());
		} finally {
			semaphoreMapLock.r.unlock();
		}
	}

	void getSemaphoreOwners(String semaphore_id, Collection<String> owners) {
		semaphoreMapLock.r.lock();
		try {
			Set<String> ows = semaphoreMap.get(semaphore_id);
			if (ows == null)
				return;
			for (String owner : ows)
				owners.add(owner);
		} finally {
			semaphoreMapLock.r.unlock();
		}
	}

	void registerSemaphore(String semaphore_id, String script_id) {
		semaphoreMapLock.w.lock();
		try {
			if (logger.isInfoEnabled())
				logger.info("Register semaphore: " + semaphore_id + " to scripts: " + script_id);
			Set<String> owners = semaphoreMap.get(semaphore_id);
			if (owners == null) {
				owners = new HashSet<String>();
				semaphoreMap.put(semaphore_id, owners);
			}
			owners.add(script_id);
		} finally {
			semaphoreMapLock.w.unlock();
		}
	}

	void unregisterSemaphore(String semaphore_id, String script_id) {
		semaphoreMapLock.w.lock();
		try {
			if (logger.isInfoEnabled())
				logger.info("Unregister semaphore: " + semaphore_id + " to scripts: " + script_id);
			Set<String> owners = semaphoreMap.get(semaphore_id);
			if (owners == null)
				return;
			owners.remove(script_id);
			if (owners.isEmpty())
				semaphoreMap.remove(semaphore_id);
		} finally {
			semaphoreMapLock.w.unlock();
		}
	}

}
