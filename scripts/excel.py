#!/usr/bin/python

import openpyxl
import sqlite3
import sys


def logs(connection, sheet):
    sheet.title = 'Logs'
    sheet.append(['Stamp', 'Priority', 'Tag', 'Entry'])
    cursor = connection.cursor()
    for row in cursor.execute('SELECT * FROM logs ORDER BY stamp'):
        sheet.append(list(row))


def data(connection, sheet):
    sheet.title = 'Data'
    sheet.append(['Stamp', 'Timestamp', 'Type', 'Value0',
                  'Value1', 'Value2', 'Value3', 'Value4', 'Value5'])
    cursor = connection.cursor()
    for row in cursor.execute('SELECT * FROM data ORDER BY stamp'):
        sheet.append(list(row))


def unload(name):
    with sqlite3.connect(name) as connection:
        xlsx = openpyxl.Workbook()
        data(connection, xlsx.active)
        logs(connection, xlsx.create_sheet())
        xlsx.save(name.replace('.sqlite3', '.xlsx'))


def main():
    if len(sys.argv) < 2:
        print('Please give a path to a SQLITE3 file as a parameter')
        sys.exit(0)
    unload(sys.argv[1])


if __name__ == '__main__':
    main()
