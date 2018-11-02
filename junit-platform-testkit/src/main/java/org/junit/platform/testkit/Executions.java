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

import static java.util.stream.Collectors.toList;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.assertj.core.api.ListAssert;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;

/**
 * {@link Executions} is a facade that provides a fluent API for working with
 * {@linkplain Execution executions}.
 *
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
public final class Executions {

	private final List<Execution> executions;
	private final String category;

	private Executions(Stream<Execution> executions, String category) {
		Preconditions.notNull(executions, "Execution stream must not be null");

		this.executions = Collections.unmodifiableList(executions.collect(toList()));
		this.category = category;
	}

	Executions(List<ExecutionEvent> events, String category) {
		Preconditions.notNull(events, "ExecutionEvent list must not be null");
		Preconditions.containsNoNullElements(events, "ExecutionEvent list must not contain null elements");

		this.executions = createExecutions(events);
		this.category = category;
	}

	// --- Accessors -----------------------------------------------------------

	public List<Execution> list() {
		return this.executions;
	}

	public Stream<Execution> stream() {
		return this.executions.stream();
	}

	/**
	 * Shortcut for {@code executions.stream().map(mapper)}.
	 */
	public <R> Stream<R> map(Function<? super Execution, ? extends R> mapper) {
		Preconditions.notNull(mapper, "Mapping function must not be null");
		return stream().map(mapper);
	}

	/**
	 * Shortcut for {@code executions.stream().filter(predicate)}.
	 */
	public Stream<Execution> filter(Predicate<? super Execution> predicate) {
		Preconditions.notNull(predicate, "Filter predicate must not be null");
		return stream().filter(predicate);
	}

	// --- Statistics ----------------------------------------------------------

	public long count() {
		return this.executions.size();
	}

	// --- Built-in Filters ----------------------------------------------------

	public Executions skipped() {
		return new Executions(executionsByTerminationInfo(TerminationInfo::skipped), this.category + " Skipped");
	}

	public Executions started() {
		return new Executions(executionsByTerminationInfo(TerminationInfo::notSkipped), this.category + " Started");
	}

	public Executions finished() {
		return new Executions(finishedExecutions(), this.category + " Finished");
	}

	public Executions aborted() {
		return new Executions(finishedExecutionsByStatus(Status.ABORTED), this.category + " Aborted");
	}

	public Executions succeeded() {
		return new Executions(finishedExecutionsByStatus(Status.SUCCESSFUL), this.category + " Successful");
	}

	public Executions failed() {
		return new Executions(finishedExecutionsByStatus(Status.FAILED), this.category + " Failed");
	}

	// --- Assertions ----------------------------------------------------------

	public ListAssert<Execution> assertThatExecutions() {
		return org.assertj.core.api.Assertions.assertThat(list());
	}

	// --- Diagnostics ---------------------------------------------------------

	/**
	 * Print all executions to {@link System#out}.
	 *
	 * @return this {@code Executions} object for method chaining; never {@code null}
	 */
	public Executions debug() {
		debug(System.out);
		return this;
	}

	/**
	 * Print all executions to the supplied {@link OutputStream}.
	 *
	 * @return this {@code Executions} object for method chaining; never {@code null}
	 */
	public Executions debug(OutputStream out) {
		debug(new PrintWriter(out, true));
		return this;
	}

	/**
	 * Print all executions to the supplied {@link Writer}.
	 *
	 * @return this {@code Executions} object for method chaining; never {@code null}
	 */
	public Executions debug(Writer writer) {
		debug(new PrintWriter(writer, true));
		return this;
	}

	private Executions debug(PrintWriter printWriter) {
		printWriter.println(this.category + " Executions:");
		this.executions.forEach(event -> printWriter.printf("\t%s%n", event));
		return this;
	}

	// --- Internals -----------------------------------------------------------

	private Stream<Execution> finishedExecutions() {
		return executionsByTerminationInfo(TerminationInfo::executed);
	}

	private Stream<Execution> finishedExecutionsByStatus(Status status) {
		Preconditions.notNull(status, "Status must not be null");
		return finishedExecutions()//
				.filter(execution -> execution.getTerminationInfo().getExecutionResult().getStatus().equals(status));
	}

	private Stream<Execution> executionsByTerminationInfo(Predicate<TerminationInfo> predicate) {
		return this.executions.stream().filter(execution -> predicate.test(execution.getTerminationInfo()));
	}

	/**
	 * Create executions from the supplied list of events.
	 */
	private static List<Execution> createExecutions(List<ExecutionEvent> executionEvents) {
		List<Execution> executions = new ArrayList<>();
		Map<TestDescriptor, Instant> executionStarts = new HashMap<>();

		for (ExecutionEvent executionEvent : executionEvents) {
			switch (executionEvent.getType()) {
				case STARTED: {
					executionStarts.put(executionEvent.getTestDescriptor(), executionEvent.getTimestamp());
					break;
				}
				case SKIPPED: {
					Instant startInstant = executionStarts.get(executionEvent.getTestDescriptor());
					Execution skippedEvent = Execution.skipped(executionEvent.getTestDescriptor(),
						startInstant != null ? startInstant : executionEvent.getTimestamp(),
						executionEvent.getTimestamp(), executionEvent.getPayloadAs(String.class));
					executions.add(skippedEvent);
					executionStarts.remove(executionEvent.getTestDescriptor());
					break;
				}
				case FINISHED: {
					Execution finishedEvent = Execution.finished(executionEvent.getTestDescriptor(),
						executionStarts.get(executionEvent.getTestDescriptor()), executionEvent.getTimestamp(),
						executionEvent.getPayloadAs(TestExecutionResult.class));
					executions.add(finishedEvent);
					executionStarts.remove(executionEvent.getTestDescriptor());
					break;
				}
				default: {
					// Ignore other events
					break;
				}
			}
		}

		return executions;
	}

}
