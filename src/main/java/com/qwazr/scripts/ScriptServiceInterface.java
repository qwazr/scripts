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

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.TargetRuleEnum;
import com.qwazr.utils.server.RemoteService;
import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

@RolesAllowed(ScriptManager.SERVICE_NAME_SCRIPT)
@Path("/scripts")
@ServiceName(ScriptManager.SERVICE_NAME_SCRIPT)
public interface ScriptServiceInterface extends ServiceInterface {

	@GET
	@Path("/run/{script_path : .+}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	List<ScriptRunStatus> runScript(@PathParam("script_path") String scriptPath, @QueryParam("group") String group,
			@QueryParam("rule") TargetRuleEnum rule);

	@POST
	@Path("/run/{script_path : .+}")
	@Consumes(ServiceInterface.APPLICATION_JSON_UTF8)
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	List<ScriptRunStatus> runScriptVariables(@PathParam("script_path") String scriptPath,
			@QueryParam("group") String group, @QueryParam("rule") TargetRuleEnum rule, Map<String, String> variables);

	@GET
	@Path("/status")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Map<String, ScriptRunStatus> getRunsStatus();

	@GET
	@Path("/status/{run_id}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	ScriptRunStatus getRunStatus(@PathParam("run_id") String run_id);

	@GET
	@Path("/status/{run_id}/out")
	@Produces(MediaType.TEXT_PLAIN)
	StreamingOutput getRunOut(@PathParam("run_id") String run_id);

	@GET
	@Path("/status/{run_id}/err")
	@Produces(MediaType.TEXT_PLAIN)
	StreamingOutput getRunErr(@PathParam("run_id") String run_id);

	static ScriptServiceInterface getClient(Boolean local, String group) throws URISyntaxException {
		if (local != null && local)
			return new ScriptServiceImpl();
		SortedSet<String> nodes =
				ClusterManager.INSTANCE.getNodesByGroupByService(group, ScriptManager.SERVICE_NAME_SCRIPT);
		if (nodes == null)
			throw new WebApplicationException("The script service is not available");
		if (nodes.size() == 0)
			throw new WebApplicationException("No available script node for the group: " + group,
					Response.Status.EXPECTATION_FAILED);
		if (nodes.size() == 1)
			return new ScriptSingleClient(new RemoteService(nodes.first()));
		return new ScriptMultiClient(RemoteService.build(nodes));
	}

}
