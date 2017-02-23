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

import com.qwazr.server.ServerException;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JavaRunThread extends RunThreadAbstract {

	private final Map<String, Object> variables;
	private final Class<?> scriptClass;
	private final ScriptManager scriptManager;

	JavaRunThread(final ScriptManager scriptManager, final String className, final Map<String, ?> initialVariables) {
		super(scriptManager.myAddress, className, initialVariables);
		this.scriptManager = scriptManager;
		try {
			scriptClass = scriptManager.classLoaderManager.findClass(className);
		} catch (ClassNotFoundException e) {
			throw new ServerException(Response.Status.NOT_FOUND, "Class not found: " + className);
		}
		variables = new HashMap<>();
		if (initialVariables != null)
			variables.putAll(initialVariables);
	}

	@Override
	protected void runner() throws Exception {
		Objects.requireNonNull("Cannot create instance of " + scriptClass);
		final Object script = scriptManager.classLoaderManager.newInstance(scriptClass);
		if (script instanceof ScriptInterface)
			((ScriptInterface) script).run(variables);
		else if (script instanceof Runnable)
			((Runnable) script).run();
		else
			throw new IllegalAccessException("Class execution not supported: " + scriptClass);
	}

}
