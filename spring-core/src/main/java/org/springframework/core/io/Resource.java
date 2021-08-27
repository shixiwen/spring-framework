/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.springframework.lang.Nullable;

/**
 * Interface for a resource descriptor that abstracts from the actual
 * type of underlying resource, such as a file or class path resource.
 *
 * <p>An InputStream can be opened for every resource if it exists in
 * physical form, but a URL or File handle can just be returned for
 * certain resources. The actual behavior is implementation-specific.
 *
 * @author Juergen Hoeller
 * @see #getInputStream()
 * @see #getURL()
 * @see #getURI()
 * @see #getFile()
 * @see WritableResource
 * @see ContextResource
 * @see UrlResource
 * @see FileUrlResource
 * @see FileSystemResource
 * @see ClassPathResource
 * @see ByteArrayResource
 * @see InputStreamResource
 * @since 28.12.2003
 */
public interface Resource extends InputStreamSource {

	/**
	 * 确定该资源是否以物理形式实际存在。
	 * <p>此方法执行明确的存在检查，而 {@code Resource} 句柄的存在仅保证有效的描述符句柄。
	 */
	boolean exists();

	/**
	 * 指示是否可以通过 {@link #getInputStream()} 读取此资源的非空内容。
	 * <p>对于存在的典型资源描述符将是 {@code true}，因为它严格暗示了 5.1 的 {@link #exists()} 语义。
	 * 请注意，尝试实际阅读内容时可能仍会失败。但是，{@code false} 值是无法读取资源内容的明确指示。
	 *
	 * @see #getInputStream()
	 * @see #exists()
	 */
	default boolean isReadable() {
		return exists();
	}

	/**
	 * 资源所代表的句柄是否被一个 stream 打开了。
	 * 如果 {@code true}，则 InputStream 不能被多次读取，必须读取并关闭以避免资源泄漏。
	 * <p>对于典型的资源描述符将是 {@code false}。
	 */
	default boolean isOpen() {
		return false;
	}

	/**
	 * 确定此资源是否代表文件系统中的文件。
	 * {@code true} 值强烈建议（但不保证）{@link #getFile()} 调用会成功。
	 * <p>默认情况下这是保守的 {@code false}。
	 *
	 * @see #getFile()
	 * @since 5.0
	 */
	default boolean isFile() {
		return false;
	}

	/**
	 * 返回此资源的 URL 句柄。
	 *
	 * @throws IOException 如果资源不能被解析为 URL，即如果资源不能作为描述符
	 */
	URL getURL() throws IOException;

	/**
	 * 返回此资源的 URI 句柄。
	 *
	 * @throws IOException 如果资源不能被解析为 URI，即如果资源不能作为描述符
	 * @since 2.5
	 */
	URI getURI() throws IOException;

	/**
	 * 返回此资源的文件句柄。
	 *
	 * @throws java.io.FileNotFoundException 如果资源无法解析为绝对文件路径，即如果资源在文件系统中不可用
	 * @throws IOException                   在一般分辨率读取失败的情况下
	 * @see #getInputStream()
	 */
	File getFile() throws IOException;

	/**
	 * 返回一个 {@link ReadableByteChannel}。
	 * <p>预计每次调用都会创建一个 <i>fresh<i> 通道。
	 * <p>默认实现返回 {@link Channels#newChannel(InputStream)} 和 {@link #getInputStream()} 的结果。
	 *
	 * @return 底层资源的字节通道（不得为 {@code null}）
	 * @throws java.io.FileNotFoundException 如果底层资源不存在
	 * @throws IOException                   如果无法打开内容频道
	 * @see #getInputStream()
	 * @since 5.0
	 */
	default ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * 确定此资源的内容长度
	 *
	 * @throws IOException 如果资源无法解析（在文件系统中或作为其他已知的物理资源类型）
	 */
	long contentLength() throws IOException;

	/**
	 * 确定此资源的最后修改时间戳。
	 *
	 * @throws IOException 如果资源无法解析（在文件系统中或作为其他已知的物理资源类型）
	 */
	long lastModified() throws IOException;

	/**
	 * 创建与此资源相关的资源。
	 *
	 * @param relativePath 相对路径（相对于此资源）
	 * @return 相对资源的资源句柄
	 * @throws IOException 如果无法确定相对资源
	 */
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * 确定此资源的文件名，即通常是路径的最后一部分：例如，“myfile.txt”。 <p>如果此类资源没有文件名，则返回 {@code null}。
	 */
	@Nullable
	String getFilename();

	/**
	 * 返回此资源的描述，用于在使用资源时输出错误。
	 * <p>还鼓励实现从其 {@code toString} 方法返回此值。
	 *
	 * @see Object#toString()
	 */
	String getDescription();

}
