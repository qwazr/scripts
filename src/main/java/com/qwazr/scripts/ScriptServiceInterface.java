/**
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
 **/
package com.qwazr.scripts;

import com.qwazr.server.ServerException;
import com.qwazr.server.ServiceInterface;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RolesAllowed(ScriptServiceInterface.SERVICE_NAME)
@Path("/" + ScriptServiceInterface.SERVICE_NAME)
public interface ScriptServiceInterface extends ServiceInterface {

	String SERVICE_NAME = "scripts";

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

	default RunThreadAbstract runSync(String scriptPath, Map<String, ?> objects)
			throws ServerException, IOException, ClassNotFoundException {
		throw new NotImplementedException("runSync");
	}

	default ScriptRunStatus runAsync(final String scriptPath, final Map<String, ?> objects)
			throws ServerException, IOException, ClassNotFoundException {
		throw new NotImplementedException("runSync");
	}

}
