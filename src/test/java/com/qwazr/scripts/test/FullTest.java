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
import com.qwazr.scripts.ScriptManager;
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
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FullTest {

	@Test
	public void test000startServer() throws Exception {
		final File dataDir = Files.createTempDir();
		System.setProperty("QWAZR_DATA", dataDir.getAbsolutePath());
		System.setProperty("PUBLIC_ADDR", "localhost");
		System.setProperty("LISTEN_ADDR", "localhost");
		ScriptsServer.main(new String[] {});
		Assert.assertNotNull(ScriptManager.getInstance());
	}

	private static ScriptServiceInterface SERVICE = null;

	@Test
	public void test005getClient() throws URISyntaxException, InterruptedException {
		for (int i = 0; i < 10; i++) {
			try {
				SERVICE = ScriptServiceInterface.getClient(false, null);
				Assert.assertNotNull(SERVICE);
				return;
			} catch (WebApplicationException e) {
				Assert.assertNotEquals(Response.Status.EXPECTATION_FAILED, e.getResponse().getStatus());
			}
			Thread.sleep(2000);
		}
		Assert.fail("Timeout while getting client");
	}

	@Test
	public void test100list() {
		Map<String, ScriptRunStatus> statusMap = SERVICE.getRunsStatus();
		Assert.assertNotNull(statusMap);
		Assert.assertTrue(statusMap.isEmpty());
	}

	@Test
	public void test200start() throws InterruptedException {
		Map<String, String> variables = new HashMap<>();
		variables.put("ScriptTest", "ScriptTest");
		List<ScriptRunStatus> list = SERVICE.runScriptVariables(TaskScript.class.getName(), null, null, variables);
		Assert.assertNotNull(list);
		Assert.assertEquals(1, list.size());
		for (int i = 0; i < 10; i++) {
			if (TaskScript.EXECUTION_COUNT.get() > 0)
				return;
			Thread.sleep(2000);
		}
		Assert.fail("Timeout while waiting for execution");
	}

	@Test
	public void test300startClassError() throws InterruptedException {
		try {
			List<ScriptRunStatus> list = SERVICE.runScriptVariables("dummy", null, null, null);
			Assert.fail("Exception not thrown");
		} catch (WebApplicationException e) {
			Assert.assertEquals(404, e.getResponse().getStatus());
		}
	}

	@Test
	public void test300startJSError() throws InterruptedException {
		try {
			List<ScriptRunStatus> list = SERVICE.runScriptVariables("dummy.js", null, null, null);
			Assert.fail("Exception not thrown");
		} catch (WebApplicationException e) {
			Assert.assertEquals(404, e.getResponse().getStatus());
		}
	}

	@Test
	public void test999httpClient() {
		final PoolStats stats = HttpClients.CNX_MANAGER.getTotalStats();
		Assert.assertEquals(0, HttpClients.CNX_MANAGER.getTotalStats().getLeased());
		Assert.assertEquals(0, stats.getPending());
		Assert.assertTrue(stats.getAvailable() > 0);
	}
}
