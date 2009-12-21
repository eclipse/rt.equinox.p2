/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ql.expression;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.eclipse.equinox.p2.ql.IEvaluationContext;

/**
 * A collection filter that limits the number of entries in the collection
 */
final class Limit extends Binary {

	/**
	 * An iterator that stops iterating after a given number of iterations.
	 */
	static final class CountingIterator implements Iterator {
		private final Iterator innerIterator;
		private int counter;

		public CountingIterator(Iterator iterator, int count) {
			this.innerIterator = iterator;
			this.counter = count;
		}

		public boolean hasNext() {
			return counter > 0 && innerIterator.hasNext();
		}

		public Object next() {
			if (counter > 0) {
				--counter;
				return innerIterator.next();
			}
			throw new NoSuchElementException();
		}

		public void remove() {
			innerIterator.remove();
		}
	}

	Limit(Expression operand, Expression param) {
		super(operand, param);
		assertNotBoolean(operand, "operand"); //$NON-NLS-1$
		assertNotCollection(param, "parameter"); //$NON-NLS-1$
	}

	Limit(Expression operand, int limit) {
		this(operand, Constant.create(new Integer(limit)));
	}

	public Object evaluate(IEvaluationContext context) {
		Object rval = rhs.evaluate(context);
		int limit = -1;
		if (rval instanceof Integer)
			limit = ((Integer) rval).intValue();
		if (limit <= 0)
			throw new IllegalArgumentException("limit expression did not evalutate to a positive integer"); //$NON-NLS-1$
		return new CountingIterator(lhs.evaluateAsIterator(context), limit);
	}

	public int getExpressionType() {
		return TYPE_LIMIT;
	}

	public void toString(StringBuffer bld) {
		CollectionFilter.appendProlog(bld, lhs, getOperator());
		appendOperand(bld, rhs, PRIORITY_COMMA);
		bld.append(')');
	}

	String getOperator() {
		return KEYWORD_LIMIT;
	}

	int getPriority() {
		return PRIORITY_COLLECTION;
	}

	boolean isCollection() {
		return true;
	}

	boolean isElementBoolean() {
		return lhs.isElementBoolean();
	}
}