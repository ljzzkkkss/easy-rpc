package com.github.zhaoli.rpc.sample.spring.client.call;

import com.github.zhaoli.rpc.sample.spring.api.domain.User;
import com.github.zhaoli.rpc.common.context.RPCThreadLocalContext;
import com.github.zhaoli.rpc.sample.spring.api.service.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author zhaoli
 * @date 2018/6/10
 */
@Slf4j
@Component
public class AsyncCallService {
//    @RPCReference(async=true)  
    private HelloService helloService;
    
    public void test() throws Exception {
        helloService.hello(new User("1"));
        Future<String> hello1Future = RPCThreadLocalContext.getContext().getFuture();
        log.info("async:{}",hello1Future.get());
        
        
        helloService.hello(new User("2"));
        Future<String> hello2Future = RPCThreadLocalContext.getContext().getFuture();
        log.info("async:{}",hello2Future.get());
        
        Thread.sleep(3000);
        helloService.hello(new User("3"));
        Future<String> hello3Future = RPCThreadLocalContext.getContext().getFuture();
        log.info("async:{}",hello3Future.get());
        
        
        Thread.sleep(8000);
        helloService.hello(new User("4"));
        Future<String> hello4Future = RPCThreadLocalContext.getContext().getFuture();
        log.info("async:{}",hello4Future.get());
    }
    
    public void testOnceCall() throws ExecutionException, InterruptedException {
        helloService.hello(new User("1"));
        Future<String> hello1Future = RPCThreadLocalContext.getContext().getFuture();
        log.info("async:{}",hello1Future.get());
    }
}
