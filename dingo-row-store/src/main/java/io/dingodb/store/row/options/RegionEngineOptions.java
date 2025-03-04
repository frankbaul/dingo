/*
 * Copyright 2021 DataCanvas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dingodb.store.row.options;

import io.dingodb.raft.option.NodeOptions;
import io.dingodb.raft.util.BytesUtil;
import io.dingodb.raft.util.Copiable;
import io.dingodb.raft.util.Endpoint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Refer to SOFAJRaft: <A>https://github.com/sofastack/sofa-jraft/<A/>
@Getter
@Setter
@ToString
public class RegionEngineOptions implements Copiable<RegionEngineOptions> {
    private String        regionId;
    private String      startKey;
    private byte[]      startKeyBytes;
    private String      endKey;
    private byte[]      endKeyBytes;
    private NodeOptions nodeOptions;
    // Should extends from StoreEngineOptions
    private String      raftGroupId;
    // Should extends from StoreEngineOptions
    private String      raftDataPath;
    // Should extends from StoreEngineOptions
    private Endpoint serverAddress;
    // Should extends from StoreEngineOptions
    private String      initialServerList;
    // Can extends from StoreEngineOptions
    private long        metricsReportPeriod;
    private HeartbeatOptions          heartbeatOptions;
    private RaftStoreOptions raftStoreOptions;

    public void setStartKey(String startKey) {
        this.startKey = startKey;
        this.startKeyBytes = BytesUtil.writeUtf8(startKey);
    }

    public void setEndKey(String endKey) {
        this.endKey = endKey;
        this.endKeyBytes = BytesUtil.writeUtf8(endKey);
    }

    @Override
    public RegionEngineOptions copy() {
        final RegionEngineOptions copy = new RegionEngineOptions();
        copy.setRegionId(this.regionId);
        copy.setStartKey(this.startKey);
        copy.setStartKeyBytes(this.startKeyBytes);
        copy.setEndKey(this.endKey);
        copy.setEndKeyBytes(this.endKeyBytes);
        copy.setNodeOptions(this.nodeOptions == null ? new NodeOptions() : this.nodeOptions.copy());
        copy.setRaftGroupId(this.raftGroupId);
        copy.setRaftDataPath(this.raftDataPath);
        copy.setServerAddress(this.serverAddress);
        copy.setInitialServerList(this.initialServerList);
        copy.setMetricsReportPeriod(this.metricsReportPeriod);
        copy.setRaftStoreOptions(this.raftStoreOptions);
        return copy;
    }
}
