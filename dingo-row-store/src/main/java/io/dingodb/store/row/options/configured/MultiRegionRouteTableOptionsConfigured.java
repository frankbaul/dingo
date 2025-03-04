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

package io.dingodb.store.row.options.configured;

import io.dingodb.raft.util.Requires;
import io.dingodb.store.row.options.RegionRouteTableOptions;
import io.dingodb.store.row.util.Configured;
import io.dingodb.store.row.util.Lists;
import io.dingodb.store.row.util.Maps;

import java.util.List;
import java.util.Map;

// Refer to SOFAJRaft: <A>https://github.com/sofastack/sofa-jraft/<A/>
public final class MultiRegionRouteTableOptionsConfigured implements Configured<List<RegionRouteTableOptions>> {
    private final Map<String, RegionRouteTableOptions> optsTable;

    public static MultiRegionRouteTableOptionsConfigured newConfigured() {
        return new MultiRegionRouteTableOptionsConfigured(Maps.newHashMap());
    }

    public MultiRegionRouteTableOptionsConfigured withStartKey(final String regionId, final String startKey) {
        getOrCreateOptsById(regionId).setStartKey(startKey);
        return this;
    }

    public MultiRegionRouteTableOptionsConfigured withStartKeyBytes(final String regionId, final byte[] startKeyBytes) {
        getOrCreateOptsById(regionId).setStartKeyBytes(startKeyBytes);
        return this;
    }

    public MultiRegionRouteTableOptionsConfigured withEndKey(final String regionId, final String endKey) {
        getOrCreateOptsById(regionId).setEndKey(endKey);
        return this;
    }

    public MultiRegionRouteTableOptionsConfigured withEndKeyBytes(final String regionId, final byte[] endKeyBytes) {
        getOrCreateOptsById(regionId).setEndKeyBytes(endKeyBytes);
        return this;
    }

    public MultiRegionRouteTableOptionsConfigured withInitialServerList(final String regionId,
                                                                        final String initialServerList) {
        getOrCreateOptsById(regionId).setInitialServerList(initialServerList);
        return this;
    }

    @Override
    public List<RegionRouteTableOptions> config() {
        return Lists.newArrayList(this.optsTable.values());
    }

    private RegionRouteTableOptions getOrCreateOptsById(final String regionId) {
        Requires.requireNonNull(regionId, "regionId");
        RegionRouteTableOptions opts = this.optsTable.get(regionId);
        if (opts != null) {
            return opts;
        }
        opts = new RegionRouteTableOptions();
        opts.setRegionId(regionId);
        this.optsTable.put(regionId, opts);
        return opts;
    }

    public MultiRegionRouteTableOptionsConfigured(Map<String, RegionRouteTableOptions> optsTable) {
        this.optsTable = optsTable;
    }
}
