package com.github.zhaoli.rpc.protocol.api.support;

import com.github.zhaoli.rpc.config.ReferenceConfig;
import com.github.zhaoli.rpc.protocol.api.InvokeParam;
import com.github.zhaoli.rpc.common.domain.RPCRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhaoli
 * @date 2018/7/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RPCInvokeParam implements InvokeParam {
    protected ReferenceConfig referenceConfig;
    protected RPCRequest rpcRequest;

    public ReferenceConfig getReferenceConfig() {
        return referenceConfig;
    }

    public RPCRequest getRpcRequest() {
        return rpcRequest;
    }

    @Override
    public String getInterfaceName() {
        return rpcRequest.getInterfaceName();
    }

    @Override
    public String getMethodName() {
        return rpcRequest.getMethodName();
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return rpcRequest.getParameterTypes();
    }

    @Override
    public Object[] getParameters() {
        return rpcRequest.getParameters();
    }

    @Override
    public String getRequestId() {
        return rpcRequest.getRequestId();
    }

    @Override
    public String toString() {
        return "RPCInvokeParam{" +
                rpcRequest +
                '}';
    }
}
