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
     * Obtain bean's root to construct path
     * @param beanClass root bean class
     * @return bean root to construct path
     */
    public static <BEAN> BeanRoot<BEAN> $(Class<BEAN> beanClass) {
        return new BeanRoot<>(beanClass);
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

    /**
     * Obtain path for nested property or path
     * @param path name of property or a path of properties separated by .
     */
    public static <BEAN> BeanPath<BEAN, ?> $(Class<BEAN> beanClass, String path) {
        return $(beanClass).$(path);
    }

    /**
     * Obtain path for nested property or path
     * @param path name of property or a path of properties separated by .
     * @param type resulting property's type
     */
    public static <BEAN, T> BeanPath<BEAN, T> $(Class<BEAN> beanClass, String path, Class<T> type) {
        return $(beanClass).$(path, type);
    }
}
