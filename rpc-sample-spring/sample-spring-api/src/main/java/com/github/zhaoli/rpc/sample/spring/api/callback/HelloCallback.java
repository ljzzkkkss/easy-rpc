package com.github.zhaoli.rpc.sample.spring.api.callback;

import java.io.Serializable;

/**
 * @author zhaoli
 * @date 2018/6/10
 */
public interface HelloCallback extends Serializable {
    void callback(String result);
}
