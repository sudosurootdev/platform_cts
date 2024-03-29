/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.renderscript.cts;

import android.renderscript.*;
import android.util.Log;

public class IntrinsicBlur extends IntrinsicBase {
    private ScriptIntrinsicBlur mIntrinsic;
    private int MAX_RADIUS = 25;
    private ScriptC_intrinsic_blur mScript;
    private float mRadius = MAX_RADIUS;
    private float mSaturation = 1.0f;
    private Allocation mScratchPixelsAllocation1;
    private Allocation mScratchPixelsAllocation2;



    private void initTest(int w, int h, Element e) {
        makeBuffers(w, h, e);

        Type.Builder tb = new Type.Builder(mRS, Element.F32_4(mRS));
        tb.setX(w);
        tb.setY(h);
        mScratchPixelsAllocation1 = Allocation.createTyped(mRS, tb.create());
        mScratchPixelsAllocation2 = Allocation.createTyped(mRS, tb.create());

        mIntrinsic = ScriptIntrinsicBlur.create(mRS, e);
        mIntrinsic.setRadius(MAX_RADIUS);
        mIntrinsic.setInput(mAllocSrc);

        mScript = new ScriptC_intrinsic_blur(mRS);
        mScript.set_width(w);
        mScript.set_height(h);
        mScript.invoke_setRadius(MAX_RADIUS);

        mScript.set_ScratchPixel1(mScratchPixelsAllocation1);
        mScript.set_ScratchPixel2(mScratchPixelsAllocation2);

        // Make reference
        copyInput();
        mScript.forEach_horz(mScratchPixelsAllocation2);
        mScript.forEach_vert(mScratchPixelsAllocation1);
        copyOutput();
    }

    private void copyInput() {
        if (mAllocSrc.getType().getElement().isCompatible(Element.U8(mRS))) {
            mScript.forEach_convert1_uToF(mAllocSrc, mScratchPixelsAllocation1);
            return;
        }
        if (mAllocSrc.getType().getElement().isCompatible(Element.U8_4(mRS))) {
            mScript.forEach_convert4_uToF(mAllocSrc, mScratchPixelsAllocation1);
            return;
        }
        throw new IllegalArgumentException("bad type");
    }

    private void copyOutput() {
        if (mAllocSrc.getType().getElement().isCompatible(Element.U8(mRS))) {
            mScript.forEach_convert1_fToU(mScratchPixelsAllocation1, mAllocRef);
            return;
        }
        if (mAllocSrc.getType().getElement().isCompatible(Element.U8_4(mRS))) {
            mScript.forEach_convert4_fToU(mScratchPixelsAllocation1, mAllocRef);
            return;
        }
        throw new IllegalArgumentException("bad type");
    }

    public void testU8_1() {
        final int w = 97;
        final int h = 97;
        Element e = Element.U8(mRS);
        initTest(w, h, e);

        mIntrinsic.forEach(mAllocDst);

        mVerify.set_gAllowedIntError(1);
        mVerify.invoke_verify(mAllocRef, mAllocDst, mAllocSrc);
        mRS.finish();
        checkError();
    }

    public void testU8_4() {
        final int w = 97;
        final int h = 97;
        Element e = Element.U8_4(mRS);
        initTest(w, h, e);

        mIntrinsic.forEach(mAllocDst);

        mVerify.set_gAllowedIntError(1);
        mVerify.invoke_verify(mAllocRef, mAllocDst, mAllocSrc);
        mRS.finish();
        checkError();
    }

}
