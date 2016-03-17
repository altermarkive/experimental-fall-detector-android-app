/*
The MIT License (MIT)

Copyright (c) 2016

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

#ifndef GUARDIAN_DETECTOR_H
#define GUARDIAN_DETECTOR_H

#include <android/log.h>
#include <android/sensor.h>
#include <pthread.h>

#if defined(DEBUG)
#define LOG(level, ...) __android_log_print(level, "DETECTOR", __VA_ARGS__)
#else
#define LOG(level, ...)
#endif

#define INTERVAL_MS altermarkive_guardian_Detector_INTERVAL_MS
#define N altermarkive_guardian_Detector_N
#define SIZE_BUFFER altermarkive_guardian_Detector_SIZE
#define SPAN_MAXMIN (100 / INTERVAL_MS)
#define SPAN_FALLING (1000 / INTERVAL_MS)
#define SPAN_IMPACT (2000 / INTERVAL_MS)
#define SPAN_AVERAGING (400 / INTERVAL_MS)

#define FILTER_NZEROS 2
#define FILTER_NPOLES 2
#define FILTER_LPF_GAIN 4.143204922e+03
#define FILTER_HPF_GAIN 1.022463023e+00
#define FILTER_FACTOR_0 -0.9565436765
#define FILTER_FACTOR_1 +1.9555782403

#define G 1.0

#define FALLING_WAIST_SV_TOT altermarkive_guardian_Detector_FALLING_WAIST_SV_TOT
#define IMPACT_WAIST_SV_TOT altermarkive_guardian_Detector_IMPACT_WAIST_SV_TOT
#define IMPACT_WAIST_SV_D altermarkive_guardian_Detector_IMPACT_WAIST_SV_D
#define IMPACT_WAIST_SV_MAXMIN altermarkive_guardian_Detector_IMPACT_WAIST_SV_MAXMIN
#define IMPACT_WAIST_Z_2 altermarkive_guardian_Detector_IMPACT_WAIST_Z_2
#define LYING_AVERAGE_Z_LPF 0.5

typedef struct {
    JNIEnv *JNI;
    jobject Context;
    jclass AlarmClass;
    jmethodID AlarmCall;
    jdoubleArray BufferArray;
    jdouble *Buffer;
    jint TimeoutFalling;
    jint TimeoutImpact;
    jint Position;
    jdouble *X;
    jdouble *Y;
    jdouble *Z;
    jdouble *X_LPF;
    jdouble *Y_LPF;
    jdouble *Z_LPF;
    jdouble *X_HPF;
    jdouble *Y_HPF;
    jdouble *Z_HPF;
    jdouble *X_MAXMIN;
    jdouble *Y_MAXMIN;
    jdouble *Z_MAXMIN;
    jdouble *SV_TOT;
    jdouble *SV_D;
    jdouble *SV_MAXMIN;
    jdouble *Z_2;
    jdouble *Falling;
    jdouble *Impact;
    jdouble *Lying;
    jdouble XLPFXV[FILTER_NZEROS + 1];
    jdouble XLPFYV[FILTER_NPOLES + 1];
    jdouble YLPFXV[FILTER_NZEROS + 1];
    jdouble YLPFYV[FILTER_NPOLES + 1];
    jdouble ZLPFXV[FILTER_NZEROS + 1];
    jdouble ZLPFYV[FILTER_NPOLES + 1];
    jdouble XHPFXV[FILTER_NZEROS + 1];
    jdouble XHPFYV[FILTER_NPOLES + 1];
    jdouble YHPFXV[FILTER_NZEROS + 1];
    jdouble YHPFYV[FILTER_NPOLES + 1];
    jdouble ZHPFXV[FILTER_NZEROS + 1];
    jdouble ZHPFYV[FILTER_NPOLES + 1];
    jdouble AnteX;
    jdouble AnteY;
    jdouble AnteZ;
    jlong AnteTime;
    jlong Regular;
    pthread_mutex_t Lock;
    ASensorEventQueue *Queue;
} StateStructure;

#endif //GUARDIAN_DETECTOR_H
