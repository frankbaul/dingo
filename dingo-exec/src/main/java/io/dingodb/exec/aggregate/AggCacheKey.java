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

package io.dingodb.exec.aggregate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
public class AggCacheKey {
    @Getter
    private final Object[] tuple;

    @Override
    public int hashCode() {
        return Arrays.hashCode(tuple);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AggCacheKey) {
            return Arrays.equals(this.tuple, ((AggCacheKey) obj).tuple);
        }
        return false;
    }
}
