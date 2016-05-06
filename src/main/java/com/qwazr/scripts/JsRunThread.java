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

import com.qwazr.library.LibraryManager;
import com.qwazr.utils.IOUtils;

import javax.script.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class JsRunThread extends RunThreadAbstract {

	private final SimpleScriptContext scriptContext;
	private final ScriptEngine scriptEngine;
	private final File scriptFile;
	private final Set<String> semaphores;

	JsRunThread(ScriptEngine scriptEngine, File scriptFile, Map<String, ?> initialVariables) {
		super(scriptFile.getName(), initialVariables);
		this.scriptEngine = scriptEngine;
		semaphores = new HashSet<>();

		scriptContext = new SimpleScriptContext();
		scriptContext.setBindings(new GlobalBindings(), ScriptContext.GLOBAL_SCOPE);

		if (initialVariables != null)
			initialVariables
					.forEach((key, value) -> scriptContext.setAttribute(key, value, ScriptContext.ENGINE_SCOPE));

		LibraryManager libraries = LibraryManager.getInstance();
		if (libraries != null)
			scriptContext.setAttribute("library", libraries, ScriptContext.ENGINE_SCOPE);
		scriptContext.setAttribute("closeable", closeables, ScriptContext.ENGINE_SCOPE);

		this.scriptFile = scriptFile;
		scriptContext.setWriter(new StringWriter());
		scriptContext.setErrorWriter(new StringWriter());
	}

	@Override
	protected void runner() throws FileNotFoundException, ScriptException {
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(scriptFile);
			scriptEngine.eval(fileReader, scriptContext);
		} finally {
			IOUtils.close(fileReader);
		}
	}

	public class GlobalBindings extends HashMap<String, Object> implements Bindings {

		/**
		 *
		 */
		private static final long serialVersionUID = -7250097260119419346L;

		private GlobalBindings() {
			this.put("console", new ScriptConsole());
		}

		public void sleep(int msTimeout) throws InterruptedException {
			Thread.sleep(msTimeout);
		}
	}
}
