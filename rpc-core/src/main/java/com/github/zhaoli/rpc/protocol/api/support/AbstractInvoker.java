package com.github.zhaoli.rpc.protocol.api.support;

import com.github.zhaoli.rpc.config.GlobalConfig;
import com.github.zhaoli.rpc.filter.Filter;
import com.github.zhaoli.rpc.registry.api.ServiceURL;
import com.github.zhaoli.rpc.common.domain.RPCRequest;
import com.github.zhaoli.rpc.common.domain.RPCResponse;
import com.github.zhaoli.rpc.common.enumeration.ErrorEnum;
import com.github.zhaoli.rpc.common.enumeration.InvocationType;
import com.github.zhaoli.rpc.common.exception.RPCException;
import com.github.zhaoli.rpc.protocol.api.InvokeParam;
import com.github.zhaoli.rpc.protocol.api.Invoker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author zhaoli
 * @date 2018/7/21
 */
@Slf4j
public abstract class AbstractInvoker<T> implements Invoker<T> {
    private Class<T> interfaceClass;
    private String interfaceName;
    private GlobalConfig globalConfig;
    
    @Override
    public RPCResponse invoke(InvokeParam invokeParam) throws RPCException {
        Function<RPCRequest, Future<RPCResponse>> logic = getProcessor();
        return InvocationType.get(invokeParam).invoke(invokeParam,logic);
    }

    /**
     * 如果没有重写invoke方法，则必须重写该方法
     *
     * @return
     */
    protected Function<RPCRequest, Future<RPCResponse>> getProcessor() {
        throw new RPCException(ErrorEnum.GET_PROCESSOR_MUST_BE_OVERRIDE_WHEN_INVOKE_DID_NOT_OVERRIDE, "没有重写AbstractInvoker#invoke方法的时候，必须重写getProcessor方法");
    }

    /**
     * 最终给ClusterInvoker的invoker，是用户接触到的invoker
     * @param filters
     * @param <T>
     * @return
     */
    public <T> Invoker<T> buildFilterChain(List<Filter> filters) {
        // refer 得到的，包含了endpoint

        return new InvokerDelegate<T>((Invoker<T>) this) {
            // 比较的时候就是在比较interfaceClass

            private ThreadLocal<AtomicInteger> filterIndex = new ThreadLocal() {
                @Override
                protected Object initialValue() {
                    return new AtomicInteger(0);
                }
            };

            @Override
            public RPCResponse invoke(InvokeParam invokeParam) throws RPCException {
                log.info("filterIndex:{}, invokeParam:{}", filterIndex.get().get(), invokeParam);
                final Invoker<T> invokerWrappedFilters = this;
                if (filterIndex.get().get() < filters.size()) {
                    return filters.get(filterIndex.get().getAndIncrement()).invoke(new AbstractInvoker() {
                        @Override
                        public Class<T> getInterface() {
                            return getDelegate().getInterface();
                        }

                        @Override
                        public String getInterfaceName() {
                            return getDelegate().getInterfaceName();
                        }

                        @Override
                        public ServiceURL getServiceURL() {
                            return getDelegate().getServiceURL();
                        }

                        @Override
                        public RPCResponse invoke(InvokeParam invokeParam) throws RPCException {
                            return invokerWrappedFilters.invoke(invokeParam);
                        }
                    }, invokeParam);
                }
                filterIndex.get().set(0);
                return getDelegate().invoke(invokeParam);
            }
        };
    }

    @Override
    public Class<T> getInterface() {
        return interfaceClass;
    }

    @Override
    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    @Override
    public ServiceURL getServiceURL() {
        return ServiceURL.DEFAULT_SERVICE_URL;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    protected GlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    public void setGlobalConfig(GlobalConfig globalConfig) {
        this.globalConfig = globalConfig;
    }
}
