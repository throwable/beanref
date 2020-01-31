package com.github.throwable.beanref;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class BeanRoot<BEAN> {
    private final Class<BEAN> beanClass;

    public BeanRoot(Class<BEAN> beanClass) {
        this.beanClass = beanClass;
    }

    /**
     * Obtain bean's property reference
     * @param methodReferenceLambda getter method reference
     */
    public <TYPE> BeanProperty<BEAN, TYPE> $(MethodReferenceLambda<BEAN, TYPE> methodReferenceLambda) {
        return BeanPropertyResolver.resolveBeanProperty(methodReferenceLambda);
    }

    /**
     * Obtain bean's collection property reference
     * @param methodReferenceLambda collection getter method reference
     */
    public <TYPE> BeanProperty<BEAN, TYPE> $$(
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
    public <TYPE> BeanProperty<BEAN, TYPE> $$(
            MethodReferenceLambda<BEAN, Collection<TYPE>> methodReferenceLambda,
            Supplier<Collection<TYPE>> collectionSupplier)
    {
        return BeanPropertyResolver.resolveCollectionBeanProperty(methodReferenceLambda, collectionSupplier);
    }


    /**
     * Obtain path for nested property or path
     * @param path name of property or a path of properties separated by .
     */
    public BeanPath<BEAN, ?> $(String path) {
        final int i = path.indexOf('.');
        if (i < 0)
            return DynamicBeanPropertyResolver.resolveBeanProperty(
                    beanClass, path);
        else
            return DynamicBeanPropertyResolver.resolveBeanProperty(beanClass, path.substring(0, i))
                    .$(path.substring(i+1));
    }

    /**
     * Obtain path for nested property or path
     * @param path name of property or a path of properties separated by .
     * @param type resulting property's type
     */
    public <T> BeanPath<BEAN, T> $(String path, Class<T> type) {
        final int i = path.indexOf('.');
        if (i < 0)
            return DynamicBeanPropertyResolver.resolveBeanProperty(
                    beanClass, path, type);
        else
            return DynamicBeanPropertyResolver.resolveBeanProperty(beanClass, path.substring(0, i))
                    .$(path.substring(i+1), type);
    }

    /**
     * List all properties of the nested bean
     * @return list of BeanPaths to access every property of nested bean
     */
    public Set<BeanPath<BEAN, ?>> all() {
        return new HashSet<>(DynamicBeanPropertyResolver.resolveAllBeanProperties(beanClass).values());
    }
}
