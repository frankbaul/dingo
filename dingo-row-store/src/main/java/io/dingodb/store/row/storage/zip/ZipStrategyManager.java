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

package io.dingodb.store.row.storage.zip;

import io.dingodb.store.row.options.DingoRowStoreOptions;

// Refer to SOFAJRaft: <A>https://github.com/sofastack/sofa-jraft/<A/>
public final class ZipStrategyManager {
    private static ZipStrategy[] zipStrategies = new ZipStrategy[5];
    private static byte DEFAULT_STRATEGY = 1;
    public static final byte JDK_STRATEGY = 1;
    public static final byte PARALLEL_STRATEGY = 2;

    static {
        addZipStrategy(JDK_STRATEGY, new JDKZipStrategy());
    }

    public static void addZipStrategy(final int idx, final ZipStrategy zipStrategy) {
        if (zipStrategies.length <= idx) {
            final ZipStrategy[] newZipStrategies = new ZipStrategy[idx + 5];
            System.arraycopy(zipStrategies, 0, newZipStrategies, 0, zipStrategies.length);
            zipStrategies = newZipStrategies;
        }
        zipStrategies[idx] = zipStrategy;
    }

    public static ZipStrategy getZipStrategy(final int idx) {
        return zipStrategies[idx];
    }

    public static ZipStrategy getDefault() {
        return zipStrategies[DEFAULT_STRATEGY];
    }

    public static void init(final DingoRowStoreOptions opts) {
        // add parallel zip strategy
        if (opts.isUseParallelCompress()) {
            if (zipStrategies[PARALLEL_STRATEGY] != null) {
                final ZipStrategy zipStrategy = new ParallelZipStrategy(opts.getCompressThreads(),
                    opts.getDeCompressThreads());
                ZipStrategyManager.addZipStrategy(ZipStrategyManager.PARALLEL_STRATEGY, zipStrategy);
                DEFAULT_STRATEGY = PARALLEL_STRATEGY;
            }
        }
    }

    private ZipStrategyManager() {
    }
}
