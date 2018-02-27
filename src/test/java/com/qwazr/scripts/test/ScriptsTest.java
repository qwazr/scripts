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

import com.qwazr.scripts.RunThreadAbstract;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.scripts.ScriptMultiClient;
import com.qwazr.scripts.ScriptRunStatus;
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
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(Suite.class)
@Suite.SuiteClasses({ ScriptsTest.LibraryTest.class,
		ScriptsTest.LocalTest.class,
		ScriptsTest.SingleClientTest.class,
		ScriptsTest.MultiClientTest.class })
public class ScriptsTest {

	public static class LocalTest extends AbstractScriptsTest {

		@Override
		protected ScriptServiceInterface getClient() throws URISyntaxException, InterruptedException {
			final ScriptServiceInterface client = ScriptsServer.getInstance().getScriptService();
			Assert.assertNotNull(client);
			Assert.assertNotNull(ScriptsServer.getInstance().getScriptService());
			return client;
		}

		@Override
		public void test250runSync() throws IOException, ClassNotFoundException {
			final RunThreadAbstract runThread = client.runSync(TaskNoVarScript.class.getName(), null);
			Assert.assertNotNull(runThread);
			Assert.assertNotNull(runThread.getStatus());
			Assert.assertNull(runThread.getException());
			Assert.assertEquals(true, runThread.getResult());
		}

		@Override
		public void test250runAsync() throws IOException, ClassNotFoundException, InterruptedException {
			final ScriptRunStatus status =
					waitFor(client.runAsync(TaskNoVarScript.class.getName(), null).uuid, Objects::nonNull);
			Assert.assertNotNull(status);
		}
	}

	public static class LibraryTest extends LocalTest {

		static ExecutorService executor;

		@BeforeClass
		public static void setup() {
			AbstractScriptsTest.client = null;
			executor = Executors.newCachedThreadPool();
		}

		@AfterClass
		public static void cleanup() {
			executor.shutdown();
			ScriptsServer.shutdown();
		}

		@Override
		protected ScriptServiceInterface getClient() throws URISyntaxException, InterruptedException {
			final ScriptServiceInterface client =
					new ScriptManager(executor, Paths.get("src/test").toFile()).getService();
			Assert.assertNotNull(client);
			return client;
		}

	}

	public static class SingleClientTest extends AbstractScriptsTest {

		@Override
		protected ScriptServiceInterface getClient() throws InterruptedException, URISyntaxException {
			for (int i = 0; i < 10; i++) {
				try {
					final ScriptSingleClient client =
							new ScriptSingleClient(RemoteService.of("http://localhost:9091").build());
					Assert.assertNotNull(client);
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

	public static class MultiClientTest extends AbstractScriptsTest {

		@Override
		protected ScriptServiceInterface getClient() throws InterruptedException, URISyntaxException {
			for (int i = 0; i < 10; i++) {
				try {
					final ScriptMultiClient client =
							new ScriptMultiClient(ScriptsServer.getInstance().getExecutorService(),
									RemoteService.of("http://localhost:9091").build(),
									RemoteService.of("http://localhost:9091").build());
					Assert.assertNotNull(client);
					Assert.assertTrue(client instanceof ScriptMultiClient);
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
