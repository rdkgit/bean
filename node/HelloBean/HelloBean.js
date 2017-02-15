// HelloBean.js

// use ble-bean module to talk to various beans
// or, use bean-sdk to talk to various beans
// in Nodejs

var lightblue = require('bean-sdk');

var Bean = lightblue.sdk();

Bean.on('discover',function(scannedDevice) {
    Bean.connectScannedDevice(scannedDevice,function(err,bean) {

	if (err) {
            console.log("bean connection failed: "+err);
	}
	else {
	    console.log("Found bean "+bean.getName()+", "+bean.getAddress());
	    console.log("Bean is connected "+bean.isConnected());
	    
	    bean.lookupServices(function(err) {

		var aService,aService1;

		aService = bean.getDeviceInformationService();
		console.log("Got device info service "+aService);
		aService.getModelNumber(function(err,result) {
                    console.log("Device model "+result);
		});
		aService.getFirmwareVersion(function(err,result) {
                    console.log("Firmware version "+result);
		});
		aService1 = bean.getBatteryService();
		console.log("Got battery service "+aService1+" for "+bean.getName());
		if (aService1 != null) {
  	   	   aService1.getVoltage(function(err,result) {
                       console.log("Battery voltage "+result); 
	   	   });
		}
		
 	       bean.setLed(0,0,255,function(err) {
	  	   if (err) { console.log("Error setting bean led"); }
		   console.log("Set led on bean "+bean.getName());
	       });

	       console.log("Going to read accelerometer from "+bean.getName());

	       bean.readAccelerometer(function(err,results) {

		  if (error) {
		     console.log("bean read accelerometer failed "+err);
	 	  }
		  else {
		    console.log("bean read accelerometer!!!");
		  }
               }); // readAccel

	    }); // lookupServices

	} // else

    }); // connect

}); // on 

Bean.startScanning();
