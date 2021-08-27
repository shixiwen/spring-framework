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

import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 给定 {@link InputStream} 的 {@link Resource} 实现。
 *
 * <p>仅当没有其他特定的 {@code Resource} 实现适用时才应使用。
 * 特别是，在可能的情况下，更喜欢 {@link ByteArrayResource} 或任何基于文件的 {@code Resource} 实现。
 *
 * <p>与其他 {@code Resource} 实现相反，这是一个 <i>已打开<i> 资源的描述符 - 因此从 {@link #isOpen()} 返回 {@code true}。
 * 如果需要将资源描述符保留在某处，或者需要多次从流中读取，请不要使用 {@code InputStreamResource}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see ByteArrayResource
 * @see ClassPathResource
 * @see FileSystemResource
 * @see UrlResource
 * @since 28.12.2003
 */
public class InputStreamResource extends AbstractResource {

	private final InputStream inputStream;

	private final String description;

	private boolean read = false;


	/**
	 * Create a new InputStreamResource.
	 *
	 * @param inputStream the InputStream to use
	 */
	public InputStreamResource(InputStream inputStream) {
		this(inputStream, "resource loaded through InputStream");
	}

	/**
	 * Create a new InputStreamResource.
	 *
	 * @param inputStream the InputStream to use
	 * @param description where the InputStream comes from
	 */
	public InputStreamResource(InputStream inputStream, @Nullable String description) {
		Assert.notNull(inputStream, "InputStream must not be null");
		this.inputStream = inputStream;
		this.description = (description != null ? description : "");
	}


	/**
	 * This implementation always returns {@code true}.
	 */
	@Override
	public boolean exists() {
		return true;
	}

	/**
	 * This implementation always returns {@code true}.
	 */
	@Override
	public boolean isOpen() {
		return true;
	}

	/**
	 * This implementation throws IllegalStateException if attempting to
	 * read the underlying stream multiple times.
	 */
	@Override
	public InputStream getInputStream() throws IOException, IllegalStateException {
		if (this.read) {
			throw new IllegalStateException("InputStream has already been read - " +
					"do not use InputStreamResource if a stream needs to be read multiple times");
		}
		this.read = true;
		return this.inputStream;
	}

	/**
	 * This implementation returns a description that includes the passed-in
	 * description, if any.
	 */
	@Override
	public String getDescription() {
		return "InputStream resource [" + this.description + "]";
	}


	/**
	 * This implementation compares the underlying InputStream.
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof InputStreamResource &&
				((InputStreamResource) other).inputStream.equals(this.inputStream)));
	}

	/**
	 * This implementation returns the hash code of the underlying InputStream.
	 */
	@Override
	public int hashCode() {
		return this.inputStream.hashCode();
	}

}
