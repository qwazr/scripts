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
import com.qwazr.utils.server.ServerException;

import java.net.URISyntaxException;
import java.util.Map;

public class ScriptClusterServiceImpl extends ScriptSingleServiceImpl {

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus(Boolean local, String group, Integer msTimeout) {
		if (local != null && local)
			return super.getRunsStatus(local, group, msTimeout);
		try {
			return getMultiClient(group, msTimeout).getRunsStatus(false, group, msTimeout);
		} catch (URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	public static ScriptMultiClient getMultiClient(String group, Integer msTimeout) throws URISyntaxException {
		String[] urls = ClusterManager.getInstance().getClusterClient()
				.getActiveNodesByService(ScriptManager.SERVICE_NAME_SCRIPT, group);
		return new ScriptMultiClient(ScriptManager.INSTANCE.executorService, urls, msTimeout);
	}
}
