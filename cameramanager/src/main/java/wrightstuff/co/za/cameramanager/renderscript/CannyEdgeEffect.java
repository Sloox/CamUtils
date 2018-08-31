package wrightstuff.co.za.cameramanager.renderscript;

import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import wrightstuff.co.za.cameramanager.renderscripttesting.ScriptC_canny;
//Steps for canny
//1) Grayscale
//2) Gaussian
//3) Sobel
//4) Non Max suppresion
//5) Hysteresis

public class CannyEdgeEffect {
    private final int RADIUS = 4;
    RenderScript rs;
    ScriptC_canny canny;
    ScriptIntrinsicBlur blur;
    int width, height;
    private Allocation outputAllocation;
    private Allocation inputAllocation;


    public CannyEdgeEffect(RenderScript renderScript, int imgWidth, int imgHeight) {
        rs = renderScript;
        width = imgWidth;
        height = imgHeight;

        /*Canny Script*/
        canny = new ScriptC_canny(rs);
        canny.set_gHeight(height);
        canny.set_gWidth(width);
        /*Blur script*/
        blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        blur.setRadius(RADIUS);
    }


    public Bitmap doCanny(Bitmap screenBitmap) {
        //setup allocation

        inputAllocation = Allocation.createFromBitmap(rs, screenBitmap);
        outputAllocation = createOrReuseOutputAllocation(inputAllocation);
        canny.forEach_mono(inputAllocation, outputAllocation); //step 1
        handleBlur(outputAllocation);
        outputAllocation = handleSobel(outputAllocation);

        outputAllocation.copyTo(screenBitmap);
        return screenBitmap;
    }


    private Allocation handleSobel(Allocation inputAllocation) {
        Allocation outputAlloc = outputAllocation = Allocation.createTyped(rs, inputAllocation.getType());
        canny.set_gHeight(height);
        canny.set_gWidth(width);
        canny.set_gIn(inputAllocation);
        canny.forEach_sobel(outputAlloc);
        return outputAlloc;
    }

    private void handleBlur(Allocation allocation) {
        //Set input for script
        blur.setInput(allocation);
        //Call script for output allocation
        blur.forEach(allocation);
    }


    private Allocation createOrReuseOutputAllocation(Allocation t) {
        if (outputAllocation == null) {
            outputAllocation = Allocation.createTyped(rs, t.getType());
        }
        return outputAllocation;
    }

    public void reset(RenderScript renderScript, int imgWidth, int imgHeight) {
        rs = renderScript;
        width = imgWidth;
        height = imgHeight;
        /*Canny Script*/
        canny = new ScriptC_canny(rs);
        canny.set_gHeight(height);
        canny.set_gWidth(width);
        /*Blur script*/
        blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        blur.setRadius(RADIUS);
        outputAllocation = null;
        inputAllocation = null;

    }
}
