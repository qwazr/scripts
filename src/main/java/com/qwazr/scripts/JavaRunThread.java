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

import com.qwazr.library.LibraryManager;
import com.qwazr.server.ServerException;
import com.qwazr.utils.ClassLoaderUtils;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class JavaRunThread extends RunThreadAbstract {

	private final Map<String, Object> variables;
	private final Class<?> scriptClass;
	private final LibraryManager libraryManager;

	JavaRunThread(final ScriptManager scriptManager, final LibraryManager libraryManager, final String className,
			final Map<String, ?> initialVariables) {
		super(scriptManager.myAddress, className, initialVariables);
		this.libraryManager = libraryManager;
		try {
			scriptClass = ClassLoaderUtils.findClass(className);
		} catch (ClassNotFoundException e) {
			throw new ServerException(Response.Status.NOT_FOUND, "Class not found: " + className);
		}
		variables = new HashMap<>();
		if (initialVariables != null)
			variables.putAll(initialVariables);
	}

	@Override
	protected boolean runner() throws Exception {
		Objects.requireNonNull(scriptClass, "Cannot create instance of " + scriptClass);
		final Object script = scriptClass.newInstance();
		if (libraryManager != null)
			libraryManager.inject(script);
		if (script instanceof ScriptInterface)
			return ((ScriptInterface) script).run(variables);
		else if (script instanceof Runnable)
			((Runnable) script).run();
		else
			throw new IllegalAccessException("Class execution not supported: " + scriptClass);
		return true;
	}

}
