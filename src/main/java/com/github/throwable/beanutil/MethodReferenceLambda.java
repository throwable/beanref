package com.github.throwable.beanutil;

import java.io.Serializable;
import java.util.function.Function;

public interface MethodReferenceLambda<BEAN, TYPE> extends Function<BEAN, TYPE>, Serializable {
}
