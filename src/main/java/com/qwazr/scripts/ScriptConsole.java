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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class ScriptConsole implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLogger.class);

	private PrintWriter stdError;

	public ScriptConsole(final Writer errorWriter) {
		this.stdError =
				errorWriter == null ? null :
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

		public void info(final Object object) {
			if (object == null)
				return;
			if (!LOGGER.isInfoEnabled())
				return;
			if (object instanceof Throwable)
				LOGGER.info(object.toString(), (Throwable) object);
			else
				LOGGER.info(object.toString());
		}

		public void warn(final Object object) {
			if (object == null)
				return;
			if (!LOGGER.isWarnEnabled())
				return;
			if (object instanceof Throwable)
				LOGGER.warn(object.toString(), (Throwable) object);
			else
				LOGGER.warn(object.toString());
		}

		public void error(final Object object) {
			if (object == null)
				return;
			if (!LOGGER.isErrorEnabled())
				return;
			if (object instanceof Throwable)
				LOGGER.error(object.toString(), (Throwable) object);
			else
				LOGGER.error(object.toString());
		}

		public void debug(Object object) {
			if (object == null)
				return;
			if (!LOGGER.isDebugEnabled())
				return;
			if (object instanceof Throwable)
				LOGGER.debug(object.toString(), (Throwable) object);
			else
				LOGGER.debug(object.toString());
		}

		public void trace(Object object) {
			if (object == null)
				return;
			if (!LOGGER.isTraceEnabled())
				return;
			if (object instanceof Throwable)
				LOGGER.trace(object.toString(), (Throwable) object);
			else
				LOGGER.trace(object.toString());
		}

	}
}
