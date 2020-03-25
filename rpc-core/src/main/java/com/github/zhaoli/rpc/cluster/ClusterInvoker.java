package com.github.zhaoli.rpc.cluster;

import com.github.zhaoli.rpc.common.context.RPCThreadLocalContext;
import com.github.zhaoli.rpc.common.domain.RPCResponse;
import com.github.zhaoli.rpc.common.enumeration.ErrorEnum;
import com.github.zhaoli.rpc.common.exception.RPCException;
import com.github.zhaoli.rpc.common.util.InvokeParamUtil;
import com.github.zhaoli.rpc.config.GlobalConfig;
import com.github.zhaoli.rpc.config.ReferenceConfig;
import com.github.zhaoli.rpc.protocol.api.InvokeParam;
import com.github.zhaoli.rpc.protocol.api.Invoker;
import com.github.zhaoli.rpc.protocol.api.support.AbstractRemoteProtocol;
import com.github.zhaoli.rpc.protocol.injvm.InJvmProtocol;
import com.github.zhaoli.rpc.registry.api.ServiceURL;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author zhaoli
 * @date 2018/7/15
 * 代表一个interface的集群，核心类，持有其他cluster组件，如loadbalancer和failureHandler
 * 一个接口的一个地址可以定位到一个invoker，但只需要一个地址就可以定位到endpoint
 * invoker与endpoint不是一一对应的
 */
@Slf4j
public class ClusterInvoker<T> implements Invoker<T> {
    private Class<T> interfaceClass;
    private String interfaceName;
    /**
     * key是address，value是一个invoker
     */
    private Map<String, Invoker<T>> addressInvokers = new ConcurrentHashMap<>();
    private GlobalConfig globalConfig;


    public ClusterInvoker(Class<T> interfaceClass, String interfaceName, GlobalConfig globalConfig) {
        this.interfaceClass = interfaceClass;
        this.interfaceName = interfaceName;
        this.globalConfig = globalConfig;
        init();
    }

    //TODO 这里写的比较僵硬，如果是injvm协议，就完全不考虑注册中心了
    private void init() {
        if (globalConfig.getProtocol() instanceof InJvmProtocol) {
            addOrUpdate(ServiceURL.DEFAULT_SERVICE_URL);
        } else {
            globalConfig.getServiceRegistry().discover(interfaceName, (newServiceURLs -> {
                removeNotExisted(newServiceURLs);
            }), (serviceURL -> {
                addOrUpdate(serviceURL);
            }));
        }
    }

    /**
     * addr1,addr2,addr3 -> addr2?weight=20,addr3,addr4
     * <p>
     * 1) addOrUpdate(addr2) -> updateEndpointConfig(addr2)
     * 2) addOrUpdate(addr3) -> updateEndpointConfig(addr3)
     * 3) addOrUpdate(addr4) -> add(addr4)
     * 4) removeNotExisted(addr2,addr3,addr4) -> remove(addr1)
     *
     * @param serviceURL
     */
    private synchronized void addOrUpdate(ServiceURL serviceURL) {
        // 地址多了/更新
        // 更新
        if (addressInvokers.containsKey(serviceURL.getAddress())) {
            // 我们知道只有远程服务才有可能会更新
            // 更新配置与invoker无关，只需要Protocol负责
            //TODO refactor this
            if (globalConfig.getProtocol() instanceof AbstractRemoteProtocol) {
                AbstractRemoteProtocol protocol = (AbstractRemoteProtocol) globalConfig.getProtocol();
                log.info("update config:{},当前interface为:{}", serviceURL, interfaceName);
                protocol.updateEndpointConfig(serviceURL);
            }
        } else {
            // 添加
            // 需要修改
            log.info("add invoker:{},serviceURL:{}", interfaceName, serviceURL);
            Invoker invoker = globalConfig.getProtocol().refer(ReferenceConfig.getReferenceConfigByInterfaceName(interfaceName), serviceURL);
            // refer拿到的是InvokerDelegate
            addressInvokers.put(serviceURL.getAddress(), invoker);
        }
    }

    public List<Invoker> getInvokers() {
        // 拷贝一份返回
        return new ArrayList<>(addressInvokers.values());
    }

