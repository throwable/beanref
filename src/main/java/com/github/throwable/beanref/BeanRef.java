package com.github.throwable.beanref;

import java.util.Collection;
import java.util.function.Supplier;

public class BeanRef {
    public static <BEAN, TYPE> BeanProperty<BEAN, TYPE> $(MethodReferenceLambda<BEAN, TYPE> methodReferenceLambda) {
        return BeanPropertyResolver.resolveBeanProperty(methodReferenceLambda);
    }

    /**
     * Obtain path for collection nested property
     * @param methodReferenceLambda reference
     */
    public static <BEAN, TYPE> BeanProperty<BEAN, TYPE> $$(
            MethodReferenceLambda<BEAN, Collection<TYPE>> methodReferenceLambda)
    {
        return $$(methodReferenceLambda, null);
    }

    /**
     * Obtain path for collection nested property
     * @param methodReferenceLambda reference
     */
    public static <BEAN, TYPE> BeanProperty<BEAN, TYPE> $$(
            MethodReferenceLambda<BEAN, Collection<TYPE>> methodReferenceLambda,
            Supplier<Collection<TYPE>> collectionSupplier)
    {
        return BeanPropertyResolver.resolveCollectionBeanProperty(methodReferenceLambda, collectionSupplier);
    }
}
