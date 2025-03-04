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

package io.dingodb.net.netty.channel.impl;

import io.dingodb.net.netty.channel.ChannelId;
import io.dingodb.net.netty.channel.ChannelIdAllocator;
import io.dingodb.net.netty.channel.ChannelIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class UnlimitedChannelIdAllocator<I extends ChannelId> implements ChannelIdAllocator<I> {
    private static final Logger logger = LoggerFactory.getLogger(UnlimitedChannelIdAllocator.class);

    private final ChannelIdProvider<I> channelIdProvider;
    private final AtomicInteger idAcc;

    public UnlimitedChannelIdAllocator(ChannelIdProvider<I> channelIdProvider) {
        this.channelIdProvider = channelIdProvider;
        idAcc = new AtomicInteger(1);
    }

    @Override
    public I alloc() {
        return channelIdProvider.get(idAcc.getAndIncrement());
    }

    @Override
    public I alloc(long timeout, TimeUnit unit) {
        return alloc();
    }

    @Override
    public void release(I channelId) {

    }
}
