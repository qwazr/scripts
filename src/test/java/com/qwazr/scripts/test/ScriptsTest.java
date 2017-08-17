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
package com.qwazr.scripts.test;

import com.qwazr.scripts.ScriptManager;
import com.qwazr.scripts.ScriptServiceBuilder;
import com.qwazr.scripts.ScriptServiceInterface;
import com.qwazr.scripts.ScriptSingleClient;
import com.qwazr.scripts.ScriptsServer;
import com.qwazr.server.RemoteService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(Suite.class)
@Suite.SuiteClasses({ ScriptsTest.LibraryTest.class, ScriptsTest.LocalTest.class, ScriptsTest.SingleClientTest.class })
public class ScriptsTest {

	public static class LibraryTest extends AbstractScriptsTest {

		static ExecutorService executor;

		@BeforeClass
		public static void setup() {
			executor = Executors.newCachedThreadPool();
		}

		@AfterClass
		public static void cleanup() {
			executor.shutdown();
		}

		@Override
		protected ScriptServiceInterface getClient() throws URISyntaxException, InterruptedException {
			final ScriptServiceInterface client =
					new ScriptManager(executor, Paths.get("src/test").toFile()).getService();
			Assert.assertNotNull(client);
			return client;
		}

	}

	public static class LocalTest extends AbstractScriptsTest {

		@Override
		protected ScriptServiceInterface getClient() throws URISyntaxException, InterruptedException {
			final ScriptServiceInterface client = ScriptsServer.getInstance().getScriptManager().getService();
			Assert.assertNotNull(client);
			return client;
		}
	}

	public static class SingleClientTest extends AbstractScriptsTest {

		@Override
		protected ScriptServiceInterface getClient() throws InterruptedException, URISyntaxException {
			for (int i = 0; i < 10; i++) {
				try {
					ScriptServiceInterface client = new ScriptServiceBuilder(null, null, null).remote(
							RemoteService.of("http://localhost:9091").build());
					Assert.assertNotNull(client);
					Assert.assertTrue(client instanceof ScriptSingleClient);
					return client;
				} catch (WebApplicationException e) {
					Assert.assertNotEquals(Response.Status.EXPECTATION_FAILED, e.getResponse().getStatus());
				}
				Thread.sleep(2000);
			}
			Assert.fail("Timeout while getting client");
			return null;
		}
	}

}
