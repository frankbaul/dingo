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

package io.dingodb.store.row;

import io.dingodb.raft.util.Describer;
import io.dingodb.store.row.util.Lists;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Refer to SOFAJRaft: <A>https://github.com/sofastack/sofa-jraft/<A/>
public final class DescriberManager {
    private static final DescriberManager INSTANCE = new DescriberManager();

    private final List<Describer> describers = new CopyOnWriteArrayList<>();

    public static DescriberManager getInstance() {
        return INSTANCE;
    }

    public void addDescriber(final Describer describer) {
        this.describers.add(describer);
    }

    public List<Describer> getAllDescribers() {
        return Lists.newArrayList(this.describers);
    }
}
