package com.github.zhaoli.rpc.registry.zookeeper;

import com.github.zhaoli.rpc.common.constant.CharsetConst;
import com.github.zhaoli.rpc.common.enumeration.ErrorEnum;
import com.github.zhaoli.rpc.common.exception.RPCException;
import com.github.zhaoli.rpc.config.RegistryConfig;
import com.github.zhaoli.rpc.registry.api.ServiceURL;
import com.github.zhaoli.rpc.registry.api.ServiceURLAddOrUpdateCallback;
import com.github.zhaoli.rpc.registry.api.ServiceURLRemovalCallback;
import com.github.zhaoli.rpc.registry.api.support.AbstractServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by zhaoli on 2017/9/27.
 * 服务器进行服务注册或者客户端进行服务发现
 */
@Slf4j
public class ZkServiceRegistry extends AbstractServiceRegistry {
    private ZkSupport zkSupport;

    private static long TEN_SEC = 10000000000L;

    private volatile Thread discoveringThread;

    public ZkServiceRegistry() {
    }


    @Override
    public void init() {
        zkSupport = new ZkSupport();
        zkSupport.connect(registryConfig.getAddress());
    }

    /**
     * 服务发现
     * 返回值的key是接口名，返回值的value是IP地址列表
     * 
     * @return
     */
    @Override
    public void discover(String interfaceName, ServiceURLRemovalCallback callback, ServiceURLAddOrUpdateCallback serviceURLAddOrUpdateCallback) {
        // 如果该接口对应的地址不存在，那么watchNode
        log.info("discovering...");
        this.discoveringThread = Thread.currentThread();
        watchInterface(interfaceName, callback, serviceURLAddOrUpdateCallback);
        log.info("开始Park... ");
        LockSupport.parkNanos(this, TEN_SEC);
        log.info("Park结束");
    }

    /**
     * 数据格式：
     * /easy/AService/192.168.1.1:1221 -> 192.168.1.1:1221
     * /easy/AService/192.168.1.2:1221 -> 192.168.1.2:1221
     * /easy/BService/192.168.1.3:1221 -> 192.168.1.3:1221
     * 
     * 
     * 两个回调方法，ServiceURLRemovalCallback
     */
    private void watchInterface(String interfaceName, ServiceURLRemovalCallback serviceURLRemovalCallback, ServiceURLAddOrUpdateCallback serviceURLAddOrUpdateCallback) {
        try {
            String path = generatePath(interfaceName);
            List<String> addresses = zkSupport.getChildren(path, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchInterface(interfaceName, serviceURLRemovalCallback, serviceURLAddOrUpdateCallback);
                    }
                }
            });
            log.info("interfaceName:{} -> addresses:{}", interfaceName, addresses);
            List<ServiceURL> dataList = new ArrayList<>();
            for (String node : addresses) {
                dataList.add(watchService(interfaceName, node, serviceURLAddOrUpdateCallback));
            }
            log.info("node data: {}", dataList);
            serviceURLRemovalCallback.removeNotExisted(dataList);
            LockSupport.unpark(discoveringThread);
        } catch (KeeperException | InterruptedException e) {
            throw new RPCException(ErrorEnum.REGISTRY_ERROR,"ZK故障", e);
        }
    }

    private ServiceURL watchService(String interfaceName, String address, ServiceURLAddOrUpdateCallback serviceURLAddOrUpdateCallback) {
        String path = generatePath(interfaceName);
        try {
            byte[] bytes = zkSupport.getData(path + "/" + address, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getType() == Event.EventType.NodeDataChanged) {
                        watchService(interfaceName, address, serviceURLAddOrUpdateCallback);
                    }
                }
            });
            ServiceURL serviceURL = ServiceURL.parse(new String(bytes, CharsetConst.UTF_8));
            serviceURLAddOrUpdateCallback.addOrUpdate(serviceURL);
            return serviceURL;
        } catch (KeeperException | InterruptedException e) {
            throw new RPCException(ErrorEnum.REGISTRY_ERROR,"ZK故障", e);
        }
    }

    /**
     * 服务注册
     *
     * @param address
     * @param interfaceName
     */
    @Override
    public void register(String address, String interfaceName) {
        String path = generatePath(interfaceName);
        try {
            zkSupport.createPathIfAbsent(path, CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            throw new RPCException(ErrorEnum.REGISTRY_ERROR,"ZK故障", e);
        }
        zkSupport.createNodeIfAbsent(address, path);
    }

    @Override
    public void close() {
        zkSupport.close();
    }

    @Override
    public void unregister(String address, String interfaceName) {

    }

}
