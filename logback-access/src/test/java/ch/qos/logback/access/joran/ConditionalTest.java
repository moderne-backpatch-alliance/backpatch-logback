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
package ch.qos.logback.access.joran;

import ch.qos.logback.access.AccessTestConstants;
import ch.qos.logback.access.spi.AccessContext;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.conditional.IfAction;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CoreTestConstants;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNull;

/**
 * @author Ceki G&uuml;lc&uuml;
 */
public class ConditionalTest {

    AccessContext context = new AccessContext();
    StatusChecker checker = new StatusChecker(context);

    int diff = RandomUtil.getPositiveInt();
    String randomOutputDir = CoreTestConstants.OUTPUT_DIR_PREFIX + diff + "/";

    @Before
    public void setUp() {
        InetAddress localhost = null;
        try {
            localhost = InetAddress.getLocalHost();
            context.putProperty("aHost", localhost.getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    void configure(String file) throws JoranException {
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(context);
        jc.doConfigure(file);
    }

    @Test
    public void conditionalConsoleApp_IF_THEN_NotEvaluatedWhenHostMatches() throws JoranException, UnknownHostException {
        configure(AccessTestConstants.TEST_DIR_PREFIX + "input/joran/conditional/conditionalConsole.xml");
        ConsoleAppender consoleAppender = (ConsoleAppender) context.getAppender("CON");
        assertNull(consoleAppender);
        verifyConditionRemovedWarning();
    }

    @Test
    public void conditionalConsoleApp_IF_THEN_NotEvaluatedWhenHostIsUnset() throws JoranException, IOException, InterruptedException {
        context.putProperty("aHost", null);
        configure(AccessTestConstants.TEST_DIR_PREFIX + "input/joran/conditional/conditionalConsole.xml");

        ConsoleAppender consoleAppender = (ConsoleAppender) context.getAppender("CON");
        assertNull(consoleAppender);
        verifyConditionRemovedWarning();
    }

    @Test
    public void conditionalConsoleApp_ELSE_NeitherBranchIsEvaluated() throws JoranException, IOException, InterruptedException {
        configure(AccessTestConstants.TEST_DIR_PREFIX + "input/joran/conditional/conditionalConsole_ELSE.xml");
        ConsoleAppender consoleAppender = (ConsoleAppender) context.getAppender("CON");
        assertNull(consoleAppender);

        ListAppender listAppender = (ListAppender) context.getAppender("LIST");
        assertNull(listAppender);
        verifyConditionRemovedWarning();
    }

    private void verifyConditionRemovedWarning() {
        checker.assertContainsMatch(Status.WARN, Pattern.quote(IfAction.CONDITION_ATTR_REMOVED_MSG));
        checker.assertContainsMatch(Status.WARN, Pattern.quote(IfAction.IF_IN_ERROR_MSG));
        checker.assertIsErrorFree();
    }
}
