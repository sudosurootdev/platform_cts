/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware.cts.helpers.sensoroperations;

import android.hardware.cts.helpers.SensorStats;

import junit.framework.TestCase;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the primitive {@link ISensorOperation}s including {@link DelaySensorOperation},
 * {@link ParallelSensorOperation}, {@link RepeatingSensorOperation} and
 * {@link SequentialSensorOperation}.
 */
public class SensorOperationTest extends TestCase {
    private static final int THRESHOLD_MS = 50;

    /**
     * Test that the {@link FakeSensorOperation} functions correctly. Other tests in this class
     * rely on this operation.
     */
    public void testFakeSensorOperation() {
        final int opDurationMs = 100;

        ISensorOperation op = new FakeSensorOperation(opDurationMs, TimeUnit.MILLISECONDS);

        assertFalse(op.getStats().flatten().containsKey("executed"));
        long start = System.currentTimeMillis();
        op.execute();
        long duration = System.currentTimeMillis() - start;
        assertTrue(Math.abs(opDurationMs - duration) < THRESHOLD_MS);
        assertTrue(op.getStats().flatten().containsKey("executed"));

        op = new FakeSensorOperation(true, 0, TimeUnit.MILLISECONDS);
        try {
            op.execute();
            fail("AssertionError expected");
        } catch (AssertionError e) {
            // Expected
        }
        assertTrue(op.getStats().flatten().keySet().contains(SensorStats.ERROR));
    }

    /**
     * Test that the {@link DelaySensorOperation} functions correctly.
     */
    public void testDelaySensorOperation() {
        final int opDurationMs = 500;
        final int subOpDurationMs = 100;

        FakeSensorOperation subOp = new FakeSensorOperation(subOpDurationMs, TimeUnit.MILLISECONDS);
        ISensorOperation op = new DelaySensorOperation(subOp, opDurationMs, TimeUnit.MILLISECONDS);

        long start = System.currentTimeMillis();
        op.execute();
        long duration = System.currentTimeMillis() - start;
        assertTrue(Math.abs(opDurationMs + subOpDurationMs - duration) < THRESHOLD_MS);
    }

    /**
     * Test that the {@link ParallelSensorOperation} functions correctly.
     */
    public void testParallelSensorOperation() {
        final int subOpCount = 100;
        final int subOpDurationMs = 500;

        ParallelSensorOperation op = new ParallelSensorOperation();
        for (int i = 0; i < subOpCount; i++) {
            ISensorOperation subOp = new FakeSensorOperation(subOpDurationMs,
                    TimeUnit.MILLISECONDS);
            op.add(subOp);
        }

        Set<String> statsKeys = op.getStats().flatten().keySet();
        assertEquals(0, statsKeys.size());

        long start = System.currentTimeMillis();
        op.execute();
        long duration = System.currentTimeMillis() - start;
        assertTrue(Math.abs(subOpDurationMs - duration) < THRESHOLD_MS);

        statsKeys = op.getStats().flatten().keySet();
        assertEquals(subOpCount, statsKeys.size());
        for (int i = 0; i < subOpCount; i++) {
            assertTrue(statsKeys.contains(String.format("%s_%03d%sexecuted",
                    ParallelSensorOperation.STATS_TAG, i, SensorStats.DELIMITER)));
        }
    }

