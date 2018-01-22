package io.github.syst3ms.skriptparser.registration;

import io.github.syst3ms.skriptparser.pattern.PatternElement;

import java.util.List;

/**
 * Represents something that returns something and has patterns which
 * may result it being matched
 */
public class ExpressionInfo<C, T> extends SyntaxInfo<C> {
	private Type<T> returnType;

	public ExpressionInfo(Class<C> c, List<PatternElement> patterns, Type<T> returnType) {
		super(c, patterns);
		this.returnType = returnType;
	}

	public Type<T> getReturnType() {
		return returnType;
	}
}