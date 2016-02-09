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
import com.qwazr.library.LibraryManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JavaRunThread extends RunThreadAbstract {

	private final Map<String, ?> variables;
	private final Class<?> scriptClass;

	JavaRunThread(String className, Map<String, ?> initialVariables) throws ClassNotFoundException {
		super(className, initialVariables);
		scriptClass = ClassLoaderManager.findClass(className);
		Objects.requireNonNull(scriptClass, "Class not found: " + className);
		variables = initialVariables == null ? null : new HashMap<>(initialVariables);
	}

	@Override
	protected void runner() throws Exception {
		Object script = scriptClass.newInstance();
		Objects.requireNonNull("Cannot create instance of " + scriptClass);
		LibraryManager.inject(script);
		if (script instanceof ScriptInterface)
			((ScriptInterface) script).run(variables);
		else if (script instanceof Runnable)
			((Runnable) script).run();
		else
			throw new IllegalAccessException("Class execution not supported: " + scriptClass);
	}

}
