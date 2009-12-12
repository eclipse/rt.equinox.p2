package org.eclipse.equinox.internal.p2.ql;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.ql.IQueryContext;
import org.eclipse.equinox.p2.ql.ITranslationSupport;

public class QueryContext implements IQueryContext {

	private final IQueryable queryable;

	private Map translationSupports;

	public QueryContext(IQueryable queryable) {
		this.queryable = queryable;
	}

	public QueryContext(Iterator iterator) {
		final IRepeatableIterator repeatable = RepeatableIterator.create(iterator);
		this.queryable = new IQueryable() {
			public Collector query(IQuery query, IProgressMonitor monitor) {
				return query.perform(repeatable.getCopy(), new Collector());
			}
		};
	}

	public synchronized ITranslationSupport getTranslationSupport(Locale locale) {
		if (translationSupports == null)
			translationSupports = new HashMap();

		TranslationSupport ts = (TranslationSupport) translationSupports.get(locale);
		if (ts == null) {
			ts = new TranslationSupport();
			ts.setTranslationSource(queryable);
			ts.setLocale(locale);
			translationSupports.put(locale, ts);
		}
		return ts;
	}

	public Iterator iterator() {
		final Iterator[] iteratorCatcher = new Iterator[1];
		queryable.query(new ContextQuery() {
			public Collector perform(Iterator iterator, Collector result) {
				iteratorCatcher[0] = iterator;
				return null;
			}
		}, new NullProgressMonitor());
		return iteratorCatcher[0];
	}
}
