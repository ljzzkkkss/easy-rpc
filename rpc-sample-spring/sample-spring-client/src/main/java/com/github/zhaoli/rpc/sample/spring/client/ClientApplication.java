package com.github.zhaoli.rpc.sample.spring.client;

import com.github.zhaoli.rpc.sample.spring.client.call.AsyncCallService;
import com.github.zhaoli.rpc.sample.spring.client.call.CallbackCallService;
import com.github.zhaoli.rpc.sample.spring.client.call.SyncCallService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author zhaoli
 */
@Slf4j
@SpringBootApplication
public class ClientApplication implements CommandLineRunner {
    @Autowired
    private SyncCallService syncCallService;
    @Autowired
    private AsyncCallService asyncCallService;
    @Autowired
    private CallbackCallService callbackCallService;
        
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(ClientApplication.class);
        app.setWebEnvironment(false);
        app.run(args);
    }
    
    @Override
    public void run(String... args) throws Exception {
//        syncCallService.testHeartBeatAndReconnect();
//        syncCallService.concurrentTest();
//        syncCallService.test();
//        asyncCallService.testOnceCall();
        asyncCallService.test();
//        callbackCallService.test();
//        callbackCallService.testOnceCall();
    }
}