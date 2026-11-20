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
package ch.qos.logback.classic.boolex;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class JaninoEventEvaluatorRemovedTest {

    LoggerContext loggerContext = new LoggerContext();
    Logger logger = loggerContext.getLogger(JaninoEventEvaluatorRemovedTest.class);
    StatusChecker checker = new StatusChecker(loggerContext);
    JaninoEventEvaluator jee = new JaninoEventEvaluator();

    @Before
    public void setUp() {
        jee.setContext(loggerContext);
        jee.setName("jee");
        jee.setExpression("true");
    }

    @Test
    public void expressionIsNotCompiledAndEvaluatorDoesNotStart() {
        jee.start();
        assertFalse(jee.isStarted());
        checker.assertContainsMatch(Status.ERROR, ".*CVE-2024-12798");
    }

    @Test
    public void evaluateRefusesEvenAfterStart() {
        jee.start();
        try {
            jee.evaluate(new LoggingEvent("fqcn", logger, Level.INFO, "msg", null, null));
            fail("EvaluationException expected");
        } catch (EvaluationException expected) {
        }
    }

    @Test
    public void evaluatorFilterHasNoDefaultEvaluatorClass() throws JoranException {
        configure("evaluatorFilter.xml");
        checker.assertContainsMatch(Status.ERROR, "Could not find an appropriate class for property \\[Evaluator\\]");
    }

    @Test
    public void declaredEvaluatorIsNotRegistered() throws JoranException {
        configure("ignore.xml");
        Map<?, ?> evaluatorMap = (Map<?, ?>) loggerContext.getObject(CoreConstants.EVALUATOR_MAP);
        assertNull(evaluatorMap.get("IGNORE_EVAL"));
    }

    private void configure(String file) throws JoranException {
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(loggerContext);
        jc.doConfigure(ClassicTestConstants.JORAN_INPUT_PREFIX + file);
    }
}
