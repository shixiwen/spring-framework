/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * 用于加载资源（例如，类路径或文件系统资源）的策略接口。
 * 需要 {@link org.springframework.context.ApplicationContext} 来提供此功能以及扩展的 {@link org.springframework.core.io.support.ResourcePatternResolver} 支持。
 *
 * <p>{@link DefaultResourceLoader} 是一个独立的实现，可以在 ApplicationContext 之外使用，也被 {@link ResourceEditor} 使用。
 *
 * <p>{@code Resource} 和 {@code Resource[]} 类型的 Bean 属性可以在 ApplicationContext 中运行时从字符串填充，使用特定上下文的资源加载策略。
 *
 * @author Juergen Hoeller
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 * @since 10.03.2004
 */
public interface ResourceLoader {

	/**
	 * Pseudo URL prefix for loading from the class path: "classpath:".
	 */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * 返回指定资源位置的 {@code Resource} 句柄。
	 * 根据所提供资源的路径 location 返回 Resource 实例，但是它不确保该 Resource 一定存在，需要调用 Resource#exist() 方法来判断。
	 *
	 * <p>句柄应该始终是一个可重用的资源描述符，允许多个 {@link ResourcegetInputStream()} 调用。
	 * <p>
	 *     <ul>
	 *         <li>必须支持完全限定的 URL，例如“文件：C：test.dat”。
	 *         <li>必须支持类路径伪 URL，例如“类路径：test.dat”。
	 *         <li>应该支持相对文件路径，例如“WEB-INFtest.dat”。 （这将是特定于实现的，通常由 ApplicationContext 实现提供。）
	 *     <ul>
	 *
	 * <p>请注意，{@code Resource} 句柄并不意味着现有资源；您需要调用 {@link Resourceexists} 来检查是否存在。
	 *
	 * @param location the resource location
	 * @return a corresponding {@code Resource} handle (never {@code null})
	 * @see #CLASSPATH_URL_PREFIX
	 * @see Resource#exists()
	 * @see Resource#getInputStream()
	 */
	Resource getResource(String location);

	/**
	 * 公开此 {@code ResourceLoader} 使用的 {@link ClassLoader}。
	 * <p>需要直接访问 {@code ClassLoader} 的客户端可以通过 {@code ResourceLoader} 以统一的方式访问，而不是依赖于线程上下文 {@code ClassLoader}。
	 *
	 * @return the {@code ClassLoader}
	 * (only {@code null} if even the system {@code ClassLoader} isn't accessible)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getClassLoader();

}
