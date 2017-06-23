/*
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

import com.qwazr.utils.LoggerUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptConsole implements Closeable {

	private static final Logger LOGGER = LoggerUtils.getLogger(ConsoleLogger.class);

	private PrintWriter stdError;

	public ScriptConsole(final Writer errorWriter) {
		this.stdError = errorWriter == null ?
				null :
				errorWriter instanceof PrintWriter ? (PrintWriter) errorWriter : new PrintWriter(errorWriter);
	}

	public void log(final Object object) {
		getLog().info(object);
	}

	public ConsoleLogger getLog() {
		return ConsoleLogger.INSTANCE;
	}

	public void error(final Object object) throws IOException {
		if (object != null)
			if (stdError != null)
				stdError.println(object.toString());
			else
				ConsoleLogger.INSTANCE.error(object.toString());
	}

	@Override
	public void close() {
		if (stdError != null)
			stdError.close();
	}

	public static class ConsoleLogger {

		private static final ConsoleLogger INSTANCE = new ConsoleLogger();

		private void log(Level level, Object object) {
			if (object == null)
				return;
			if (!LOGGER.isLoggable(level))
				return;
			if (object instanceof Throwable)
				LOGGER.log(level, object.toString(), (Throwable) object);
			else
				LOGGER.info(object.toString());
		}

		public void info(final Object object) {
			log(Level.INFO, object);
		}

		public void warn(final Object object) {
			log(Level.WARNING, object);

		}

		public void error(final Object object) {
			log(Level.SEVERE, object);
		}

		public void debug(Object object) {
			log(Level.FINEST, object);
		}

		public void trace(Object object) {
			log(Level.FINER, object);
		}

	}
}
