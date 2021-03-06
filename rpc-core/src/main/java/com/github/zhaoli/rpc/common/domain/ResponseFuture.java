/**
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
package com.github.zhaoli.rpc.common.domain;

import java.util.concurrent.CompletableFuture;

public class ResponseFuture {
    private final String requestId;
    private final CompletableFuture<RPCResponse> future;
    private final long timestamp;

    public ResponseFuture(String requestId, CompletableFuture<RPCResponse> future) {
        this.requestId = requestId;
        this.future = future;
        this.timestamp = System.nanoTime();
    }

    public String getRequestId() {
        return requestId;
    }

    public CompletableFuture<RPCResponse> getFuture() {
        return future;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
