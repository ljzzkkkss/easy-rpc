package com.github.zhaoli.rpc.registry.api;

import java.util.List;

/**
 * @author zhaoli
 * @date 2018/7/16
 */
@FunctionalInterface
public interface ServiceURLRemovalCallback {
    void removeNotExisted(List<ServiceURL> newAddresses);
}
