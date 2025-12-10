package gov.ca.water.wrims.engine.core.components;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ControllerDebugTest {

    private ControllerDebug controller;

    @BeforeEach
    void setUp() {
        controller = new ControllerDebug(new String[0], null);
    }

    @AfterEach
    void tearDown() {
        // Make sure any waiting thread can complete between tests
        try {
            controller.requestTerminate();
        } catch (Throwable ignored) {
        }
    }

    @Test
    void requestPause_setsPausedTrue() throws Exception {
        // precondition
        assertFalse(controller.isPaused(), "paused should start as false");

        controller.requestPause();

        assertTrue(controller.isPaused(), "paused should be true after requestPause()");
    }

    @Test
    void pauseHereUntilResumed_blocksUntilResumed_evenWhenNotPreviouslyPaused() throws Exception {
        // The current implementation marks paused=true inside pauseHereUntilResumed and waits until resumed or terminated.
        CountDownLatch started = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            started.countDown();
            try {
                controller.pauseHereUntilResumed();
            } catch (Exception e) {
                fail("Exception in pause thread: " + e.getMessage());
            }
        });
        t.start();

        // Wait for the thread to start and attempt to pause
        assertTrue(started.await(500, TimeUnit.MILLISECONDS), "worker thread did not start");

        // Give it a moment to enter wait state
        Thread.sleep(100);
        assertTrue(t.isAlive(), "Thread should be waiting inside pauseHereUntilResumed until resumed");

        // Now resume and verify it unblocks and clears paused
        controller.safeResume();

        // It should finish quickly
        t.join(2000);
        assertFalse(t.isAlive(), "Thread should have unblocked and finished after safeResume()");
        assertFalse(controller.isPaused(), "paused should be false after safeResume()");
    }

    @Test
    void pauseHereUntilResumed_blocksWhilePaused_and_safeResume_unblocks() throws Exception {
        // Pause and ensure not terminated (default false)
        controller.requestPause();

        CountDownLatch started = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            started.countDown();
            try {
                controller.pauseHereUntilResumed();
            } catch (Exception e) {
                fail("Exception in pause thread: " + e.getMessage());
            }
        });
        t.start();

        // Wait for the thread to start and attempt to pause
        assertTrue(started.await(500, TimeUnit.MILLISECONDS), "worker thread did not start");

        // Give it a moment to enter wait state
        Thread.sleep(100);
        assertTrue(t.isAlive(), "Thread should be waiting while paused");

        // Now resume and verify it unblocks and clears paused
        controller.safeResume();

        // It should finish quickly
        t.join(2000);
        assertFalse(t.isAlive(), "Thread should have unblocked and finished after safeResume()");
        assertFalse(controller.isPaused(), "paused should be false after safeResume()");
    }

    @Test
    void requestTerminate_setsTerminatedTrue_and_unblocksWaitingPause() throws Exception {
        // Set paused so the call blocks
        controller.requestPause();

        Thread t = new Thread(() -> {
            try {
                controller.pauseHereUntilResumed();
            } catch (Exception e) {
                fail("Exception in pause thread: " + e.getMessage());
            }
        });
        t.start();

        // Give it time to start and block
        Thread.sleep(100);
        assertTrue(t.isAlive(), "Thread should be waiting before terminate");

        controller.requestTerminate();

        // It should finish quickly after terminate
        t.join(2000);
        assertFalse(t.isAlive(), "Thread should have unblocked and finished after requestTerminate()");
        assertTrue(controller.isControllerTerminated(), "terminated should be true after requestTerminate()");
        // paused may be cleared by safeResume called within requestTerminate
        assertFalse(controller.isPaused(), "paused should be false after requestTerminate() triggers safeResume()");
    }
}
