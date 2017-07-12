package alec.enviromonitorapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
    private View view;
    private AlertDialog dialog;

    private List<EnvData> enviromentData;

    private List<Entry> temperatureEntries;
    private List<Entry> humidityEntries;

    private Calendar calendar = Calendar.getInstance();
    private long readPeriod;

    private String address;
    private String name;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Sets the initial view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        mTextMessage = (TextView) findViewById(R.id.message);

        //Load from memory
        temperatureEntries = new LinkedList<>();
        humidityEntries = new LinkedList<>();

        enviromentData = new LinkedList<>();
        load();



        pairedDevices = new ArrayList<>();

        BA = BluetoothAdapter.getDefaultAdapter();

        view = getLayoutInflater().inflate(R.layout.popup, null);
        lv = (ListView)view.findViewById(R.id.listView);

        on();

        //Graph Set Up
        /*for(int i = 0; i < 100; i++) {
            humidityEntries.add(new Entry(calendar.getTimeInMillis(), 50 + (int)(Math.random() * 20)-10));
            calendar.add(Calendar.SECOND,3);
        }*/

        //----------Temperatures Chart----------
        temperatureEntries.add(new Entry(calendar.getTimeInMillis(),20));
        populateGraph(temperatureEntries, humidityEntries, enviromentData);
        final LineChart t_chart = (LineChart) findViewById(R.id.t_chart);

        final LineDataSet temperaturesDataSet = new LineDataSet(temperatureEntries, "Temperatures");
        temperaturesDataSet.setColor(Color.RED);
        temperaturesDataSet.setDrawHighlightIndicators(false);
        final LineData temperaturesLineData = new LineData(temperaturesDataSet);
        t_chart.setData(temperaturesLineData);
        temperaturesDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        temperaturesLineData.setDrawValues(false);
        temperaturesDataSet.setDrawCircles(false);
        //temperaturesDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        temperaturesDataSet.setDrawFilled(true);

        //Formating
        XAxis xaxis  = t_chart.getXAxis();
        xaxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xaxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                SimpleDateFormat mFormat = new SimpleDateFormat("hh:mm");
                return mFormat.format(value);
            }
        });

        YAxis laxis  = t_chart.getAxisLeft();
        laxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                DecimalFormat formatter = new DecimalFormat("###,###,##0");
                return formatter.format(value) + " °C";
            }
        });
        t_chart.getAxisRight().setEnabled(false);
        laxis.setAxisMinimum(-10f); // start at zero
        laxis.setAxisMaximum(40f); // the axis maximum is 100

        t_chart.setDescription(null);

        //t_chart.setVisibleXRange(enviromentData.get(0).getTime(), enviromentData.get(enviromentData.size()-1).getTime());

        t_chart.invalidate(); // refresh
        //----------Temperatures Chart----------


        //----------Humidities Chart----------
        final LineChart h_chart = (LineChart) findViewById(R.id.h_chart);
        humidityEntries.add(new Entry(calendar.getTimeInMillis(),50));

        final LineDataSet humiditiesDataSet = new LineDataSet(humidityEntries, "Humidities");
        humiditiesDataSet.setColor(Color.BLUE);
        humiditiesDataSet.setDrawHighlightIndicators(false);
        final LineData humiditiesLineData = new LineData(humiditiesDataSet);
        humiditiesLineData.setDrawValues(false);
        humiditiesDataSet.setDrawCircles(false);
        //humiditiesDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        humiditiesDataSet.setDrawFilled(true);
        h_chart.setData(humiditiesLineData);
        humiditiesDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

        XAxis hXAxis  = h_chart.getXAxis();
        hXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        hXAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                SimpleDateFormat mFormat = new SimpleDateFormat("hh:mm");
                return mFormat.format(value);
            }
        });

        YAxis hYAxis  = h_chart.getAxisLeft();
        hYAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                //DecimalFormat formatter = new DecimalFormat("###,###,##0");
                return Float.toString(value) + "%";
            }
        });

        h_chart.getAxisRight().setEnabled(false);
        hYAxis.setAxisMinimum(0f); // start at zero
        hYAxis.setAxisMaximum(100f); // the axis maximum is 100
        //raxis.setGranularity(5f); // interval 1
        //raxis.setLabelCount(10, true); // force 6 labels

        h_chart.setDescription(null);
        h_chart.invalidate(); // refresh
        //----------Humidities Chart----------


        mTextMessage.setText(name);
        if(address != null) {
            Log.d(TAG,"Attemmping to start new thread" + address);
            ConnectThread connect = new ConnectThread(BA.getRemoteDevice(address));
            connect.start();
        }
        //populateGraph(mainGraph);

        //Connect to selected device
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(getApplicationContext(), "Click ListItem Number " + position, Toast.LENGTH_LONG).show();
                dialog.dismiss();

                BluetoothDevice bt = pairedDevices.get(position);
                address = bt.getAddress();
                name = bt.getName();
                Log.d(TAG, bt.getAddress());
                Log.d(TAG, bt.getName());

                ConnectThread thread = new ConnectThread(bt);
                thread.start();
            }
        });

        //Handle messages passed from the device ConnectedThread
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == ConnectedThread.MessageConstants.MESSAGE_READ) {
                    if(msg.arg2 == 'u') {
                        byte[] message = (byte[]) msg.obj;
                        String s = " " + message[1] + " " + message[2];
                        Log.d(TAG, s);
                        long time = calendar.getTimeInMillis();
                        temperaturesDataSet.addEntry(new Entry(time, message[1]));
                        temperaturesLineData.notifyDataChanged();

                        humiditiesDataSet.addEntry(new Entry(time, message[2]));
                        humiditiesLineData.notifyDataChanged();

                        t_chart.notifyDataSetChanged();
                        t_chart.invalidate(); // refresh
                        h_chart.notifyDataSetChanged();
                        h_chart.invalidate(); // refresh

                        enviromentData.add(new EnvData(time, message[1], message[2]));

                        //calendar.add(Calendar.SECOND, 30);
                        mTextMessage.setText(s + " " + enviromentData.size() + " " + temperatureEntries.get(temperatureEntries.size()-1).getX());
                        //saveGraphs();
                    }
                }
            }
        };

        //Enables bluetooth and displays the list of paired devices
        final Button connectButton = (Button) findViewById(R.id.Connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                list();
            }
        });

        //Disconnects the paired bluetooth device
        final Button disconnectButton = (Button) findViewById(R.id.Disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                off();
            }
        });

    }

    private void saveGraphs() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();

        String json = gson.toJson(temperatureEntries);

        editor.putString(TAG, json);
        editor.apply();
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
                tmp = device.createRfcommSocketToServiceRecord(uuid);
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
                // Unable to connect; close the socket and return.
                cancel();
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            ConnectedThread connected = new ConnectedThread(mHandler, mmSocket, readPeriod, temperatureEntries, humidityEntries);
            connected.start();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
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

    public void off(){
        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }

    public void visible(){
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
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

        address = settings.getString("bluetoothAddress", null);
        name = settings.getString("bluetoothName", null);
        readPeriod = settings.getLong("readPeriod", 3000);
        //this.deleteFile(ARRAY_STORAGE_FILE);
        //this.deleteFile(SETTINGS_FILE);

        try
        {
            FileInputStream fis = openFileInput(ARRAY_STORAGE_FILE);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Gson gson = new Gson();
            String json = (String) ois.readObject();
            enviromentData = gson.fromJson(json, new TypeToken<ArrayList<EnvData>>(){}.getType());
            ois.close();
            fis.close();
            Log.d(TAG, "Loading Old Data" + json);

        }catch(FileNotFoundException e){
            Log.e(TAG, "App did not have a file", e);

        } catch (Exception e) {
            Log.e(TAG, "App did not Load properly", e);

        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences settings = getSharedPreferences(SETTINGS_FILE, 0);
        SharedPreferences.Editor sEditor = settings.edit();

        sEditor.putString("bluetoothAddress", address);
        sEditor.putString("bluetoothName", name);
        sEditor.putLong("readPeriod", readPeriod);

        //Will clear SharedPreferences
        /*if(false) {
            sEditor.clear();
        }*/

        sEditor.apply();

        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<EnvData>>() {}.getType();
            String json = gson.toJson(enviromentData, listType);
            FileOutputStream fos = openFileOutput(ARRAY_STORAGE_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(json);
            //oos.writeObject(humidityEntries);
            oos.close();
            fos.close();
            Log.d(TAG, "Saving Old Data");

        }
        catch (IOException e)
        {
            Log.e(TAG, "App did not save properly", e);
        }

    }

    private void populateGraph(List<Entry> tempEntries, List<Entry> huEntries, List<EnvData> data) {
        for(EnvData d : data) {
            tempEntries.add(new Entry(d.getTime(), d.getTemperature()));
            huEntries.add(new Entry(d.getTime(), d.getHumidity()));
        }

    }
}