    /**
     * 在该方法调用前，会将新的加进来，所以这里只需要去掉新的没有的。
     * 旧的一定包含了新的，遍历旧的，如果不在新的里面，则需要删掉
     *
     * @param newServiceURLs
     */
    public synchronized void removeNotExisted(List<ServiceURL> newServiceURLs) {
        Map<String, ServiceURL> newAddressesMap = newServiceURLs.stream().collect(Collectors.toMap(
                url -> url.getAddress(), url -> url
        ));

        // 地址少了
        // 说明一个服务器挂掉了或出故障了，我们需要把该服务器对应的所有invoker都关掉。
        for (Iterator<Map.Entry<String, Invoker<T>>> it = addressInvokers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Invoker<T>> curr = it.next();
            if (!newAddressesMap.containsKey(curr.getKey())) {
                log.info("remove address:{},当前interface为:{}", curr.getKey(), interfaceName);
                if (globalConfig.getProtocol() instanceof AbstractRemoteProtocol) {
                    AbstractRemoteProtocol protocol = (AbstractRemoteProtocol) globalConfig.getProtocol();
                    protocol.closeEndpoint(curr.getKey());
                }
                it.remove();
            }
        }
    }

    @Override
    public Class<T> getInterface() {
        return interfaceClass;
    }

    @Override
    public String getInterfaceName() {
        return interfaceName;
    }

    /**
     * 从可用的invoker中选择一个，如果没有或者不可用，则抛出异常
     *
     * @param availableInvokers
     * @param invokeParam
     * @return
     */
    private Invoker doSelect(List<Invoker> availableInvokers, InvokeParam invokeParam) {
        if (availableInvokers.size() == 0) {
            log.error("未找到可用服务器");
            throw new RPCException(ErrorEnum.NO_SERVER_AVAILABLE, "未找到可用服务器");
        }
        Invoker invoker;
        if (availableInvokers.size() == 1) {
            invoker = availableInvokers.get(0);
            if (invoker.isAvailable()) {
                return invoker;
            } else {
                log.error("未找到可用服务器");
                throw new RPCException(ErrorEnum.NO_SERVER_AVAILABLE, "未找到可用服务器");
            }
        }
        invoker = globalConfig.getLoadBalancer().select(availableInvokers, InvokeParamUtil.extractRequestFromInvokeParam(invokeParam));
        if (invoker.isAvailable()) {
            return invoker;
        } else {
            availableInvokers.remove(invoker);
            return doSelect(availableInvokers, invokeParam);
        }
    }

    @Override
    public RPCResponse invoke(InvokeParam invokeParam) throws RPCException {
        Invoker invoker = doSelect(getInvokers(), invokeParam);
        RPCThreadLocalContext.getContext().setInvoker(invoker);
        try {
            // 这里只会抛出RPCException
            RPCResponse response = invoker.invoke(invokeParam);
            // response有可能是null，比如callback、oneway和future
            if (response == null) {
                return null;
            }
            // 不管是传输时候抛异常，还是服务端抛出异常，都算异常
            if (response.hasError()) {
                // 回收response
                Throwable cause = response.getCause();
                response.recycle();
                throw new RPCException(cause, ErrorEnum.SERVICE_INVOCATION_FAILURE, "invocation failed");
            }
            // 第一次就OK
            return response;
        } catch (RPCException e) {
            // 重试后OK
            // 在这里再抛出异常，就没有返回值了
            return globalConfig.getFaultToleranceHandler().handle(this, invokeParam, e);
        }
    }

    /**
     * 这里不需要捕获invoker#invoke的异常，会由retryer来捕获
     *
     * @param availableInvokers
     * @param invokeParam
     * @return
     */
    public RPCResponse invokeForFaultTolerance(List<Invoker> availableInvokers, InvokeParam invokeParam) {
        Invoker invoker = doSelect(availableInvokers, invokeParam);
        RPCThreadLocalContext.getContext().setInvoker(invoker);
        // 这里只会抛出RPCException
        RPCResponse response = invoker.invoke(invokeParam);
        if (response == null) {
            return null;
        }
        // 不管是传输时候抛异常，还是服务端抛出异常，都算异常
        if (response.hasError()) {
            throw new RPCException(response.getCause(), ErrorEnum.SERVICE_INVOCATION_FAILURE, "invocation failed");
        }
        return response;
    }

    @Override
    public ServiceURL getServiceURL() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
