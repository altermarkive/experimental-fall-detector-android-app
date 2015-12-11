#!/bin/bash
cd app/src/main/
javah -d jni -classpath `find / -name "android.jar" 2> /dev/null | head -1`:../../build/intermediates/classes/all/debug altermarkive.uploader.Sampler
