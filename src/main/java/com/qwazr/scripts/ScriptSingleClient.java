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
import com.qwazr.cluster.service.TargetRuleEnum;
import com.qwazr.utils.UBuilder;
import com.qwazr.utils.http.HttpRequest;
import com.qwazr.utils.http.HttpResponseEntityException;
import com.qwazr.utils.http.HttpUtils;
import com.qwazr.utils.json.client.JsonClientAbstract;
import com.qwazr.utils.server.RemoteService;
import org.apache.http.client.methods.CloseableHttpResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ScriptSingleClient extends JsonClientAbstract implements ScriptServiceInterface {

	private final static String SCRIPT_PREFIX = "/scripts/";
	private final static String SCRIPT_PREFIX_RUN = SCRIPT_PREFIX + "run/";
	private final static String SCRIPT_PREFIX_STATUS = SCRIPT_PREFIX + "status/";

	ScriptSingleClient(final RemoteService remote) {
		super(remote);
	}

	public final static TypeReference<List<ScriptRunStatus>> ListRunStatusTypeRef =
			new TypeReference<List<ScriptRunStatus>>() {
			};

	@Override
	public List<ScriptRunStatus> runScript(final String scriptPath, final String group, final TargetRuleEnum rule) {
		final UBuilder uriBuilder =
				RemoteService.getNewUBuilder(remote, SCRIPT_PREFIX_RUN, scriptPath).setParameter("group", group)
						.setParameter("rule", rule == null ? null : rule.name());
		final HttpRequest request = HttpRequest.Get(uriBuilder.buildNoEx());
		return commonServiceRequest(request, null, null, ListRunStatusTypeRef, 200, 202);
	}

	@Override
	public List<ScriptRunStatus> runScriptVariables(final String scriptPath, final String group,
			final TargetRuleEnum rule, final Map<String, String> variables) {
		if (variables == null)
			return runScript(scriptPath, group, rule);
		final UBuilder uriBuilder =
				RemoteService.getNewUBuilder(remote, SCRIPT_PREFIX_RUN, scriptPath).setParameter("group", group)
						.setParameter("rule", rule == null ? null : rule.name());
		final HttpRequest request = HttpRequest.Post(uriBuilder.buildNoEx());
		return commonServiceRequest(request, variables, null, ListRunStatusTypeRef, 200, 202);
	}

	@Override
	public ScriptRunStatus getRunStatus(final String run_id) {
		final UBuilder uriBuilder = RemoteService.getNewUBuilder(remote, SCRIPT_PREFIX_STATUS, run_id);
		final HttpRequest request = HttpRequest.Get(uriBuilder.buildNoEx());
		return commonServiceRequest(request, null, null, ScriptRunStatus.class, 200);
	}

	public final static TypeReference<TreeMap<String, ScriptRunStatus>> MapRunStatusTypeRef =
			new TypeReference<TreeMap<String, ScriptRunStatus>>() {
			};

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus() {
		final UBuilder uriBuilder = RemoteService.getNewUBuilder(remote, SCRIPT_PREFIX_STATUS);
		final HttpRequest request = HttpRequest.Get(uriBuilder.buildNoEx());
		return commonServiceRequest(request, null, null, MapRunStatusTypeRef, 200);
	}

	@Override
	public String getRunOut(final String run_id) {

		final UBuilder uriBuilder = RemoteService.getNewUBuilder(remote, SCRIPT_PREFIX_STATUS, run_id, "/out");
		final HttpRequest request = HttpRequest.Get(uriBuilder.buildNoEx());
		try (final CloseableHttpResponse response = execute(request, null, null)) {
			return HttpUtils.checkTextPlainEntity(response, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e, Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public String getRunErr(final String run_id) {
		final UBuilder uriBuilder = RemoteService.getNewUBuilder(remote, SCRIPT_PREFIX_STATUS, run_id, "/err");
		final HttpRequest request = HttpRequest.Get(uriBuilder.buildNoEx());
		try (final CloseableHttpResponse response = execute(request, null, null)) {
			return HttpUtils.checkTextPlainEntity(response, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e, Status.INTERNAL_SERVER_ERROR);
		}
	}
}
