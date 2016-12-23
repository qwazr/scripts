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

import com.qwazr.classloader.ClassLoaderManager;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.library.LibraryManager;
import com.qwazr.server.GenericServer;
import com.qwazr.server.RemoteService;
import com.qwazr.server.ServerException;
import com.qwazr.utils.LockUtils.ReadWriteLock;
import com.qwazr.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;

public class ScriptManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptManager.class);

	private static ScriptEngine scriptEngine;

	// Load Nashorn
	static {
		final ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");
	}

	private final ReadWriteLock runsMapLock = new ReadWriteLock();
	private final HashMap<String, RunThreadAbstract> runsMap;

	private final ExecutorService executorService;
	final LibraryManager libraryManager;
	final ClassLoaderManager classLoaderManager;
	final ClusterManager clusterManager;
	private final ScriptServiceInterface service;

	private final File dataDir;

	public ScriptManager(final ExecutorService executorService, final ClassLoaderManager classLoaderManager,
			final ClusterManager clusterManager, final LibraryManager libraryManager, final File rootDirectory)
			throws IOException, URISyntaxException {
		this.executorService = executorService;
		this.classLoaderManager = classLoaderManager;
		this.clusterManager = clusterManager;
		this.libraryManager = libraryManager;
		dataDir = rootDirectory;
		runsMap = new HashMap<>();
		service = new ScriptServiceImpl(this);
	}

	public ScriptManager(final ExecutorService executorService, final ClassLoaderManager classLoaderManager,
			final ClusterManager clusterManager, final LibraryManager libraryManager,
			final GenericServer.Builder builder) throws IOException, URISyntaxException {
		this(executorService, classLoaderManager, clusterManager, libraryManager,
				builder.getConfiguration().dataDirectory);
		builder.webService(ScriptServiceImpl.class);
		builder.contextAttribute(this);
	}

	ScriptEngine getScriptEngine() {
		return scriptEngine;
	}

	public ScriptServiceInterface getService() {
		return service;
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

	private RunThreadAbstract getNewScriptRunThread(final String scriptPath, final Map<String, ?> objects)
			throws ServerException, IOException, ClassNotFoundException {
		final RunThreadAbstract scriptRunThread;
		if (scriptPath.endsWith(".js"))
			scriptRunThread = new JsRunThread(this, getScriptFile(scriptPath), objects);
		else
			scriptRunThread = new JavaRunThread(this, scriptPath, objects);
		addScriptRunThread(scriptRunThread);
		return scriptRunThread;
	}

	public RunThreadAbstract runSync(String scriptPath, Map<String, ?> objects)
			throws ServerException, IOException, ClassNotFoundException {
		if (LOGGER.isInfoEnabled())
			LOGGER.info("Run sync: " + scriptPath);
		RunThreadAbstract scriptRunThread = getNewScriptRunThread(scriptPath, objects);
		scriptRunThread.run();
		expireScriptRunThread();
		return scriptRunThread;
	}

	public ScriptRunStatus runAsync(final String scriptPath, final Map<String, ?> objects)
			throws ServerException, IOException, ClassNotFoundException {
		if (LOGGER.isInfoEnabled())
			LOGGER.info("Run async: " + scriptPath);
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
			if (LOGGER.isInfoEnabled())
				LOGGER.info("Expire " + uuidsToDelete.size() + " jobs");
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

	/**
	 * Return a script service client
	 *
	 * @param local set true to require a local client. False to avoid a local client. Null to let the method decide.
	 * @param group an optional group
	 * @return a script service client
	 * @throws URISyntaxException
	 */
	public ScriptServiceInterface getClient(final Boolean local, final String group) throws URISyntaxException {
		if (local != null && local)
			return service;
		final SortedSet<String> nodes =
				clusterManager.getNodesByGroupByService(group, ScriptServiceInterface.SERVICE_NAME);
		if (nodes == null)
			throw new WebApplicationException("The script service is not available");
		if (nodes.size() == 0)
			throw new WebApplicationException("No available script node for the group: " + group,
					Response.Status.EXPECTATION_FAILED);
		if (nodes.size() == 1) {
			final String node = nodes.first();
			if (local == null && clusterManager.getHttpAddressKey().equals(node))
				return service;
			return new ScriptSingleClient(new RemoteService(node));
		}
		return new ScriptMultiClient(RemoteService.build(nodes));
	}
}
