package com.github.throwable.beanref.lfp;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.throwable.beanref.BeanPropertyResolver;

@SuppressWarnings("unchecked")
public class BeanRefUtils {

	private static final Duration CACHE_EXPIRE_AFTER_ACCESS = Duration.ofSeconds(5);
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	public static <K, V> Caffeine<K, V> cacheBuilder() {
		return (Caffeine<K, V>) Caffeine.newBuilder().expireAfterAccess(CACHE_EXPIRE_AFTER_ACCESS).softValues();
	}

	public static Stream<ClassLoader> streamClassLoaders() {
		List<Supplier<ClassLoader>> loaders = Arrays.asList(new Supplier<ClassLoader>() {

			@Override
			public ClassLoader get() {
				return Thread.currentThread().getContextClassLoader();
			}
		}, new Supplier<ClassLoader>() {

			@Override
			public ClassLoader get() {
				return BeanPropertyResolver.class.getClassLoader();
			}
		}, new Supplier<ClassLoader>() {

			@Override
			public ClassLoader get() {
				return ClassLoader.getSystemClassLoader();
			}
		}, new Supplier<ClassLoader>() {

			@Override
			public ClassLoader get() {
				return ClassLoader.getPlatformClassLoader();
			}
		});
		return loaders.stream().filter(Objects::nonNull).map(Supplier::get).filter(Objects::nonNull).distinct();
	}

	public static String hash(Serializable... serializables) {
		MessageDigest messageDigest = null;
		try {
			if (serializables != null)
				for (Serializable serializable : serializables) {
					if (serializable == null)
						continue;
					if (messageDigest == null)
						messageDigest = MessageDigest.getInstance("MD5");
					update(messageDigest, serializable);
				}
		} catch (NoSuchAlgorithmException | IOException e) {
			throw RuntimeException.class.isInstance(e) ? RuntimeException.class.cast(e) : new RuntimeException(e);
		}
		byte[] digest = messageDigest != null ? messageDigest.digest() : EMPTY_BYTE_ARRAY;
		return Base64.getEncoder().encodeToString(digest);
	}

	public static <U> Set<Class<?>> getNamedClassTypes(Class<? extends U> classType) {
		Objects.requireNonNull(classType);
		Set<Class<?>> classTypes = new LinkedHashSet<>();
		addNamedClassTypes(classType, classTypes);
		if (classTypes.isEmpty())
			classTypes.add(classType);
		return Collections.unmodifiableSet(classTypes);
	}

	private static void update(MessageDigest messageDigest, Serializable serializable) throws IOException {
		try (OutputStream os = OutputStream.nullOutputStream();
				DigestOutputStream digestOutputStream = new DigestOutputStream(os, messageDigest);
				ObjectOutputStream oos = new ObjectOutputStream(digestOutputStream);) {
			oos.writeObject(serializable);
		}
	}

	private static void addNamedClassTypes(Class<?> classType, Set<Class<?>> classTypes) {
		if (classType == null || classTypes.contains(classType))
			return;
		if (isNamedClassType(classType)) {
			classTypes.add(classType);
			return;
		}
		Class<?>[] ifaces = classType.getInterfaces();
		if (ifaces.length == 0) {
			Class<?> superclass = classType.getSuperclass();
			if (superclass != null)
				addNamedClassTypes(superclass, classTypes);
			return;
		}
		for (Class<?> iface : ifaces)
			addNamedClassTypes(iface, classTypes);
	}

	private static boolean isNamedClassType(Class<?> classType) {
		if (classType == null)
			return false;
		if (classType.isAnonymousClass())
			return false;
		if (isLambdaClassType(classType))
			return false;
		if (Proxy.isProxyClass(classType))
			return false;
		return true;
	}

	private static boolean isLambdaClassType(Class<?> classType) {
		if (classType == null)
			return false;
		if (classType.isInterface() || Modifier.isAbstract(classType.getModifiers()) || classType.isAnonymousClass())
			return false;
		return classType.getName().contains("$$Lambda");
	}

}
