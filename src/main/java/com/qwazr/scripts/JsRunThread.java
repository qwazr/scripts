/*
 * Copyright 2014-2018 Emmanuel Keller / QWAZR
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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class JsRunThread extends RunThreadAbstract<Boolean> {

    private final SimpleScriptContext scriptContext;
    private final ScriptEngine scriptEngine;
    private final Path scriptFilePath;

    JsRunThread(final ScriptManager scriptManager, final Path scriptFilePath, final Map<String, ?> initialVariables) {
        super(scriptManager.myAddress, scriptFilePath.getFileName().toString(), initialVariables);
        this.scriptEngine = scriptManager.getScriptEngine();
        scriptContext = new SimpleScriptContext();
        scriptContext.setBindings(new GlobalBindings(), ScriptContext.GLOBAL_SCOPE);

        if (initialVariables != null)
            initialVariables.forEach(
                    (key, value) -> scriptContext.setAttribute(key, value, ScriptContext.ENGINE_SCOPE));

        if (scriptManager.libraryService != null)
            scriptContext.setAttribute("library", scriptManager.libraryService, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("closeable", closeables, ScriptContext.ENGINE_SCOPE);

        this.scriptFilePath = scriptFilePath;
        scriptContext.setWriter(outputWriter);
        scriptContext.setErrorWriter(errorWriter);
        scriptEngine.setContext(scriptContext);
    }

    @Override
    protected Boolean runner() throws IOException, ScriptException {
        try (final BufferedReader reader = Files.newBufferedReader(scriptFilePath, StandardCharsets.UTF_8)) {
            Object result = scriptEngine.eval(reader, scriptContext);
            if (result == null)
                return true;
            if (result instanceof Boolean)
                return (Boolean) result;
            return true;
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
