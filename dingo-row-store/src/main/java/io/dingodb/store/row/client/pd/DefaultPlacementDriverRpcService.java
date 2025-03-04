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

package io.dingodb.store.row.client.pd;

import io.dingodb.raft.Status;
import io.dingodb.raft.rpc.InvokeCallback;
import io.dingodb.raft.rpc.InvokeContext;
import io.dingodb.raft.rpc.RpcClient;
import io.dingodb.raft.rpc.impl.BoltRpcClient;
import io.dingodb.raft.util.Endpoint;
import io.dingodb.raft.util.ExecutorServiceHelper;
import io.dingodb.raft.util.NamedThreadFactory;
import io.dingodb.raft.util.Requires;
import io.dingodb.raft.util.ThreadPoolUtil;
import io.dingodb.store.row.client.failover.FailoverClosure;
import io.dingodb.store.row.cmd.pd.BaseRequest;
import io.dingodb.store.row.cmd.pd.BaseResponse;
import io.dingodb.store.row.errors.Errors;
import io.dingodb.store.row.errors.ErrorsHelper;
import io.dingodb.store.row.options.RpcOptions;
import io.dingodb.store.row.rpc.ExtSerializerSupports;
import io.dingodb.store.row.util.concurrent.CallerRunsPolicyWithReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

// Refer to SOFAJRaft: <A>https://github.com/sofastack/sofa-jraft/<A/>
public class DefaultPlacementDriverRpcService implements PlacementDriverRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultPlacementDriverRpcService.class);

    private final PlacementDriverClient pdClient;
    private final RpcClient rpcClient;

    private ThreadPoolExecutor rpcCallbackExecutor;
    private int rpcTimeoutMillis;

    private boolean started;

    public DefaultPlacementDriverRpcService(PlacementDriverClient pdClient) {
        this.pdClient = pdClient;
        this.rpcClient = ((AbstractPlacementDriverClient) pdClient).getRpcClient();
    }

    @Override
    public synchronized boolean init(final RpcOptions opts) {
        if (this.started) {
            LOG.info("[DefaultPlacementDriverRpcService] already started.");
            return true;
        }
        this.rpcCallbackExecutor = createRpcCallbackExecutor(opts);
        this.rpcTimeoutMillis = opts.getRpcTimeoutMillis();
        Requires.requireTrue(this.rpcTimeoutMillis > 0, "opts.rpcTimeoutMillis must > 0");
        LOG.info("[DefaultPlacementDriverRpcService] start successfully, options: {}.", opts);
        return this.started = true;
    }

    @Override
    public synchronized void shutdown() {
        ExecutorServiceHelper.shutdownAndAwaitTermination(this.rpcCallbackExecutor);
        this.started = false;
        LOG.info("[DefaultPlacementDriverRpcService] shutdown successfully.");
    }

    @Override
    public <V> CompletableFuture<V> callPdServerWithRpc(final BaseRequest request, final FailoverClosure<V> closure,
                                                        final Errors lastCause) {
        final boolean forceRefresh = ErrorsHelper.isInvalidPeer(lastCause);
        final Endpoint endpoint = this.pdClient.getPdLeader(forceRefresh, this.rpcTimeoutMillis);
        internalCallPdWithRpc(endpoint, request, closure);
        return closure.future();
    }

    private <V> void internalCallPdWithRpc(final Endpoint endpoint, final BaseRequest request,
                                           final FailoverClosure<V> closure) {
        final InvokeContext invokeCtx = new InvokeContext();
        invokeCtx.put(BoltRpcClient.BOLT_CTX, ExtSerializerSupports.getInvokeContext());
        final InvokeCallback invokeCallback = new InvokeCallback() {

            @Override
            public void complete(final Object result, final Throwable err) {
                if (err == null) {
                    final BaseResponse<?> response = (BaseResponse<?>) result;
                    if (response.isSuccess()) {
                        closure.setData(response.getValue());
                        closure.run(Status.OK());
                    } else {
                        closure.setError(response.getError());
                        closure.run(new Status(-1, "RPC failed with address: %s, response: %s", endpoint, response));
                    }
                } else {
                    closure.failure(err);
                }
            }

            @Override
            public Executor executor() {
                return rpcCallbackExecutor;
            }
        };

        try {
            this.rpcClient.invokeAsync(endpoint, request, invokeCtx, invokeCallback, this.rpcTimeoutMillis);
        } catch (final Throwable t) {
            closure.failure(t);
        }
    }

    private ThreadPoolExecutor createRpcCallbackExecutor(final RpcOptions opts) {
        final int callbackExecutorCorePoolSize = opts.getCallbackExecutorCorePoolSize();
        final int callbackExecutorMaximumPoolSize = opts.getCallbackExecutorMaximumPoolSize();
        if (callbackExecutorCorePoolSize <= 0 || callbackExecutorMaximumPoolSize <= 0) {
            return null;
        }

        final String name = "dingo-row-store-pd-rpc-callback";
        return ThreadPoolUtil.newBuilder() //
            .poolName(name) //
            .enableMetric(true) //
            .coreThreads(callbackExecutorCorePoolSize) //
            .maximumThreads(callbackExecutorMaximumPoolSize) //
            .keepAliveSeconds(120L) //
            .workQueue(new ArrayBlockingQueue<>(opts.getCallbackExecutorQueueCapacity())) //
            .threadFactory(new NamedThreadFactory(name, true)) //
            .rejectedHandler(new CallerRunsPolicyWithReport(name)) //
            .build();
    }
}
