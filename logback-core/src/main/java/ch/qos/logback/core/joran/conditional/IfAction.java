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
package ch.qos.logback.core.joran.conditional;

import java.util.List;
import java.util.Stack;

import ch.qos.logback.core.CoreConstants;
import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.util.OptionHelper;

public class IfAction extends Action {
    private static final String CONDITION_ATTR = "condition";

    public static final String MISSING_JANINO_MSG = "Could not find Janino library on the class path. Skipping conditional processing.";
    public static final String MISSING_JANINO_SEE = "See also " + CoreConstants.CODES_URL + "#ifJanino";

    public static final String NEW_OPERATOR_DISALLOWED_MSG = "The 'condition' attribute may not contain the 'new' operator.";
    public static final String NEW_OPERATOR_DISALLOWED_SEE = "See also " + CoreConstants.CODES_URL + "#conditionNew";

    public static final String CONDITION_ATTR_REMOVED_MSG = "The 'condition' attribute in <if> element is no longer evaluated, Janino based conditional processing has been removed.";
    public static final String IF_IN_ERROR_MSG = "The [if] statement will be marked IN_ERROR and none of its branches will be executed.";

    Stack<IfState> stack = new Stack<IfState>();

    @Override
    public void begin(InterpretationContext ic, String name, Attributes attributes) throws ActionException {

        IfState state = new IfState();
        boolean emptyStack = stack.isEmpty();
        stack.push(state);

        if (!emptyStack) {
            return;
        }

        ic.pushObject(this);
        state.active = true;
        String conditionAttribute = attributes.getValue(CONDITION_ATTR);

        if (!OptionHelper.isEmpty(conditionAttribute)) {
            addWarn(CONDITION_ATTR_REMOVED_MSG);
            addWarn(IF_IN_ERROR_MSG);
            state.conditionRemoved = true;
        }
    }

    @Override
    public void end(InterpretationContext ic, String name) throws ActionException {

        IfState state = stack.pop();
        if (!state.active) {
            return;
        }

        Object o = ic.peekObject();
        if (o == null) {
            throw new IllegalStateException("Unexpected null object on stack");
        }
        if (!(o instanceof IfAction)) {
            throw new IllegalStateException("Unexpected object of type [" + o.getClass() + "] on stack");
        }

        if (o != this) {
            throw new IllegalStateException("IfAction different then current one on stack");
        }
        ic.popObject();

        if (state.conditionRemoved) {
            return;
        }

        if (state.boolResult == null) {
            addError("Failed to determine \"if then else\" result");
            return;
        }

        Interpreter interpreter = ic.getJoranInterpreter();
        List<SaxEvent> listToPlay = state.thenSaxEventList;
        if (!state.boolResult) {
            listToPlay = state.elseSaxEventList;
        }

        // if boolResult==false & missing else, listToPlay may be null
        if (listToPlay != null) {
            // insert past this event
            interpreter.getEventPlayer().addEventsDynamically(listToPlay, 1);
        }

    }

    public void setThenSaxEventList(List<SaxEvent> thenSaxEventList) {
        IfState state = stack.firstElement();
        if (state.active) {
            state.thenSaxEventList = thenSaxEventList;
        } else {
            throw new IllegalStateException("setThenSaxEventList() invoked on inactive IfAction");
        }
    }

    public void setElseSaxEventList(List<SaxEvent> elseSaxEventList) {
        IfState state = stack.firstElement();
        if (state.active) {
            state.elseSaxEventList = elseSaxEventList;
        } else {
            throw new IllegalStateException("setElseSaxEventList() invoked on inactive IfAction");
        }

    }

    public boolean isActive() {
        if (stack == null)
            return false;
        if (stack.isEmpty())
            return false;
        return stack.peek().active;
    }
}

class IfState {
    Boolean boolResult;
    List<SaxEvent> thenSaxEventList;
    List<SaxEvent> elseSaxEventList;
    boolean active;
    boolean conditionRemoved;
}
