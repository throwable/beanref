package com.github.throwable.beanref;

import java.util.Collection;
import java.util.function.Supplier;

public class BeanRef {
    /**
     * Obtain bean's property reference
     * @param methodReferenceLambda getter method reference
     */
    public static <BEAN, TYPE> BeanProperty<BEAN, TYPE> $(MethodReferenceLambda<BEAN, TYPE> methodReferenceLambda) {
        return BeanPropertyResolver.resolveBeanProperty(methodReferenceLambda);
    }

    /**
     * Obtain bean's collection property reference
     * @param methodReferenceLambda collection getter method reference
     */
    public static <BEAN, TYPE> BeanProperty<BEAN, TYPE> $$(
            MethodReferenceLambda<BEAN, Collection<TYPE>> methodReferenceLambda)
    {
        return $$(methodReferenceLambda, null);
    }

    /**
     * Obtain bean's collection property reference
     * @param methodReferenceLambda collection getter method reference
     * @param collectionSupplier supplier to automatically create a new collection setting a contained element
     *                          and the collection is null
     */
    public static <BEAN, TYPE> BeanProperty<BEAN, TYPE> $$(
            MethodReferenceLambda<BEAN, Collection<TYPE>> methodReferenceLambda,
            Supplier<Collection<TYPE>> collectionSupplier)
    {
        return BeanPropertyResolver.resolveCollectionBeanProperty(methodReferenceLambda, collectionSupplier);
    }
}
