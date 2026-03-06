package gov.ca.water.wrims.engine.core.solver.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.ca.water.wrims.engine.core.solver.lookup.examplesolvers.SolverA;
import gov.ca.water.wrims.engine.core.solver.lookup.examplesolvers.SolverA_Rev2;
import gov.ca.water.wrims.engine.core.solver.lookup.examplesolvers.SolverB;
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

		ISolver solver = SolverBroker.findSolver(SolverTypes.CBC);
		assertNotNull(solver);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		ISolver.SolverInfo info = solver.getSolverInformation();
		assertEquals(ISolver.LOOKUP_PATH + SolverTypes.CBC, info.getPath());
		assertEquals(SolverTypes.CBC, info.getLookupName());
		assertEquals(500, info.getPosition());

		assertNotNull(events);
		assertEquals("Solver 1: test.solve", events.get(0).getMessage().getFormattedMessage());
		assertEquals("1 * 2 * 3 = 6", events.get(1).getMessage().getFormattedMessage());

		solver = SolverBroker.findSolver(SolverTypes.GUROBI);
		assertNotNull(solver);
		solver.init();
		solver.setLP("test2.solve");
		solver.solve();
		info = solver.getSolverInformation();
		assertEquals(ISolver.LOOKUP_PATH + SolverTypes.GUROBI, info.getPath());
		assertEquals(SolverTypes.GUROBI, info.getLookupName());
		assertEquals(1000, info.getPosition());
		assertTrue(events.stream()
				.anyMatch(log -> log.getMessage().getFormattedMessage()
						.equalsIgnoreCase("Solver 2: test2.solve")));
		assertTrue(events.stream()
				.anyMatch(log -> log.getMessage().getFormattedMessage()
						.equalsIgnoreCase("1 + 2 = 3")));

		solver = SolverBroker.findSolver(SolverTypes.CLP);
		assertNull(solver);

		solver = SolverBroker.findSolver(SolverTypes.XA);
		assertNull(solver);
	}

	@Test
	void testSolverOrder()
	{
		ISolver solver = SolverBroker.findSolver(SolverTypes.CBC);
		assertNotNull(solver);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		ISolver.SolverInfo info = solver.getSolverInformation();
		assertEquals(ISolver.LOOKUP_PATH + SolverTypes.CBC, info.getPath());
		assertEquals(SolverTypes.CBC, info.getLookupName());
		assertEquals(500, info.getPosition());
	}

	@Test
	void testGetAll()
	{
		SolverBroker.clearCache();
		List<ISolver> solvers = SolverBroker.getAllSolvers();
		assertNotNull(solvers);
		assertEquals(3, solvers.size());
		int count = 0;

		for (ISolver solver : solvers)
		{
			assertNotNull(solver);

			ISolver.SolverInfo info = solver.getSolverInformation();
			assertNotNull(info);

			switch(solver)
			{
				case SolverA solverA ->
				{
					assertEquals(ISolver.LOOKUP_PATH + SolverTypes.CBC, info.getPath());
					assertEquals(SolverTypes.CBC, info.getLookupName());
					assertEquals(1000, info.getPosition());
					count++;
				}
				case SolverA_Rev2 solverARev2 ->
				{
					assertEquals(ISolver.LOOKUP_PATH + SolverTypes.CBC, info.getPath());
					assertEquals(SolverTypes.CBC, info.getLookupName());
					assertEquals(500, info.getPosition());
					count++;
				}
				case SolverB solverB ->
				{
					assertEquals(ISolver.LOOKUP_PATH + SolverTypes.GUROBI, info.getPath());
					assertEquals(SolverTypes.GUROBI, info.getLookupName());
					assertEquals(1000, info.getPosition());
					count++;
				}
				default ->
				{
					fail(String.format("Unexpected solver type: %s", solver.getClass().getName()));
				}
			}
		}
		assertEquals(3, count);
	}

	@Test
	void testSolverCache()
	{
		SolverBroker.clearCache();
		ISolver solver = SolverBroker.findSolver(SolverTypes.CBC, SolverBroker.CacheMode.CACHE_ONLY);
		assertNull(solver);

		solver = SolverBroker.findSolver(SolverTypes.CBC, SolverBroker.CacheMode.BYPASS);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		ISolver.SolverInfo info = solver.getSolverInformation();
		assertEquals(ISolver.LOOKUP_PATH + SolverTypes.CBC, info.getPath());
		assertEquals(SolverTypes.CBC, info.getLookupName());
		assertEquals(500, info.getPosition());

		solver = SolverBroker.findSolver(SolverTypes.CBC, SolverBroker.CacheMode.CACHE_ONLY);
		assertNull(solver);

		solver = SolverBroker.findSolver(SolverTypes.CBC, SolverBroker.CacheMode.CACHE);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		info = solver.getSolverInformation();
		assertEquals(ISolver.LOOKUP_PATH + SolverTypes.CBC, info.getPath());
		assertEquals(SolverTypes.CBC, info.getLookupName());
		assertEquals(500, info.getPosition());

		solver = SolverBroker.findSolver(SolverTypes.CBC, SolverBroker.CacheMode.CACHE_ONLY);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		info = solver.getSolverInformation();
		assertEquals(ISolver.LOOKUP_PATH + SolverTypes.CBC, info.getPath());
		assertEquals(SolverTypes.CBC, info.getLookupName());
		assertEquals(500, info.getPosition());
	}

	@Test
	void testInstances() throws Exception
	{
		SolverBroker.clearCache();

		ISolver solver = SolverBroker.findSolver(SolverTypes.GUROBI, SolverBroker.CacheMode.BYPASS);
		assertNotNull(solver);

		ISolver.SolverInfo info = solver.getSolverInformation();
		assertEquals(ISolver.LOOKUP_PATH + SolverTypes.GUROBI, info.getPath());
		assertEquals(SolverTypes.GUROBI, info.getLookupName());
		assertEquals(1000, info.getPosition());
		long uniqueIdentifier = info.getIdentifier();

		// Verify we're not working on cache
		SolverBroker.clearCache();
		assertFalse(SolverBroker.inCache(SolverTypes.GUROBI));

		Thread.sleep(5000);

		solver = SolverBroker.findSolver(SolverTypes.GUROBI, SolverBroker.CacheMode.BYPASS);
		assertNotNull(solver);

		info = solver.getSolverInformation();
		assertEquals(ISolver.LOOKUP_PATH + SolverTypes.GUROBI, info.getPath());
		assertEquals(SolverTypes.GUROBI, info.getLookupName());
		assertEquals(1000, info.getPosition());
		assertEquals(uniqueIdentifier, info.getIdentifier(), "Unique identifier should remain the same");
	}

	@Test
	void testInstancesGetAll() throws Exception
	{
		SolverBroker.clearCache();

		List<ISolver> solvers = SolverBroker.getAllSolvers(SolverBroker.CacheMode.BYPASS);
		assertFalse(solvers.isEmpty());

		Map<String, Long> uniqueIdentifiers = new HashMap<>();

		solvers.forEach(solver ->
				uniqueIdentifiers.put(solver.getSolverInformation().getLookupName()
						+ solver.getSolverInformation().getPosition(), solver.getSolverInformation().getIdentifier()));
		assertEquals(3, uniqueIdentifiers.size());

		// Verify we're not working on cache
		SolverBroker.clearCache();
		assertFalse(SolverBroker.inCache(SolverTypes.GUROBI));
		assertFalse(SolverBroker.inCache(SolverTypes.CBC));

		Thread.sleep(5000);

		solvers = SolverBroker.getAllSolvers(SolverBroker.CacheMode.BYPASS);
		assertFalse(solvers.isEmpty());

		for (ISolver solver : solvers)
		{
			Long uniqueIdentifier = uniqueIdentifiers.get(solver.getSolverInformation().getLookupName()
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
