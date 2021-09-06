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

package org.springframework.util.xml;

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Detects whether an XML stream is using DTD- or XSD-based validation.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 */
public class XmlValidationModeDetector {

	/**
	 * 禁用验证。
	 */
	public static final int VALIDATION_NONE = 0;

	/**
	 * 自动猜测验证模式，因为我们找不到明确的指示（可能被某些特殊字符或类似字符阻塞）。
	 */
	public static final int VALIDATION_AUTO = 1;

	/**
	 * 使用DTD验证（我们发现了“DOCTYPE”声明）。
	 */
	public static final int VALIDATION_DTD = 2;

	/**
	 * 使用XSD验证（未找到“DOCTYPE”声明）。
	 */
	public static final int VALIDATION_XSD = 3;


	/**
	 * XML文档中的令牌，它声明要用于验证的DTD，从而声明正在使用的DTD验证。
	 */
	private static final String DOCTYPE = "DOCTYPE";

	/**
	 * The token that indicates the start of an XML comment.
	 */
	private static final String START_COMMENT = "<!--";

	/**
	 * The token that indicates the end of an XML comment.
	 */
	private static final String END_COMMENT = "-->";


	/**
	 * 指示当前解析位置是否在 XML 注释内。
	 */
	private boolean inComment;


	/**
	 * 在提供的 {@link InputStream} 中检测 XML 文档的验证模式。
	 * 请注意，提供的 {@link InputStream} 在返回之前被此方法关闭。
	 *
	 * @param inputStream the InputStream to parse
	 * @throws IOException in case of I/O failure
	 * @see #VALIDATION_DTD
	 * @see #VALIDATION_XSD
	 */
	public int detectValidationMode(InputStream inputStream) throws IOException {
		// 查看文件以查找 DOCTYPE。
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			// 是否为 DTD 校验模式。默认为，非 DTD 模式，即 XSD 模式
			boolean isDtdValidated = false;
			String content;
			// <0> 循环，逐行读取 XML 文件的内容
			while ((content = reader.readLine()) != null) {
				content = consumeCommentTokens(content);
				// 跳过，如果是注释，或者没有文字
				if (this.inComment || !StringUtils.hasText(content)) {
					continue;
				}
				// <1> 包含 DOCTYPE 为 DTD 模式
				if (hasDoctype(content)) {
					isDtdValidated = true;
					break;
				}
				// <2>  hasOpeningTag 方法会校验，如果这一行有 < ，并且 < 后面跟着的是字母，则返回 true 。
				if (hasOpeningTag(content)) {
					// 有意义的数据结束...
					break;
				}
			}
			// 返回 VALIDATION_DTD or VALIDATION_XSD 模式
			return (isDtdValidated ? VALIDATION_DTD : VALIDATION_XSD);
		} catch (CharConversionException ex) {
			// <3> 返回 VALIDATION_AUTO 模式

			// 被某种字符编码阻塞了...将决定权留给调用者。
			return VALIDATION_AUTO;
		}
	}


	/**
	 * 内容是否包含 DTD DOCTYPE 声明？
	 */
	private boolean hasDoctype(String content) {
		return content.contains(DOCTYPE);
	}

	/**
	 * 提供的内容是否包含 XML 开始标记。
	 * 如果解析状态当前处于 XML 注释中，则此方法始终返回 false。
	 * 预计在将剩余部分传递给此方法之前，所有评论标记都将用于提供的内容。
	 */
	private boolean hasOpeningTag(String content) {
		if (this.inComment) {
			return false;
		}
		int openTagIndex = content.indexOf('<');
		return (openTagIndex > -1 // < 存在
				&& (content.length() > openTagIndex + 1) // < 后面还有内容
				&& Character.isLetter(content.charAt(openTagIndex + 1))); // < 后面的内容是字母
	}

	/**
	 * Consume all leading and trailing comments in the given String and return
	 * the remaining content, which may be empty since the supplied content might
	 * be all comment data.
	 */
	@Nullable
	private String consumeCommentTokens(String line) {
		int indexOfStartComment = line.indexOf(START_COMMENT);
		//验证是否为注释
		if (indexOfStartComment == -1 && !line.contains(END_COMMENT)) {
			return line;
		}

		String result = "";
		String currLine = line;
		if (indexOfStartComment >= 0) {
			result = line.substring(0, indexOfStartComment);
			currLine = line.substring(indexOfStartComment);
		}

		while ((currLine = consume(currLine)) != null) {
			if (!this.inComment && !currLine.trim().startsWith(START_COMMENT)) {
				return result + currLine;
			}
		}
		return null;
	}

	/**
	 * 使用下一个注释标记，更新“inComment”标志并返回剩余的内容。
	 */
	@Nullable
	private String consume(String line) {
		int index = (this.inComment ? endComment(line) : startComment(line));
		return (index == -1 ? null : line.substring(index));
	}

	/**
	 * 尝试使用{@link #START_COMMENT}标记。
	 *
	 * @see #commentToken(String, String, boolean)
	 */
	private int startComment(String line) {
		return commentToken(line, START_COMMENT, true);
	}

	private int endComment(String line) {
		return commentToken(line, END_COMMENT, false);
	}

	/**
	 * 尝试根据提供的内容使用提供的令牌，并将注释解析状态更新为提供的值。
	 * 将索引返回到标记后面的内容中，如果找不到标记，则返回-1。
	 */
	private int commentToken(String line, String token, boolean inCommentIfPresent) {
		int index = line.indexOf(token);
		if (index > -1) {
			this.inComment = inCommentIfPresent;
		}
		return (index == -1 ? index : index + token.length());
	}

}
