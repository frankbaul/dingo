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

package io.dingodb.raft.option;

import io.dingodb.raft.FSMCaller;
import io.dingodb.raft.conf.ConfigurationManager;
import io.dingodb.raft.core.NodeMetrics;
import io.dingodb.raft.entity.codec.LogEntryCodecFactory;
import io.dingodb.raft.entity.codec.v2.LogEntryV2CodecFactory;
import io.dingodb.raft.storage.LogStorage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogManagerOptions {
    private LogStorage logStorage;
    private ConfigurationManager configurationManager;
    private FSMCaller fsmCaller;
    private int disruptorBufferSize  = 1024;
    private RaftOptions raftOptions;
    private NodeMetrics nodeMetrics;
    private RaftLogStorageOptions raftLogStorageOptions;
    private LogEntryCodecFactory logEntryCodecFactory = LogEntryV2CodecFactory.getInstance();

    public LogEntryCodecFactory getLogEntryCodecFactory() {
        return this.logEntryCodecFactory;
    }
}
