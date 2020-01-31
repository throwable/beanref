package com.github.throwable.beanref;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * BeanPath represents a direct or transitive reference to a nested property.
 * @param <ROOT> root class
 * @param <TYPE> property's type
 */
public class BeanPath<ROOT, TYPE> implements Iterable<BeanProperty<?, ?>>
{
    private final List<BeanProperty<?,?>> accessorPath = new ArrayList<>();

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
     * @param methodReferenceLambda getter method reference
     */
    public <T> BeanPath<ROOT, T> $(MethodReferenceLambda<TYPE, T> methodReferenceLambda) {
        final BeanProperty<TYPE, T> beanProperty = BeanPropertyResolver.resolveBeanProperty(methodReferenceLambda);
        return new BeanPath<>(this, beanProperty);
    }

    /**
     * Obtain path for nested property or path
     * @param path name of property or a path of properties separated by .
     */
    public BeanPath<ROOT, ?> $(String path) {
        final int i = path.indexOf('.');
        if (i < 0) {
            final BeanProperty<TYPE, ?> beanProperty = DynamicBeanPropertyResolver.resolveBeanProperty(
                    getLastBeanProperty().getType(), path);
            return new BeanPath<>(this, beanProperty);
        } else {
            final BeanProperty<TYPE, ?> beanProperty = DynamicBeanPropertyResolver.resolveBeanProperty(
                    getLastBeanProperty().getType(), path.substring(0, i));
            return new BeanPath<>(this, beanProperty).$(path.substring(i+1));
        }
    }

    /**
     * Obtain path for nested property or path
     * @param path name of property or a path of properties separated by .
     * @param type resulting property's type
     */
    public <T> BeanPath<ROOT, T> $(String path, Class<T> type) {
        final int i = path.indexOf('.');
        if (i < 0) {
            final BeanProperty<TYPE, T> beanProperty = DynamicBeanPropertyResolver.resolveBeanProperty(
                    getLastBeanProperty().getType(), path, type);
            return new BeanPath<>(this, beanProperty);
        } else {
            final BeanProperty<TYPE, ?> beanProperty = DynamicBeanPropertyResolver.resolveBeanProperty(
                    getLastBeanProperty().getType(), path.substring(0, i));
            return new BeanPath<>(this, beanProperty).$(path.substring(i+1), type);
        }
    }

    /**
     * Obtain path for nested collection property
     * @param methodReferenceLambda collection getter method reference
     */
    public <T> BeanPath<ROOT, T> $$(MethodReferenceLambda<TYPE, Collection<T>> methodReferenceLambda) {
        return $$(methodReferenceLambda, null);
    }

    /**
     * Obtain path for nested collection property
     * @param methodReferenceLambda collection getter method reference
     * @param collectionSupplier supplier to automatically create a new collection setting a contained element
     *                          and the collection is null
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
     * List all properties of the nested bean
     * @return list of BeanPaths to access every property of nested bean
     */
    public Set<BeanPath<ROOT, ?>> all() {
        final Map<String, BeanProperty<TYPE, ?>> beanProperties = DynamicBeanPropertyResolver
                .resolveAllBeanProperties(getLastBeanProperty().getType());
        return beanProperties.values().stream()
                .map(it -> new BeanPath<>(this, it))
                .collect(Collectors.toSet());
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
            final BeanProperty<Object, Object> beanProperty  = (BeanProperty<Object, Object>) accessorPath.get(i);
            Object propValue = beanProperty.get(currentBean);
            if (propValue == null) {
                final Supplier<Object> instantiator = beanProperty.getInstantiator();
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
        for (BeanProperty<?,?> beanProperty : accessorPath) {
            if (current == null) {
                return null;
            } else
                current = ((BeanProperty<Object,Object>) beanProperty).get(current);
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
        for (BeanProperty<?, ?> beanProperty : accessorPath) {
            if (current == null) {
                return idx >= accessorPath.size();
            } else
                current = ((BeanProperty<Object, Object>) beanProperty).get(current);
            idx++;
        }
        return true;
    }

    /**
     * @return the type of this property
     */
    public Class<TYPE> getType() {
        return getLastBeanProperty().getType();
    }

    @Override
    public Iterator<BeanProperty<?,?>> iterator() {
        return accessorPath.iterator();
    }


    @SuppressWarnings("unchecked")
    private BeanProperty<?, TYPE> getLastBeanProperty() {
        return (BeanProperty<?, TYPE>) accessorPath.get(accessorPath.size()-1);
    }

    @SuppressWarnings("unchecked")
    private BeanProperty<ROOT, ?> getRootBeanProperty() {
        return (BeanProperty<ROOT, ?>) accessorPath.get(0);
    }

    /**
     * @return the type of enclosing object
     */
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

    /**
     * @return path as string formed by property names separated by '.'
     */
    public String getPath() {
        return getPath(null);
    }

    /**
     * @param root root prefix
     * @return path as string prefixed with root
     */
    public String getPath(String root) {
        return StreamSupport.stream(this.spliterator(), false)
                .map(BeanProperty::getPath)
                .collect(Collectors.joining(".",
                        (root != null && !root.isEmpty()) ? root : "", ""));
    }
}
