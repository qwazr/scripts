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

import com.qwazr.cluster.ClusterServer;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.connectors.ConnectorManagerImpl;
import com.qwazr.tools.ToolsManagerImpl;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServletApplication;
import io.undertow.security.idm.IdentityManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.quartz.SchedulerException;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class ScriptsServer extends AbstractServer {

	public final static String SERVICE_NAME_SCRIPT = "scripts";

	private final static ServerDefinition serverDefinition = new ServerDefinition();

	static {
		serverDefinition.defaultWebApplicationTcpPort = 9098;
		serverDefinition.mainJarPath = "qwazr-scripts.jar";
		serverDefinition.defaultDataDirName = "qwazr";
	}

	private ScriptsServer() {
		super(serverDefinition);
	}

	@ApplicationPath("/")
	public static class ScriptsApplication extends RestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>> classes = super.getClasses();
			classes.add(ClusterServiceImpl.class);
			classes.add(ScriptServiceImpl.class);
			return classes;
		}
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException, ParseException {
	}

	public static void loadScript(File dataDir) throws IOException {
		ScriptManager.load(dataDir);
	}

	@Override
	public void load() throws IOException {
		File currentDataDir = getCurrentDataDir();
		ClusterServer.load(getWebServicePublicAddress(), currentDataDir);
		ConnectorManagerImpl.load(currentDataDir);
		ToolsManagerImpl.load(currentDataDir);
		loadScript(currentDataDir);
	}

	public static void main(String[] args)
			throws IOException, ParseException, ServletException, SchedulerException, InstantiationException,
			IllegalAccessException {
		new ScriptsServer().start(args);
	}

	@Override
	protected Class<ScriptsApplication> getRestApplication() {
		return ScriptsApplication.class;
	}

	@Override
	protected Class<ServletApplication> getServletApplication() {
		return null;
	}

	@Override
	protected IdentityManager getIdentityManager(String realm) {
		return null;
	}

}
