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

// Digital filters (low-pass & high-pass) designed by mkfilter/mkshape/gencode A.J. Fisher
// http://www-users.cs.york.ac.uk/~fisher/mkfilter/

#include "altermarkive_guardian_Detector.h"
#include <string.h>
#include <android/looper.h>
#include <math.h>
#include "detector.h"

#define LINEAR(Before, Ante, After, Post, Now) (Ante + (Post - Ante) * (jdouble)(Now - Before) / (jdouble)(After - Before))
#define AT(Array, Index, Size) (Array[(Index + Size) % Size])
#define EXPIRE(Timeout) (Timeout > -1 ? Timeout - 1 : -1)

StateStructure *State = NULL;

jdouble SV(jdouble X, jdouble Y, jdouble Z) {
    return sqrt(X * X + Y * Y + Z * Z);
}

jdouble Min(jdouble *Array) {
    jint I;
    jdouble Min = AT(Array, State->Position, N);
    for (I = 1; I < SPAN_MAXMIN; I++) {
        jdouble Value = AT(Array, State->Position - I, N);
        if (!isnan(Value) && Value < Min) {
            Min = Value;
        }
    }
    return Min;
}

jdouble Max(jdouble *Array) {
    jint I;
    jdouble Max = AT(Array, State->Position, N);
    for (I = 1; I < SPAN_MAXMIN; I++) {
        jdouble Value = AT(Array, State->Position - I, N);
        if (!isnan(Value) && Max < Value) {
            Max = Value;
        }
    }
    return Max;
}


// Low-pass Butterworth filter, 2nd order, 50 Hz sampling rate, corner frequency 0.25 Hz
jdouble LPF(jdouble Value, jdouble *XV, jdouble *YV) {
    XV[0] = XV[1];
    XV[1] = XV[2];
    XV[2] = Value / FILTER_LPF_GAIN;
    YV[0] = YV[1];
    YV[1] = YV[2];
    YV[2] = (XV[0] + XV[2]) + 2 * XV[1] + (FILTER_FACTOR_0 * YV[0]) + (FILTER_FACTOR_1 * YV[1]);
    return YV[2];
}

// High-pass Butterworth filter, 2nd order, 50 Hz sampling rate, corner frequency 0.25 Hz
jdouble HPF(jdouble Value, jdouble *XV, jdouble *YV) {
    XV[0] = XV[1];
    XV[1] = XV[2];
    XV[2] = Value / FILTER_HPF_GAIN;
    YV[0] = YV[1];
    YV[1] = YV[2];
    YV[2] = (XV[0] + XV[2]) - 2 * XV[1] + (FILTER_FACTOR_0 * YV[0]) + (FILTER_FACTOR_1 * YV[1]);
    return YV[2];
}

jint Count(jdouble *Events) {
    jint I, Count = 0;
    for (I = 0; I < N; I++) {
        if (1 == AT(Events, I, N)) {
            Count++;
        }
    }
    return (Count);
}

void Call() {
    JNIEnv *JNI = State->JNI;
    if (0 == State->AlarmCall) {
        return;
    }
    (*JNI)->CallStaticVoidMethod(JNI, State->AlarmClass, State->AlarmCall, State->Context);
}

