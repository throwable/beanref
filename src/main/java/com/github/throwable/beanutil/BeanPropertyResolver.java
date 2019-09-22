package com.github.throwable.beanutil;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

final class BeanPropertyResolver {
    private BeanPropertyResolver() {}

    static <BEAN, TYPE> BeanProperty<BEAN, TYPE> resolveBeanProperty(MethodReferenceLambda<BEAN, TYPE> methodReferenceLambda)
    {
        // TODO: cache
        final SerializedLambda serialized = serialized(methodReferenceLambda);
        final Class<BEAN> beanClass = getContainingClass(serialized);
        final Method getterMethod = findGetterMethod(beanClass, serialized.getImplMethodName());
        @SuppressWarnings("unchecked")
        final Class<TYPE> type = (Class<TYPE>) getterMethod.getReturnType();
        final String propertyName = resolvePropertyName(getterMethod.getName());
        final Method setterMethod = findSetterMethod(beanClass, propertyName, type, getterMethod.getName());
        return new BeanProperty<>(beanClass, type, propertyName, methodReferenceLambda, getterMethod, setterMethod);
    }


    private static <BEAN> Method findGetterMethod(Class<BEAN> beanClass, String getterMethodName) {
        final Method getterMethod;
        try {
            getterMethod = beanClass.getMethod(getterMethodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalBeanPathException(e);
        }

        if (getterMethod.getParameterCount() > 0)
            throw new IllegalBeanPathException("Illegal getter method: " + getterMethodName);
        if (Void.TYPE.equals(getterMethod.getReturnType()))
            throw new IllegalBeanPathException("Illegal getter method return type: " + getterMethodName +
                    "(" + getterMethod.getReturnType() + ")");
        return getterMethod;
    }

    private static String resolvePropertyName(String getterMethodName)
    {
        final String propertyName;

        if (getterMethodName.startsWith("get") && getterMethodName.length() > 3 &&
                Character.isUpperCase(getterMethodName.charAt(3)))
        {
            propertyName = Character.toLowerCase(getterMethodName.charAt(3)) + getterMethodName.substring(4);
        }
        else if (getterMethodName.startsWith("is") && getterMethodName.length() > 2 &&
                Character.isUpperCase(getterMethodName.charAt(2)))
        {
            propertyName = Character.toLowerCase(getterMethodName.charAt(2)) + getterMethodName.substring(3);
        }
        else {
            // non-canonical name: use method name as-is
            propertyName = getterMethodName;
        }
        return propertyName;
    }


    static <TYPE> Supplier<TYPE> resolveInstantiator(Class<TYPE> type) {
        if ((type.getModifiers() & Modifier.ABSTRACT) != 0)
            return null;
        final Constructor<TYPE> constructor;
        try {
            constructor = type.getConstructor();
        } catch (NoSuchMethodException e) {
            return null;
        }
        return () -> {
            try {
                return constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof RuntimeException)
                    throw (RuntimeException) e.getTargetException();
                else
                    throw new RuntimeException(e.getTargetException());
            }
        };
    }


    private static <BEAN, TYPE> /* Nullable */ Method findSetterMethod(Class<BEAN> beanClass, String propertyName, Class<TYPE> type, String getterMethodName) {
        if (!getterMethodName.equals(propertyName)) {
            // canonical getXXX(): search for setXXX()
            final String setterMethodName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            try {
                return beanClass.getMethod(setterMethodName, type);
            } catch (NoSuchMethodException e) {
                // Read-only
                return null;
            }
        } else {
            // non-canonical xxx(): search for xxx(value)
            try {
                return beanClass.getMethod(propertyName, type);
            } catch (NoSuchMethodException e) {
                // Read-only
                return null;
            }
        }
    }


    private static SerializedLambda serialized(Object lambda) {
        try {
            Method writeMethod = lambda.getClass().getDeclaredMethod("writeReplace");
            writeMethod.setAccessible(true);
            return (SerializedLambda) writeMethod.invoke(lambda);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getContainingClass(SerializedLambda lambda) {
        try {
            String className = lambda.getImplClass().replaceAll("/", ".");
            //System.out.println(lambda.getInstantiatedMethodType());
            return (Class<T>) Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
