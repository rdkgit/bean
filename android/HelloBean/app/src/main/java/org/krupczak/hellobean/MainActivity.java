// HelloBean
// java code using Punchthrough Design's SDK to talk to their
// bean arduino microcontroller
// Copyright 2017, Bobby Krupczak
// rdk@krupczak.org

// code borrowed, inspired, adapted from com.k1computing.hellobean;
// Module-Arduiino-BLE-Bean-master from https://github.com/kichoi
// https://github.com/kichoi/Mobile-Arduino-BLE-Bean

package org.krupczak.hellobean;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.Acceleration;
import com.punchthrough.bean.sdk.message.BatteryLevel;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.message.LedColor;
import com.punchthrough.bean.sdk.message.ScratchBank;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BeanDiscoveryListener
{
    final String TAG = "HelloBlueBean";
    final List<MetaBean> beanList = new ArrayList<>();
    TextView textView =null;
    BeanManager beanManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG,"MainActivity onCreate");

        textView = (TextView)findViewById(R.id.main_text);

        // get a bean manager and configure scan timeout
        beanManager = BeanManager.getInstance();

    } //onCreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume()
    {
        Log.d(TAG,"MainActivity onResume");

        beanManager.forgetBeans();
        beanManager.setScanTimeout(5);
        beanManager.startDiscovery(this);

        clearText();
        appendText("Starting Bluebean discovery ...\n");
        Log.d(TAG,"Starting Bluebean discovery ...");

        super.onResume();
    }

    @Override
    protected void onStop()
    {
        Log.d(TAG,"MainActivity onStop");

        super.onStop();
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG,"MainActivity onPause");

        refreshBeans();

        super.onPause();
    }

    @Override
    public void onBeanDiscovered(Bean bean, int rssi)
    {
        Log.d(TAG,"A bean is found: "+bean);

        //appendText("Found a bean "+bean.describe()+"\n");
        //appendText(""+bean.getDevice().getName()+" address: "+bean.getDevice().getAddress());
        //appendText("\n");

        appendText(".");

        // only add bean if its not already in our list
        if (isBeanOnList(bean) == false) {
            appendText("+");
            beanList.add(new MetaBean(bean, this));
        }

    } // onBeanDiscovered

    @Override
    public void onDiscoveryComplete() {
        appendText("\n");
        appendText("Discovery complete\n");
        appendText("\n");
        appendText("Found "+beanList.size()+" beans\n");

        for (MetaBean aMetaBean : beanList) {
            Log.d(TAG, "Bean name: "+aMetaBean.getBeanName());
            Log.d(TAG, "Bean address: "+aMetaBean.getBeanAddress());
            appendText("Bean: "+aMetaBean.getBeanName()+", address: "+aMetaBean.getBeanAddress()+"\n");
        }

        if (beanList.size() > 0) {
            for (MetaBean aBean : beanList) {
                 appendText("Going to connect to bean "+aBean.getBeanName()+"\n");
                 aBean.connectBean();
             }
        }
    }

    private boolean isBeanOnList(Bean aBean)
    {
        for (MetaBean aMetaBean: beanList) {
            if (aMetaBean.getBean() == aBean)
            {
                return true;
            }
        }

        return false;
    }

    private void disconnectAllBeans()
    {
        // disconnect everything
        for (MetaBean aMetaBean: beanList) {
            if (aMetaBean.isBeanConnected()) {
                aMetaBean.disconnectBean();
            }
        }
    }

    private void turnOnAllLeds()
    {
        for (MetaBean aMetaBean : beanList) {
            aMetaBean.turnOnLed();
        }

    }

    private void turnOffAllLeds()
    {
        for (MetaBean aMetaBean : beanList) {
            aMetaBean.turnOffLed();
        }

    }

    private void showInventory()
    {
        BatteryLevel aLevel;
        Integer aTemp;

        appendText("I have "+beanList.size()+" beans in my inventory\n");
        for (MetaBean aMetaBean : beanList) {
            appendText("Bean: "+aMetaBean.getBeanName()+"\n");
            appendText("      Connected: "+aMetaBean.isBeanConnected()+"\n");
            appendText("      Addr: "+aMetaBean.getBeanAddress()+"\n");
            appendText("      HWVers: "+aMetaBean.getBeanHardwareVersion()+"\n");
            appendText("      FWVers: "+aMetaBean.getBeanFirmwareVersion()+"\n");
            appendText("      SWVers: "+aMetaBean.getBeanSoftwareVersion()+"\n");
            if ((aTemp = aMetaBean.getBeanTemperature()) != null) {
                appendText("      Temp: " + aMetaBean.getBeanTemperature() + "\n");
            }
            aLevel = aMetaBean.getBeanBatteryLevel();
            if (aLevel != null) {
                appendText("      Battery: " + aLevel.getPercentage()+"% or "+aLevel.getVoltage()+"v\n");
            }
        }

    }

    private void refreshBeans()
    {
        // stop polling all beans
        // turn off led
        // disconnect
        for (MetaBean aMetaBean: beanList) {
            aMetaBean.turnOffLed();
            aMetaBean.stopPolling();
            aMetaBean.disconnectBean();
        }
        beanManager.forgetBeans();

        // empty our bean list
        beanList.clear();

    } // refreshBeans

    public void startPollingBeans()
    {
        // turn on all leds and start polling
        // for each bean, kick off polling
        for (MetaBean aMetaBean: beanList) {
            aMetaBean.turnOnLed();
            aMetaBean.startPolling();
        }
    }

    public void stopPollingBeans()
    {
        // turn off all LEDs
        // for each bean, stop polling
        for (MetaBean aMetaBean: beanList) {
            aMetaBean.turnOffLed();
            aMetaBean.stopPolling();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent;
        View popupView;
        int ret;

        //Log.d(TAG,"onOptionsItemSelected starting");
        //appendText("Option item selected\n");

        int id = item.getItemId();

        // refresh and re-scan
        if (id == R.id.action_refresh) {
            clearText();
            appendText("Disconnecting and starting Bluebean discovery ...\n");

            refreshBeans();
            beanManager.setScanTimeout(5);
            beanManager.startDiscovery(this);
        }
        if (id == R.id.action_about) {


        }
        if (id == R.id.action_start) {
            appendText("Starting/re-starting bean polling\n");
            startPollingBeans();
        }
        if (id == R.id.action_stop) {
            appendText("Stopping bean polling\n");
            stopPollingBeans();
        }

        if (id == R.id.action_ledoff) {
            appendText("Turning off all LEDs\n");
            turnOffAllLeds();
        }

        if (id == R.id.action_ledon) {
            appendText("Turning on all LEDs\n");
            turnOnAllLeds();
        }

        if (id == R.id.action_inventory) {
            appendText("Bean inventory\n");
            showInventory();
        }

        if (id == R.id.action_monitor) {
            refreshBeans();
            intent = new Intent(getApplicationContext(),MonitorActivity.class);
            // pack up our list of MetaBeans to send to monitor
            Bundle b = new Bundle();
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);


    } // onOptionsItemSelected


    protected void appendText(final String aStr)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.append(aStr);
            }
        });

    } // appendText to textView but do so on UI thread

    public void clearText()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText("");
            }
        });

    } // clear textView but do so on UI thread

} // class MainActivity

