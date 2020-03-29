package com.github.zhaoli.rpc.sample.spring.client.call;

import com.github.zhaoli.rpc.sample.spring.api.domain.User;
import com.github.zhaoli.rpc.autoconfig.annotation.RPCReference;
import com.github.zhaoli.rpc.sample.spring.api.service.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhaoli
 * @date 2018/6/10
 */
@Slf4j
@Component
public class SyncCallService {
//    @RPCReference
    private HelloService helloService;

    public void testOnceCall() throws Exception {
        log.info("开始方法调用...");
        log.info("sync:{}", helloService.hello(new User("1")));
    }

    public void testHeartBeatAndReconnect() throws Exception {
        // 前提是将心跳时间设置为5s
        log.info("sync:{}", helloService.hello(new User("1")));
        log.info("sync:{}", helloService.hello(new User("2")));

        Thread.sleep(3000);
        log.info("sync:{}", helloService.hello(new User("3")));
        Thread.sleep(8000);
        // 等待完5s后，客户端会发送一次心跳
        log.info("sync:{}", helloService.hello(new User("4")));
    }

    public void concurrentTest() {
        ExecutorService pool = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 20; i++) {
            String currentUsername = String.valueOf(i + 1);
            pool.submit(() -> {
                log.info("sync:{}", helloService.hello(new User(currentUsername)));
            });
        }
    }
}
