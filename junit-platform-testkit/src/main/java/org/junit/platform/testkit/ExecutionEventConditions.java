/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit;

import static java.util.function.Predicate.isEqual;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.engine.TestExecutionResult.Status.ABORTED;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;
import static org.junit.platform.testkit.ExecutionEvent.Type.DYNAMIC_TEST_REGISTERED;
import static org.junit.platform.testkit.ExecutionEvent.Type.FINISHED;
import static org.junit.platform.testkit.ExecutionEvent.Type.SKIPPED;
import static org.junit.platform.testkit.ExecutionEvent.Type.STARTED;
import static org.junit.platform.testkit.ExecutionEvent.byPayload;
import static org.junit.platform.testkit.ExecutionEvent.byTestDescriptor;
import static org.junit.platform.testkit.ExecutionEvent.byType;

import java.util.List;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Index;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * Collection of AssertJ conditions for {@link ExecutionEvent}s.
 *
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
public class ExecutionEventConditions {

	private ExecutionEventConditions() {
	}

	@SafeVarargs
	public static void assertExecutionEventsMatchExactly(List<ExecutionEvent> executionEvents,
			Condition<? super ExecutionEvent>... conditions) {
		SoftAssertions softly = new SoftAssertions();
		Assertions.assertThat(executionEvents).hasSize(conditions.length);
		for (int i = 0; i < conditions.length; i++) {
			softly.assertThat(executionEvents).has(conditions[i], Index.atIndex(i));
		}
		softly.assertAll();
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	public static Condition<ExecutionEvent> event(Condition<? super ExecutionEvent>... conditions) {
		return Assertions.allOf(conditions);
	}

	public static Condition<ExecutionEvent> engine() {
		return new Condition<>(byTestDescriptor(EngineDescriptor.class::isInstance), "is an engine");
	}

	public static Condition<ExecutionEvent> test(String uniqueIdSubstring) {
		return Assertions.allOf(test(), uniqueIdSubstring(uniqueIdSubstring));
	}

	public static Condition<ExecutionEvent> test(String uniqueIdSubstring, String displayName) {
		return Assertions.allOf(test(), uniqueIdSubstring(uniqueIdSubstring), displayName(displayName));
	}

	public static Condition<ExecutionEvent> test() {
		return new Condition<>(byTestDescriptor(TestDescriptor::isTest), "is a test");
	}

	public static Condition<ExecutionEvent> container(Class<?> clazz) {
		return container(clazz.getName());
	}

	public static Condition<ExecutionEvent> container(String uniqueIdSubstring) {
		return container(uniqueIdSubstring(uniqueIdSubstring));
	}

	public static Condition<ExecutionEvent> container(Condition<ExecutionEvent> condition) {
		return Assertions.allOf(container(), condition);
	}

	public static Condition<ExecutionEvent> container() {
		return new Condition<>(byTestDescriptor(TestDescriptor::isContainer), "is a container");
	}

	public static Condition<ExecutionEvent> nestedContainer(Class<?> clazz) {
		return Assertions.allOf(container(uniqueIdSubstring(clazz.getEnclosingClass().getName())),
			container(uniqueIdSubstring(clazz.getSimpleName())));
	}

	public static Condition<ExecutionEvent> dynamicTestRegistered(String uniqueIdSubstring) {
		return dynamicTestRegistered(uniqueIdSubstring(uniqueIdSubstring));
	}

	public static Condition<ExecutionEvent> dynamicTestRegistered(Condition<ExecutionEvent> condition) {
		return Assertions.allOf(type(DYNAMIC_TEST_REGISTERED), condition);
	}

	public static Condition<ExecutionEvent> uniqueIdSubstring(String uniqueIdSubstring) {
		Predicate<UniqueId.Segment> predicate = segment -> {
			String text = segment.getType() + ":" + segment.getValue();
			return text.contains(uniqueIdSubstring);
		};
		return new Condition<>(
			byTestDescriptor(
				where(TestDescriptor::getUniqueId, uniqueId -> uniqueId.getSegments().stream().anyMatch(predicate))),
			"descriptor with uniqueId substring '%s'", uniqueIdSubstring);
	}

	public static Condition<ExecutionEvent> displayName(String displayName) {
		return new Condition<>(byTestDescriptor(where(TestDescriptor::getDisplayName, isEqual(displayName))),
			"descriptor with display name '%s'", displayName);
	}

	public static Condition<ExecutionEvent> skippedWithReason(String expectedReason) {
		return Assertions.allOf(type(SKIPPED), reason(expectedReason));
	}

	public static Condition<ExecutionEvent> skippedWithReason(Predicate<String> predicate) {
		return Assertions.allOf(type(SKIPPED), reason(predicate));
	}

	public static Condition<ExecutionEvent> started() {
		return type(STARTED);
	}

	public static Condition<ExecutionEvent> abortedWithReason(Condition<? super Throwable> causeCondition) {
		return finishedWithCause(ABORTED, causeCondition);
	}

	public static Condition<ExecutionEvent> finishedWithFailure(Condition<? super Throwable> causeCondition) {
		return finishedWithCause(FAILED, causeCondition);
	}

	private static Condition<ExecutionEvent> finishedWithCause(Status expectedStatus,
			Condition<? super Throwable> causeCondition) {

		return finished(Assertions.allOf(TestExecutionResultConditions.status(expectedStatus),
			TestExecutionResultConditions.cause(causeCondition)));
	}

	public static Condition<ExecutionEvent> finishedWithFailure() {
		return finished(TestExecutionResultConditions.status(FAILED));
	}

	public static Condition<ExecutionEvent> finishedSuccessfully() {
		return finished(TestExecutionResultConditions.status(SUCCESSFUL));
	}

	public static Condition<ExecutionEvent> finished(Condition<TestExecutionResult> resultCondition) {
		return Assertions.allOf(type(FINISHED), result(resultCondition));
	}

	public static Condition<ExecutionEvent> type(ExecutionEvent.Type expectedType) {
		return new Condition<>(byType(expectedType), "type is %s", expectedType);
	}

	public static Condition<ExecutionEvent> result(Condition<TestExecutionResult> condition) {
		return new Condition<>(byPayload(TestExecutionResult.class, condition::matches), "event with result where %s",
			condition);
	}

	public static Condition<ExecutionEvent> reason(String expectedReason) {
		return new Condition<>(byPayload(String.class, isEqual(expectedReason)), "event with reason '%s'",
			expectedReason);
	}

	public static Condition<ExecutionEvent> reason(Predicate<String> predicate) {
		return new Condition<>(byPayload(String.class, predicate), "event with custom reason predicate");
	}

}
