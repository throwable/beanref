package com.github.throwable.beanutil;

import java.io.ObjectStreamException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BeanProperty<BEAN, TYPE> extends BeanPath<BEAN, BEAN, TYPE>
{
    private transient final Class<BEAN> beanClass;
    private transient final Class<TYPE> type;
    private transient final String name;
    private final Function<BEAN, TYPE> readAccessor;
    /* Nullable */
    private transient final BiConsumer<BEAN, TYPE> writeAccessor;
    /* Nullable */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private transient volatile Optional<Supplier<TYPE>> instantiator;
    private transient final Method getterMethod;
    private transient final Method setterMethod;


    BeanProperty(Class<BEAN> beanClass, Class<TYPE> type, String name,
                 Function<BEAN, TYPE> readAccessor,
                 Method getterMethod, Method setterMethod)
    {
        this.beanClass = beanClass;
        this.type = type;
        this.name = name;
        this.readAccessor = readAccessor;
        this.getterMethod = getterMethod;
        this.setterMethod = setterMethod;
        if (this.setterMethod != null)
            this.writeAccessor = this::setImpl;
        else this.writeAccessor = null;
    }

    @Override
    public Class<BEAN> getBeanClass() {
        return beanClass;
    }

    @Override
    public Class<TYPE> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return name;
    }

    public Function<BEAN, TYPE> getReadAccessor() {
        return readAccessor;
    }

    public BiConsumer<BEAN, TYPE> getWriteAccessor() {
        return writeAccessor;
    }

    public Method getGetterMethod() {
        return getterMethod;
    }

    public Method getSetterMethod() {
        return setterMethod;
    }

    @Override
    public boolean isReadOnly() {
        return writeAccessor == null;
    }

    @Override
    public TYPE get(BEAN bean) {
        /*try {
            return type.cast(getter.invoke(bean));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }*/
        return readAccessor.apply(bean);
    }

    @Override
    public void set(BEAN bean, TYPE value) {
        if (isReadOnly())
            throw new ReadOnlyPropertyException("Property '" + toString() + "' is read-only");
        writeAccessor.accept(bean, value);
    }

    Supplier<TYPE> getInstantiator() {
        //noinspection OptionalAssignedToNull
        if (instantiator == null)
            instantiator = Optional.ofNullable(BeanPropertyResolver.resolveInstantiator(type));
        return instantiator.orElse(null);
    }

    private void setImpl(BEAN bean, TYPE value) {
        try {
            setterMethod.invoke(bean, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException)
                // unchecked: throw as is
                throw (RuntimeException) e.getTargetException();
            else
                // checked: wrap into runtime
                throw new RuntimeException(e.getTargetException());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeanProperty<?, ?> that = (BeanProperty<?, ?>) o;
        return beanClass.equals(that.beanClass) &&
                name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beanClass, name);
    }

    private Object readResolve() throws ObjectStreamException {
        return BeanPropertyResolver.resolveBeanProperty((MethodReferenceLambda<BEAN,TYPE>) readAccessor);
    }
}