    /**
     * Test that the {@link ParallelSensorOperation} functions correctly if there is a failure in
     * a child operation.
     */
    public void testParallelSensorOperation_fail() {
        final int subOpCount = 100;

        ParallelSensorOperation op = new ParallelSensorOperation();
        for (int i = 0; i < subOpCount; i++) {
            // Trigger failures in the 5th, 55th operations at t=5ms, t=55ms
            ISensorOperation subOp = new FakeSensorOperation(i % 50 == 5, i, TimeUnit.MILLISECONDS);
            op.add(subOp);
        }

        Set<String> statsKeys = op.getStats().flatten().keySet();
        assertEquals(0, statsKeys.size());

        try {
            op.execute();
            fail("AssertionError expected");
        } catch (AssertionError e) {
            // Expected
            System.out.println(e.getMessage());
            // TODO: Verify that the exception rethrown was at t=5ms.
        }

        statsKeys = op.getStats().flatten().keySet();
        assertEquals(subOpCount + 3, statsKeys.size());
        for (int i = 0; i < subOpCount; i++) {
            assertTrue(statsKeys.contains(String.format("%s_%03d%sexecuted",
                    ParallelSensorOperation.STATS_TAG, i, SensorStats.DELIMITER)));
            if (i % 50 == 5) {
                assertTrue(statsKeys.contains(String.format("%s_%03d%s%s",
                        ParallelSensorOperation.STATS_TAG, i, SensorStats.DELIMITER,
                        SensorStats.ERROR)));
            }

        }
        assertTrue(statsKeys.contains(SensorStats.ERROR));
    }

    /**
     * Test that the {@link ParallelSensorOperation} functions correctly if a child exceeds the
     * timeout.
     */
    public void testParallelSensorOperation_timeout() {
        final int subOpCount = 100;

        ParallelSensorOperation op = new ParallelSensorOperation(100, TimeUnit.MILLISECONDS);
        for (int i = 0; i < subOpCount; i++) {
            // Trigger timeouts in the 5th, 55th operations (5 seconds vs 0 seconds)
            ISensorOperation subOp = new FakeSensorOperation(i % 50 == 5 ? 5 : 0, TimeUnit.SECONDS);
            op.add(subOp);
        }

        Set<String> statsKeys = op.getStats().flatten().keySet();
        assertEquals(0, statsKeys.size());

        try {
            op.execute();
            fail("AssertionError expected");
        } catch (AssertionError e) {
            // Expected
            System.out.println(e.getMessage());
            // TODO: Verify that the exception rethrown was at t=5ms.
        }

        statsKeys = op.getStats().flatten().keySet();
        assertEquals(subOpCount - 2, statsKeys.size());
        for (int i = 0; i < subOpCount; i++) {
            if (i % 50 != 5) {
                assertTrue(statsKeys.contains(String.format("%s_%03d%sexecuted",
                        ParallelSensorOperation.STATS_TAG, i, SensorStats.DELIMITER)));
            }
        }
    }

    /**
     * Test that the {@link RepeatingSensorOperation} functions correctly.
     */
    public void testRepeatingSensorOperation() {
        final int iterations = 10;
        final int subOpDurationMs = 100;

        ISensorOperation subOp = new FakeSensorOperation(subOpDurationMs, TimeUnit.MILLISECONDS);
        ISensorOperation op = new RepeatingSensorOperation(subOp, iterations);

        Set<String> statsKeys = op.getStats().flatten().keySet();
        assertEquals(0, statsKeys.size());

        long start = System.currentTimeMillis();
        op.execute();
        long duration = System.currentTimeMillis() - start;
        assertTrue(Math.abs(subOpDurationMs * iterations - duration) < THRESHOLD_MS);

        statsKeys = op.getStats().flatten().keySet();
        assertEquals(iterations, statsKeys.size());
        for (int i = 0; i < iterations; i++) {
            assertTrue(statsKeys.contains(String.format("%s_%03d%sexecuted",
                    RepeatingSensorOperation.STATS_TAG, i, SensorStats.DELIMITER)));
        }
    }

