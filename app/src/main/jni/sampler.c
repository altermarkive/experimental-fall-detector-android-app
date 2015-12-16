/*
This code is free  software: you can redistribute it and/or  modify it under the
terms of the GNU Lesser General Public License as published by the Free Software
Foundation,  either version  3 of  the License,  or (at  your option)  any later
version.

This code  is distributed in the  hope that it  will be useful, but  WITHOUT ANY
WARRANTY; without even the implied warranty  of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Lesser  General Public License for more details.

You should have  received a copy of the GNU  Lesser General Public License along
with code. If not, see http://www.gnu.org/licenses/.
*/
#include "altermarkive_uploader_Sampler.h"
#include "sampler.h"

StateStructure *State = NULL;

jlong CallSystemCurrentTimeMillis(JNIEnv *JNI) {
    jclass SystemClass = (*JNI)->FindClass(JNI, "java/lang/System");
    jmethodID SystemCurrentTimeMillis = (*JNI)->GetStaticMethodID(JNI, SystemClass,
                                                                  "currentTimeMillis", "()J");
    if (0 == SystemCurrentTimeMillis) {
        return 0;
    }
    return (*JNI)->CallStaticLongMethod(JNI, SystemClass, SystemCurrentTimeMillis);
}

jobject CallSelfConfig(JNIEnv *JNI, jobject Self) {
    jclass SelfClass = (*JNI)->GetObjectClass(JNI, Self);
    jmethodID SelfConfig = (*JNI)->GetMethodID(JNI, SelfClass, "config",
                                               "()Laltermarkive/uploader/Config;");
    if (0 == SelfConfig) {
        LOG(ANDROID_LOG_ERROR, "Failed to call altermarkive.uploader.Sampler.config");
        return NULL;
    }
    return (*JNI)->CallObjectMethod(JNI, Self, SelfConfig);
}

jintArray CallConfigInitiate(JNIEnv *JNI, jobject Config, jintArray Types, jobjectArray Vendors,
                             jobjectArray Names, jintArray Delays,
                             jfloatArray Resolutions, jintArray Sizes) {
    jclass ConfigClass = (*JNI)->GetObjectClass(JNI, Config);
    jmethodID ConfigInitiate = (*JNI)->GetMethodID(JNI, ConfigClass, "initiate",
                                                   "([I[Ljava/lang/String;[Ljava/lang/String;[I[F[I)[I");
    if (0 == ConfigInitiate) {
        LOG(ANDROID_LOG_ERROR, "Failed to call altermarkive.uploader.Config.initiate");
        return NULL;
    }
    return (*JNI)->CallObjectMethod(JNI, Config, ConfigInitiate, Types, Vendors, Names, Delays,
                                    Resolutions, Sizes);
}

jobject CallSelfData(JNIEnv *JNI, jobject Self) {
    jclass SelfClass = (*JNI)->GetObjectClass(JNI, Self);
    jmethodID SelfData = (*JNI)->GetMethodID(JNI, SelfClass, "data",
                                             "()Laltermarkive/uploader/Data;");
    if (0 == SelfData) {
        LOG(ANDROID_LOG_ERROR, "Failed to call altermarkive.uploader.Sampler.data");
        return NULL;
    }
    return (*JNI)->CallObjectMethod(JNI, Self, SelfData);
}

void CallDataDispatch(JNIEnv *JNI, jobject Data, jint Type, jint Index, jlong Stamp,
                      jfloatArray Values, jint Axes) {
    jclass DataClass = (*JNI)->GetObjectClass(JNI, Data);
    jmethodID DataDispatch = (*JNI)->GetMethodID(JNI, DataClass, "dispatch", "(IIJ[DI)V");
    if (0 == DataDispatch) {
        LOG(ANDROID_LOG_ERROR, "Failed to call altermarkive.uploader.Data.dispatch");
        return;
    }
    (*JNI)->CallVoidMethod(JNI, Data, DataDispatch, Type, Index, Stamp, Values, Axes);
}

int TypeToAxes(int Type) {
    switch (Type) {
        case SENSOR_TYPE_ACCELEROMETER:
        case SENSOR_TYPE_MAGNETIC_FIELD:
        case SENSOR_TYPE_ORIENTATION:
        case SENSOR_TYPE_GYROSCOPE:
        case SENSOR_TYPE_GRAVITY:
        case SENSOR_TYPE_LINEAR_ACCELERATION:
        case SENSOR_TYPE_ROTATION_VECTOR:
        case SENSOR_TYPE_GAME_ROTATION_VECTOR:
        case SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR:
            return 3;
        case SENSOR_TYPE_LIGHT:
        case SENSOR_TYPE_PRESSURE:
        case SENSOR_TYPE_TEMPERATURE:
        case SENSOR_TYPE_PROXIMITY:
        case SENSOR_TYPE_RELATIVE_HUMIDITY:
        case SENSOR_TYPE_AMBIENT_TEMPERATURE:
        case SENSOR_TYPE_HEART_RATE:
        case SENSOR_TYPE_STEP_COUNTER:
            return 1;
        case SENSOR_TYPE_MAGNETIC_FIELD_UNCALIBRATED:
        case SENSOR_TYPE_GYROSCOPE_UNCALIBRATED:
            return 6;
        case SENSOR_TYPE_SIGNIFICANT_MOTION:
        case SENSOR_TYPE_STEP_DETECTOR:
            return 0;
        default:
            return 0;
    }
}

