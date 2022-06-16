package com.github.throwable.beanref;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.throwable.beanref.lfp.BeanRefUtils;

public class DynamicBeanPropertyResolver {

	private static final Cache<Set<Class<?>>, Map<String, BeanProperty<?, ?>>> RESOLVED_BEAN_PROPERTIES_CACHE = BeanRefUtils
			.cacheBuilder().build();

	@SuppressWarnings("unchecked")
	static <BEAN, T> BeanProperty<BEAN, T> resolveBeanProperty(Class<BEAN> beanClass, String propertyName,
			Class<T> type) {
		final BeanProperty<BEAN, ?> beanProperty = resolveBeanProperty(beanClass, propertyName);
		if (!type.isAssignableFrom(beanProperty.getType()))
			throw new IllegalArgumentException("Wrong type specified for property '" + propertyName
					+ "' does not exist in bean " + beanClass.getSimpleName());
		return (BeanProperty<BEAN, T>) beanProperty;
	}

	static <BEAN> BeanProperty<BEAN, ?> resolveBeanProperty(Class<BEAN> beanClass, String propertyName) {
		final BeanProperty<BEAN, ?> beanProperty = resolveAllBeanProperties(beanClass).get(propertyName);
		if (beanProperty == null)
			throw new IllegalArgumentException(
					"Property '" + propertyName + "' does not exist in bean " + beanClass.getSimpleName());
		return beanProperty;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <BEAN> Map<String, BeanProperty<BEAN, ?>> resolveAllBeanProperties(Class<BEAN> beanClass) {
		Set<Class<?>> namedClassTypes = BeanRefUtils.getNamedClassTypes(beanClass);
		return (Map) RESOLVED_BEAN_PROPERTIES_CACHE.get(namedClassTypes, nil -> {
			Map<String, BeanProperty<?, ?>> beanProperties = new HashMap<>();
			for (Class<?> namedClassType : namedClassTypes)
				beanProperties.putAll(resolveAllBeanPropertiesImpl(namedClassType));
			return Collections.unmodifiableMap(beanProperties);
		});
	}

	private static <BEAN> Map<String, BeanProperty<BEAN, ?>> resolveAllBeanPropertiesImpl(Class<BEAN> beanClass) {
		final HashMap<String, BeanProperty<BEAN, ?>> map = new HashMap<>();
		final Method[] methods = beanClass.getMethods();

		for (Method getterMethod : methods) {
			if ((getterMethod.getModifiers() & Modifier.STATIC) != 0x0 || getterMethod.getParameterCount() > 0
					|| Void.TYPE.equals(getterMethod.getReturnType()))
				continue;
			// skip Object.class methods
			switch (getterMethod.getName()) {
			case "hashCode":
			case "getClass":
			case "clone": // Cloneable objects may re-define it as public
			case "toString":
			case "notify":
			case "notifyAll":
				continue;
			}
			@SuppressWarnings("unchecked")
			final Class<Object> type = (Class<Object>) getterMethod.getReturnType();
			final String propertyName = BeanPropertyResolver.resolvePropertyName(getterMethod.getName());
			final Method setterMethod = BeanPropertyResolver.findSetterMethod(beanClass, propertyName, type,
					getterMethod.getName());
			final BiConsumer<BEAN, Object> writeAccessor = setterMethod != null
					? new BeanPropertyResolver.SetterWriteAccessor<>(setterMethod)
					: null;
			final BeanProperty<BEAN, Object> property = new BeanProperty<>(beanClass, type, propertyName,
					new GetterReadAccessor<>(getterMethod), writeAccessor,
					new BeanPropertyResolver.InstantiatorResolver<>(type));
			map.put(propertyName, property);
		}
		return Collections.unmodifiableMap(map);
	}

	public static class GetterReadAccessor<BEAN, TYPE> implements Function<BEAN, TYPE> {
		private final Method getterMethod;

		public GetterReadAccessor(Method getterMethod) {
			this.getterMethod = getterMethod;
		}

		@SuppressWarnings("unchecked")
		@Override
		public TYPE apply(BEAN bean) {
			try {
				return (TYPE) getterMethod.invoke(bean);
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
	}
}
