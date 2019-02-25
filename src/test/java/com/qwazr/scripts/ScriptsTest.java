/*
 * Copyright 2015-2019 Emmanuel Keller / QWAZR
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qwazr.server.RemoteService;
import com.qwazr.utils.concurrent.ExecutorUtils;
import org.graalvm.polyglot.Value;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Suite.class)
@Suite.SuiteClasses({ ScriptsTest.LibraryTest.class,
		ScriptsTest.LocalTest.class,
		ScriptsTest.SingleClientTest.class,
		ScriptsTest.MultiClientTest.class })
public class ScriptsTest {

	public static class LocalTest extends AbstractScriptsTest {

		@Override
		protected ScriptServiceInterface getClient() {
			final ScriptServiceInterface client = ScriptsServer.getInstance().getScriptServiceBuilder().local();
			Assert.assertNotNull(client);
			Assert.assertEquals(ScriptServiceImpl.class, client.getClass());
			return client;
		}

		@Override
		public void test250runSync() {
			final RunThreadAbstract runThread = client.runSync(TaskNoVarScript.class.getName(), null);
			Assert.assertNotNull(runThread);
			Assert.assertNotNull(runThread.getStatus());
			Assert.assertNull(runThread.getException());
			Assert.assertEquals(true, runThread.getResult());
		}

		@Override
		public void test250runAsync() throws InterruptedException {
			final ScriptRunStatus status =
					waitFor(client.runAsync(TaskNoVarScript.class.getName(), null).uuid, Objects::nonNull);
			Assert.assertNotNull(status);
		}

	}

	public static class LibraryTest extends LocalTest {

		static ExecutorService executor;
		static private ScriptManager scriptManager;

		@BeforeClass
		public static void setup() {
			client = null;
			executor = Executors.newCachedThreadPool();
			scriptManager = new ScriptManager(executor, Paths.get("src/test"));
		}

		@AfterClass
		public static void cleanup() throws InterruptedException {
			ExecutorUtils.close(executor, 5, TimeUnit.MINUTES);
			ScriptsServer.shutdown();
		}

		@Override
		protected ScriptServiceInterface getClient() {
			final ScriptServiceInterface client = scriptManager.getService();
			Assert.assertNotNull(client);
			return client;
		}

		@Test
		public void jsonStringifyTest() {
			getClient().runSync(Paths.get("js/javacall.js").toString(), Map.of("javacall", new JavaCall()));
		}

		public static class JavaCall {

			public void call(Value value) throws IOException {
				final Json json = ScriptUtils.fromJson(value, Json.class);
				assertThat(json.callKey, equalTo("callValue"));
				assertThat(json.list, equalTo(Arrays.asList(1, 2, 3, 4, 5)));
				assertThat(json.map, equalTo(Map.of("key1", "value1", "key2", "value2")));
			}
		}

		public static class Json {

			public final String callKey;
			public final List<Integer> list;
			public final Map<String, String> map;

			@JsonCreator
			Json(@JsonProperty("callKey") final String callValue, @JsonProperty("list") final List<Integer> list,
					@JsonProperty("map") final Map<String, String> map) {
				this.callKey = callValue;
				this.list = list;
				this.map = map;
			}

		}
	}

	public static class SingleClientTest extends AbstractScriptsTest {

		@Override
		protected ScriptServiceInterface getClient() throws InterruptedException, URISyntaxException {
			for (int i = 0; i < 10; i++) {
				try {
					final ScriptServiceInterface client = ScriptsServer.getInstance()
							.getScriptServiceBuilder()
							.remote(RemoteService.of("http://localhost:9091").build());
					Assert.assertNotNull(client);
					Assert.assertEquals(ScriptSingleClient.class, client.getClass());
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
					final ScriptServiceInterface client = ScriptsServer.getInstance()
							.getScriptServiceBuilder()
							.remotes(RemoteService.of("http://localhost:9091").build(),
									RemoteService.of("http://localhost:9091").build());
					Assert.assertNotNull(client);
					Assert.assertEquals(ScriptMultiClient.class, client.getClass());
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
