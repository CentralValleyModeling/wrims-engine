package gov.ca.water.wrims.engine.core.solver.lookup;

import java.util.ArrayList;
import java.util.List;

import gov.ca.water.wrims.engine.core.solver.lookup.examplesolvers.TestSolver1;
import gov.ca.water.wrims.engine.core.solver.lookup.examplesolvers.TestSolver2;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

		ISolver solver = SolverBroker.findSolver("test1", ISolver.SolverType.GUROBI);
		assertNotNull(solver);
		solver.init();
		solver.setLP("test.solve");
		solver.solve();
		solver.getSolverInformation();

		assertNotNull(events);
		assertEquals("Solver 1: test.solve", events.get(0).getMessage().getFormattedMessage());
		assertEquals("1 * 2 * 3 = 6", events.get(1).getMessage().getFormattedMessage());
		assertEquals(TestSolver1.class.getName(), events.get(2).getMessage().getFormattedMessage());

		solver = SolverBroker.findSolver("test2", ISolver.SolverType.CBC);
		assertNotNull(solver);
		solver.init();
		solver.setLP("test2.solve");
		solver.solve();
		solver.getSolverInformation();
		assertEquals("Solver 2: test2.solve", events.get(3).getMessage().getFormattedMessage());
		assertEquals("1 + 2 = 3", events.get(4).getMessage().getFormattedMessage());
		assertEquals(TestSolver2.class.getName(), events.get(5).getMessage().getFormattedMessage());

		solver = SolverBroker.findSolver("test2", ISolver.SolverType.LPSOLVE);
		assertNull(solver);

		solver = SolverBroker.findSolver("test3", ISolver.SolverType.GUROBI);
		assertNull(solver);
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
