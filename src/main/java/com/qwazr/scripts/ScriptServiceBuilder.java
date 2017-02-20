/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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

import com.qwazr.cluster.ClusterManager;
import com.qwazr.cluster.ServiceBuilderAbstract;
import com.qwazr.server.RemoteService;

public class ScriptServiceBuilder extends ServiceBuilderAbstract<ScriptServiceInterface> {

	public ScriptServiceBuilder(final ClusterManager clusterManager, final ScriptManager scriptManager) {
		super(clusterManager, ScriptServiceInterface.SERVICE_NAME,
				scriptManager == null ? null : scriptManager.getService());
	}

	@Override
	public ScriptServiceInterface remote(final RemoteService remote) {
		return new ScriptSingleClient(remote);
	}

	@Override
	public ScriptServiceInterface remotes(final RemoteService[] remotes) {
		return new ScriptMultiClient(remotes);
	}
}