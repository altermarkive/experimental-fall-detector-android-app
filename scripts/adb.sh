#!/bin/bash
ADB=$(find / -name adb -executable 2> /dev/null | grep platform)
NDK=$(dirname $ADB)
export PATH=$PATH:$NDK
