#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(wrightstuff.co.za.cameramanager.renderscripttesting)

const static float3 gMonoMult = {0.299f, 0.587f, 0.114f}; //for monochrome

float gCoeffsx[9] = {1, 0, -1, 2, 0, -2, 1, 0, -1};
float gCoeffsy[9] = {1, 2, 1, 0, 0, 0, -1, -2, -1};

int32_t gWidth;
int32_t gHeight;

rs_allocation gIn, direction;

uchar4 RS_KERNEL mono(uchar4 in)
{
    float4 f4 = rsUnpackColor8888(in);
    float3 mono = dot(f4.rgb, gMonoMult);
    return rsPackColorTo8888(mono);
}

uchar4 __attribute__((kernel)) sobel(uint32_t x, uint32_t y) {
   uint32_t x1 = min((int32_t)x+1, gWidth-1);
       uint32_t x2 = max((int32_t)x-1, 0);
       uint32_t y1 = min((int32_t)y+1, gHeight-1);
       uint32_t y2 = max((int32_t)y-1, 0);

       float4 p00x = convert_float4(rsGetElementAt_uchar4(gIn, x1, y1));
       float4 p01x = convert_float4(rsGetElementAt_uchar4(gIn, x, y1));
       float4 p02x = convert_float4(rsGetElementAt_uchar4(gIn, x2, y1));
       float4 p10x = convert_float4(rsGetElementAt_uchar4(gIn, x1, y));
       float4 p11x = convert_float4(rsGetElementAt_uchar4(gIn, x, y));
       float4 p12x = convert_float4(rsGetElementAt_uchar4(gIn, x2, y));
       float4 p20x = convert_float4(rsGetElementAt_uchar4(gIn, x1, y2));
       float4 p21x = convert_float4(rsGetElementAt_uchar4(gIn, x, y2));
       float4 p22x = convert_float4(rsGetElementAt_uchar4(gIn, x2, y2));

       float4 p00y = convert_float4(rsGetElementAt_uchar4(gIn, x1, y1));
       float4 p01y = convert_float4(rsGetElementAt_uchar4(gIn, x, y1));
       float4 p02y = convert_float4(rsGetElementAt_uchar4(gIn, x2, y1));
       float4 p10y = convert_float4(rsGetElementAt_uchar4(gIn, x1, y));
       float4 p11y = convert_float4(rsGetElementAt_uchar4(gIn, x, y));
       float4 p12y = convert_float4(rsGetElementAt_uchar4(gIn, x2, y));
       float4 p20y = convert_float4(rsGetElementAt_uchar4(gIn, x1, y2));
       float4 p21y = convert_float4(rsGetElementAt_uchar4(gIn, x, y2));
       float4 p22y = convert_float4(rsGetElementAt_uchar4(gIn, x2, y2));


       p00x *= gCoeffsx[0];
       p01x *= gCoeffsx[1];
       p02x *= gCoeffsx[2];
       p10x *= gCoeffsx[3];
       p11x *= gCoeffsx[4];
       p12x *= gCoeffsx[5];
       p20x *= gCoeffsx[6];
       p21x *= gCoeffsx[7];
       p22x *= gCoeffsx[8];
       p00x += p01x;
       p02x += p10x;
       p11x += p12x;
       p20x += p21x;
       p22x += p00x;
       p02x += p11x;
       p20x += p22x;
       p20x += p02x;
       p20x = clamp(p20x, 0.f, 255.f);


       p00y *= gCoeffsy[0];
       p01y *= gCoeffsy[1];
       p02y *= gCoeffsy[2];
       p10y *= gCoeffsy[3];
       p11y *= gCoeffsy[4];
       p12y *= gCoeffsy[5];
       p20y *= gCoeffsy[6];
       p21y *= gCoeffsy[7];
       p22y *= gCoeffsy[8];
       p00y += p01y;
       p02y += p10y;
       p11y += p12y;
       p20y += p21y;
       p22y += p00y;
       p02y += p11y;
       p20y += p22y;
       p20y += p02y;
       p20y = clamp(p20y, 0.f, 255.f);

    float4 pfinal = sqrt((p20x*p20x)+(p20y*p20y));
    return convert_uchar4(pfinal);
}