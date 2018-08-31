package wrightstuff.co.za.cameramanager.renderscript;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.renderscript.Type;

import java.nio.ByteBuffer;

import wrightstuff.co.za.cameramanager.renderscripttesting.ScriptC_generalrs;
import wrightstuff.co.za.cameramanager.renderscripttesting.ScriptC_histEq;
import wrightstuff.co.za.cameramanager.renderscripttesting.ScriptC_yuvtorgb;

public class RenderscriptHelper {
    public static final String TAG = RenderscriptHelper.class.getSimpleName();
    public final boolean DISABLEDLOGGING = false;
    RenderScript rs;
    CannyEdgeEffect cannyEdgeEffect;

    /*Globals to speed up the Renderscript*/
    ScriptC_yuvtorgb mYuv420;
    Type.Builder typeUcharY, typeUcharUV;
    private Bitmap outBitmap;
    private Allocation yAlloc;
    private Allocation vAlloc;
    private Allocation outAlloc;

    public RenderscriptHelper(Context context) {
        rs = RenderScript.create(context);
        mYuv420 = new ScriptC_yuvtorgb(rs);
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
        histEqScript.set_size(width * height);

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
        return bitmap;
    }
    public Bitmap convolveBitmap(Bitmap bitmap) {
        //Create allocation from Bitmap //
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        Type t = allocation.getType();
        //Create allocation with the same type
        Allocation convolved = Allocation.createTyped(rs, t);
        //Create script
        ScriptIntrinsicConvolve3x3 convolve3x3 = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        convolve3x3.setCoefficients(new float[]{1, 0, -1, 2, 0, -2, 1, 0, -1});
        convolve3x3.setInput(allocation);
        convolve3x3.forEach(convolved);
        convolved.copyTo(bitmap);
        //Destroy everything to free memory
        try {
            allocation.destroy();
            convolved.destroy();
            t.destroy();
            rs.destroy();
        } catch (Exception ignored) {

        }
        return bitmap;
    }


    public Bitmap saturation(Bitmap bitmap, float amount) {
        //Create allocation from Bitmap
        Allocation allocationin = Allocation.createFromBitmap(rs, bitmap);
        Type t = allocationin.getType();
        //Create allocation with the same type
        Allocation saturatedAllocationOut = Allocation.createTyped(rs, t);
        ScriptC_generalrs saturation = new ScriptC_generalrs(rs);
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
        Allocation monoAllocationOut = Allocation.createTyped(rs, t);
        ScriptC_generalrs saturation = new ScriptC_generalrs(rs);
        saturation.forEach_mono(allocationin, monoAllocationOut);
        monoAllocationOut.copyTo(bitmap);

        try {
            allocationin.destroy();
            t.destroy();
            rs.destroy();
        } catch (Exception ignored) {

        }
        return bitmap;
    }

    public Bitmap brightness(Bitmap bitmap, float brightness) {
        //Create allocation from Bitmap
        Allocation allocationin = Allocation.createFromBitmap(rs, bitmap);
        Type t = allocationin.getType();
        //Create allocation with the same type

        Allocation brightnessOut = Allocation.createTyped(rs, t);
        ScriptC_generalrs saturation = new ScriptC_generalrs(rs);
        saturation.set_saturationValue(brightness);
        saturation.forEach_brightness(allocationin, brightnessOut);
        brightnessOut.copyTo(bitmap);

        try {
            allocationin.destroy();
            t.destroy();
            rs.destroy();
        } catch (Exception ignored) {

        }
        return bitmap;

    }

    public Bitmap cannyEdgeDetect(Bitmap bitmap) {
        if (cannyEdgeEffect == null) {
            cannyEdgeEffect = new CannyEdgeEffect(rs, bitmap.getWidth(), bitmap.getHeight());
        }
        return cannyEdgeEffect.doCanny(bitmap);
    }


    public Bitmap sobel(Bitmap bitmap) {
        //Create allocation from Bitmap
        Allocation allocationin = Allocation.createFromBitmap(rs, bitmap);
        Type t = allocationin.getType();
        //Create allocation with the same type

        Allocation brightnessOut = Allocation.createTyped(rs, t);
        ScriptC_generalrs saturation = new ScriptC_generalrs(rs);
        saturation.set_gCoeffsx(new float[]{1, 0, -1, 2, 0, -2, 1, 0, -1});
        saturation.set_gCoeffsy(new float[]{1, 2, 1, 0, 0, 0, -1, -2, -1});
        saturation.set_gHeight(bitmap.getHeight());
        saturation.set_gWidth(bitmap.getWidth());
        saturation.set_gIn(allocationin);
        saturation.forEach_sobel(brightnessOut);
        brightnessOut.copyTo(bitmap);
        try {
            allocationin.destroy();
            t.destroy();
            rs.destroy();
        } catch (Exception ignored) {

        }
        return bitmap;

    }