int TypeToSize(int Type) {
    int Axes = TypeToAxes(Type);
    return sizeof(int64_t) + Axes * sizeof(float);
}

int SampleHandler(int FD, int Events, void *Data) {
    JNIEnv *JNI = State->JNI;
    jlong Now = CallSystemCurrentTimeMillis(JNI) * 1000;
    InfoStructure *Info = (InfoStructure *) Data;
    ASensorEvent Event;
    pthread_mutex_lock(&State->Lock);
    while (ASensorEventQueue_getEvents(Info->Queue, &Event, 1) > 0) {
        if (0 == Info->Shift) {
            continue;
        }
        int64_t Stamp = Info->Shift + Event.timestamp / 1000;
        int I;
        jfloat Values[6];
        switch (Event.type) {
            case SENSOR_TYPE_ACCELEROMETER:
            case SENSOR_TYPE_MAGNETIC_FIELD:
            case SENSOR_TYPE_ORIENTATION:
            case SENSOR_TYPE_GYROSCOPE:
            case SENSOR_TYPE_LIGHT:
            case SENSOR_TYPE_PRESSURE:
            case SENSOR_TYPE_TEMPERATURE:
            case SENSOR_TYPE_PROXIMITY:
            case SENSOR_TYPE_GRAVITY:
            case SENSOR_TYPE_LINEAR_ACCELERATION:
            case SENSOR_TYPE_ROTATION_VECTOR:
            case SENSOR_TYPE_RELATIVE_HUMIDITY:
            case SENSOR_TYPE_AMBIENT_TEMPERATURE:
            case SENSOR_TYPE_MAGNETIC_FIELD_UNCALIBRATED:
            case SENSOR_TYPE_GAME_ROTATION_VECTOR:
            case SENSOR_TYPE_GYROSCOPE_UNCALIBRATED:
            case SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR:
            case SENSOR_TYPE_HEART_RATE:
                for (I = 0; I < Info->Axes; I++) {
                    Values[I] = Event.data[I];
                }
                (*JNI)->SetFloatArrayRegion(JNI, State->Exchange, 0, Info->Axes, Values);
                break;
            case SENSOR_TYPE_STEP_COUNTER:
                Values[0] = Event.u64.step_counter;
                (*JNI)->SetFloatArrayRegion(JNI, State->Exchange, 0, Info->Axes, Values);
                break;
            default:
                break;
        }
        CallDataDispatch(JNI, State->Data, Event.type, Info->Index, Stamp, State->Exchange,
                         Info->Axes);
    }
    Info->Shift = Now - Event.timestamp / 1000;
    pthread_mutex_unlock(&State->Lock);
    return (1);
}

void QuerySensors() {
    int I;
    State->Manager = ASensorManager_getInstance();
    State->Count = ASensorManager_getSensorList(State->Manager, &State->Sensors);
    State->Info = (InfoStructure *) malloc(State->Count * sizeof(InfoStructure));
    LOG(ANDROID_LOG_INFO, "Found %d sensors", State->Count);
    for (I = 0; I < State->Count; I++) {
        State->Info[I].Index = I;
        State->Info[I].Type = ASensor_getType(State->Sensors[I]);
        State->Info[I].Vendor = ASensor_getVendor(State->Sensors[I]);
        State->Info[I].Name = ASensor_getName(State->Sensors[I]);
        State->Info[I].Delay = ASensor_getMinDelay(State->Sensors[I]);
        State->Info[I].Resolution = ASensor_getResolution(State->Sensors[I]);
        LOG(ANDROID_LOG_INFO, "Sensor: %d, %s, %s, %dus, %f", State->Info[I].Type,
            State->Info[I].Vendor,
            State->Info[I].Name, State->Info[I].Delay,
            State->Info[I].Resolution);
        State->Info[I].Shift = 0;
    }
}