void Process() {
    jint At = State->Position;
    State->TimeoutFalling = EXPIRE(State->TimeoutFalling);
    State->TimeoutImpact = EXPIRE(State->TimeoutImpact);
    State->X_LPF[At] = LPF(State->X[At], State->XLPFXV, State->XLPFYV);
    State->Y_LPF[At] = LPF(State->Y[At], State->YLPFXV, State->YLPFYV);
    State->Z_LPF[At] = LPF(State->Z[At], State->ZLPFXV, State->ZLPFYV);
    State->X_HPF[At] = HPF(State->X[At], State->XHPFXV, State->XHPFYV);
    State->Y_HPF[At] = HPF(State->Y[At], State->YHPFXV, State->YHPFYV);
    State->Z_HPF[At] = HPF(State->Z[At], State->ZHPFXV, State->ZHPFYV);
    State->X_MAXMIN[At] = Max(State->X) - Min(State->X);
    State->Y_MAXMIN[At] = Max(State->Y) - Min(State->Y);
    State->Z_MAXMIN[At] = Max(State->Z) - Min(State->Z);
    jdouble SV_TOT = State->SV_TOT[At] = SV(State->X[At], State->Y[At], State->Z[At]);
    jdouble SV_D = State->SV_D[At] = SV(State->X_HPF[At], State->Y_HPF[At], State->Z_HPF[At]);
    State->SV_MAXMIN[At] = SV(State->X_MAXMIN[At], State->Y_MAXMIN[At], State->Z_MAXMIN[At]);
    State->Z_2[At] = (SV_TOT * SV_TOT - SV_D * SV_D - G * G) / (2.0 * G);
    jdouble SV_TOT_BEFORE = AT(State->SV_TOT, At - 1, N);
    State->Falling[At] = 0;
    if (FALLING_WAIST_SV_TOT <= SV_TOT_BEFORE && SV_TOT < FALLING_WAIST_SV_TOT) {
        State->TimeoutFalling = SPAN_FALLING;
        State->Falling[At] = 1;
    }
    State->Impact[At] = 0;
    if (-1 < State->TimeoutFalling) {
        jdouble SV_MAXMIN = State->SV_MAXMIN[At];
        jdouble Z_2 = State->Z_2[At];
        if (IMPACT_WAIST_SV_TOT <= SV_TOT || IMPACT_WAIST_SV_D <= SV_D ||
            IMPACT_WAIST_SV_MAXMIN <= SV_MAXMIN || IMPACT_WAIST_Z_2 <= Z_2) {
            State->TimeoutImpact = SPAN_IMPACT;
            State->Impact[At] = 1;
        }
    }
    State->Lying[At] = 0;
    if (0 == State->TimeoutImpact) {
        jint I;
        jdouble Sum = 0, Count = 0;
        for (I = 0; I < SPAN_AVERAGING; I++) {
            jdouble Value = AT(State->Z_LPF, At - I, N);
            if (!isnan(Value)) {
                Sum += Value;
                Count += 1;
            }
        }
        if (LYING_AVERAGE_Z_LPF < (Sum / Count)) {
            State->Lying[At] = 1;
            Call();
        }
    }
}

// Android sampling is irregular, thus the signal is (linearly) resampled at 50 Hz
void Resample(int64_t PostTime, jdouble PostX, jdouble PostY, jdouble PostZ) {
    if (0 == State->AnteTime) {
        State->Regular = PostTime + INTERVAL_MS;
        return;
    }
    while (State->Regular < PostTime) {
        jint At = State->Position;
        State->X[At] = LINEAR(State->AnteTime, State->AnteX, PostTime, PostX, State->Regular);
        State->Y[At] = LINEAR(State->AnteTime, State->AnteY, PostTime, PostY, State->Regular);
        State->Z[At] = LINEAR(State->AnteTime, State->AnteZ, PostTime, PostZ, State->Regular);
        Process();
        State->Position = (State->Position + 1) % N;
        State->Regular += INTERVAL_MS;
    }
}

void Protect(int64_t PostTime, jdouble PostX, jdouble PostY, jdouble PostZ) {
    pthread_mutex_lock(&State->Lock);
    Resample(PostTime, PostX, PostY, PostZ);
    pthread_mutex_unlock(&State->Lock);
}

int Sampled(int FD, int Events, void *Data) {
    ASensorEvent Event;
    while (ASensorEventQueue_getEvents(State->Queue, &Event, 1) > 0) {
        jlong PostTime = Event.timestamp / 1000000;
        if (ASENSOR_TYPE_ACCELEROMETER == Event.type) {
            double PostX = Event.acceleration.x / ASENSOR_STANDARD_GRAVITY;
            double PostY = Event.acceleration.y / ASENSOR_STANDARD_GRAVITY;
            double PostZ = Event.acceleration.z / ASENSOR_STANDARD_GRAVITY;
            Protect(PostTime, PostX, PostY, PostZ);
            State->AnteTime = PostTime;
            State->AnteX = PostX;
            State->AnteY = PostY;
            State->AnteZ = PostZ;
        }
    }
    return (1);
}

