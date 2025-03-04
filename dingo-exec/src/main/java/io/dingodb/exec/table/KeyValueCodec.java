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

package io.dingodb.exec.table;

import io.dingodb.common.codec.AvroCodec;
import io.dingodb.common.table.TupleMapping;
import io.dingodb.common.table.TupleSchema;
import io.dingodb.store.api.KeyValue;

import java.io.IOException;
import javax.annotation.Nonnull;

public class KeyValueCodec {
    private final AvroCodec keyCodec;
    private final AvroCodec valueCodec;
    private final TupleMapping keyMapping;
    private final TupleMapping valueMapping;

    public KeyValueCodec(@Nonnull TupleSchema schema, @Nonnull TupleMapping keyMapping) {
        this.keyMapping = keyMapping;
        this.valueMapping = keyMapping.inverse(schema.size());
        keyCodec = new AvroCodec(schema.select(keyMapping).getAvroSchema());
        valueCodec = new AvroCodec(schema.select(valueMapping).getAvroSchema());
    }

    public Object[] decode(@Nonnull KeyValue keyValue) throws IOException {
        Object[] result = new Object[keyMapping.size() + valueMapping.size()];
        keyCodec.decode(result, keyValue.getKey(), keyMapping);
        valueCodec.decode(result, keyValue.getValue(), valueMapping);
        return result;
    }

    public KeyValue encode(@Nonnull Object[] record) throws IOException {
        return new KeyValue(
            keyCodec.encode(record, keyMapping),
            valueCodec.encode(record, valueMapping)
        );
    }

    public byte[] encodeKey(@Nonnull Object[] keys) throws IOException {
        return keyCodec.encode(keys);
    }

    public Object[] mapKeyAndDecodeValue(@Nonnull Object[] keys, byte[] bytes) throws IOException {
        Object[] result = new Object[keyMapping.size() + valueMapping.size()];
        keyMapping.map(result, keys);
        valueCodec.decode(result, bytes, valueMapping);
        return result;
    }
}
