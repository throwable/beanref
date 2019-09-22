package com.github.throwable.beanutil;

public class BeanUtil {
    public static <BEAN, TYPE> BeanProperty<BEAN, TYPE> $(MethodReferenceLambda<BEAN, TYPE> methodReferenceLambda) {
        return BeanPropertyResolver.resolveBeanProperty(methodReferenceLambda);
    }
}
