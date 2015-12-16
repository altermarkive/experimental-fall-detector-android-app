# Android-Sensor-Readings-Uploader

Depending on the values set in the settings this app will sample sensors
and upload (with a HTTP POST) the readings to a provided URL.

The app starts automatically after power-on and operates in the background.

To convert the ZIP file with recorded sensor sample data to an Excel file
use the script [excel.py](scripts/excel.py) 
