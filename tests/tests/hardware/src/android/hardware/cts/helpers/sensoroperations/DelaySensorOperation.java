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

import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorStats;

import java.util.concurrent.TimeUnit;

/**
 * An {@link ISensorOperation} which delays for a specified period of time before performing another
 * {@link ISensorOperation}.
 */
public class DelaySensorOperation implements ISensorOperation {
    private final ISensorOperation mOperation;
    private final long mDelay;
    private final TimeUnit mTimeUnit;

    /**
     * Constructor for {@link DelaySensorOperation}
     *
     * @param operation the child {@link ISensorOperation} to perform after the delay
     * @param delay the amount of time to delay
     * @param timeUnit the unit of the delay
     */
    public DelaySensorOperation(ISensorOperation operation, long delay, TimeUnit timeUnit) {
        if (operation == null || timeUnit == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        mOperation = operation;
        mDelay = delay;
        mTimeUnit = timeUnit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        sleep(mDelay, mTimeUnit);
        mOperation.execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SensorStats getStats() {
        return mOperation.getStats();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DelaySensorOperation clone() {
        return new DelaySensorOperation(mOperation.clone(), mDelay, mTimeUnit);
    }

    /**
     * Helper method to sleep for a given number of ns. Exposed for unit testing.
     */
    void sleep(long delay, TimeUnit timeUnit) {
        SensorCtsHelper.sleep(delay, timeUnit);
    }
}