void Fill(jdouble *Array, jint Offset, jint Length, jdouble Value) {
    jint I;
    for (I = Offset; I < Offset + Length; I++) {
        Array[I] = Value;
    }
}

void InitiateBuffer(StateStructure *State) {
    JNIEnv *JNI = State->JNI;
    State->BufferArray = (*JNI)->NewGlobalRef(JNI, (*JNI)->NewDoubleArray(JNI, SIZE_BUFFER));
    State->Buffer = (*JNI)->GetDoubleArrayElements(JNI, State->BufferArray, NULL);
    Fill(State->Buffer, 0, SIZE_BUFFER, FP_NAN);
    (*JNI)->ReleaseDoubleArrayElements(JNI, State->BufferArray, State->Buffer, JNI_COMMIT);
    State->TimeoutImpact = -1;
    State->TimeoutFalling = -1;
    State->Position = 0;
}

void InitiateSamples(StateStructure *State) {
    State->X = State->Buffer + altermarkive_guardian_Detector_OFFSET_X;
    State->Y = State->Buffer + altermarkive_guardian_Detector_OFFSET_Y;
    State->Z = State->Buffer + altermarkive_guardian_Detector_OFFSET_Z;
}

void InitiateResampling(StateStructure *State) {
    State->AnteX = State->AnteY = State->AnteZ = FP_NAN;
    State->AnteTime = 0;
}

void InitiateFiltering(StateStructure *State) {
    State->X_LPF = State->Buffer + altermarkive_guardian_Detector_OFFSET_X_LPF;
    State->Y_LPF = State->Buffer + altermarkive_guardian_Detector_OFFSET_Y_LPF;
    State->Z_LPF = State->Buffer + altermarkive_guardian_Detector_OFFSET_Z_LPF;
    State->X_HPF = State->Buffer + altermarkive_guardian_Detector_OFFSET_X_HPF;
    State->Y_HPF = State->Buffer + altermarkive_guardian_Detector_OFFSET_Y_HPF;
    State->Z_HPF = State->Buffer + altermarkive_guardian_Detector_OFFSET_Z_HPF;
    Fill(State->XLPFXV, 0, FILTER_NZEROS + 1, 0);
    Fill(State->XLPFYV, 0, FILTER_NPOLES + 1, 0);
    Fill(State->YLPFXV, 0, FILTER_NZEROS + 1, 0);
    Fill(State->YLPFYV, 0, FILTER_NPOLES + 1, 0);
    Fill(State->ZLPFXV, 0, FILTER_NZEROS + 1, 0);
    Fill(State->ZLPFYV, 0, FILTER_NPOLES + 1, 0);
    Fill(State->XHPFXV, 0, FILTER_NZEROS + 1, 0);
    Fill(State->XHPFYV, 0, FILTER_NPOLES + 1, 0);
    Fill(State->YHPFXV, 0, FILTER_NZEROS + 1, 0);
    Fill(State->YHPFYV, 0, FILTER_NPOLES + 1, 0);
    Fill(State->ZHPFXV, 0, FILTER_NZEROS + 1, 0);
    Fill(State->ZHPFYV, 0, FILTER_NPOLES + 1, 0);
}

void InitiateDeltas(StateStructure *State) {
    State->X_MAXMIN = State->Buffer + altermarkive_guardian_Detector_OFFSET_X_D;
    State->Y_MAXMIN = State->Buffer + altermarkive_guardian_Detector_OFFSET_Y_D;
    State->Z_MAXMIN = State->Buffer + altermarkive_guardian_Detector_OFFSET_Z_D;
}

