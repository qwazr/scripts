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

import com.qwazr.cluster.service.TargetRuleEnum;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@RolesAllowed(ScriptManager.SERVICE_NAME_SCRIPT)
@Path("/scripts")
public interface ScriptServiceInterface {

	@GET
	@Path("/run/{script_path : .+}")
	@Produces(MediaType.APPLICATION_JSON)
	List<ScriptRunStatus> runScript(@PathParam("script_path") String scriptPath, @QueryParam("local") Boolean local,
			@QueryParam("group") String group, @QueryParam("timeout") Integer msTimeout,
			@QueryParam("rule") TargetRuleEnum rule);

	@POST
	@Path("/run/{script_path : .+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	List<ScriptRunStatus> runScriptVariables(@PathParam("script_path") String scriptPath,
			@QueryParam("local") Boolean local, @QueryParam("group") String group,
			@QueryParam("timeout") Integer msTimeout, @QueryParam("rule") TargetRuleEnum rule,
			Map<String, String> variables);

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	Map<String, ScriptRunStatus> getRunsStatus(@QueryParam("local") Boolean local, @QueryParam("group") String group,
			@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/status/{run_id}")
	@Produces(MediaType.APPLICATION_JSON)
	ScriptRunStatus getRunStatus(@PathParam("run_id") String run_id, @QueryParam("local") Boolean local,
			@QueryParam("group") String group, @QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/status/{run_id}/out")
	@Produces(MediaType.TEXT_PLAIN)
	String getRunOut(@PathParam("run_id") String run_id, @QueryParam("local") Boolean local,
			@QueryParam("group") String group, @QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/status/{run_id}/err")
	@Produces(MediaType.TEXT_PLAIN)
	String getRunErr(@PathParam("run_id") String run_id, @QueryParam("local") Boolean local,
			@QueryParam("group") String group, @QueryParam("timeout") Integer msTimeout);

}
