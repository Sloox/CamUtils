package wrightstuff.co.za.cameramanager.renderscript;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.Type;

import wrightstuff.co.za.cameramanager.renderscripttesting.ScriptC_histEq;
import wrightstuff.co.za.cameramanager.renderscripttesting.ScriptC_saturation;
import wrightstuff.co.za.cameramanager.utils.WorkLogger;

public class RenderscriptHelper {
    public static final String TAG = RenderscriptHelper.class.getSimpleName();
    public final boolean DISABLEDLOGGING = false;
    RenderScript rs;

    public RenderscriptHelper(Context context) {
        rs = RenderScript.create(context);
    }
    public Bitmap histogramEqualization(Bitmap image) {
        //Get image size
        int width = image.getWidth();
        int height = image.getHeight();

        //Create new bitmap
        Bitmap res = image.copy(image.getConfig(), true);

        //Create allocation from Bitmap
        Allocation allocationA = Allocation.createFromBitmap(rs, res);


        //Create allocation with same type
        Allocation allocationB = Allocation.createTyped(rs, allocationA.getType());

        //Create script from rs file.
        ScriptC_histEq histEqScript = new ScriptC_histEq(rs);

        //Set size in script
        histEqScript.set_size(width*height);

        //Call the first kernel.
        histEqScript.forEach_root(allocationA, allocationB);

        //Call the rs method to compute the remap array
        histEqScript.invoke_createRemapArray();

        //Call the second kernel
        histEqScript.forEach_remaptoRGB(allocationB, allocationA);

        //Copy script result into bitmap
        allocationA.copyTo(res);

        //Destroy everything to free memory
        allocationA.destroy();
        allocationB.destroy();
        histEqScript.destroy();
        rs.destroy();

        return res;
    }

    public Bitmap blurBitmap(Bitmap bitmap, float radius) {
        WorkLogger logger = new WorkLogger(TAG, "Blur-Renderscript", DISABLEDLOGGING);
        logger.addSplit("StartBlur");

        //Create allocation from Bitmap
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        Type t = allocation.getType();
        //Create allocation with the same type
        Allocation blurredAllocation = Allocation.createTyped(rs, t);
        //Create script
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        //Set blur radius (maximum 25.0)
        blurScript.setRadius(radius);
        //Set input for script
        blurScript.setInput(allocation);
        //Call script for output allocation
        blurScript.forEach(blurredAllocation);
        //Copy script result into bitmap
        blurredAllocation.copyTo(bitmap);
        //Destroy everything to free memory
        try {
            allocation.destroy();
            blurredAllocation.destroy();
            blurScript.destroy();
            t.destroy();
            rs.destroy();
        } catch (Exception ignored) {

        }
        logger.dumpToLog();

        return bitmap;
    }

    public Bitmap saturation(Bitmap bitmap, float amount) {
        //Create allocation from Bitmap
        Allocation allocationin = Allocation.createFromBitmap(rs, bitmap);
        Type t = allocationin.getType();
        //Create allocation with the same type
        Allocation saturatedAllocationOut = Allocation.createTyped(rs, t);
        ScriptC_saturation saturation = new ScriptC_saturation(rs);
        saturation.set_saturationValue(amount);
        saturation.forEach_saturation(allocationin, saturatedAllocationOut);
        saturatedAllocationOut.copyTo(bitmap);

        try {
            allocationin.destroy();
            t.destroy();
            rs.destroy();
        } catch (Exception ignored) {

        }
        return bitmap;
    }

    public Bitmap mono(Bitmap bitmap) {
        //Create allocation from Bitmap
        Allocation allocationin = Allocation.createFromBitmap(rs, bitmap);
        Type t = allocationin.getType();
        //Create allocation with the same type
        Allocation saturatedAllocationOut = Allocation.createTyped(rs, t);
        ScriptC_saturation saturation = new ScriptC_saturation(rs);
        saturation.forEach_mono(allocationin, saturatedAllocationOut);
        saturatedAllocationOut.copyTo(bitmap);

        try {
            allocationin.destroy();
            t.destroy();
            rs.destroy();
        } catch (Exception ignored) {

        }
        return bitmap;
    }
}
