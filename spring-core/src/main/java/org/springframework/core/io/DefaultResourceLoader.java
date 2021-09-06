/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ResourceLoader} 接口的默认实现。
 * 由 {@link ResourceEditor} 使用，并作为 {@link org.springframework.context.support.AbstractApplicationContext} 的基类。也可以单独使用。
 *
 * <p>如果位置值为 URL，将返回 {@link UrlResource}，如果是非 URL 路径或“classpath:”伪 URL，将返回 {@link ClassPathResource}。
 *
 * @author Juergen Hoeller
 * @see FileSystemResourceLoader
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 * @since 10.03.2004
 */
public class DefaultResourceLoader implements ResourceLoader {

	@Nullable
	private ClassLoader classLoader;

	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);

	private final Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);


	/**
	 * 创建一个新的 DefaultResourceLoader。
	 * <p>ClassLoader 访问将在实际资源访问时使用线程上下文类加载器（自 5.3 起）。
	 * 如需更多控制，请将特定的 ClassLoader 传递给 {@link DefaultResourceLoader(ClassLoader)}。 @see java.lang.ThreadgetContextClassLoader()
	 */
	public DefaultResourceLoader() {
	}

	/**
	 * 创建一个新的 DefaultResourceLoader。
	 *
	 * @param classLoader 用于加载类路径资源的类加载器，或{@code null} 用于在实际访问资源时使用线程上下文类加载器
	 */
	public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 指定 ClassLoader 来加载类路径资源，或者 {@code null} 用于在实际访问资源时使用线程上下文类加载器。
	 *
	 * <p>默认情况下，ClassLoader 访问将在实际资源访问时使用线程上下文类加载器（自 5.3 起）。
	 */
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * 返回 ClassLoader 以加载类路径资源。 <p>将被传递给由这个资源加载器创建的所有 ClassPathResource 对象的 ClassPathResource 的构造函数。
	 *
	 * @see ClassPathResource
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 使用此资源加载器注册给定的解析器，允许处理其他协议。
	 * <p>任何此类解析器都将在此加载程序的标准解析规则之前被调用。因此，它也可以覆盖任何默认规则。
	 *
	 * @see #getProtocolResolvers()
	 * @since 4.3
	 */
	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		this.protocolResolvers.add(resolver);
	}

	/**
	 * 返回当前注册的协议解析器的集合，允许自省和修改。
	 *
	 * @since 4.3
	 */
	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}

	/**
	 * Obtain a cache for the given value type, keyed by {@link Resource}.
	 *
	 * @param valueType the value type, e.g. an ASM {@code MetadataReader}
	 * @return the cache {@link Map}, shared at the {@code ResourceLoader} level
	 * @since 5.0
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
		return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
	}

	/**
	 * Clear all resource caches in this resource loader.
	 *
	 * @see #getResourceCache
	 * @since 5.0
	 */
	public void clearResourceCaches() {
		this.resourceCaches.clear();
	}


	@Override
	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");

		// 首先，通过 ProtocolResolver 来加载资源
		for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				return resource;
			}
		}

		// 其次，以 / 开头，返回 ClassPathContextResource 类型的资源
		if (location.startsWith("/")) {
			return getResourceByPath(location);
			// 再次，以 classpath: 开头，返回 ClassPathResource 类型的资源
		} else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
			// 然后，根据是否为文件 URL ，是则返回 FileUrlResource 类型的资源，否则返回 UrlResource 类型的资源
		} else {
			try {
				// 尝试将位置解析为 URL...
				URL url = new URL(location);
				return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
			} catch (MalformedURLException ex) {
				// 最后，返回 ClassPathContextResource 类型的资源
				// 没有 URL -> 解析为资源路径。
				return getResourceByPath(location);
			}
		}
	}

	/**
	 * 返回给定路径上资源的资源句柄。
	 * <p>默认实现支持类路径位置。这应该适用于独立的实现，但可以被覆盖，例如用于针对 Servlet 容器的实现。
	 *
	 * @param path the path to the resource
	 * @return the corresponding Resource handle
	 * @see ClassPathResource
	 * @see org.springframework.context.support.FileSystemXmlApplicationContext#getResourceByPath
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}


	/**
	 * ClassPathResource that explicitly expresses a context-relative path
	 * through implementing the ContextResource interface.
	 */
	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		public ClassPathContextResource(String path, @Nullable ClassLoader classLoader) {
			super(path, classLoader);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}

}
