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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class GraphView extends AppCompatActivity {

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

    private long readPeriod;

    private String address;
    private String name;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Sets the initial view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        view = getLayoutInflater().inflate(R.layout.popup, null);
        lv = (ListView)view.findViewById(R.id.listView);


        mTextMessage = (TextView) findViewById(R.id.message);
        BA = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new ArrayList<>();


        //Load stuff from memory
        temperatureEntries = new LinkedList<>();
        humidityEntries = new LinkedList<>();
        enviromentData = new LinkedList<>();

        load();
        on();

        /*LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.graphs, null);

        // insert into main view
        ViewGroup insertPoint = (ViewGroup) findViewById(R.id.graphscontainer);
        insertPoint.addView(v, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        */

        //----------Temperatures Chart----------
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.getInstance().get(Calendar.YEAR),0,0,0,0,0);
        long year = cal.getTimeInMillis();

        //int time = cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
        populateGraph(temperatureEntries, humidityEntries, enviromentData);
        final LineChart t_chart = (LineChart) findViewById(R.id.t_chart);

        final LineDataSet temperaturesDataSet = new LineDataSet(temperatureEntries, "Temperature");
        temperaturesDataSet.setColor(Color.RED);
        temperaturesDataSet.setDrawHighlightIndicators(false);
        final LineData temperaturesLineData = new LineData(temperaturesDataSet);
        t_chart.setData(temperaturesLineData);
        temperaturesDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        temperaturesLineData.setDrawValues(false);
        temperaturesDataSet.setDrawCircles(false);
        temperaturesDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        temperaturesDataSet.setDrawFilled(true);

        //Formating
        XAxis xaxis  = t_chart.getXAxis();
        xaxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xaxis.setValueFormatter(new TimeValueFormatter(year));
        xaxis.setAxisMinimum(0);
        xaxis.setAxisMaximum(86400);
        //xaxis.setLabelCount(24);
        xaxis.setGranularityEnabled(true);
        xaxis.setGranularity(60);
        t_chart.setVisibleXRangeMaximum(3600);
        t_chart.setVisibleXRangeMinimum(60);

        t_chart.moveViewToX(temperaturesDataSet.getEntryForIndex(0).getX());
        /*for(int i = 0; i < 24*4; i++) {
            LimitLine line = new LimitLine(i * 15 * 60);
            xaxis.addLimitLine(line);
        }*/

        YAxis laxis  = t_chart.getAxisLeft();
        laxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                DecimalFormat formatter = new DecimalFormat("##0");
                return formatter.format(value) + " °C";
            }
        });

        t_chart.getAxisRight().setEnabled(false);
        laxis.setAxisMinimum(-10); // start at zero
        laxis.setAxisMaximum(40); // the axis maximum is 100
        //t_chart.setVisibleYRangeMaximum(10f, YAxis.AxisDependency.LEFT);
        t_chart.setDescription(null);
        //t_chart.setVisibleXRange(0,86400);
        //t_chart.setVisibleXRange(enviromentData.get(0).getTime(), enviromentData.get(enviromentData.size()-1).getTime());


        //t_chart.moveViewToX(temperaturesDataSet.getEntryForIndex(0).getX());
        t_chart.setScaleEnabled(true);
        t_chart.invalidate(); // refresh
        //----------Temperatures Chart----------


        //----------Humidities Chart----------
        final LineChart h_chart = (LineChart) findViewById(R.id.h_chart);
        ///humidityEntries.add(new Entry(time, 0));
        //humidityEntries.add(new Entry(time, 100));

        final LineDataSet humiditiesDataSet = new LineDataSet(humidityEntries, "Humidities");
        humiditiesDataSet.setColor(Color.BLUE);
        humiditiesDataSet.setDrawHighlightIndicators(false);
        final LineData humiditiesLineData = new LineData(humiditiesDataSet);
        humiditiesLineData.setDrawValues(false);
        humiditiesDataSet.setDrawCircles(false);
        humiditiesDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        humiditiesDataSet.setDrawFilled(true);
        h_chart.setData(humiditiesLineData);
        humiditiesDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        XAxis hXAxis  = h_chart.getXAxis();
        hXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        hXAxis.setValueFormatter(new TimeValueFormatter(year));

        YAxis hYAxis  = h_chart.getAxisLeft();
        hYAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                DecimalFormat mFormat = new DecimalFormat("##0");
                return mFormat.format(value) + "%";
            }
        });

        h_chart.getAxisRight().setEnabled(false);
        hYAxis.setAxisMinimum(0); // start at zero
        hYAxis.setAxisMaximum(100); // the axis maximum is 100

        ViewPortHandler hand = h_chart.getViewPortHandler();
        //hand.setMinMaxScaleY(1f, 1f);
        //raxis.setGranularity(5f); // interval 1
        //raxis.setLabelCount(10, true); // force 6 labels

        h_chart.setDescription(null);
        h_chart.invalidate(); // refresh
        //----------Humidities Chart----------

        if(BA.isEnabled()) {
            mTextMessage.setText(name);
            if (address != null) {
                Log.d(TAG, "Attemmping to start new thread" + address);
                GraphView.ConnectThread connect = new GraphView.ConnectThread(BA.getRemoteDevice(address));
                connect.start();
            }
        }



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

                GraphView.ConnectThread thread = new GraphView.ConnectThread(bt);
                thread.start();
            }
        });

        //Handle messages passed from the device ConnectedThread
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                byte[] message = (byte[]) msg.obj;
                Calendar cal = Calendar.getInstance();
                if (msg.arg2 == 'u') {
                    if (true) {
                        long realtime = (cal.getTimeInMillis());
                        int time = cal.get(Calendar.HOUR_OF_DAY) * (3600) + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
                        String s = "Logging Values: Temperature:" + message[1] + " Humidity:" + message[2] + " Time:" + Long.toString(realtime);
                        Log.d(TAG, s);

                        //Error from sensor
                        if (message[1] == 0 && message[2] == 0
                                && enviromentData.get(enviromentData.size() - 1).getTemperature() != 0
                                && enviromentData.get(enviromentData.size() - 1).getTemperature() != 0) {
                            Log.d(TAG, "All Zeros Recorded");
                            message[1] = (byte) enviromentData.get(enviromentData.size() - 1).getTemperature();
                            message[2] = (byte) enviromentData.get(enviromentData.size() - 1).getTemperature();
                        }

                        enviromentData.add(new EnvData(realtime, message[1], message[2]));

                        temperaturesDataSet.addEntry(new Entry(time, message[1]));
                        temperaturesLineData.notifyDataChanged();
                        t_chart.notifyDataSetChanged();

                        humiditiesDataSet.addEntry(new Entry(time, message[2]));
                        humiditiesLineData.notifyDataChanged();
                        h_chart.notifyDataSetChanged();

                        /*
                        int seconds = time%60;
                        int minutes = (time/60) % 60;
                        int hours = (time/3600) % 60;
                        Log.d(TAG, "Time: " + hours + ":" +  minutes + ":" + seconds);
                        */

                        t_chart.invalidate(); // refresh
                        h_chart.invalidate(); // refresh

                        mTextMessage.setText(s + " " + enviromentData.size());
                    }
                    /*else {
                        cal.add(Calendar.MILLISECOND, (int)(-1 * readPeriod * msg.arg1));
                        for(int i = 0; i < (msg.arg1-1)/2 -1; i++) {
                            long startTime = cal.getTimeInMillis();

                            int tempIndex = 2*i+1;
                            int huIndex = 2*i+2;
                            if(message[tempIndex] == 0 && message[huIndex] == 0
                                    && enviromentData.get(enviromentData.size()-1).getTemperature() != 0
                                    && enviromentData.get(enviromentData.size()-1).getTemperature() != 0) {
                                Log.d(TAG, "All Zeros Recorded");
                                message[1] = (byte) enviromentData.get(enviromentData.size()-1).getTemperature();
                                message[2] = (byte) enviromentData.get(enviromentData.size()-1).getTemperature();
                            }

                            int time = cal.get(Calendar.HOUR_OF_DAY) * (3600) + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
                            String s = " t:" + message[tempIndex] + " h:" + message[huIndex] + " time:" + Long.toString(startTime);
                            Log.d(TAG, s);

                            enviromentData.add(new EnvData(startTime, message[tempIndex], message[huIndex]));

                            temperaturesDataSet.addEntry(new Entry(time, message[1]));
                            temperaturesLineData.notifyDataChanged();

                            humiditiesDataSet.addEntry(new Entry(time, message[2]));
                            humiditiesLineData.notifyDataChanged();

                            cal.add(Calendar.MILLISECOND, (int)readPeriod);

                            mTextMessage.setText(s + " " + enviromentData.size());

                        }
                        t_chart.notifyDataSetChanged();
                        t_chart.invalidate(); // refresh
                        h_chart.notifyDataSetChanged();
                        h_chart.invalidate(); // refresh
                    }*/

                }
                else if (msg.arg2 == 'l') {

                    long endTime = cal.getTimeInMillis();
                    long startTime = enviromentData.get(enviromentData.size() - 1).getTime() + readPeriod;
                    long difference = (endTime - startTime) / readPeriod;

                    Log.d(TAG, Long.toString(difference));
                    cal.setTimeInMillis(startTime);

                    for (int i = 0; i < (msg.arg1 - 1) / 2 - 1; i++) {

                        int tempIndex = 2 * i + 1;
                        int huIndex = 2 * i + 2;

                        if (message[tempIndex] == 0 && message[huIndex] == 0
                                && enviromentData.get(enviromentData.size() - 1).getTemperature() != 0
                                && enviromentData.get(enviromentData.size() - 1).getTemperature() != 0) {
                            Log.d(TAG, "All Zeros Recorded");
                            message[1] = (byte) enviromentData.get(enviromentData.size() - 1).getTemperature();
                            message[2] = (byte) enviromentData.get(enviromentData.size() - 1).getTemperature();
                        }

                        int time = cal.get(Calendar.HOUR_OF_DAY) * (3600) + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
                        String s = " t:" + message[tempIndex] + " h:" + message[huIndex] + " time:" + Long.toString(startTime);
                        Log.d(TAG, s);

                        enviromentData.add(new EnvData(startTime, message[tempIndex], message[huIndex]));

                        temperaturesDataSet.addEntry(new Entry(time, message[1]));
                        temperaturesLineData.notifyDataChanged();

                        humiditiesDataSet.addEntry(new Entry(time, message[2]));
                        humiditiesLineData.notifyDataChanged();

                        cal.add(Calendar.MILLISECOND, (int) readPeriod);

                        mTextMessage.setText(enviromentData.size());

                    }

                    t_chart.notifyDataSetChanged();
                    t_chart.invalidate(); // refresh
                    h_chart.notifyDataSetChanged();
                    h_chart.invalidate(); // refresh

                } else if (msg.arg2 == 'a') {
                    cal.add(Calendar.MILLISECOND, (int) (-1 * readPeriod * msg.arg1));
                    for (int i = 0; i < (msg.arg1 - 1) / 2 - 1; i++) {
                        long startTime = cal.getTimeInMillis();

                        int tempIndex = 2 * i + 1;
                        int huIndex = 2 * i + 2;

                        if (message[tempIndex] == 0 && message[huIndex] == 0
                                && enviromentData.get(enviromentData.size() - 1).getTemperature() != 0
                                && enviromentData.get(enviromentData.size() - 1).getTemperature() != 0) {
                            Log.d(TAG, "All Zeros Recorded");
                            message[1] = (byte) enviromentData.get(enviromentData.size() - 1).getTemperature();
                            message[2] = (byte) enviromentData.get(enviromentData.size() - 1).getTemperature();
                        }

                        int time = cal.get(Calendar.HOUR_OF_DAY) * (3600) + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
                        String s = " t:" + message[tempIndex] + " h:" + message[huIndex] + " time:" + Long.toString(startTime);
                        Log.d(TAG, s);

                        enviromentData.add(new EnvData(startTime, message[tempIndex], message[huIndex]));

                        temperaturesDataSet.addEntry(new Entry(time, message[1]));
                        temperaturesLineData.notifyDataChanged();

                        humiditiesDataSet.addEntry(new Entry(time, message[2]));
                        humiditiesLineData.notifyDataChanged();

                        cal.add(Calendar.MILLISECOND, (int) readPeriod);

                        mTextMessage.setText(s + " " + enviromentData.size());

                    }
                    t_chart.notifyDataSetChanged();
                    t_chart.invalidate(); // refresh
                    h_chart.notifyDataSetChanged();
                    h_chart.invalidate(); // refresh
                }
                else if (msg.arg2 == 'p') {
                    byte b = message[2];
                    readPeriod = (((int)message[1]) << 8) + (b & 0xFF);
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
        final ArrayAdapter adapter = new ArrayAdapter(GraphView.this, android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);

        //AlertDialog.Builder builder = new AlertDialog.Builder(mainScreen.this);
        AlertDialog.Builder builder = new AlertDialog.Builder(GraphView.this);
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
    protected void onPause() {

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
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.SECOND, -1);

        for(EnvData d : data) {
            cal.setTimeInMillis(d.getTime());
            //Currently should only do 1 hour of time
            int time = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
            tempEntries.add(new Entry(time, d.getTemperature()));
            huEntries.add(new Entry(time, d.getHumidity()));
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
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.switch_screen:
                startActivity(new Intent(this, GraphView.class));
                return true;
            case R.id.options:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            ConnectedThread connected = new ConnectedThread(mHandler, mmSocket, readPeriod, enviromentData);
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


}
