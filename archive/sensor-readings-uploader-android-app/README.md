# Sensor Readings Uploader Android App

Depending on the values set in the settings this app will sample sensors
and upload (with a HTTP POST) the readings to a provided URL.

The app starts automatically after power-on and operates in the background.

To convert the ZIP file with recorded sensor sample data to an XLSX spreadsheet
use the script [excel.py](scripts/excel.py).

Install the app by clicking
[here](sensor-uploader/build/outputs/apk/sensor-uploader-all-debug.apk)
