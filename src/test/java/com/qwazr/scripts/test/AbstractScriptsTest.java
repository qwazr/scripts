/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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

import com.google.common.io.Files;
import com.qwazr.scripts.ScriptRunStatus;
import com.qwazr.scripts.ScriptServiceInterface;
import com.qwazr.scripts.ScriptsServer;
import com.qwazr.utils.http.HttpClients;
import org.apache.http.pool.PoolStats;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractScriptsTest {

	public static boolean serverStarted = false;

	@Test
	public void test000startServer() throws Exception {
		if (serverStarted)
			return;
		final File dataDir = Files.createTempDir();
		System.setProperty("QWAZR_DATA", dataDir.getAbsolutePath());
		System.setProperty("PUBLIC_ADDR", "localhost");
		System.setProperty("LISTEN_ADDR", "localhost");
		ScriptsServer.main(new String[]{});
		serverStarted = true;
	}

	private static ScriptServiceInterface client;

	protected abstract ScriptServiceInterface getClient() throws InterruptedException, URISyntaxException;

	@Test
	public void test005getClient() throws URISyntaxException, InterruptedException {
		this.client = getClient();
	}

	@Test
	public void test100list() throws URISyntaxException {
		Map<String, ScriptRunStatus> statusMap = client.getRunsStatus();
		Assert.assertNotNull(statusMap);
	}

	private ScriptRunStatus waitFor(List<ScriptRunStatus> list, Function<ScriptRunStatus, Boolean> function)
			throws InterruptedException {
		Assert.assertNotNull(list);
		Assert.assertEquals(1, list.size());
		String runId = list.get(0).uuid;
		for (int i = 0; i < 10; i++) {
			final ScriptRunStatus status = client.getRunStatus(runId);
			if (function.apply(status))
				return status;
			Thread.sleep(1000);
		}
		Assert.fail("Timeout while waiting for execution");
		return null;
	}

	@Test
	public void test200startClass() throws URISyntaxException, InterruptedException {
		Map<String, String> variables = new HashMap<>();
		variables.put("ScriptTest", "ScriptTest");
		final List<ScriptRunStatus> list = client.runScriptVariables(TaskScript.class.getName(), null, null, variables);
		waitFor(list, status -> status.end != null && status.state ==
				ScriptRunStatus.ScriptState.terminated);
		Assert.assertTrue(TaskScript.EXECUTION_COUNT.get() > 0);
	}

	@Test
	public void test200startJs() throws URISyntaxException, InterruptedException, IOException {
		Map<String, String> variables = new HashMap<>();
		variables.put("ScriptTestJS", "ScriptTestJS");
		final List<ScriptRunStatus> list = client.runScriptVariables("src/test/js/test.js", null, null, variables);
		ScriptRunStatus finalStatus = waitFor(list, status -> status.end != null && status.state ==
				ScriptRunStatus.ScriptState.terminated);
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			client.getRunOut(finalStatus.uuid).write(baos);
			Assert.assertEquals("Hello World! ScriptTestJS", baos.toString().trim());
			baos.reset();
			client.getRunErr(finalStatus.uuid).write(baos);
			Assert.assertEquals("World Hello! ScriptTestJS", baos.toString().trim());
		}
	}

	@Test
	public void test300startClassNotFound() throws InterruptedException, URISyntaxException {
		try {
			client.runScriptVariables("dummy", null, null, null);
			Assert.fail("Exception not thrown");
		} catch (WebApplicationException e) {
			Assert.assertEquals(404, e.getResponse().getStatus());
		}
	}

	@Test
	public void test300startJSNotFound() throws InterruptedException, URISyntaxException {
		try {
			client.runScriptVariables("dummy.js", null, null, null);
			Assert.fail("Exception not thrown");
		} catch (WebApplicationException e) {
			Assert.assertEquals(404, e.getResponse().getStatus());
		}
	}

	@Test
	public void test400startJSError() throws InterruptedException, URISyntaxException, IOException {
		final List<ScriptRunStatus> list = client.runScriptVariables("src/test/js/error.js", null, null, null);
		final ScriptRunStatus finalStatus = waitFor(list, status -> status.end != null && status.state ==
				ScriptRunStatus.ScriptState.error);
		Assert.assertEquals("ReferenceError: \"erroneous\" is not defined in <eval> at line number 1",
				finalStatus.error);
	}

	private void checkNotFound(Runnable runner) {
		try {
			runner.run();
			Assert.fail("Not found exception not thrown");
		} catch (WebApplicationException e) {
			if (e.getResponse().getStatus() != 404)
				throw e;
		}
	}

	@Test
	public void test500notFound() {
		checkNotFound(() -> client.getRunStatus("dummy"));
		checkNotFound(() -> client.getRunOut("dummy"));
		checkNotFound(() -> client.getRunErr("dummy"));
	}

	@Test
	public void test999httpClient() {
		final PoolStats stats = HttpClients.CNX_MANAGER.getTotalStats();
		Assert.assertEquals(0, HttpClients.CNX_MANAGER.getTotalStats().getLeased());
		Assert.assertEquals(0, stats.getPending());
		Assert.assertTrue(stats.getAvailable() >= 0);
	}
}
