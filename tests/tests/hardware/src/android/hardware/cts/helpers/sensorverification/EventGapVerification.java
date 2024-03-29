package android.hardware.cts.helpers.sensorverification;

import android.hardware.Sensor;
import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.SensorTestInformation;
import android.hardware.cts.helpers.SensorTestInformation.SensorReportingMode;
import android.hardware.cts.helpers.TestSensorEvent;

import junit.framework.Assert;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ISensorVerification} which verifies that there are no missing events. This is done by
 * checking the last received sensor timestamp and checking that it is within 1.8 * the expected
 * period.
 */
public class EventGapVerification extends AbstractSensorVerification {
    public static final String PASSED_KEY = "missing_event_passed";

    // Fail if no events are delivered within 1.8 times the expected interval
    private static final double THRESHOLD = 1.8;

    // Number of indices to print in assert message before truncating
    private static final int TRUNCATE_MESSAGE_LENGTH = 3;

    private final int mExpectedDelayUs;

    private final List<IndexedEventPair> mEventGaps = new LinkedList<IndexedEventPair>();
    private TestSensorEvent mPreviousEvent = null;
    private int mIndex = 0;

    /**
     * Construct a {@link EventGapVerification}
     *
     * @param expectedDelayUs the expected period in us.
     */
    public EventGapVerification(int expectedDelayUs) {
        mExpectedDelayUs = expectedDelayUs;
    }

    /**
     * Get the default {@link EventGapVerification}.
     *
     * @param sensor the {@link Sensor}
     * @param rateUs the requested rate in us
     * @return the verification or null if the verification is not a continuous mode sensor.
     */
    public static EventGapVerification getDefault(Sensor sensor, int rateUs) {
        if (!SensorReportingMode.CONTINUOUS.equals(SensorTestInformation.getReportingMode(
                sensor.getType()))) {
            return null;
        }
        return new EventGapVerification(SensorCtsHelper.getDelay(sensor, rateUs));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(SensorStats stats) {
        final int count = mEventGaps.size();
        stats.addValue(PASSED_KEY, count == 0);
        stats.addValue(SensorStats.EVENT_GAP_COUNT_KEY, count);

        final int[] indices = new int[count];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = mEventGaps.get(i).index;
        }
        stats.addValue(SensorStats.EVENT_GAP_POSITIONS_KEY, indices);

        if (count > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(count).append(" events gaps: ");
            for (int i = 0; i < Math.min(count, TRUNCATE_MESSAGE_LENGTH); i++) {
                IndexedEventPair info = mEventGaps.get(i);
                sb.append(String.format("position=%d, delta_time=%dns; ", info.index,
                        info.event.timestamp - info.previousEvent.timestamp));
            }
            if (count > TRUNCATE_MESSAGE_LENGTH) {
                sb.append(count - TRUNCATE_MESSAGE_LENGTH).append(" more; ");
            }
            sb.append(String.format("(expected <%dns)",
                    TimeUnit.NANOSECONDS.convert((int) (THRESHOLD * mExpectedDelayUs),
                            TimeUnit.MICROSECONDS)));
            Assert.fail(sb.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventGapVerification clone() {
        return new EventGapVerification(mExpectedDelayUs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addSensorEventInternal(TestSensorEvent event) {
        if (mPreviousEvent != null) {
            long deltaNs = event.timestamp - mPreviousEvent.timestamp;
            long deltaUs = TimeUnit.MICROSECONDS.convert(deltaNs, TimeUnit.NANOSECONDS);
            if (deltaUs > mExpectedDelayUs * THRESHOLD) {
                mEventGaps.add(new IndexedEventPair(mIndex, event, mPreviousEvent));
            }
        }

        mPreviousEvent = event;
        mIndex++;
    }
}
