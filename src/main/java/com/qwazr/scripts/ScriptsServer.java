/**
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
import com.qwazr.database.TableManager;
import com.qwazr.database.TableServiceInterface;
import com.qwazr.library.LibraryManager;
import com.qwazr.server.BaseServer;
import com.qwazr.server.GenericServer;
import com.qwazr.server.WelcomeShutdownService;
import com.qwazr.server.configuration.ServerConfiguration;
import com.qwazr.utils.reflection.InstancesSupplier;

import javax.management.MBeanException;
import javax.management.OperationsException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScriptsServer implements BaseServer {

	private final GenericServer server;
	private final ScriptManager scriptManager;
	private final ScriptServiceBuilder serviceBuilder;

	private ScriptsServer(final ServerConfiguration configuration) throws IOException, URISyntaxException {
		final ExecutorService executorService = Executors.newCachedThreadPool();
		final GenericServer.Builder builder = GenericServer.of(configuration, executorService);
		final ClusterManager clusterManager =
				new ClusterManager(executorService, configuration).registerHttpClientMonitoringThread(builder)
																  .registerProtocolListener(builder)
																  .registerWebService(builder);
		final TableManager tableManager = new TableManager(
				builder.getConfiguration().dataDirectory.toPath().resolve(TableServiceInterface.SERVICE_NAME))
				.registerContextAttribute(builder).registerShutdownListener(builder);
		final InstancesSupplier instancesSupplier = InstancesSupplier.withConcurrentMap();
		instancesSupplier.registerInstance(TableServiceInterface.class, tableManager.getService());
		final LibraryManager libraryManager =
				new LibraryManager(configuration.dataDirectory, configuration.getEtcFiles(), instancesSupplier)
						.registerWebService(builder).registerIdentityManager(builder);
		scriptManager = new ScriptManager(executorService, clusterManager, libraryManager, configuration.dataDirectory)
				.registerWebService(builder);
		serviceBuilder = new ScriptServiceBuilder(clusterManager, scriptManager);
		builder.webService(WelcomeShutdownService.class);
		server = builder.build();
	}

	public ScriptManager getScriptManager() {
		return scriptManager;
	}

	public ScriptServiceBuilder getServiceBuilder() {
		return serviceBuilder;
	}

	@Override
	public GenericServer getServer() {
		return server;
	}

	private static volatile ScriptsServer INSTANCE;

	public static ScriptsServer getInstance() {
		return INSTANCE;
	}

	public static synchronized void main(final String... args)
			throws IOException, ReflectiveOperationException, OperationsException, ServletException, MBeanException,
			URISyntaxException, InterruptedException {
		if (INSTANCE != null)
			shutdown();
		INSTANCE = new ScriptsServer(new ServerConfiguration(args));
		INSTANCE.start();
	}

	public static synchronized void shutdown() {
		if (INSTANCE != null)
			INSTANCE.stop();
		INSTANCE = null;
	}

}