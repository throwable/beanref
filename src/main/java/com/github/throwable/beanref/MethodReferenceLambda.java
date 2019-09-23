package com.github.throwable.beanref;

import java.io.Serializable;
import java.util.function.Function;

public interface MethodReferenceLambda<BEAN, TYPE> extends Function<BEAN, TYPE>, Serializable {
}
