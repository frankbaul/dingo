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

package io.dingodb.raft.rpc.impl;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.dingodb.raft.Status;
import io.dingodb.raft.error.InvokeTimeoutException;
import io.dingodb.raft.error.RaftError;
import io.dingodb.raft.error.RemotingException;
import io.dingodb.raft.option.RpcOptions;
import io.dingodb.raft.rpc.ClientService;
import io.dingodb.raft.rpc.InvokeCallback;
import io.dingodb.raft.rpc.InvokeContext;
import io.dingodb.raft.rpc.ProtobufMsgFactory;
import io.dingodb.raft.rpc.RaftRpcFactory;
import io.dingodb.raft.rpc.RpcClient;
import io.dingodb.raft.rpc.RpcRequests;
import io.dingodb.raft.rpc.RpcResponseClosure;
import io.dingodb.raft.rpc.RpcResponseFactory;
import io.dingodb.raft.rpc.RpcUtils;
import io.dingodb.raft.util.Endpoint;
import io.dingodb.raft.util.NamedThreadFactory;
import io.dingodb.raft.util.RpcFactoryHelper;
import io.dingodb.raft.util.ThreadPoolMetricSet;
import io.dingodb.raft.util.ThreadPoolUtil;
import io.dingodb.raft.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

// Refer to SOFAJRaft: <A>https://github.com/sofastack/sofa-jraft/<A/>
public abstract class AbstractClientService implements ClientService {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractClientService.class);

    static {
        ProtobufMsgFactory.load();
    }

    protected volatile RpcClient rpcClient;
    protected ThreadPoolExecutor rpcExecutor;
    protected RpcOptions rpcOptions;

    public RpcClient getRpcClient() {
        return this.rpcClient;
    }

    @Override
    public boolean isConnected(final Endpoint endpoint) {
        final RpcClient rc = this.rpcClient;
        return rc != null && isConnected(rc, endpoint);
    }

    private static boolean isConnected(final RpcClient rpcClient, final Endpoint endpoint) {
        return rpcClient.checkConnection(endpoint);
    }

    @Override
    public boolean checkConnection(final Endpoint endpoint, final boolean createIfAbsent) {
        final RpcClient rc = this.rpcClient;
        if (rc == null) {
            throw new IllegalStateException("Client service is uninitialized.");
        }
        return rc.checkConnection(endpoint, createIfAbsent);
    }

    @Override
    public synchronized boolean init(final RpcOptions rpcOptions) {
        if (this.rpcClient != null) {
            return true;
        }
        this.rpcOptions = rpcOptions;
        return initRpcClient(this.rpcOptions.getRpcProcessorThreadPoolSize());
    }

    protected void configRpcClient(final RpcClient rpcClient) {
        // NO-OP
    }

    protected boolean initRpcClient(final int rpcProcessorThreadPoolSize) {
        final RaftRpcFactory factory = RpcFactoryHelper.rpcFactory();
        this.rpcClient = factory.createRpcClient(factory.defaultJRaftClientConfigHelper(this.rpcOptions));
        configRpcClient(this.rpcClient);
        this.rpcClient.init(null);
        this.rpcExecutor = ThreadPoolUtil.newBuilder() //
            .poolName("JRaft-RPC-Processor") //
            .enableMetric(true) //
            .coreThreads(rpcProcessorThreadPoolSize / 3) //
            .maximumThreads(rpcProcessorThreadPoolSize) //
            .keepAliveSeconds(60L) //
            .workQueue(new ArrayBlockingQueue<>(10000)) //
            .threadFactory(new NamedThreadFactory("JRaft-RPC-Processor-", true)) //
            .build();
        if (this.rpcOptions.getMetricRegistry() != null) {
            this.rpcOptions.getMetricRegistry().register("raft-rpc-client-thread-pool",
                new ThreadPoolMetricSet(this.rpcExecutor));
            Utils.registerClosureExecutorMetrics(this.rpcOptions.getMetricRegistry());
        }
        return true;
    }

    @Override
    public synchronized void shutdown() {
        if (this.rpcClient != null) {
            this.rpcClient.shutdown();
            this.rpcClient = null;
            this.rpcExecutor.shutdown();
        }
    }

    @Override
    public boolean connect(final Endpoint endpoint) {
        final RpcClient rc = this.rpcClient;
        if (rc == null) {
            throw new IllegalStateException("Client service is uninitialized.");
        }
        if (isConnected(rc, endpoint)) {
            return true;
        }
        try {
            final RpcRequests.PingRequest req = RpcRequests.PingRequest.newBuilder() //
                .setSendTimestamp(System.currentTimeMillis()) //
                .build();
            final RpcRequests.ErrorResponse resp = (RpcRequests.ErrorResponse) rc.invokeSync(endpoint, req,
                this.rpcOptions.getRpcConnectTimeoutMs());
            return resp.getErrorCode() == 0;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (final RemotingException e) {
            LOG.error("Fail to connect {}, remoting exception: {}.", endpoint, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean disconnect(final Endpoint endpoint) {
        final RpcClient rc = this.rpcClient;
        if (rc == null) {
            return true;
        }
        LOG.info("Disconnect from {}.", endpoint);
        rc.closeConnection(endpoint);
        return true;
    }

    @Override
    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final RpcResponseClosure<T> done, final int timeoutMs) {
        return invokeWithDone(endpoint, request, done, timeoutMs, this.rpcExecutor);
    }

    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final RpcResponseClosure<T> done, final int timeoutMs,
                                                              final Executor rpcExecutor) {
        return invokeWithDone(endpoint, request, null, done, timeoutMs, rpcExecutor);
    }

    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final InvokeContext ctx,
                                                              final RpcResponseClosure<T> done, final int timeoutMs) {
        return invokeWithDone(endpoint, request, ctx, done, timeoutMs, this.rpcExecutor);
    }

    public <T extends Message> Future<Message> invokeWithDone(final Endpoint endpoint, final Message request,
                                                              final InvokeContext ctx,
                                                              final RpcResponseClosure<T> done, final int timeoutMs,
                                                              final Executor rpcExecutor) {
        final RpcClient rc = this.rpcClient;
        final FutureImpl<Message> future = new FutureImpl<>();
        final Executor currExecutor = rpcExecutor != null ? rpcExecutor : this.rpcExecutor;
        try {
            if (rc == null) {
                future.failure(new IllegalStateException("Client service is uninitialized."));
                // should be in another thread to avoid dead locking.
                RpcUtils.runClosureInExecutor(currExecutor, done, new Status(RaftError.EINTERNAL,
                    "Client service is uninitialized."));
                return future;
            }

            rc.invokeAsync(endpoint, request, ctx, new InvokeCallback() {

                @SuppressWarnings({ "unchecked", "ConstantConditions" })
                @Override
                public void complete(final Object result, final Throwable err) {
                    if (future.isCancelled()) {
                        onCanceled(request, done);
                        return;
                    }

                    if (err == null) {
                        Status status = Status.OK();
                        Message msg;
                        if (result instanceof RpcRequests.ErrorResponse) {
                            status = handleErrorResponse((RpcRequests.ErrorResponse) result);
                            msg = (Message) result;
                        } else if (result instanceof Message) {
                            final Descriptors.FieldDescriptor fd = ((Message) result).getDescriptorForType() //
                                .findFieldByNumber(RpcResponseFactory.ERROR_RESPONSE_NUM);
                            if (fd != null && ((Message) result).hasField(fd)) {
                                final RpcRequests.ErrorResponse eResp = (RpcRequests.ErrorResponse) ((Message) result).getField(fd);
                                status = handleErrorResponse(eResp);
                                msg = eResp;
                            } else {
                                msg = (T) result;
                            }
                        } else {
                            msg = (T) result;
                        }
                        if (done != null) {
                            try {
                                if (status.isOk()) {
                                    done.setResponse((T) msg);
                                }
                                done.run(status);
                            } catch (final Throwable t) {
                                LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
                            }
                        }
                        if (!future.isDone()) {
                            future.setResult(msg);
                        }
                    } else {
                        if (done != null) {
                            try {
                                done.run(new Status(err instanceof InvokeTimeoutException ? RaftError.ETIMEDOUT
                                    : RaftError.EINTERNAL, "RPC exception:" + err.getMessage()));
                            } catch (final Throwable t) {
                                LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
                            }
                        }
                        if (!future.isDone()) {
                            future.failure(err);
                        }
                    }
                }

                @Override
                public Executor executor() {
                    return currExecutor;
                }
            }, timeoutMs <= 0 ? this.rpcOptions.getRpcDefaultTimeout() : timeoutMs);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            future.failure(e);
            // should be in another thread to avoid dead locking.
            RpcUtils.runClosureInExecutor(currExecutor, done,
                new Status(RaftError.EINTR, "Sending rpc was interrupted"));
        } catch (final RemotingException e) {
            future.failure(e);
            // should be in another thread to avoid dead locking.
            RpcUtils.runClosureInExecutor(currExecutor, done, new Status(RaftError.EINTERNAL,
                "Fail to send a RPC request:" + e.getMessage()));

        }

        return future;
    }

    private static Status handleErrorResponse(final RpcRequests.ErrorResponse eResp) {
        final Status status = new Status();
        status.setCode(eResp.getErrorCode());
        if (eResp.hasErrorMsg()) {
            status.setErrorMsg(eResp.getErrorMsg());
        }
        return status;
    }

    private <T extends Message> void onCanceled(final Message request, final RpcResponseClosure<T> done) {
        if (done != null) {
            try {
                done.run(new Status(RaftError.ECANCELED, "RPC request was canceled by future."));
            } catch (final Throwable t) {
                LOG.error("Fail to run RpcResponseClosure, the request is {}.", request, t);
            }
        }
    }

}
