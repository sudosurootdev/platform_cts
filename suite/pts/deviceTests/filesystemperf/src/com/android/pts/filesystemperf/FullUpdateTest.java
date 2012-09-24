/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.pts.filesystemperf;

import android.cts.util.TimeoutReq;
import com.android.pts.util.MeasureRun;
import com.android.pts.util.MeasureTime;
import com.android.pts.util.PtsAndroidTestCase;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;
import com.android.pts.util.SystemUtil;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FullUpdateTest extends PtsAndroidTestCase {
    private static final String DIR_INITIAL_FILL = "INITIAL_FILL";
    private static final String DIR_WORK = "WORK";
    private static final String TAG = "FullUpdateTest";

    @Override
    protected void tearDown() throws Exception {
        FileUtil.removeFileOrDir(getContext(), DIR_INITIAL_FILL);
        FileUtil.removeFileOrDir(getContext(), DIR_WORK);
        super.tearDown();
    }

    // fill disk almost, update exceeding free space, then update some amount
    // idea is to drain all free blocks and measure update performance
    @TimeoutReq(minutes = 30)
    public void testAlmostFilledUpdate() throws IOException {
        long freeDisk = SystemUtil.getFreeDiskSize(getContext());
        final long FREE_SPACE_TO_LEAVE = 400L * 1024L * 1024L; // leave this much
        long diskToFill = freeDisk - FREE_SPACE_TO_LEAVE;
        Log.i(TAG, "free disk " + freeDisk + ", to fill " + diskToFill);
        final long MAX_FILE_SIZE_TO_FILL = 1024L * 1024L * 1024L;
        long filled = 0;
        while (filled < diskToFill) {
            long toFill = diskToFill - filled;
            if (toFill > MAX_FILE_SIZE_TO_FILL) {
                toFill = MAX_FILE_SIZE_TO_FILL;
            }
            Log.i(TAG, "Generating file " + toFill);
            FileUtil.createNewFilledFile(getContext(),
                    DIR_INITIAL_FILL, toFill);
            filled += toFill;
        }

        // now about freeSpaceToLeave should be left
        // and try updating exceeding the free space size
        final long FILE_SIZE = 300L * 1024L * 1024L;
        File file = FileUtil.createNewFilledFile(getContext(),
                DIR_WORK, FILE_SIZE);
        final int BUFFER_SIZE = 10 * 1024 * 1024;
        final byte[] data = FileUtil.generateRandomData(BUFFER_SIZE);
        final int NUMBER_REPEATITION = 10;
        double[] worsts = new double[NUMBER_REPEATITION];
        double[] averages = new double[NUMBER_REPEATITION];
        for (int i = 0; i < NUMBER_REPEATITION; i++) {
            final FileOutputStream out = new FileOutputStream(file);
            int numberRepeat = (int)(FILE_SIZE / BUFFER_SIZE);
            double[] times = MeasureTime.measure(numberRepeat, new MeasureRun() {

                @Override
                public void run(int i) throws IOException {
                    out.write(data);
                    out.flush();
                }
            });
            out.close();
            double[] mbps = ReportLog.calcRatePerSecArray(BUFFER_SIZE / 1024 / 1024, times);
            getReportLog().printArray(i + "-th round MB/s",
                    mbps, true);
            Stat.StatResult stat = Stat.getStat(mbps);
            worsts[i] = stat.mMin;
            averages[i] = stat.mAverage;
        }
        Stat.StatResult statWorsts = Stat.getStat(worsts);
        Stat.StatResult statAverages = Stat.getStat(averages);
        getReportLog().printSummary("MB/s", statWorsts.mMin, statAverages.mAverage);
    }
}