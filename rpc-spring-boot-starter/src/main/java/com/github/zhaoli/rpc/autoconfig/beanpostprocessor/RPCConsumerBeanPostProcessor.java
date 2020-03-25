package com.github.zhaoli.rpc.autoconfig.beanpostprocessor;

import com.github.zhaoli.rpc.common.ExtensionLoader;
import com.github.zhaoli.rpc.common.enumeration.ErrorEnum;
import com.github.zhaoli.rpc.common.exception.RPCException;
import com.github.zhaoli.rpc.config.ReferenceConfig;
import com.github.zhaoli.rpc.filter.Filter;
import com.github.zhaoli.rpc.autoconfig.annotation.RPCReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;

import java.lang.reflect.Field;

/**
 * @author zhaoli
 * @date 2018/7/14
 */
@Slf4j
public class RPCConsumerBeanPostProcessor extends AbstractRPCBeanPostProcessor{
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            Class<?> interfaceClass = field.getType();
            RPCReference reference = field.getAnnotation(RPCReference.class);
            if (reference != null) {
                ReferenceConfig config = ReferenceConfig.createReferenceConfig(
                        interfaceClass.getName(),
                        interfaceClass,
                        reference.async(),
                        reference.callback(),
                        reference.oneway(),
                        reference.timeout(),
                        reference.callbackMethod(),
                        reference.callbackParamIndex(),
                        false,
                        ExtensionLoader.getInstance().load(Filter.class)
                );
                initConfig(config);
                try {
                    field.set(bean,config.get());
                } catch (IllegalAccessException e) {
                    throw new RPCException(e, ErrorEnum.AUTOWIRE_REFERENCE_PROXY_ERROR,"set proxy failed");
                }
                log.info("注入依赖:{}",interfaceClass);
            }
        }
        return bean;
    }
}
