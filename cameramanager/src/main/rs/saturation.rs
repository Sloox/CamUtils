#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(wrightstuff.co.za.cameramanager.renderscripttesting)

const static float3 gMonoMult = {0.299f, 0.587f, 0.114f};

float saturationValue = 0.f;
/*
 * RenderScript kernel that performs saturation manipulation.
 */
uchar4 __attribute__((kernel)) saturation(uchar4 in)
{
    float4 f4 = rsUnpackColor8888(in);
    float3 result = dot(f4.rgb, gMonoMult);
    result = mix(result, f4.rgb, saturationValue);

    return rsPackColorTo8888(result);
}

uchar4 __attribute__((kernel)) mono(uchar4 in)
{
    float4 f4 = rsUnpackColor8888(in);
    float val = 0.2989 * f4.r + 0.5870 * f4.g + 0.1140 * f4.b;
    f4.r = f4.g = f4.b = val;
    float3 mono = {f4.r, f4.g, f4.b};
    return rsPackColorTo8888(mono);
}
