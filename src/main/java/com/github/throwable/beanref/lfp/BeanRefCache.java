package com.github.throwable.beanref.lfp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import de.adito.picoservice.IPicoRegistration;
import de.adito.picoservice.PicoService;

public interface BeanRefCache {

	public static BeanRefCache instance() {
		return Static.instance();
	}

	<K, V> V get(K key, Function<K, V> loader);

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	@PicoService
	public static @interface BeanRefCacheService {

		// alias for value
		String[] requiredClassNames() default {};

		int priority()

		default 0;

	}

	static enum Static {
		;

		private static final Object INSTANCE_MUTEX = new Object();
		private static BeanRefCache _INSTANCE;

		private static BeanRefCache instance() {
			if (_INSTANCE == null)
				synchronized (INSTANCE_MUTEX) {
					if (_INSTANCE == null)
						_INSTANCE = loadInstance();
				}
			return _INSTANCE;
		}

		private static BeanRefCache loadInstance() {
			var clStream = Stream.concat(Stream.of((ClassLoader) null), BeanRefUtils.streamDefaultClassLoaders())
					.distinct();
			var serviceProviderStream = clStream.flatMap(cl -> {
				var serviceLoader = cl == null ? ServiceLoader.load(IPicoRegistration.class)
						: ServiceLoader.load(IPicoRegistration.class, cl);
				return serviceLoader.stream();
			}).distinct();
			var registrationStream = serviceProviderStream.map(ServiceLoader.Provider::get);
			registrationStream = registrationStream
					.filter(v -> BeanRefCache.class.isAssignableFrom(v.getAnnotatedClass()));
			registrationStream = registrationStream.filter(registration -> {
				return registration.getAnnotatedClass().getAnnotation(BeanRefCacheService.class) != null;
			});
			registrationStream = registrationStream
					.sorted(Comparator.<IPicoRegistration, Integer>comparing(registration -> {
						return registration.getAnnotatedClass().getAnnotation(BeanRefCacheService.class).priority();
					}));
			var beanRefCache = registrationStream.map(registration -> {
				var ct = registration.getAnnotatedClass();
				var enumConstants = ct.getEnumConstants();
				if (enumConstants != null && enumConstants.length == 1)
					return enumConstants[0];
				try {
					return ct.getConstructor().newInstance();
				} catch (RuntimeException e) {
					throw e;
				} catch (InstantiationException | IllegalAccessException | InvocationTargetException
						| NoSuchMethodException e) {
					throw new RuntimeException(e);
				}
			}).map(v -> (BeanRefCache) v).findFirst().orElse(null);
			if (beanRefCache != null)
				return beanRefCache;
			var cache = new WeakHashMap<Object, Object>();
			var concurrentCache = new ConcurrentHashMap<Object, Object>();
			return new BeanRefCache() {

				@SuppressWarnings("unchecked")
				@Override
				public <K, V> V get(K key, Function<K, V> loader) {
					Objects.requireNonNull(key);
					Objects.requireNonNull(loader);
					var value = cache.get(key);
					if (value == null) {
						Object[] resultRef = new Object[1];
						concurrentCache.compute(key, (nilk, nilv) -> {
							resultRef[0] = cache.computeIfAbsent(key, nil -> {
								return loader.apply(key);
							});
							return null;
						});
						value = resultRef[0];
					}
					return (V) value;
				}
			};
		}

	}

}
