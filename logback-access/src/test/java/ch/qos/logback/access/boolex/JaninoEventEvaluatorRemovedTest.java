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
package ch.qos.logback.access.boolex;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.access.dummy.DummyAccessEventBuilder;
import ch.qos.logback.access.spi.AccessContext;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class JaninoEventEvaluatorRemovedTest {

    AccessContext context = new AccessContext();
    StatusChecker checker = new StatusChecker(context);
    JaninoEventEvaluator evaluator = new JaninoEventEvaluator();

    @Before
    public void setUp() {
        evaluator.setContext(context);
        evaluator.setName("evaluator");
        evaluator.setExpression("true");
    }

    @Test
    public void expressionIsNotCompiledAndEvaluatorDoesNotStart() {
        evaluator.start();
        assertFalse(evaluator.isStarted());
        checker.assertContainsMatch(Status.ERROR, ".*CVE-2024-12798");
    }

    @Test
    public void evaluateRefusesEvenAfterStart() {
        evaluator.start();
        try {
            evaluator.evaluate(DummyAccessEventBuilder.buildNewAccessEvent());
            fail("EvaluationException expected");
        } catch (EvaluationException expected) {
        }
    }
}
