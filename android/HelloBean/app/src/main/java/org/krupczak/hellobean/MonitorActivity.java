// HelloBean
// java code using Punchthrough Design's SDK to talk to their
// bean arduino microcontroller
// Copyright 2017, Bobby Krupczak
// rdk@krupczak.org

// code borrowed, inspired, adapted from com.k1computing.hellobean;
// Module-Arduiino-BLE-Bean-master from https://github.com/kichoi
// https://github.com/kichoi/Mobile-Arduino-BLE-Bean

package org.krupczak.hellobean;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanManager;

import java.util.ArrayList;
import java.util.List;

public class MonitorActivity extends AppCompatActivity implements BeanDiscoveryListener
{
    final String TAG = "BeanMonitor";
    final List<MetaBean> beanList = new ArrayList<>();
    TextView textView = null;
    BeanManager beanManager;
    Handler aHandler;
    ImageView imageView;
    boolean pollBeans = false;
    Runnable myRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"BeanMonitor onCreate");

        setContentView(R.layout.activity_monitor);

        textView = (TextView)findViewById(R.id.monitor_text);
        beanManager = BeanManager.getInstance();
        aHandler = new Handler();
        imageView = (ImageView)findViewById(R.id.status_icon);

    } // onCreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_monitor, menu);
        return true;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG,"MonitorActivity onResume");

        // configure bean manager scan timeout and start scanning
        beanManager.setScanTimeout(5);
        beanManager.startDiscovery(this);

        clearText();
        appendText("Starting monitoring discovery ");
        Log.d(TAG,"Starting monitoring");

    }

    @Override
    protected void onPause()
    {
        Log.d(TAG,"MonitorActivity onPause");
        refreshBeans();

        super.onPause();
    }

    @Override
    protected void onStop()
    {
        Log.d(TAG,"MonitorActivity onStop");

        super.onStop();
    }

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

    private void refreshBeans()
    {
        // stop polling all beans
        pollBeans = false;

        // turn off led
        // disconnect
        for (MetaBean aMetaBean: beanList) {
            aMetaBean.turnOffLed();
            aMetaBean.stopPolling();
            aMetaBean.disconnectBean();
        }
        beanManager.cancelDiscovery();
        beanManager.forgetBeans();

        // empty our bean list
        beanList.clear();

    } // refreshBeans


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
        appendText(" complete\n");
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
            // set icon
            imageView.setImageResource(R.mipmap.status_ok);
        }

    } // onDiscoveryComplete

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

    public void startPollingBeans()
    {
        Log.d(TAG,"Going to start monitoring beans");
        // turn on all leds and start polling
        // for each bean, kick off polling
        for (MetaBean aMetaBean: beanList) {
            pollBeans = true;
            aMetaBean.turnOnLed();
            aMetaBean.startPolling();
        }

        // now, start our own polling of MetaBean status
        myRunnable = new Runnable() {
            @Override
            public void run() {
                pollMetaBeans();
            }
        };
        // call pollBean to kick it off
        aHandler.postDelayed(myRunnable, 100);
    }

    public void stopPollingBeans()
    {
        Log.d(TAG,"Going to stop monitoring beans");

        // turn off all LEDs
        // for each bean, stop polling
        pollBeans = false;
        for (MetaBean aMetaBean: beanList) {
            aMetaBean.turnOffLed();
            aMetaBean.stopPolling();
        }
    }

    void pollMetaBeans()
    {
        Log.d(TAG,"Monitor polling metabean status");
        boolean haveMoved = false;
        boolean moistureDetected = false;

        if (pollBeans == true) {

            for (MetaBean aMetaBean: beanList) {
                if (aMetaBean.detectMovement() == true) {
                    haveMoved = true;
                }
            }

            for (MetaBean aMetaBean: beanList) {
                if (aMetaBean.isMoistureDetected()) {
                    moistureDetected = true;
                }
            }

            if ((haveMoved == true) || (moistureDetected == true)) {
                Log.d(TAG,"Monitor setting status to bad");
                if (moistureDetected)
                    imageView.setImageResource(R.mipmap.moisture512);
                else
                    imageView.setImageResource(R.mipmap.movement512);
            }

            if ((haveMoved == false) && (moistureDetected == false)) {
                Log.d(TAG,"Monitor setting status to OK");
                imageView.setImageResource(R.mipmap.status_ok);
            }

            aHandler.postDelayed(myRunnable, 1000);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        View popupView;
        int ret;

        int id = item.getItemId();

        // refresh and re-scan
        if (id == R.id.action_refresh) {
        }
        if (id == R.id.action_start_monitoring) {
            appendText("Starting/re-starting bean polling\n");
            startPollingBeans();
        }
        if (id == R.id.action_stop_monitoring) {
            appendText("Stopping bean polling\n");
            stopPollingBeans();
        }

        return super.onOptionsItemSelected(item);

    } // onOptionsItemSelected


} // MonitorActivity
