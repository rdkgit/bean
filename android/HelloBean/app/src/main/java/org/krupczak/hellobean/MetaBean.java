// HelloBean
// java code using Punchthrough Design's SDK to talk to their
// bean arduino microcontroller
// Copyright 2017, Bobby Krupczak
// rdk@krupczak.org

// code borrowed, inspired, adapted from com.k1computing.hellobean;
// Module-Arduiino-BLE-Bean-master from https://github.com/kichoi
// https://github.com/kichoi/Mobile-Arduino-BLE-Bean

package org.krupczak.hellobean;

// Wrap a bean instance in a class that will poll it and keep track of it
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.message.Acceleration;
import com.punchthrough.bean.sdk.message.BatteryLevel;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.message.LedColor;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.message.ScratchData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.support.v7.widget.StaggeredGridLayoutManager.TAG;
import static com.punchthrough.bean.sdk.message.ScratchBank.BANK_1;

public class MetaBean implements BeanListener
{
    private Bean myBean;
    private String myName;
    private String myAddress;
    private Handler myHandler;
    private boolean pollBean;
    private Runnable myRunnable;
    private Context myContext;
    private boolean isConnected;
    private String TAG = "MetaBean";
    private String myHardwareVersion = null;
    private String myFirmwareVersion = null;
    private String mySoftwareVersion = null;
    private Acceleration lastAcceleration = null;
    private BatteryLevel lastBatteryLevel;
    private Integer lastTemperature;
    private boolean haveMoved = false;
    private double delta = 0.2;
    //private GattClient gc;
    private boolean readScratchDataBool = true;
    private ScratchBank aBank;
    private int lastMoisture;
    private boolean moistureDetected = false;
    private int moistureThreshold = 800;
    //private int temperatureThreshold = 37; 98.6F
    private int temperatureThreshold = 33;
    private boolean temperatureWarning = false;
    private boolean batteryWarning = false;
    private int batteryThreshold = 5;

    public MetaBean(Bean aBean, Context aContext)
    {
        myContext = aContext;
        myBean = aBean;
        myName = aBean.getDevice().getName();
        myAddress = aBean.getDevice().getAddress();
        pollBean = true;
        myHandler = new Handler();
        isConnected = false;
    }

    public Bean getBean() { return myBean; }

    public void connectBean()
    {
        myBean.connect(myContext,this);
    }

    public void stopPolling()
    {
        pollBean = false;
    }

    public void startPolling()
    {
        Log.d(TAG,"Starting to poll bean "+myName);

        pollBean = true;
        myRunnable = new Runnable() {
            @Override
            public void run() {
                pollBeanData();
            }
        };
        // call pollBean to kick it off
        myHandler.postDelayed(myRunnable, 100);
    };

    //public void createGattClient()
    //{
    //    gc = new GattClient(myHandler,myBean.getDevice());
    //    gc.connect(myContext,myBean.getDevice());
    //}
    //public GattClient getGattClient() { return gc; }
    //public void closeGattClient() { gc.disconnect(); gc.close(); }

    // if I'm already polling, start/stop reading scratch data
    public void startReadingScratchData() { readScratchDataBool = true; }
    public void stopReadingScratchData() { readScratchDataBool = false; }

    public void disconnectBean()
    {
        myBean.disconnect();
        isConnected = false;
    }

    private void readInventory()
    {
        myBean.readDeviceInfo(new Callback<DeviceInfo>() {
            @Override
            public void onResult(DeviceInfo devInfo) {
                Log.d(TAG,"Bean "+myName+" retrieved device info");
                myHardwareVersion = devInfo.hardwareVersion();
                myFirmwareVersion = devInfo.firmwareVersion();
                mySoftwareVersion = devInfo.softwareVersion();
            }
        });
    }

    private double calculateDelta(Acceleration last, Acceleration now)
    {
        double delta;

        if ((last == null) || (now == null))
            return 0.0;

        double deltaX = Math.abs(now.x() - last.x());
        double deltaY = Math.abs(now.y() - last.y());
        double deltaZ = Math.abs(now.z() - last.z());

        delta = deltaX + deltaY + deltaZ;

        Log.d(TAG,"MetaBean acceleration delta is "+delta);

        return delta;
    }

