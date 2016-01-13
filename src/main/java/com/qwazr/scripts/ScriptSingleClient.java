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

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwazr.utils.http.HttpResponseEntityException;
import com.qwazr.utils.http.HttpUtils;
import com.qwazr.utils.json.client.JsonClientAbstract;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

public class ScriptSingleClient extends JsonClientAbstract implements ScriptServiceInterface {

	private final static String SCRIPT_PREFIX = "/scripts/";
	private final static String SCRIPT_PREFIX_RUN = SCRIPT_PREFIX + "run/";
	private final static String SCRIPT_PREFIX_STATUS = SCRIPT_PREFIX + "status/";

	ScriptSingleClient(String url, Integer msTimeOut) throws URISyntaxException {
		super(url, msTimeOut);
	}

	@Override
	public ScriptRunStatus runScript(String scriptPath) {
		UBuilder uriBuilder = new UBuilder(SCRIPT_PREFIX_RUN, scriptPath);
		Request request = Request.Get(uriBuilder.build());
		return commonServiceRequest(request, null, null, ScriptRunStatus.class, 200, 202);
	}

	@Override
	public ScriptRunStatus runScriptVariables(String scriptPath, Map<String, String> variables) {
		if (variables == null)
			return runScript(scriptPath);
		UBuilder uriBuilder = new UBuilder(SCRIPT_PREFIX_RUN, scriptPath);
		Request request = Request.Post(uriBuilder.build());
		return commonServiceRequest(request, variables, null, ScriptRunStatus.class, 200, 202);
	}

	@Override
	public ScriptRunStatus getRunStatus(String run_id) {
		UBuilder uriBuilder = new UBuilder(SCRIPT_PREFIX_STATUS, run_id);
		Request request = Request.Get(uriBuilder.build());
		return commonServiceRequest(request, null, null, ScriptRunStatus.class, 200);
	}

	public final static TypeReference<TreeMap<String, ScriptRunStatus>> MapRunStatusTypeRef = new TypeReference<TreeMap<String, ScriptRunStatus>>() {
	};

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus(Boolean local, String group, Integer msTimeout) {
		UBuilder uriBuilder = new UBuilder(SCRIPT_PREFIX_STATUS).setParameters(local, group, msTimeout);
		Request request = Request.Get(uriBuilder.build());
		return commonServiceRequest(request, null, null, MapRunStatusTypeRef, 200);
	}

	@Override
	public String getRunOut(String run_id) {
		try {
			UBuilder uriBuilder = new UBuilder(SCRIPT_PREFIX_STATUS, run_id, "/out");
			Request request = Request.Get(uriBuilder.build());
			HttpResponse response = execute(request, null, null);
			return HttpUtils.checkTextPlainEntity(response, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e, Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public String getRunErr(String run_id) {
		try {
			UBuilder uriBuilder = new UBuilder(SCRIPT_PREFIX_STATUS, run_id, "/err");
			Request request = Request.Get(uriBuilder.build());
			HttpResponse response = execute(request, null, null);
			return HttpUtils.checkTextPlainEntity(response, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e, Status.INTERNAL_SERVER_ERROR);
		}
	}
}
