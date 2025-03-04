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

package io.dingodb.common.concurrent;

import io.dingodb.common.util.PreParameters;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThreadPoolBuilder {

    private static final Integer AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final RejectedExecutionHandler DEFAULT_HANDLER = new ThreadPoolExecutor.AbortPolicy();

    private String name;
    private Integer coreThreads = AVAILABLE_PROCESSORS;
    private Integer maximumThreads = AVAILABLE_PROCESSORS << 2;
    private Long keepAliveSeconds = 60L;
    private BlockingQueue<Runnable> workQueue;
    private ThreadFactory threadFactory;
    private RejectedExecutionHandler handler;

    public ThreadPoolBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ThreadPoolBuilder coreThreads(Integer coreThreads) {
        this.coreThreads = coreThreads;
        return this;
    }

    public ThreadPoolBuilder maximumThreads(Integer maximumThreads) {
        this.maximumThreads = maximumThreads;
        return this;
    }

    public ThreadPoolBuilder keepAliveSeconds(Long keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
        return this;
    }

    public ThreadPoolBuilder workQueue(BlockingQueue<Runnable> workQueue) {
        this.workQueue = workQueue;
        return this;
    }

    public ThreadPoolBuilder threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public ThreadPoolBuilder handler(RejectedExecutionHandler handler) {
        this.handler = handler;
        return this;
    }

    protected ThreadFactory generateThreadFactory() {
        return new ThreadFactoryBuilder().name(name).build();
    }

    public ThreadPoolExecutor build() {
        PreParameters.nonNull(name, "Name must not null.");
        workQueue = PreParameters.cleanNull(workQueue, LinkedBlockingQueue::new);
        handler = PreParameters.cleanNull(handler, DEFAULT_HANDLER);
        threadFactory = PreParameters.cleanNull(threadFactory, this::generateThreadFactory);
        return new ThreadPoolExecutor(
            coreThreads,
            maximumThreads,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            workQueue,
            threadFactory,
            handler
        );
    }

}