    public void pollBeanData()
    {
        if ((isConnected) && (pollBean == true)) {

            Log.d(TAG,"Going to query/ready bean data from "+myName);

            myBean.readAcceleration(new Callback<Acceleration>() {
                @Override
                public void onResult(Acceleration result) {
                    Log.d(TAG,"Accelartion "+result+" read from "+myName);
                    if (lastAcceleration != result) {
                        if (calculateDelta(lastAcceleration,result) > delta) {
                            haveMoved = true;
                            Log.d(TAG,"MetaBean detected movement from "+myName);
                        }
                        else {
                            haveMoved = false;
                            Log.d(TAG,"MetaBean detected no movement from "+myName);
                        }
                    }
                    lastAcceleration = result;
                }
            });

            myBean.readBatteryLevel(new Callback<BatteryLevel>() {
                @Override
                public void onResult(BatteryLevel aLevel) {
                    Log.d(TAG,"Battery level "+aLevel+" read from "+myName);
                    lastBatteryLevel = aLevel;
                    if (lastBatteryLevel.getPercentage() <= batteryThreshold) {
                        Log.d(TAG,"MetaBean: battery percentage warning "+lastBatteryLevel.getPercentage()+" from "+myName);
                        batteryWarning = true;
                    }
                }
            });
            myBean.readTemperature(new Callback<Integer>() {
                @Override
                public void onResult(Integer temp) {
                    Log.d(TAG,"Temperature "+temp+" read from "+myName);
                    lastTemperature = temp;
                    if (lastTemperature >= temperatureThreshold) {
                        Log.d(TAG,"Temperature warning from "+myName);
                        temperatureWarning = true;
                    }
                    else {
                        temperatureWarning = false;
                    }
                }

            });

            if (readScratchDataBool) {
                myBean.readScratchData(BANK_1, new Callback<ScratchData>() {
                   @Override
                   public void onResult(ScratchData result)
                   {
                       ByteBuffer bb;

                       //Log.d(TAG,"Read Scratch Data "+result);
                       //Log.d(TAG,"Read Scratch Data number: "+result.number());
                       //Log.d(TAG,"Read Scratch Data length "+result.data().length);
                       // if no moisture sensor is detected, we still get a reading XXX

                       bb = ByteBuffer.wrap(result.data());
                       bb.order(ByteOrder.LITTLE_ENDIAN);
                       lastMoisture = bb.getInt();
                       Log.d(TAG,"Read Scratch Data conversion: "+lastMoisture+" from "+myName);
                       // when no moisture detected, the value is around 1023
                       // when moisture detected, connection is made and
                       // value drops to under 800 or so
                       if (lastMoisture <= moistureThreshold) {
                           moistureDetected = true;
                       }
                       else {
                           moistureDetected = false;
                       }
                   }
                });
            } // if readScratchDataBool

            myHandler.postDelayed(myRunnable, 1000);

        } // if isConnected

    } // pollBeanData

    private void setLedColor(LedColor aColor) {  }

    public void turnOffLed()
    {
        if (isConnected) {
            LedColor ledColor = LedColor.create(0, 0, 0);
            myBean.setLed(ledColor);
        }
    }

    public void turnOnLed()
    {
        if (isConnected) {
            LedColor ledColor = LedColor.create(0, 100, 0);
            myBean.setLed(ledColor);
        }
    }

    public boolean isBeanConnected() { return isConnected; }

    public String getBeanName() { return myName; }
    public String getBeanAddress() { return myAddress; }
    public String getBeanHardwareVersion() { return myHardwareVersion; }
    public String getBeanSoftwareVersion() { return mySoftwareVersion; }
    public String getBeanFirmwareVersion() { return myFirmwareVersion; }
    public BatteryLevel getBeanBatteryLevel() { return lastBatteryLevel; }
    public Integer getBeanTemperature() { return lastTemperature; }
    public int getMoisture() { return lastMoisture; }
    public Acceleration getBeanAcceleration() { return lastAcceleration; }
    public boolean detectMovement() { return haveMoved; }
    public int getMoistureThreshold() { return moistureThreshold; }
    public void setMoistureThreshold(int anInt) { moistureThreshold = anInt; }
    public boolean isMoistureDetected() { return moistureDetected; }
    public int getTemperatureThreshold() { return temperatureThreshold; }
    public void setTemperatureThreshold(int anInt) { temperatureThreshold = anInt; }
    public boolean isTemperatureWarning() { return temperatureWarning; }
    public boolean isBatteryLow() { return batteryWarning; }
    public int getBatteryThreshold() { return batteryThreshold; }
    public void setBatteryThreshold(int anInt) { batteryThreshold = anInt; }

    @Override
    public void onConnected()
    {
        Log.d(TAG,"Bean "+myName+" connection succeeded");
        isConnected = true;

        readInventory();

    }

    @Override
    public void onConnectionFailed()
    {
        Log.d(TAG,"Bean "+myName+" connection failed");
        isConnected = false;
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG,"Bean "+myName+" is disconnected");
        isConnected = false;
    }

    @Override
    public void onError(BeanError error)
    {
        Log.d(TAG,"Bean "+myName+" encountered error "+error);
    }

    @Override
    public void onReadRemoteRssi(int rssi) { }


    @Override
    public void onScratchValueChanged(ScratchBank bank, byte[] value)
    {
        //Log.d(TAG,"onScratchValueChanged");
        //Log.d(TAG,"bank: "+bank+"\tvalue: "+value);

        //appendText("Scratch value changed\n");

    }

    @Override
    public void onSerialMessageReceived(byte[] data)
    {
        //Log.d(TAG,"Bean "+myName+" serial message received of "+data.length+" bytes");
    }


} // BeanWrapper

