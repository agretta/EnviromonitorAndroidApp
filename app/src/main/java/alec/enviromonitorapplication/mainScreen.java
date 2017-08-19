package alec.enviromonitorapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.WindowDecorActionBar;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jjoe64.graphview.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class mainScreen extends AppCompatActivity {
    private TextView mTextMessage;
    private Handler mHandler; // handler that gets info from Bluetooth service

    private BluetoothAdapter BA;

    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "Debug";
    public static final String SETTINGS_FILE = "EnviroMonitorSettings";
    public static final String ARRAY_STORAGE_FILE = "EnvMonitorStorage";

    private ArrayList<BluetoothDevice> pairedDevices;
    private ListView lv;
    private AlertDialog dialog;

    private List<List<EnvData>> enviromentDataLists;
    private List<EnvData> enviromentData;

    private List<Entry> temperatureEntries;
    private List<Entry> humidityEntries;

    private long readPeriod;

    private List<Integer> activities;
    private List<String> addresses;
    private List<String> names;

    private LineChart t_chart;
    private LineChart h_chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Sets the initial view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        View view = getLayoutInflater().inflate(R.layout.popup, null);
        lv = (ListView)view.findViewById(R.id.listView);


        mTextMessage = (TextView) findViewById(R.id.message);
        mTextMessage.setText("Main Screen");
        BA = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new ArrayList<>();

        //Load stuff from memory

        enviromentDataLists = new LinkedList<>();

        addresses = new LinkedList<>();
        names = new LinkedList<>();
        activities = new ArrayList<>();

        load();
        on();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long year = cal.getTimeInMillis();

        //populateGraph(temperatureEntries, humidityEntries, enviromentData);

        if(BA.isEnabled()) {
            BA.startDiscovery();
            for(int i = 0; i < addresses.size(); i++) {
                //mTextMessage.setText(name);
                if (addresses.get(i) != null) {
                    Intent intent;
                    if(activities.get(i) == 1) {
                        intent = new Intent(mainScreen.this, GraphView.class);
                        intent.putExtra("address", addresses.get(i));
                    } else {
                        intent = new Intent(mainScreen.this, TiltGraph.class);
                        intent.putExtra("address", addresses.get(i));
                    }

                    startActivity(intent);
                }
            }
            BA.cancelDiscovery();
        }


        //Enables bluetooth and displays the list of paired devices
        final Button connectButton = (Button) findViewById(R.id.Connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                list();
            }
        });

        //Connect to selected device
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(getApplicationContext(), "Click ListItem Number " + position, Toast.LENGTH_LONG).show();
                dialog.dismiss();

                BluetoothDevice bt = pairedDevices.get(position);
                addresses.add(bt.getAddress());
                names.add(bt.getName());

                Log.d(TAG, "Size:" + addresses.size());
                Log.d(TAG, "Connected To " + bt.getName() + " : " + bt.getAddress());

                Intent intent;
                if(activities.get(position) == 1) {
                    intent = new Intent(mainScreen.this, GraphView.class);
                    intent.putExtra("address", bt.getAddress());
                } else {
                    intent = new Intent(mainScreen.this, TiltGraph.class);
                    intent.putExtra("address", bt.getAddress());
                }

                startActivity(intent);

                BA.cancelDiscovery();
            }
        });


        //Disconnects the paired bluetooth device
        final Button disconnectButton = (Button) findViewById(R.id.Disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getApplication().deleteFile(ARRAY_STORAGE_FILE);
                enviromentData.clear();
                //off();
            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    public void off(){
        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }

    public void list(){
        ((ViewGroup) lv.getParent()).removeView(lv);
        pairedDevices.clear();

        BA.startDiscovery();
        pairedDevices.addAll(BA.getBondedDevices());
        ArrayList<String> list = new ArrayList<>();

        for(BluetoothDevice bt : pairedDevices) {
            list.add(bt.getName());
        }

        Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new ArrayAdapter(mainScreen.this, android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);

        //AlertDialog.Builder builder = new AlertDialog.Builder(mainScreen.this);
        AlertDialog.Builder builder = new AlertDialog.Builder(mainScreen.this);
        //builder.setCancelable(true);

        //Clicking connect twice kills app, parent needs to be removed

        builder.setView(lv);
        dialog = builder.create();
        dialog.show();
    }

    /**
     * Function that accesses the stored memory of this device.
     * Loads:
     *      - Address of connected bluetooth device
     *      - Name of connected bluetooth device
     *      - current sensor reading period
     *      - temperature data
     *      - humidity data
     * if a setting doesn't exist, then it is loaded with a default value
     */
    private void load() {
        SharedPreferences settings = getSharedPreferences(SETTINGS_FILE, 0);


        Log.d(TAG, "Size:" + addresses.size());
        int numDevices = settings.getInt("numDevices", 0);
        numDevices = 2;
        for(int i = 0; i < numDevices; i++) {
            addresses.add(settings.getString("bluetoothAddress" + i, null));
            names.add(settings.getString("bluetoothName"+i, null));
            activities.add(settings.getInt("activities"+i, -1));
            //Log.e(TAG, "NumDevices" + numDevices + " //ACT" + activities.get(i));
        }
        activities.set(0, 0);
        activities.set(1, 1);

        readPeriod = settings.getLong("readPeriod", 3000);
        //this.deleteFile(ARRAY_STORAGE_FILE);

        try
        {
            FileInputStream fis = openFileInput(ARRAY_STORAGE_FILE);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Gson gson = new Gson();
            String json = (String) ois.readObject();
            enviromentDataLists = gson.fromJson(json, new TypeToken<ArrayList<ArrayList<EnvData>>>(){}.getType());
            ois.close();
            fis.close();
            Log.d(TAG, "Loading Old Data" + json);
        }catch(FileNotFoundException e){
            Log.e(TAG, "App did not have a file", e);
        } catch (Exception e) {
            Log.e(TAG, "App did not load properly", e);
        }
    }

    @Override
    protected void onPause() {

        SharedPreferences settings = getSharedPreferences(SETTINGS_FILE, 0);
        SharedPreferences.Editor sEditor = settings.edit();

        for(int i = 0; i < addresses.size(); i++) {
            sEditor.putString("bluetoothAddress"+i, addresses.get(i));
            sEditor.putString("bluetoothName"+i, names.get(i));
            sEditor.putInt("activities"+i, activities.get(i));
        }

        sEditor.putInt("numDevices", addresses.size());
        sEditor.putLong("readPeriod", readPeriod);
        //Will clear SharedPreferences
        /*if(true) {
            sEditor.clear();
        }*/

        sEditor.apply();

        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<ArrayList<EnvData>>>() {}.getType();
            String json = gson.toJson(enviromentDataLists, listType);
            FileOutputStream fos = openFileOutput(ARRAY_STORAGE_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(json);
            oos.close();
            fos.close();
            Log.d(TAG, "Saving Old Data");

        }
        catch (IOException e) {
            Log.e(TAG, "App did not save properly", e);
        }

        //add a save command to arduino

        super.onPause();
    }

    /**
     * This function populates the temperature and humidity entry list
     * @param tempEntries
     * @param huEntries
     * @param data
     */
    private void populateGraph(List<Entry> tempEntries, List<Entry> huEntries, List<EnvData> data) {
        Calendar cal = Calendar.getInstance();
        if(data.size() == 0) {
            data.add(new EnvData(cal.getTimeInMillis(), 25, 45));
        }
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        //cal.add(Calendar.SECOND, -);

        for(EnvData d : data) {
            cal.setTimeInMillis(d.getTime());
            float ftime = convertTime(cal.getTimeInMillis());

            int time = (int)ftime;
            int seconds = time % 60;
            int minutes = (time/60) % 60;
            int hours = (time/3600) % 60;
            int day = (time/(3600 * 24)) % 365;


            Calendar calendar = Calendar.getInstance();
            //calendar.set(cal.get(Calendar.YEAR),0,0,0,0,0);
            calendar.set(Calendar.DAY_OF_YEAR, day);
            calendar.set(Calendar.HOUR_OF_DAY, hours);
            calendar.set(Calendar.MINUTE, minutes);
            calendar.set(Calendar.SECOND, seconds);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/M/Y hh:mm:ss a");
            Log.d(TAG, "Time: " + dateFormat.format(calendar.getTimeInMillis()));
            //Log.d(TAG, "X loc: " + time + " | " + seconds + " | " + minutes + " | " + hours + " | " + day);

            tempEntries.add(new Entry(ftime, d.getTemperature()));
            huEntries.add(new Entry(ftime, d.getHumidity()));
        }



    }

    //Bluetooth Functionality
    public void on(){
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Get the last recorded time
        Calendar cal = Calendar.getInstance();
        long time = enviromentData.get(enviromentData.size()-1).getTime();
        cal.setTimeInMillis(time);

        switch (item.getItemId()) {
            case R.id.switch_screen:
                startActivity(new Intent(this, GraphView.class));
                return true;

            case R.id.hour_view:
                cal.set(Calendar.MINUTE,0);
                cal.set(Calendar.SECOND,0);

                float startChartTime = convertTime(cal.getTimeInMillis());

                t_chart.setVisibleXRangeMinimum(60);
                t_chart.setVisibleXRangeMaximum(60 * 60);

                t_chart.moveViewToX(startChartTime);
                return true;
            case R.id.day_view:

                cal.set(Calendar.HOUR_OF_DAY,0);
                cal.set(Calendar.MINUTE,0);
                cal.set(Calendar.SECOND,0);

                startChartTime = convertTime(cal.getTimeInMillis());

                t_chart.setVisibleXRangeMinimum(60 * 60);
                t_chart.setVisibleXRangeMaximum(60 * 60 * 24);


                t_chart.moveViewToX(startChartTime);
                return true;

            case R.id.week_view:
                cal.set(Calendar.DAY_OF_WEEK,1);
                cal.set(Calendar.HOUR_OF_DAY,0);
                cal.set(Calendar.MINUTE,0);
                cal.set(Calendar.SECOND,0);

                startChartTime = convertTime(cal.getTimeInMillis());

                t_chart.setVisibleXRangeMinimum(60 * 60 * 4);
                t_chart.setVisibleXRangeMaximum(60 * 60 * 24);

                t_chart.moveViewToX(startChartTime);
                return true;
            case R.id.month_view:
                cal.set(Calendar.DAY_OF_MONTH,1);
                cal.set(Calendar.HOUR_OF_DAY,0);
                cal.set(Calendar.MINUTE,0);
                cal.set(Calendar.SECOND,0);

                startChartTime = convertTime(cal.getTimeInMillis());

                t_chart.setVisibleXRangeMinimum(60 * 60 * 24);
                t_chart.setVisibleXRangeMaximum(60 * 60 * 24 * 31);

                t_chart.moveViewToX(startChartTime);
                return true;
            case R.id.year_view:
                cal.set(Calendar.DAY_OF_YEAR,1);
                cal.set(Calendar.HOUR_OF_DAY,0);
                cal.set(Calendar.MINUTE,0);
                cal.set(Calendar.SECOND,0);

                startChartTime = convertTime(cal.getTimeInMillis());

                t_chart.setVisibleXRangeMinimum(60 * 60 * 24 * 31);
                t_chart.setVisibleXRangeMaximum(60 * 60 * 24 * 365);

                t_chart.moveViewToX(startChartTime);
                return true;
            case R.id.options:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private float convertTime(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        float convertedTime = cal.get(Calendar.DAY_OF_YEAR) * (3600*24)
                + cal.get(Calendar.HOUR_OF_DAY) * (3600)
                + cal.get(Calendar.MINUTE) * (60)
                + cal.get(Calendar.SECOND);
        return convertedTime;
    }


    /**
     * This private inner class handles connecting a bluetooth device to the application
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;


            try {
                // Get a socket to connect with the given device.
                //tmp = device.createRfcommSocketToServiceRecord(uuid);
                tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            Log.i(TAG, "Connected successfully to " + mmSocket.getRemoteDevice().getName() + " " + mmSocket.getRemoteDevice().getAddress() + ".");
        }

        public void run() {
            BA.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();

            } catch (IOException connectException) {
                Log.d(TAG, "Device Failed to Connect: " + mmDevice.getAddress());
                connectException.printStackTrace();
                cancel();
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            ConnectedThread connected = new ConnectedThread(mHandler, mmSocket, readPeriod, enviromentData);
            connected.start();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }



}