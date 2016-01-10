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

public class ScriptConsole {

	private static final Logger logger = LoggerFactory.getLogger(ConsoleLogger.class);

	public void log(Object object) {
		getLog().info(object);
	}

	public ConsoleLogger getLog() {
		return ConsoleLogger.INSTANCE;
	}

	public static class ConsoleLogger {

		private static final ConsoleLogger INSTANCE = new ConsoleLogger();

		public void info(Object object) {
			if (object == null)
				return;
			if (!logger.isInfoEnabled())
				return;
			if (object instanceof Throwable)
				logger.info(object.toString(), (Throwable) object);
			else
				logger.info(object.toString());
		}

		public void warn(Object object) {
			if (object == null)
				return;
			if (!logger.isWarnEnabled())
				return;
			if (object instanceof Throwable)
				logger.warn(object.toString(), (Throwable) object);
			else
				logger.warn(object.toString());
		}

		public void error(Object object) {
			if (object == null)
				return;
			if (!logger.isErrorEnabled())
				return;
			if (object instanceof Throwable)
				logger.error(object.toString(), (Throwable) object);
			else
				logger.error(object.toString());
		}

		public void debug(Object object) {
			if (object == null)
				return;
			if (!logger.isDebugEnabled())
				return;
			if (object instanceof Throwable)
				logger.debug(object.toString(), (Throwable) object);
			else
				logger.debug(object.toString());
		}

		public void trace(Object object) {
			if (object == null)
				return;
			if (!logger.isTraceEnabled())
				return;
			if (object instanceof Throwable)
				logger.trace(object.toString(), (Throwable) object);
			else
				logger.trace(object.toString());
		}

	}
}
