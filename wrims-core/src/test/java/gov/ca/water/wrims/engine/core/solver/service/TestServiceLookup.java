package gov.ca.water.wrims.engine.core.solver.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import gov.ca.water.wrims.engine.core.solver.solvers.Solver;
import gov.ca.water.wrims.engine.core.solver.solvers.SolverA;
import gov.ca.water.wrims.engine.core.solver.solvers.ImprovedSolverA;
import gov.ca.water.wrims.engine.core.solver.solvers.SolverB;
import gov.ca.water.wrims.engine.core.solver.solvers.SolverIdentifier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class TestServiceLookup
{
	private static CapturingAppender appender;
	private static LoggerConfig loggerConfig;

	@BeforeEach
	void setup()
	{
		appender = new CapturingAppender();

		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();

		loggerConfig = config.getLoggerConfig(TestServiceLookup.class.getName());

		loggerConfig.addAppender(appender, Level.DEBUG, null);

		ctx.updateLoggers();
	}

	@AfterEach
	void tearDown()
	{
		loggerConfig.removeAppender(appender.getName());
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		ctx.updateLoggers();
	}

	@Test
	void testSolverLookup()
	{
		List<LogEvent> events = appender.getEvents();

		Solver solver = SolverBroker.findSolver("CBC_IMPROVED");
		assertNotNull(solver);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		Solver.SolverInfo info = solver.getSolverInformation();
		assertEquals(Solver.LOOKUP_PATH + "CBC_IMPROVED", info.getPath());
		assertEquals("CBC_IMPROVED", info.getLookupName());
		assertEquals(500, info.getPosition());

		assertNotNull(events);
		String formattedMessage = events.getFirst().getMessage().getFormattedMessage();
		assertTrue(formattedMessage.contains("Improved Solver A: "));
		assertTrue(formattedMessage.contains("\\solve\\test.solve"));
		assertEquals("1 * 2 * 3^2 = 18.0", events.get(1).getMessage().getFormattedMessage());

		solver = SolverBroker.findSolver("GUROBI");
		assertNotNull(solver);
		solver.init();
		solver.setLP("test2.solve");
		solver.solve();
		info = solver.getSolverInformation();
		assertEquals(Solver.LOOKUP_PATH + "GUROBI", info.getPath());
		assertEquals("GUROBI", info.getLookupName());
		assertEquals(1000, info.getPosition());
		assertTrue(events.stream()
				.anyMatch(log -> log.getMessage().getFormattedMessage()
						.equalsIgnoreCase("Solver B: test2.solve")));
		assertTrue(events.stream()
				.anyMatch(log -> log.getMessage().getFormattedMessage()
						.equalsIgnoreCase("1 + 2 = 3")));

		solver = SolverBroker.findSolver("CLP");
		assertNull(solver);

		solver = SolverBroker.findSolver("XA");
		assertNull(solver);
	}

	@Test
	void testSolverOrder()
	{
		Solver solver = SolverBroker.findSolver("CBC_IMPROVED");
		assertNotNull(solver);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		Solver.SolverInfo info = solver.getSolverInformation();
		assertEquals(Solver.LOOKUP_PATH + "CBC_IMPROVED", info.getPath());
		assertEquals("CBC_IMPROVED", info.getLookupName());
		assertEquals(500, info.getPosition());
	}

	@Test
	void testGetAll()
	{
		SolverBroker.clearCache();
		List<Solver> solvers = SolverBroker.getAllSolvers();
		assertNotNull(solvers);
		assertEquals(3, solvers.size());
		int count = 0;

		for (Solver solver : solvers)
		{
			assertNotNull(solver);

			Solver.SolverInfo info = solver.getSolverInformation();
			assertNotNull(info);

			switch(solver)
			{
				case ImprovedSolverA solverARev2 ->
				{
					assertEquals(Solver.LOOKUP_PATH + "CBC_IMPROVED", info.getPath());
					assertEquals("CBC_IMPROVED", info.getLookupName());
					assertEquals(500, info.getPosition());
					count++;
				}
				case SolverA solverA ->
				{
					assertEquals(Solver.LOOKUP_PATH + "CBC", info.getPath());
					assertEquals("CBC", info.getLookupName());
					assertEquals(1000, info.getPosition());
					count++;
				}
				case SolverB solverB ->
				{
					assertEquals(Solver.LOOKUP_PATH + "GUROBI", info.getPath());
					assertEquals("GUROBI", info.getLookupName());
					assertEquals(1000, info.getPosition());
					count++;
				}
				default ->
					fail(String.format("Unexpected solver type: %s", solver.getClass().getName()));
			}
		}
		assertEquals(3, count);
	}

	@Test
	void testSolverCache()
	{
		SolverBroker.clearCache();
		Solver solver = SolverBroker.findSolver("CBC_IMPROVED", SolverBroker.CacheMode.CACHE_ONLY);
		assertNull(solver);

		solver = SolverBroker.findSolver("CBC_IMPROVED", SolverBroker.CacheMode.BYPASS);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		Solver.SolverInfo info = solver.getSolverInformation();
		assertEquals(Solver.LOOKUP_PATH + "CBC_IMPROVED", info.getPath());
		assertEquals("CBC_IMPROVED", info.getLookupName());
		assertEquals(500, info.getPosition());

		solver = SolverBroker.findSolver("CBC_IMPROVED", SolverBroker.CacheMode.CACHE_ONLY);
		assertNull(solver);

		solver = SolverBroker.findSolver("CBC_IMPROVED", SolverBroker.CacheMode.CACHE);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		info = solver.getSolverInformation();
		assertEquals(Solver.LOOKUP_PATH + "CBC_IMPROVED", info.getPath());
		assertEquals("CBC_IMPROVED", info.getLookupName());
		assertEquals(500, info.getPosition());
		UUID uniqueIdentifier = ((SolverIdentifier) solver).getIdentifier();

		solver = SolverBroker.findSolver("CBC_IMPROVED", SolverBroker.CacheMode.CACHE_ONLY);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		info = solver.getSolverInformation();
		assertEquals(Solver.LOOKUP_PATH + "CBC_IMPROVED", info.getPath());
		assertEquals("CBC_IMPROVED", info.getLookupName());
		assertEquals(500, info.getPosition());
		assertEquals(uniqueIdentifier, ((SolverIdentifier) solver).getIdentifier(),
				"Unique identifier should remain the same");
	}

	@Test
	void testInstances() throws Exception
	{
		SolverBroker.clearCache();

		Solver solver = SolverBroker.findSolver("GUROBI", SolverBroker.CacheMode.BYPASS);
		assertNotNull(solver);

		Solver.SolverInfo info = solver.getSolverInformation();
		assertEquals(Solver.LOOKUP_PATH + "GUROBI", info.getPath());
		assertEquals("GUROBI", info.getLookupName());
		assertEquals(1000, info.getPosition());
		UUID uniqueIdentifier = info.getIdentifier();

		// Verify we're not working on cache
		SolverBroker.clearCache();
		assertFalse(SolverBroker.inCache("GUROBI"));

		Thread.sleep(5000);

		solver = SolverBroker.findSolver("GUROBI", SolverBroker.CacheMode.BYPASS);
		assertNotNull(solver);

		info = solver.getSolverInformation();
		assertEquals(Solver.LOOKUP_PATH + "GUROBI", info.getPath());
		assertEquals("GUROBI", info.getLookupName());
		assertEquals(1000, info.getPosition());
		assertEquals(uniqueIdentifier, info.getIdentifier(), "Unique identifier should remain the same");
	}

	@Test
	void testInstancesGetAll() throws Exception
	{
		SolverBroker.clearCache();

		List<Solver> solvers = SolverBroker.getAllSolvers(SolverBroker.CacheMode.BYPASS);
		assertFalse(solvers.isEmpty());

		Map<String, UUID> uniqueIdentifiers = new HashMap<>();

		solvers.forEach(solver ->
				uniqueIdentifiers.put(solver.getSolverInformation().getLookupName()
						+ solver.getSolverInformation().getPosition(), solver.getSolverInformation().getIdentifier()));
		assertEquals(3, uniqueIdentifiers.size());

		// Verify we're not working on cache
		SolverBroker.clearCache();
		assertFalse(SolverBroker.inCache("GUROBI"));
		assertFalse(SolverBroker.inCache("CBC"));
		assertFalse(SolverBroker.inCache("CBC_IMPROVED"));

		Thread.sleep(5000);

		solvers = SolverBroker.getAllSolvers(SolverBroker.CacheMode.BYPASS);
		assertFalse(solvers.isEmpty());

		for (Solver solver : solvers)
		{
			UUID uniqueIdentifier = uniqueIdentifiers.get(solver.getSolverInformation().getLookupName()
										+ solver.getSolverInformation().getPosition());
			assertNotNull(uniqueIdentifier);
			assertEquals(uniqueIdentifier, solver.getSolverInformation().getIdentifier(),
					"Unique identifier should remain the same");
		}
	}

	// Custom Appender to capture events in memory
	private static class CapturingAppender extends AbstractAppender
	{
		private final List<LogEvent> events = new ArrayList<>();

		CapturingAppender() {
			super("CapturingAppender", null, null, true, Property.EMPTY_ARRAY);
			this.start();
		}

		@Override
		public void append(LogEvent event) {
			// Convert to immutable to ensure the data doesn't change after the event is processed
			events.add(event.toImmutable());
		}

		public List<LogEvent> getEvents() {
			return events;
		}
	}
}
