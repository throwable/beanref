package com.github.throwable.beanref;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BeanPath<ROOT, TYPE> implements Iterable<BeanProperty>
{
    private final List<BeanProperty> accessorPath = new ArrayList<>();

    protected BeanPath() {
        accessorPath.add((BeanProperty<?,TYPE>) this);
    }

    BeanPath(/* Nullable */ BeanPath<ROOT, ?> parent, BeanProperty<?, TYPE> beanProperty) {
        if (parent != null)
            accessorPath.addAll(parent.accessorPath);
        accessorPath.add(beanProperty);
    }

    /**
     * Obtain path for nested property
     */
    public <T> BeanPath<ROOT, T> $(MethodReferenceLambda<TYPE, T> methodReferenceLambda) {
        final BeanProperty<TYPE, T> beanProperty = BeanPropertyResolver.resolveBeanProperty(methodReferenceLambda);
        return new BeanPath<>(this, beanProperty);
    }

    /**
     * Obtain path for collection nested property
     * @param methodReferenceLambda reference
     */
    public <T> BeanPath<ROOT, T> $$(MethodReferenceLambda<TYPE, Collection<T>> methodReferenceLambda) {
        return $$(methodReferenceLambda, null);
    }

    /**
     * Obtain path for collection nested property
     * @param methodReferenceLambda reference
     */
    public <T> BeanPath<ROOT, T> $$(
            MethodReferenceLambda<TYPE, Collection<T>> methodReferenceLambda,
                                          Supplier<Collection<T>> collectionSupplier)
    {
        final BeanProperty<TYPE, T> collectionBeanProperty =
                BeanPropertyResolver.resolveCollectionBeanProperty(methodReferenceLambda, collectionSupplier);
        return new BeanPath<>(this, collectionBeanProperty);
    }

    /**
     * @return true if a referenced property is read-only
     */
    public boolean isReadOnly() {
        return getLastBeanProperty().isReadOnly();
    }


    /**
     * Set nested property's value.
     * If the path is incomplete this method tries to implicitly instantiate missing beans in the path
     * and then sets the value. If an intermediate bean can not be instantiated (has no public no-args constructor
     * or corresponds to a read-only property) an exception is thrown.
     * @param bean a root bean
     * @param value value to set
     * @throws IncompletePathException if path is incomplete and correspondent beans can not be instantiated
     * @throws ReadOnlyPropertyException if a referenced property is read-only
     */
    @SuppressWarnings("unchecked")
    public void set(ROOT bean, TYPE value) {
        Object currentBean = Objects.requireNonNull(bean);
        for (int i = 0; i < accessorPath.size()-1; i++) {
            final BeanProperty beanProperty  = accessorPath.get(i);
            Object propValue = beanProperty.get(currentBean);
            if (propValue == null) {
                final Supplier instantiator = beanProperty.getInstantiator();
                if (instantiator == null)
                    throw new IncompletePathException("Property can not be accessed via path " + this.getPath()
                            + "because " + beanProperty.getPath() + " is null");
                else {
                    propValue = instantiator.get();
                    try {
                        beanProperty.set(currentBean, propValue);
                    } catch (ReadOnlyPropertyException e) {
                        throw new IncompletePathException(e);
                    }
                }
            }
            currentBean = propValue;
        }
        ((BeanProperty<Object, TYPE>) getLastBeanProperty()).set(currentBean, value);
    }

    /**
     * Get nested property's value starting from the root bean.
     * If the property value is not accessible by the given path (path is incomplete) this method returns null instead of throwing NPE.
     * @param bean a root bean
     * @return nested property value or null if property is not accessible by the given path
     */
    @SuppressWarnings("unchecked")
    public TYPE get(ROOT bean) {
        Object current = bean;
        for (BeanProperty beanProperty : accessorPath) {
            if (current == null) {
                return null;
            } else
                current = beanProperty.get(current);
        }
        return (TYPE) current;
    }

    /**
     * Indicates that the property is accessible directly by chaining requests and does not contain
     * any intermediate nullable value
     * @return true if path is complete
     */
    @SuppressWarnings("unchecked")
    public boolean isComplete(ROOT bean)
    {
        Object current = bean;
        int idx = 0;
        for (BeanProperty beanProperty : accessorPath) {
            if (current == null) {
                return idx >= accessorPath.size();
            } else
                current = beanProperty.get(current);
            idx++;
        }
        return true;
    }

    public Class<TYPE> getType() {
        return getLastBeanProperty().getType();
    }

    @Override
    public Iterator<BeanProperty> iterator() {
        return accessorPath.iterator();
    }


    @SuppressWarnings("unchecked")
    private BeanProperty<?, TYPE> getLastBeanProperty() {
        return accessorPath.get(accessorPath.size()-1);
    }

    @SuppressWarnings("unchecked")
    private BeanProperty<ROOT, ?> getRootBeanProperty() {
        return accessorPath.get(0);
    }

    public Class<ROOT> getBeanClass() {
        return getRootBeanProperty().getBeanClass();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeanPath<?, ?> beanPath = (BeanPath<?, ?>) o;
        return accessorPath.equals(beanPath.accessorPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessorPath);
    }

    @Override
    public String toString() {
        return getPath();
    }

    public String getPath() {
        return getPath(null);
    }

    public String getPath(String root) {
        return StreamSupport.stream(this.spliterator(), false)
                .map(BeanProperty::getPath)
                .collect(Collectors.joining(".",
                        (root != null && !root.isEmpty()) ? root : "", ""));
    }
}