void InitiateSV(StateStructure *State) {
    State->SV_TOT = State->Buffer + altermarkive_guardian_Detector_OFFSET_SV_TOT;
    State->SV_D = State->Buffer + altermarkive_guardian_Detector_OFFSET_SV_D;
    State->SV_MAXMIN = State->Buffer + altermarkive_guardian_Detector_OFFSET_SV_MAXMIN;
    State->Z_2 = State->Buffer + altermarkive_guardian_Detector_OFFSET_Z_2;
}

void InitiateEvents(StateStructure *State) {
    State->Falling = State->Buffer + altermarkive_guardian_Detector_OFFSET_FALLING;
    State->Impact = State->Buffer + altermarkive_guardian_Detector_OFFSET_IMPACT;
    State->Lying = State->Buffer + altermarkive_guardian_Detector_OFFSET_LYING;
}

void InitiateProtection(StateStructure *State) {
    pthread_mutex_init(&State->Lock, NULL);
}

void InitiateSensor(StateStructure *State) {
    ALooper *Looper = ALooper_forThread();
    if (NULL == Looper) {
        Looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    }
    ASensorManager *Manager = ASensorManager_getInstance();
    const ASensor *Sensor = ASensorManager_getDefaultSensor(Manager,
                                                            ASENSOR_TYPE_ACCELEROMETER);
    const char *Vendor = ASensor_getVendor(Sensor);
    const char *Name = ASensor_getName(Sensor);
    int Delay = ASensor_getMinDelay(Sensor);
    float Resolution = ASensor_getResolution(Sensor);
    LOG(ANDROID_LOG_INFO, "Sensor: %s, %s, %dus, %f", Vendor, Name, Delay, Resolution);
    State->Queue = ASensorManager_createEventQueue(Manager, Looper, 0xDEF00ABC, Sampled, NULL);
    ASensorEventQueue_enableSensor(State->Queue, Sensor);
    // To have effect, the setting of the event rate must be after the enabling of the sensor
    ASensorEventQueue_setEventRate(State->Queue, Sensor, INTERVAL_MS * 1000);
}

JNIEXPORT void JNICALL Java_altermarkive_guardian_Detector_initiate(JNIEnv *JNI, jclass SelfClass,
                                                                    jobject Context) {
    if (NULL == State) {
        StateStructure *Blank = (StateStructure *) malloc(sizeof(StateStructure));
        Blank->JNI = JNI;
        Blank->Context = (*JNI)->NewGlobalRef(JNI, Context);
        Blank->AlarmClass = (*JNI)->FindClass(JNI, "altermarkive/guardian/Alarm");
        Blank->AlarmClass = (*JNI)->NewGlobalRef(JNI, Blank->AlarmClass);
        Blank->AlarmCall = (*JNI)->GetStaticMethodID(JNI, Blank->AlarmClass, "call",
                                                     "(Landroid/content/Context;)V");
        InitiateBuffer(Blank);
        InitiateSamples(Blank);
        InitiateResampling(Blank);
        InitiateFiltering(Blank);
        InitiateDeltas(Blank);
        InitiateSV(Blank);
        InitiateEvents(Blank);
        InitiateProtection(Blank);
        InitiateSensor(Blank);
        State = Blank;
    }
}

JNIEXPORT void JNICALL Java_altermarkive_guardian_Detector_acquire(JNIEnv *JNI, jclass Self) {
    if (NULL != State) {
        pthread_mutex_lock(&State->Lock);
    }
}

JNIEXPORT jdoubleArray JNICALL Java_altermarkive_guardian_Detector_buffer(JNIEnv *JNI,
                                                                          jclass Self) {
    if (NULL != State) {
        (*JNI)->ReleaseDoubleArrayElements(JNI, State->BufferArray, State->Buffer, JNI_COMMIT);
        return State->BufferArray;
    }
    return NULL;
}

JNIEXPORT jint JNICALL Java_altermarkive_guardian_Detector_position(JNIEnv *JNI, jclass Self) {
    if (NULL != State) {
        return State->Position;
    }
    return 0;
}

JNIEXPORT void JNICALL Java_altermarkive_guardian_Detector_release(JNIEnv *JNI, jclass Self) {
    if (NULL != State) {
        pthread_mutex_unlock(&State->Lock);
    }
}