    /**
     * Test that the {@link RepeatingSensorOperation} functions correctly if there is a failure in
     * a child operation.
     */
    public void testRepeatingSensorOperation_fail() {
        final int iterations = 100;
        final int failCount = 75;

        ISensorOperation subOp = new FakeSensorOperation(0, TimeUnit.MILLISECONDS) {
            private int mExecutedCount = 0;
            private SensorStats mFakeStats = new SensorStats();

            @Override
            public void execute() {
                super.execute();
                mExecutedCount++;

                if (failCount == mExecutedCount) {
                    doFail();
                }
            }

            @Override
            public FakeSensorOperation clone() {
                // Don't clone
                mFakeStats = new SensorStats();
                return this;
            }

            @Override
            public SensorStats getStats() {
                return mFakeStats;
            }
        };
        ISensorOperation op = new RepeatingSensorOperation(subOp, iterations);

        Set<String> statsKeys = op.getStats().flatten().keySet();
        assertEquals(0, statsKeys.size());

        try {
            op.execute();
            fail("AssertionError expected");
        } catch (AssertionError e) {
            // Expected
            System.out.println(e.getMessage());
        }

        statsKeys = op.getStats().flatten().keySet();
        assertEquals(failCount + 2, statsKeys.size());
        for (int i = 0; i < failCount; i++) {
            assertTrue(statsKeys.contains(String.format("%s_%03d%sexecuted",
                    RepeatingSensorOperation.STATS_TAG, i, SensorStats.DELIMITER)));
        }
        assertTrue(statsKeys.contains(String.format("%s_%03d%s%s",
                RepeatingSensorOperation.STATS_TAG, failCount - 1, SensorStats.DELIMITER,
                SensorStats.ERROR)));
        assertTrue(statsKeys.contains(SensorStats.ERROR));
    }

    /**
     * Test that the {@link SequentialSensorOperation} functions correctly.
     */
    public void testSequentialSensorOperation() {
        final int subOpCount = 10;
        final int subOpDurationMs = 100;

        SequentialSensorOperation op = new SequentialSensorOperation();
        for (int i = 0; i < subOpCount; i++) {
            ISensorOperation subOp = new FakeSensorOperation(subOpDurationMs,
                    TimeUnit.MILLISECONDS);
            op.add(subOp);
        }

        Set<String> statsKeys = op.getStats().flatten().keySet();
        assertEquals(0, statsKeys.size());

        long start = System.currentTimeMillis();
        op.execute();
        long duration = System.currentTimeMillis() - start;
        assertTrue(Math.abs(subOpDurationMs * subOpCount - duration) < THRESHOLD_MS);

        statsKeys = op.getStats().flatten().keySet();
        assertEquals(subOpCount, statsKeys.size());
        for (int i = 0; i < subOpCount; i++) {
            assertTrue(statsKeys.contains(String.format("%s_%03d%sexecuted",
                    SequentialSensorOperation.STATS_TAG, i, SensorStats.DELIMITER)));
        }
    }

    /**
     * Test that the {@link SequentialSensorOperation} functions correctly if there is a failure in
     * a child operation.
     */
    public void testSequentialSensorOperation_fail() {
        final int subOpCount = 100;
        final int failCount = 75;

        SequentialSensorOperation op = new SequentialSensorOperation();
        for (int i = 0; i < subOpCount; i++) {
            // Trigger a failure in the 75th operation only
            ISensorOperation subOp = new FakeSensorOperation(i + 1 == failCount, 0,
                    TimeUnit.MILLISECONDS);
            op.add(subOp);
        }

        Set<String> statsKeys = op.getStats().flatten().keySet();
        assertEquals(0, statsKeys.size());

        try {
            op.execute();
            fail("AssertionError expected");
        } catch (AssertionError e) {
            // Expected
            System.out.println(e.getMessage());
        }

        statsKeys = op.getStats().flatten().keySet();
        assertEquals(failCount + 2, statsKeys.size());
        for (int i = 0; i < failCount; i++) {
            assertTrue(statsKeys.contains(String.format("%s_%03d%sexecuted",
                    SequentialSensorOperation.STATS_TAG, i, SensorStats.DELIMITER)));
        }
        assertTrue(statsKeys.contains(String.format("%s_%03d%s%s",
                SequentialSensorOperation.STATS_TAG, failCount - 1, SensorStats.DELIMITER,
                SensorStats.ERROR)));
        assertTrue(statsKeys.contains(SensorStats.ERROR));
    }
}
