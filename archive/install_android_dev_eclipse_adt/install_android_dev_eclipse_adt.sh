#!/bin/bash

# Check machine type (32 or 64-bit) & install JDK
MACHINE_TYPE=`uname -a`
sudo apt-get install openjdk-7-jdk

# Prepare /opt/Google directory 
sudo chmod ao+rw /opt
sudo chown :adm /opt
mkdir /opt/Google/

# Download Android NDK
if [[ $MACHINE_TYPE == *"x86_64"* ]]
then
wget http://dl.google.com/android/ndk/android-ndk-r10d-linux-x86_64.bin
else
wget http://dl.google.com/android/ndk/android-ndk-r10d-linux-x86.bin
fi

# Install Android NDK
chmod +x android-ndk*.bin
./android-ndk*.bin
mkdir /opt/Google/android-ndks
rm -f android-ndk-*.bin
mv android-ndk-* /opt/Google/android-ndks/

# Download Eclipse
if [[ $MACHINE_TYPE == *"x86_64"* ]]
then
wget http://ftp.snt.utwente.nl/pub/software/eclipse/technology/epp/downloads/release/luna/SR1a/eclipse-java-luna-SR1a-linux-gtk-x86_64.tar.gz
else
wget http://ftp.snt.utwente.nl/pub/software/eclipse/technology/epp/downloads/release/luna/SR1a/eclipse-java-luna-SR1a-linux-gtk.tar.gz
fi

# Install Eclipse
rm -rf /opt/eclipse
tar -zxvf eclipse-java-luna-SR1a-linux-gtk*.tar.gz -C /opt/

# Install Android SDK and configure SDK & NDK
echo "Add ADT Plugin software site at https://dl-ssl.google.com/android/eclipse/"
echo "Select Android Development Tools"
echo "Select Android Native Development Tools"
echo "When prompted by Android Development (Welcome to Android Development window)"
echo "- include Android 2.2 in installation"
echo "- at the same time point target directory to /opt/Google/android-sdks/"
echo "Add Android API 8 & 10"
echo "Point Window -> Preferences -> Android -> NDK -> NDK Location to the right path"
echo "Press ENTER to launch Eclipse"
read
/opt/eclipse/eclipse &
echo "Press ENTER when finished"
read