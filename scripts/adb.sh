#!/bin/bash
ADB=$(find /opt -name adb -executable | grep platform)
NDK=$(dirname $ADB)
export PATH=$PATH:$NDK
