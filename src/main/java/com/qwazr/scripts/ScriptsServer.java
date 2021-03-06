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

import com.qwazr.cluster.ClusterManager;
import com.qwazr.cluster.ClusterServiceInterface;
import com.qwazr.library.LibraryManager;
import com.qwazr.library.LibraryServiceInterface;
import com.qwazr.server.ApplicationBuilder;
import com.qwazr.server.BaseServer;
import com.qwazr.server.GenericServer;
import com.qwazr.server.GenericServerBuilder;
import com.qwazr.server.RestApplication;
import com.qwazr.server.WelcomeShutdownService;
import com.qwazr.server.configuration.ServerConfiguration;

import javax.management.JMException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScriptsServer implements BaseServer {

	private final GenericServer server;
	private final ScriptServiceBuilder scriptServiceBuilder;

	private ScriptsServer(final ServerConfiguration configuration) throws IOException {
		final ExecutorService executorService = Executors.newCachedThreadPool();
		final GenericServerBuilder builder = GenericServer.of(configuration, executorService);
		final Set<String> services = new HashSet<>();
		services.add(ClusterServiceInterface.SERVICE_NAME);
		services.add(ScriptServiceInterface.SERVICE_NAME);
		services.add(LibraryServiceInterface.SERVICE_NAME);
		final ApplicationBuilder webServices = ApplicationBuilder.of("/*")
				.classes(RestApplication.JSON_CLASSES)
				.singletons(new WelcomeShutdownService());

		final ClusterManager clusterManager =
				new ClusterManager(executorService, configuration).registerProtocolListener(builder, services);
		webServices.singletons(clusterManager.getService());

		final LibraryManager libraryManager =
				new LibraryManager(configuration.dataDirectory, configuration.getEtcFiles(), null);
		builder.shutdownListener(server -> libraryManager.close());
		final LibraryServiceInterface libraryService = libraryManager.getService();
		webServices.singletons(libraryService);

		final ScriptManager scriptManager =
				new ScriptManager(executorService, clusterManager, libraryService, configuration.dataDirectory);
		webServices.singletons(scriptManager.getService());
		scriptServiceBuilder = new ScriptServiceBuilder(executorService, clusterManager, scriptManager);

		builder.getWebServiceContext().jaxrs(webServices);
		server = builder.build();
	}

	public ScriptServiceBuilder getScriptServiceBuilder() {
		return scriptServiceBuilder;
	}

	@Override
	public GenericServer getServer() {
		return server;
	}

	private static volatile ScriptsServer INSTANCE;

	public static ScriptsServer getInstance() {
		return INSTANCE;
	}

	public static synchronized void main(final String... args) throws IOException, ServletException, JMException {
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