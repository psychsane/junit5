/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.conditions;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.ExecutionEventConditions.container;
import static org.junit.platform.testkit.ExecutionEventConditions.engine;
import static org.junit.platform.testkit.ExecutionEventConditions.event;
import static org.junit.platform.testkit.ExecutionEventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.ExecutionEventConditions.skippedWithReason;
import static org.junit.platform.testkit.ExecutionEventConditions.started;
import static org.junit.platform.testkit.ExecutionEventConditions.test;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.testkit.ExecutionRecorder;
import org.junit.platform.testkit.ExecutionResults;
import org.junit.platform.testkit.FilteredResults;

/**
 * Integration tests for JUnit 4's {@link Ignore @Ignore} support in JUnit
 * Jupiter provided by the {@link IgnoreCondition}.
 *
 * @since 5.4
 * @see IgnoreAnnotationIntegrationTests
 */
class IgnoreConditionTests {

	@Test
	void ignoredTestClassWithDefaultMessage() {
		Class<?> testClass = IgnoredClassWithDefaultMessageTestCase.class;

		// @formatter:off
		executeTestsForClass(testClass).events().assertEventsMatchExactly(
			event(engine(), started()),
			event(container(testClass), skippedWithReason(testClass + " is disabled via @org.junit.Ignore")),
			event(engine(), finishedSuccessfully())
		);
		// @formatter:on
	}

	@Test
	void ignoredTestClassWithCustomMessage() {
		Class<?> testClass = IgnoredClassWithCustomMessageTestCase.class;

		// @formatter:off
		executeTestsForClass(testClass).events().assertEventsMatchExactly(
			event(engine(), started()),
			event(container(testClass), skippedWithReason("Ignored Class")),
			event(engine(), finishedSuccessfully())
		);
		// @formatter:on
	}

	@Test
	void ignoredAndNotIgnoredTestMethods() {
		ExecutionResults executionResults = executeTestsForClass(IgnoredMethodsTestCase.class);
		FilteredResults containers = executionResults.containers();
		FilteredResults tests = executionResults.tests();

		executionResults.events().debug();
		// events.debug(System.err);

		containers.events().debug();

		// tests.events().debug(System.err);
		tests.events().debug();
		tests.events().skipped().debug();
		tests.events().started().debug();
		tests.events().succeeded().debug();

		executionResults.events().executions().debug();
		containers.events().executions().debug();
		tests.events().executions().debug();

		// @formatter:off
		tests.events().assertEventsMatchExactly(
			event(test("ignoredWithCustomMessage"), skippedWithReason("Ignored Method")),
			event(test("notIgnored"), started()),
			event(test("notIgnored"), finishedSuccessfully()),
			event(test("ignoredWithDefaultMessage"), skippedWithReason(
				reason -> reason.endsWith("ignoredWithDefaultMessage() is disabled via @org.junit.Ignore")))
		);
		// @formatter:on
	}

	private ExecutionResults executeTestsForClass(Class<?> testClass) {
		return ExecutionRecorder.execute(new JupiterTestEngine(), request().selectors(selectClass(testClass)).build());
	}

	// -------------------------------------------------------------------------

	@ExtendWith(IgnoreCondition.class)
	@Ignore
	static class IgnoredClassWithDefaultMessageTestCase {

		@Test
		void ignoredBecauseClassIsIgnored() {
			/* no-op */
		}
	}

	@ExtendWith(IgnoreCondition.class)
	@Ignore("Ignored Class")
	static class IgnoredClassWithCustomMessageTestCase {

		@Test
		void ignoredBecauseClassIsIgnored() {
			/* no-op */
		}
	}

	@ExtendWith(IgnoreCondition.class)
	static class IgnoredMethodsTestCase {

		@Test
		void notIgnored() {
			/* no-op */
		}

		@Test
		@Ignore
		void ignoredWithDefaultMessage() {
			fail("This method should have been disabled via @Ignore");
		}

		@Test
		@Ignore("Ignored Method")
		void ignoredWithCustomMessage() {
			fail("This method should have been disabled via @Ignore");
		}
	}

}
