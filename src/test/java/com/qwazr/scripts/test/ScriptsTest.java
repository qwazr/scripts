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

import com.qwazr.scripts.ScriptServiceImpl;
import com.qwazr.scripts.ScriptServiceInterface;
import com.qwazr.scripts.ScriptSingleClient;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;

@RunWith(Suite.class)
@Suite.SuiteClasses({ ScriptsTest.LocalTest.class, ScriptsTest.SingleClientTest.class })
public class ScriptsTest {

	public static class LocalTest extends AbstractScriptsTest {

		@Override
		protected ScriptServiceInterface getClient() throws URISyntaxException, InterruptedException {
			ScriptServiceInterface client = ScriptServiceInterface.getClient(true, null);
			Assert.assertNotNull(client);
			Assert.assertTrue(client instanceof ScriptServiceImpl);
			return client;
		}
	}

	public static class SingleClientTest extends AbstractScriptsTest {

		@Override
		protected ScriptServiceInterface getClient() throws InterruptedException, URISyntaxException {
			for (int i = 0; i < 10; i++) {
				try {
					ScriptServiceInterface client = ScriptServiceInterface.getClient(false, null);
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
