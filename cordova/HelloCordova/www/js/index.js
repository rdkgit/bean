/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

var numDevices = 0;
var aDevice;

var battery = {
    service: "180F",
    level: "2A19"
};

// Bean serial message protocol uuid
// A495-FF11-C5B1-4B44-B512-1370F02D74DE

var SERIAL_UUID = 'a495ff10c5b14b44b5121370f02d74de';
var BEAN_SERIAL_CHAR_UUID = 'a495ff11c5b14b44b5121370f02d74de';



// Bean accelerometer app message types
// 0x20, 0x10 request: client -> bean
// 0x20, 0x90 response: bean -> client

// send a read-accel message
// CC_ACCEL_READ(0x2010),
// buffer.writeByte((type.getRawValue() >> 8) & 0xff);
// buffer.writeByte(type.getRawValue() & 0xff);

//var accelerometer = {
//  service: "F000AA10-0451-4000-B000-000000000000",
//  data: "F000AA11-0451-4000-B000-000000000000", // read/notify 3 bytes X : Y :// Z
//  configuration: "F000AA12-0451-4000-B000-000000000000", // read/write 1 byte
//  period: "F000AA13-0451-4000-B000-000000000000" // read/write 1 byte Period = [Input*10]ms
//};

var accelerometer = {
    service: "2010",
    level: "0000"
};


function onAppError(error)
{
    document.getElementById("bluetoothStatus").innerHTML =
	"Error from "+aDevice.name+", "+error;
}

function onReadAccelerometerLevel(data)
{
    var a = new Uint8Array(data);
    
    document.getElementById("accelerometerStatus").innerHTML =
	"Read accelerometer data";

}

function onReadBatteryLevel(data)
{
    var a = new Uint8Array(data);

    document.getElementById("bluetoothStatus").innerHTML = "Read battery from "+aDevice.name;

    // a[0] is apparently the battery level
    document.getElementById("batteryLevel").innerHTML = "Battery Level: "+a[0];
}

function connectDevice()
{
       document.getElementById("bluetoothStatus").innerHTML =
	   	   "Connecting to "+aDevice.name;

       ble.connect(aDevice.id,onDeviceConnect,onConnectError);
}

function readDevice()
{
    document.getElementById("bluetoothStatus").innerHTML = "Going to read from "+aDevice.name+", "+aDevice.id;
    
    ble.read(aDevice.id,battery.service,battery.level,onReadBatteryLevel,
	     onAppError);

    document.getElementById("bluetoothStatus").innerHTML = "Reading from "+
	aDevice.name;

    ble.read(aDevice.id,accelerometer.service,
	     accelerometer.level,
	     onReadAccelerometerLevel,
	     onAppError);

    document.getElementById("accelerometerStatus").innerHTML = "Reading accelerometer from "+
	aDevice.name;
    
}

function disconnectDevice()
{
    document.getElementById("bluetoothStatus").innerHTML =
   	   "Disconnecting from "+aDevice.name;

    ble.disconnect(aDevice.id,onDisconnected,onAppError);
}

function onDeviceConnect()
{
    document.getElementById("bluetoothStatus").innerHTML = "Connected to "+aDevice.name+", "+aDevice.id;

}

function onDisconnected()
{
    document.getElementById("bluetoothStatus").innerHTML = "Disconnected from "+aDevice.name+", "+aDevice.id;
}

function onConnectError(error)
{
    document.getElementById("bluetoothStatus").innerHTML = "Connect error "+error;
}

function foundDevice(device)
{
   body = document.getElementById("deviceTableBody");
   row = body.insertRow(-1);
	       
   td = row.insertCell(0);
   td.appendChild(document.createTextNode(device.name));

   td = row.insertCell(1);
   td.appendChild(document.createTextNode(device.id));
  
   td = row.insertCell(2);
   td.appendChild(document.createTextNode(device.rssi));

   numDevices++;

   // connect to the device and read battery state
   if ((device.name != "") && (device.name != "undefined")
	&& (device.name != null)) {
       aDevice = device;	
       document.getElementById("bluetoothDevice").innerHTML =
	   "Device "+aDevice.name+", "+aDevice.id;
   }
}

function scanError(error)
{
   document.getElementById("bluetoothStatus").innerHTML = "Error "+error;
}

function startScan()
{
   document.getElementById("bluetoothStatus").innerHTML =
	"Bluetooth: starting scan . . ";
    
   ble.scan([],5,foundDevice,scanError);
}

function onDeviceReady()
{
    document.getElementById("platformString").innerHTML =
	"Platform: "+device.platform;
    document.getElementById("modelString").innerHTML =
	"Model: "+device.model;
    document.getElementById("versionString").innerHTML =
	 "Vers: "+device.version;
    document.getElementById("virtualString").innerHTML =
	 "Is Virtual? "+device.isVirtual;
    document.getElementById("serialString").innerHTML =
	 "Serial #: "+device.serial;
    document.getElementById("manufacturerString").innerHTML =
	"Manufacturer: "+device.manufacturer;

    document.getElementById("connectionType").innerHTML =
	"Connection: "+navigator.connection.type;

   startScan();
}

document.addEventListener('deviceready',onDeviceReady,false);
connectButton.addEventListener('touchstart',connectDevice,false);
readBatteryButton.addEventListener('touchstart',readDevice,false);
disconnectButton.addEventListener('touchstart',disconnectDevice,false);