    private Allocation getOrReuseAllocationY(Type.Builder typeUcharY) {
        if (yAlloc == null) {
            yAlloc = Allocation.createTyped(rs, typeUcharY.create());
        }
        return yAlloc;
    }

    private Allocation getOrReuseAllocationv(Type.Builder typeUcharv) {
        if (vAlloc == null) {
            vAlloc = Allocation.createTyped(rs, typeUcharv.create());
        }
        return vAlloc;
    }

    private Allocation getOrReuseAllocationBitmap(Bitmap bitmap) {
        if (outAlloc == null) {
            outAlloc = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        }
        return outAlloc;
    }

    public Bitmap YUV_420_888_toRGB_speed(Image image, int width, int height) {
        // Get the three image planes
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] y = new byte[buffer.remaining()];
        buffer.get(y);

        buffer = planes[1].getBuffer();
        byte[] u = new byte[buffer.remaining()];
        buffer.get(u);

        buffer = planes[2].getBuffer();
        byte[] v = new byte[buffer.remaining()];
        buffer.get(v);

        // get the relevant RowStrides and PixelStrides
        // (we know from documentation that PixelStride is 1 for y)
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();  // we know from   documentation that RowStride is the same for u and v.
        int uvPixelStride = planes[1].getPixelStride();  // we know from   documentation that PixelStride is the same for u and v.


        // Y,U,V are defined as global allocations, the out-Allocation is the Bitmap.
        // Note also that uAlloc and vAlloc are 1-dimensional while yAlloc is 2-dimensional.
        Type.Builder typeUcharY = new Type.Builder(rs, Element.U8(rs));
        typeUcharY.setX(yRowStride).setY(height);

        Allocation yAlloc = Allocation.createTyped(rs, typeUcharY.create());
        //Allocation yAlloc = getOrReuseAllocationY(typeUcharY);

        yAlloc.copyFrom(y);
        mYuv420.set_ypsIn(yAlloc);

        Type.Builder typeUcharUV = new Type.Builder(rs, Element.U8(rs));
        // note that the size of the u's and v's are as follows:
        //      (  (width/2)*PixelStride + padding  ) * (height/2)
        // =    (RowStride                          ) * (height/2)
        // but I noted that on the S7 it is 1 less...
        typeUcharUV.setX(u.length);
        Allocation uAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        ///Allocation uAlloc = getOrReuseAllocationv(typeUcharUV);
        uAlloc.copyFrom(u);
        mYuv420.set_uIn(uAlloc);

        Allocation vAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        //Allocation vAlloc = getOrReuseAllocationv(typeUcharUV);
        vAlloc.copyFrom(v);
        mYuv420.set_vIn(vAlloc);

        // handover parameters
        mYuv420.set_picWidth(width);
        mYuv420.set_uvRowStride(uvRowStride);
        mYuv420.set_uvPixelStride(uvPixelStride);

