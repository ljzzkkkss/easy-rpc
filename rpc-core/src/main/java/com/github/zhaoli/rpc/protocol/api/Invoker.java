package com.github.zhaoli.rpc.protocol.api;


import com.github.zhaoli.rpc.registry.api.ServiceURL;
import com.github.zhaoli.rpc.common.exception.RPCException;
import com.github.zhaoli.rpc.common.domain.RPCResponse;

/**
 * @author zhaoli
 * @date 2018/7/7
 */
public interface Invoker<T> {

    Class<T> getInterface();

    String getInterfaceName();
    
    RPCResponse invoke(InvokeParam invokeParam) throws RPCException;
    
    /**
     * 本地服务返回本地IP地址，参数为空；集群服务抛出异常；远程服务返回注册中心中的ServiceURL
     * @return
     */
    ServiceURL getServiceURL();
    
    boolean isAvailable();
}
