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
#ifndef UPLOADER_SAMPLER_H
#define UPLOADER_SAMPLER_H

#include <android/log.h>
#include <android/looper.h>
#include <android/sensor.h>
#include <pthread.h>

#if defined(DEBUG)
#define LOG(level, ...) __android_log_print(level, "SAMPLER", __VA_ARGS__)
#else
#define LOG(level, ...)
#endif

#define SENSOR_TYPE_ACCELEROMETER                 ASENSOR_TYPE_ACCELEROMETER
#define SENSOR_TYPE_MAGNETIC_FIELD                ASENSOR_TYPE_MAGNETIC_FIELD
#define SENSOR_TYPE_ORIENTATION                  3
#define SENSOR_TYPE_GYROSCOPE                     ASENSOR_TYPE_GYROSCOPE
#define SENSOR_TYPE_LIGHT                         ASENSOR_TYPE_LIGHT
#define SENSOR_TYPE_PRESSURE                     6
#define SENSOR_TYPE_TEMPERATURE                  7
#define SENSOR_TYPE_PROXIMITY                     ASENSOR_TYPE_PROXIMITY
#define SENSOR_TYPE_GRAVITY                      9
#define SENSOR_TYPE_LINEAR_ACCELERATION         10
#define SENSOR_TYPE_ROTATION_VECTOR             11
#define SENSOR_TYPE_RELATIVE_HUMIDITY           12
#define SENSOR_TYPE_AMBIENT_TEMPERATURE         13
#define SENSOR_TYPE_MAGNETIC_FIELD_UNCALIBRATED 14
#define SENSOR_TYPE_GAME_ROTATION_VECTOR        15
#define SENSOR_TYPE_GYROSCOPE_UNCALIBRATED      16
#define SENSOR_TYPE_SIGNIFICANT_MOTION          17
#define SENSOR_TYPE_STEP_DETECTOR               18
#define SENSOR_TYPE_STEP_COUNTER                19
#define SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR 20
#define SENSOR_TYPE_HEART_RATE                  21

typedef struct {
   int Index;
   int Type;
   const char *Vendor;
   const char *Name;
   int Delay;
   float Resolution;
   int Size;
   int Axes;
   int Period;
   ASensorEventQueue *Queue;
   int64_t Shift;
} InfoStructure;

typedef struct {
   pthread_mutex_t Lock;
   JNIEnv *JNI;
   jobject Data;
   jfloatArray Exchange;
   ASensorManager *Manager;
   ASensorList Sensors;
   ALooper *Looper;
   int Count;
   int Maximum;
   InfoStructure *Info;
} StateStructure;

#endif //UPLOADER_SAMPLER_H
