package com.github.zhaoli.rpc.sample.spring.server.service.impl;


import com.github.zhaoli.rpc.sample.spring.api.domain.User;
import com.github.zhaoli.rpc.autoconfig.annotation.RPCService;
import com.github.zhaoli.rpc.sample.spring.api.service.HelloService;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by zhaoli on 2017/7/30.
 */
@RPCService
@Slf4j
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(User user) {
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        if(Math.random() > 0.5) {
//            throw new RuntimeException("provider side error");
//        }
        log.info("开始方法调用");
        return "Hello, " + user.getUsername();
    }
}