void QueryConfig(JNIEnv *JNI, jobject Self) {
    int I;
    jclass StringClass = (*JNI)->FindClass(JNI, "java/lang/String");

    jintArray Types = (*JNI)->NewIntArray(JNI, State->Count);
    jobjectArray Vendors = (*JNI)->NewObjectArray(JNI, State->Count, StringClass, NULL);
    jobjectArray Names = (*JNI)->NewObjectArray(JNI, State->Count, StringClass, NULL);
    jintArray Delays = (*JNI)->NewIntArray(JNI, State->Count);
    jfloatArray Resolutions = (*JNI)->NewFloatArray(JNI, State->Count);
    jintArray Sizes = (*JNI)->NewIntArray(JNI, State->Count);
    jintArray Axes = (*JNI)->NewIntArray(JNI, State->Count);

    jint *TypesArray = (*JNI)->GetIntArrayElements(JNI, Types, NULL);
    jint *DelaysArray = (*JNI)->GetIntArrayElements(JNI, Delays, NULL);
    jfloat *ResolutionsArray = (*JNI)->GetFloatArrayElements(JNI, Resolutions, NULL);
    jint *SizesArray = (*JNI)->GetIntArrayElements(JNI, Sizes, NULL);
    jint *AxesArray = (*JNI)->GetIntArrayElements(JNI, Axes, NULL);

    State->Maximum = 0;
    for (I = 0; I < State->Count; I++) {
        TypesArray[I] = State->Info[I].Type;
        jstring Vendor = (*JNI)->NewStringUTF(JNI, State->Info[I].Vendor);
        (*JNI)->SetObjectArrayElement(JNI, Vendors, I, Vendor);
        jstring Name = (*JNI)->NewStringUTF(JNI, State->Info[I].Name);
        (*JNI)->SetObjectArrayElement(JNI, Names, I, Name);
        DelaysArray[I] = State->Info[I].Delay;
        ResolutionsArray[I] = State->Info[I].Resolution;
        SizesArray[I] = State->Info[I].Size = TypeToSize(State->Info[I].Type);
        AxesArray[I] = State->Info[I].Axes = TypeToAxes(State->Info[I].Type);
        if (State->Maximum < AxesArray[I]) {
            State->Maximum = AxesArray[I];
        }
    }

    (*JNI)->ReleaseIntArrayElements(JNI, Types, TypesArray, 0);
    (*JNI)->ReleaseIntArrayElements(JNI, Delays, DelaysArray, 0);
    (*JNI)->ReleaseFloatArrayElements(JNI, Resolutions, ResolutionsArray, 0);
    (*JNI)->ReleaseIntArrayElements(JNI, Sizes, SizesArray, 0);
    (*JNI)->ReleaseIntArrayElements(JNI, Axes, AxesArray, 0);

    jobject Config = CallSelfConfig(JNI, Self);
    jintArray Periods = CallConfigInitiate(JNI, Config, Types, Vendors, Names, Delays, Resolutions,
                                           Sizes);

    jint *PeriodsArray = (*JNI)->GetIntArrayElements(JNI, Periods, NULL);
    for (I = 0; I < State->Count; I++) {
        State->Info[I].Period = PeriodsArray[I];
    }
    (*JNI)->ReleaseIntArrayElements(JNI, Periods, PeriodsArray, JNI_ABORT);
}

void ConfigureSampling(JNIEnv *JNI, jobject Self) {
    int I;
    State->Looper = ALooper_forThread();
    if (NULL == State->Looper) {
        State->Looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    }
    for (I = 0; I < State->Count; I++) {
        if (0 == State->Info[I].Period) {
            LOG(ANDROID_LOG_INFO, "Event queue for sensor #%d not created", I);
            continue;
        }
        State->Info[I].Queue = ASensorManager_createEventQueue(State->Manager, State->Looper,
                                                               0xDEF00ABC, SampleHandler,
                                                               &(State->Info[I]));
        LOG(ANDROID_LOG_INFO, "Event queue for sensor #%d created", I);
    }
    State->Data = (*JNI)->NewGlobalRef(JNI, CallSelfData(JNI, Self));
    State->Exchange = (*JNI)->NewGlobalRef(JNI, (*JNI)->NewFloatArray(JNI, State->Maximum));
    pthread_mutex_init(&State->Lock, NULL);
}

void StartSampling() {
    int I;
    for (I = 0; I < State->Count; I++) {
        if (0 == State->Info[I].Period) {
            LOG(ANDROID_LOG_INFO, "Sensor #%d not started", I);
            continue;
        }
        int32_t Period = ((int32_t) State->Info[I].Period) * 1000;
        // Enable sampling
        if (ASensorEventQueue_enableSensor(State->Info[I].Queue, State->Sensors[I]) < 0) {
            LOG(ANDROID_LOG_ERROR, "Failed to enable sensor #%d", I);
            continue;
        }
        // To have effect, the setting of the event rate must be after the enabling of the sensor
        if (ASensorEventQueue_setEventRate(State->Info[I].Queue, State->Sensors[I], Period) < 0) {
            LOG(ANDROID_LOG_ERROR, "Failed to set event rate for sensor #%d", I);
            continue;
        }
        LOG(ANDROID_LOG_INFO, "Sensor #%d started with given period", I);
    }
}

JNIEXPORT void JNICALL Java_altermarkive_uploader_Sampler_initiate(JNIEnv *JNI, jobject Self) {
    if (NULL == State) {
        State = (StateStructure *) malloc(sizeof(StateStructure));
        State->JNI = JNI;
        QuerySensors();
        QueryConfig(JNI, Self);
        ConfigureSampling(JNI, Self);
        StartSampling();
    }
}
