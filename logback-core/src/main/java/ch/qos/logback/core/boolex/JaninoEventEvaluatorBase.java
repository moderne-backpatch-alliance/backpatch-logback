/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.boolex;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class which set the groundwork for janino based evaluations. The class
 * is kept for binary compatibility; expressions are no longer compiled or
 * evaluated, see CVE-2024-12798.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * 
 * @param <E>
 */
abstract public class JaninoEventEvaluatorBase<E> extends EventEvaluatorBase<E> {

    static public final int ERROR_THRESHOLD = 4;

    static final String REMOVED_MSG = "Janino based event evaluators have been removed, see CVE-2024-12798.";

    private String expression;

    abstract protected String getDecoratedExpression();

    abstract protected String[] getParameterNames();

    abstract protected Class<?>[] getParameterTypes();

    abstract protected Object[] getParameterValues(E event);

    protected List<Matcher> matcherList = new ArrayList<Matcher>();

    @Override
    public void start() {
        addError(REMOVED_MSG + " Evaluator [" + name + "] was not started.");
    }

    public boolean evaluate(E event) throws EvaluationException {
        throw new EvaluationException("Evaluator [" + name + "]: " + REMOVED_MSG);
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void addMatcher(Matcher matcher) {
        matcherList.add(matcher);
    }

    public List<Matcher> getMatcherList() {
        return matcherList;
    }
}
