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

package io.dingodb.common.util;

import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nonnull;

public final class Utils {
    private Utils() {
    }

    public static <T> T sole(@Nonnull Collection<T> collection) {
        if (collection.size() == 1) {
            for (T obj : collection) {
                return obj;
            }
        }
        throw new IllegalArgumentException("The collection contains zero or more than one elements.");
    }

    public static int max(@Nonnull int[] arr) {
        assert arr.length > 0;
        int max = arr[0];
        for (int i : arr) {
            if (i > max) {
                max = i;
            }
        }
        return max;
    }

    @Nonnull
    public static <T> T[] combine(@Nonnull T[] left, @Nonnull T[] right) {
        T[] res = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, res, left.length, right.length);
        return res;
    }

    public static char oddEvenHashAlphabetOrNumber(long value) {
        char[] alphabet = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                           'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        char[] number = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        if ((value & 1L) == 0) {
            return number[(int) (value % number.length)];
        } else {
            return alphabet[(int) (value % alphabet.length)];
        }
    }
}
