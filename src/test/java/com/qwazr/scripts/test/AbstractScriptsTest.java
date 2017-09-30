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

import com.qwazr.scripts.ScriptRunStatus;
import com.qwazr.scripts.ScriptServiceInterface;
import com.qwazr.scripts.ScriptsServer;
import com.qwazr.scripts.TargetRuleEnum;
import com.qwazr.utils.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractScriptsTest {

	@Test
	public void test000startServer() throws Exception {
		System.setProperty("QWAZR_DATA", new File("src/test").getAbsolutePath());
		System.setProperty("PUBLIC_ADDR", "localhost");
		System.setProperty("LISTEN_ADDR", "localhost");
		ScriptsServer.main();
	}

	static ScriptServiceInterface client;

	protected abstract ScriptServiceInterface getClient() throws InterruptedException, URISyntaxException;

	@Test
	public void test005getClient() throws URISyntaxException, InterruptedException {
		client = getClient();
		Assert.assertNotNull(client);
	}

	@Test
	public void test100list() throws URISyntaxException {
		Map<String, ScriptRunStatus> statusMap = client.getRunsStatus();
		Assert.assertNotNull(statusMap);
	}

	ScriptRunStatus waitFor(final String uuid, final Function<ScriptRunStatus, Boolean> function)
			throws InterruptedException {
		for (int i = 0; i < 10; i++) {
			final ScriptRunStatus status = client.getRunStatus(uuid);
			if (function.apply(status))
				return status;
			Thread.sleep(1000);
		}
		Assert.fail("Timeout while waiting for execution");
		return null;
	}

	private Map<String, ScriptRunStatus> waitFor(final List<ScriptRunStatus> list,
			final Function<ScriptRunStatus, Boolean> function) throws InterruptedException {
		Assert.assertNotNull(list);
		Assert.assertFalse(list.isEmpty());
		final Map<String, ScriptRunStatus> results = new HashMap<>();
		for (final ScriptRunStatus status : list)
			results.put(status.uuid, waitFor(status.uuid, function));
		return results;
	}

	private void startClassVariables(TargetRuleEnum targetRule, Map<String, String> variables)
			throws InterruptedException {
		final List<ScriptRunStatus> list =
				client.runScriptVariables(TaskVariablesScript.class.getName(), null, targetRule, variables);
		waitFor(list, status -> status.endTime != null && status.state == ScriptRunStatus.ScriptState.terminated);
		Assert.assertTrue(TaskVariablesScript.EXECUTION_COUNT.get() > 0);
	}

	private void startClass(TargetRuleEnum targetRule) throws InterruptedException {
		final List<ScriptRunStatus> list = client.runScript(TaskNoVarScript.class.getName(), null, targetRule);
		waitFor(list, status -> status.endTime != null && status.state == ScriptRunStatus.ScriptState.terminated);
		Assert.assertTrue(TaskNoVarScript.EXECUTION_COUNT.get() > 0);
	}

	@Test
	public void test200startClass() throws URISyntaxException, InterruptedException {
		startClass(null);
		startClass(TargetRuleEnum.one);
		startClass(TargetRuleEnum.all);
	}

	@Test
	public void test200startClassVariables() throws URISyntaxException, InterruptedException {
		Map<String, String> variables = new HashMap<>();
		variables.put("ScriptTest", "ScriptTest");

		startClassVariables(null, variables);
		startClassVariables(TargetRuleEnum.one, variables);
		startClassVariables(TargetRuleEnum.all, variables);
	}

	@Test
	public void test250runSync() throws IOException, ClassNotFoundException {
		try {
			client.runSync(TaskNoVarScript.class.getName(), null);
			Assert.fail("NotImplementedException not thrown");
		} catch (NotImplementedException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void test250runAsync() throws IOException, ClassNotFoundException, InterruptedException {
		try {
			waitFor(client.runAsync(TaskNoVarScript.class.getName(), null).uuid, status -> true);
			Assert.fail("NotImplementedException not thrown");
		} catch (NotImplementedException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void test200startJs() throws URISyntaxException, InterruptedException, IOException {
		Map<String, String> variables = new HashMap<>();
		variables.put("ScriptTestJS", "ScriptTestJS");
		final List<ScriptRunStatus> list = client.runScriptVariables("js/test.js", null, null, variables);
		ScriptRunStatus finalStatus = waitFor(list.get(0).uuid,
				status -> status.endTime != null && status.state == ScriptRunStatus.ScriptState.terminated);
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			final String scriptOut = IOUtils.toString(client.getRunOut(finalStatus.uuid), StandardCharsets.UTF_8);
			Assert.assertEquals("Hello World! ScriptTestJS", scriptOut.trim());
			final String scriptErr = IOUtils.toString(client.getRunErr(finalStatus.uuid), StandardCharsets.UTF_8);
			Assert.assertEquals("World Hello! ScriptTestJS", scriptErr.trim());
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
		final List<ScriptRunStatus> list = client.runScriptVariables("js/error.js", null, null, null);
		final ScriptRunStatus finalStatus = waitFor(list.get(0).uuid,
				status -> status.endTime != null && status.state == ScriptRunStatus.ScriptState.error);
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
		} catch (Exception e) {
			Assert.fail("Wrong Exception returned: " + e);
		}
	}

	@Test
	public void test500notFound() {
		checkNotFound(() -> client.getRunStatus("dummy"));
		checkNotFound(() -> client.getRunOut("dummy"));
		checkNotFound(() -> client.getRunErr("dummy"));
	}
	
}
