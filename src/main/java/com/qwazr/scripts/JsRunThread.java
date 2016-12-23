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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class JsRunThread extends RunThreadAbstract {

	private final SimpleScriptContext scriptContext;
	private final ScriptEngine scriptEngine;
	private final File scriptFile;

	JsRunThread(final ScriptManager scriptManager, final File scriptFile, final Map<String, ?> initialVariables) {
		super(scriptManager.clusterManager.getHttpAddressKey(), scriptFile.getName(), initialVariables);
		this.scriptEngine = scriptManager.getScriptEngine();

		scriptContext = new SimpleScriptContext();
		scriptContext.setBindings(new GlobalBindings(), ScriptContext.GLOBAL_SCOPE);

		if (initialVariables != null)
			initialVariables.forEach(
					(key, value) -> scriptContext.setAttribute(key, value, ScriptContext.ENGINE_SCOPE));

		if (scriptManager.libraryManager != null)
			scriptContext.setAttribute("library", scriptManager.libraryManager, ScriptContext.ENGINE_SCOPE);
		scriptContext.setAttribute("closeable", closeables, ScriptContext.ENGINE_SCOPE);

		this.scriptFile = scriptFile;
		scriptContext.setWriter(outputWriter);
		scriptContext.setErrorWriter(errorWriter);
	}

	@Override
	protected void runner() throws IOException, ScriptException {
		try (final FileReader fileReader = new FileReader(scriptFile)) {
			scriptEngine.eval(fileReader, scriptContext);
		}
	}

	public class GlobalBindings extends HashMap<String, Object> implements Bindings {

		/**
		 *
		 */
		private static final long serialVersionUID = -7250097260119419346L;

		private GlobalBindings() {
			this.put("console", closeables.add(new ScriptConsole(errorWriter)));
		}

		public void sleep(int msTimeout) throws InterruptedException {
			Thread.sleep(msTimeout);
		}
	}
}