        if (outBitmap == null) {
            outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        Allocation outAlloc = Allocation.createFromBitmap(rs, outBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        //  Allocation outAlloc = getOrReuseAllocationBitmap(outBitmap);

        Script.LaunchOptions lo = new Script.LaunchOptions();
        lo.setX(0, width);  // by this we ignore the y’s padding zone, i.e. the right side of x between width and yRowStride
        lo.setY(0, height);

        mYuv420.forEach_doConvert(outAlloc, lo);
        outAlloc.copyTo(outBitmap);
        try {
            outAlloc.destroy();
            uAlloc.destroy();
            vAlloc.destroy();
            yAlloc.destroy();
            rs.destroy();
        } catch (Exception e) {

        }

        return outBitmap;
    }

    public Bitmap YUV_420_888_toRGB_speed2(Image image, int width, int height) {
        // Get the three image planes
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] y = new byte[buffer.remaining()];
        buffer.get(y);

        buffer = planes[1].getBuffer();
        byte[] u = new byte[buffer.remaining()];
        buffer.get(u);

        buffer = planes[2].getBuffer();
        byte[] v = new byte[buffer.remaining()];
        buffer.get(v);

        // get the relevant RowStrides and PixelStrides
        // (we know from documentation that PixelStride is 1 for y)
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();  // we know from   documentation that RowStride is the same for u and v.
        int uvPixelStride = planes[1].getPixelStride();  // we know from   documentation that PixelStride is the same for u and v.

        if (typeUcharY == null) {
            typeUcharY = new Type.Builder(rs, Element.U8(rs));
        }
        if (typeUcharUV == null) {
            typeUcharUV = new Type.Builder(rs, Element.U8(rs));
        }

        // Y,U,V are defined as global allocations, the out-Allocation is the Bitmap.
        // Note also that uAlloc and vAlloc are 1-dimensional while yAlloc is 2-dimensional.

        typeUcharY.setX(yRowStride).setY(height);
        // Allocation yAlloc = Allocation.createTyped(rs, typeUcharY.create());
        Allocation yAlloc = getOrReuseAllocationY(typeUcharY);
        yAlloc.copyFrom(y);
        mYuv420.set_ypsIn(yAlloc);
        // note that the size of the u's and v's are as follows:
        //      (  (width/2)*PixelStride + padding  ) * (height/2)
        // =    (RowStride                          ) * (height/2)
        // but I noted that on the S7 it is 1 less...
        typeUcharUV.setX(u.length);
        Allocation uAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        //Allocation uAlloc = getOrReuseAllocationv(typeUcharUV);
        uAlloc.copyFrom(u);
        mYuv420.set_uIn(uAlloc);
        //Allocation vAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        Allocation vAlloc = getOrReuseAllocationv(typeUcharUV);
        vAlloc.copyFrom(v);
        mYuv420.set_vIn(vAlloc);
        // handover parameters
        mYuv420.set_picWidth(width);
        mYuv420.set_uvRowStride(uvRowStride);
        mYuv420.set_uvPixelStride(uvPixelStride);
        if (outBitmap == null) {
            outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        }
        //Allocation outAlloc = Allocation.createFromBitmap(rs, outBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation outAlloc = getOrReuseAllocationBitmap(outBitmap);
        Script.LaunchOptions lo = new Script.LaunchOptions();
        lo.setX(0, width);  // by this we ignore the y’s padding zone, i.e. the right side of x between width and yRowStride
        lo.setY(0, height);

        mYuv420.forEach_doConvert(outAlloc, lo);
        outAlloc.copyTo(outBitmap);

        return outBitmap;
    }


    public Bitmap YUV_420_888_toRGB_working(Image image, int width, int height) {
        // Get the three image planes
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] y = new byte[buffer.remaining()];
        buffer.get(y);

        buffer = planes[1].getBuffer();
        byte[] u = new byte[buffer.remaining()];
        buffer.get(u);

        buffer = planes[2].getBuffer();
        byte[] v = new byte[buffer.remaining()];
        buffer.get(v);

        // get the relevant RowStrides and PixelStrides
        // (we know from documentation that PixelStride is 1 for y)
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();  // we know from   documentation that RowStride is the same for u and v.
        int uvPixelStride = planes[1].getPixelStride();  // we know from   documentation that PixelStride is the same for u and v.


        //RenderScript rs = MainActivity.rs;
        /*  ScriptC_yuvtorgb mYuv420 = new ScriptC_yuvtorgb(rs);*/

        // Y,U,V are defined as global allocations, the out-Allocation is the Bitmap.
        // Note also that uAlloc and vAlloc are 1-dimensional while yAlloc is 2-dimensional.
        Type.Builder typeUcharY = new Type.Builder(rs, Element.U8(rs));
        typeUcharY.setX(yRowStride).setY(height);
        Allocation yAlloc = Allocation.createTyped(rs, typeUcharY.create());
        yAlloc.copyFrom(y);
        mYuv420.set_ypsIn(yAlloc);

        Type.Builder typeUcharUV = new Type.Builder(rs, Element.U8(rs));
        // note that the size of the u's and v's are as follows:
        //      (  (width/2)*PixelStride + padding  ) * (height/2)
        // =    (RowStride                          ) * (height/2)
        // but I noted that on the S7 it is 1 less...
        typeUcharUV.setX(u.length);
        Allocation uAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        uAlloc.copyFrom(u);
        mYuv420.set_uIn(uAlloc);

        Allocation vAlloc = Allocation.createTyped(rs, typeUcharUV.create());
        vAlloc.copyFrom(v);
        mYuv420.set_vIn(vAlloc);

        // handover parameters
        mYuv420.set_picWidth(width);
        mYuv420.set_uvRowStride(uvRowStride);
        mYuv420.set_uvPixelStride(uvPixelStride);

        if (outBitmap == null) {
            outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        Allocation outAlloc = Allocation.createFromBitmap(rs, outBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

        Script.LaunchOptions lo = new Script.LaunchOptions();
        lo.setX(0, width);  // by this we ignore the y’s padding zone, i.e. the right side of x between width and yRowStride
        lo.setY(0, height);

        mYuv420.forEach_doConvert(outAlloc, lo);
        outAlloc.copyTo(outBitmap);

        return outBitmap;
    }
}
