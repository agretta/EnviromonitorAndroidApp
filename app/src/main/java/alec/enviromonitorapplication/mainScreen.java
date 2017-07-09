package alec.enviromonitorapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private ArrayList<BluetoothDevice> pairedDevices;
    private ListView lv;
    private View view;
    private AlertDialog dialog;

    private List<Entry> temps;
    private List<Entry> humidity;

    private Calendar calendar;
    private long readPeriod;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Sets the initial view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        temps = new LinkedList<>();
        humidity = new LinkedList<>();
        pairedDevices = new ArrayList<>();
        readPeriod = 3000;

        BA = BluetoothAdapter.getDefaultAdapter();
        calendar = Calendar.getInstance();

        view = getLayoutInflater().inflate(R.layout.popup, null);
        lv = (ListView)view.findViewById(R.id.listView);

        mTextMessage = (TextView) findViewById(R.id.message);

        //Graph Set Up
        /*for(int i = 0; i < 100; i++) {
            temps.add(new Entry(i, i + (int)(Math.random() * 10)-5));
        }*/
        //----------Temperatures Chart----------
        temps.add(new Entry(calendar.getTimeInMillis(),20));

        final LineChart t_chart = (LineChart) findViewById(R.id.t_chart);

        final LineDataSet temperaturesDataSet = new LineDataSet(temps, "Temperatures");
        temperaturesDataSet.setColor(Color.RED);
        final LineData temperaturesLineData = new LineData(temperaturesDataSet);
        t_chart.setData(temperaturesLineData);
        temperaturesDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        temperaturesLineData.setDrawValues(false);

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
        t_chart.setDescription(new Description());
        t_chart.invalidate(); // refresh
        //----------Temperatures Chart----------

        //----------Humidities Chart----------
        final LineChart h_chart = (LineChart) findViewById(R.id.h_chart);
        humidity.add(new Entry(calendar.getTimeInMillis(),40));

        final LineDataSet humiditiesDataSet = new LineDataSet(humidity, "Humidities");
        humiditiesDataSet.setColor(Color.BLUE);
        final LineData humiditiesLineData = new LineData(humiditiesDataSet);
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
                DecimalFormat formatter = new DecimalFormat("###,###,##0");
                return formatter.format(value) + "%";
            }
        });

        h_chart.getAxisRight().setEnabled(false);
        hYAxis.setAxisMinimum(0f); // start at zero
        hYAxis.setAxisMaximum(100f); // the axis maximum is 100
        //raxis.setGranularity(5f); // interval 1
        //raxis.setLabelCount(10, true); // force 6 labels
        humiditiesLineData.setDrawValues(false);

        h_chart.invalidate(); // refresh
        //----------Humidities Chart----------



        //populateGraph(mainGraph);
        /*
        // custom label formatter to show Temperature in Celcius and date as xaxis
        mainGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        mainGraph.getGridLabelRenderer().setNumVerticalLabels(5);
        mainGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                //time stamps
                if (isValueX) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm");
                    Date d = new Date((long) (value));
                    //calendar.add(Calendar.MINUTE, 1);
                    return (dateFormat.format(d));
                } else {
                    // show currency for y values
                    return super.formatLabel(value, isValueX) + "°C";
                }
            }
        });

        */
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(getApplicationContext(), "Click ListItem Number " + position, Toast.LENGTH_LONG).show();
                dialog.dismiss();
                ConnectThread thread = new ConnectThread(pairedDevices.get(position));
                thread.start();
            }
        });

        //-----Bluetooth-----
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MessageConstants.MESSAGE_READ) {
                    if(msg.arg2 == 'u') {
                        byte[] message = (byte[]) msg.obj;
                        String s = " " + message[1] + " " + message[2];
                        Log.d(TAG, s);
                        temperaturesDataSet.addEntry(new Entry(calendar.getTimeInMillis(), message[1]));
                        temperaturesLineData.notifyDataChanged();

                        humiditiesDataSet.addEntry(new Entry(calendar.getTimeInMillis(), message[2]));
                        humiditiesLineData.notifyDataChanged();

                        t_chart.notifyDataSetChanged();
                        t_chart.invalidate(); // refresh
                        h_chart.notifyDataSetChanged();
                        h_chart.invalidate(); // refresh
                        calendar.add(Calendar.MINUTE, 1);
                        mTextMessage.setText(s);
                        //saveGraphs();
                    }
                }
            }
        };

        final Button connectButton = (Button) findViewById(R.id.Connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                on();
                list();
            }
        });

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

        String json = gson.toJson(temps);

        editor.putString(TAG, json);
        editor.apply();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            Log.i(TAG, "Connected successfully to " + mmSocket.getRemoteDevice().getName() + " " + mmSocket.getRemoteDevice().getAddress() + ".");
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
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
            ConnectedThread connected = new ConnectedThread(mmSocket);
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

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            Looper.prepare();
            //Looper.loop();
            // Keep listening to the InputStream until an exception occurs.
            while (mmSocket.isConnected()) {
                try {
                    byte[] message;
                    int type = 0;
                    if(readPeriod == 0) {
                        message = new byte[1];
                        message[0] = 'p';
                        Log.d(TAG, "P Message");
                    }
                    //Completely Empty data set
                   /* else if(temps.size() == 0) {
                        message[0] = 'a';
                    }
                    //Reconnected after a break
                    else if (temperatureSeries.getHighestValueX() < (calendar.getTimeInMillis() - readPeriod)) {
                        message[0] = 'r';
                    }*/
                    //normal update
                    else {
                        message = new byte[3];
                        message[0] = 'u';
                        message[1] = 0;
                        message[2] = 1;
                        Log.d(TAG, "Update Message");
                    }

                    write(message);

                    //try {Thread.sleep(1000);}
                    //catch(InterruptedException ex) {Thread.currentThread().interrupt();}

                    if(0 != mmInStream.available()){
                        numBytes = mmInStream.read(mmBuffer);
                        //Log.d(TAG, "Reading Message After");
                        Log.d(TAG, "Number of bytes: " + Integer.toString(numBytes));

                        String log = "Bytes Received:";
                        for(int i = 0; i < numBytes; i++) {
                            log += mmBuffer[i] + " ";
                        }
                        Log.d(TAG, log);

                        //if mmBuffer[0] is not 0, then an error occured
                        if(mmBuffer[0] == 0) {

                            switch (message[0]) {
                                case 'p': {
                                    byte b = mmBuffer[2];
                                    readPeriod = (((int)mmBuffer[1]) << 8) + (b & 0xFF);
                                    Log.d(TAG, "Test 1: " + readPeriod);
                                    break;
                                }
                                case 'u': {
                                    // Send the obtained bytes to the UI activity.
                                    Log.d(TAG, "Update Test: " + " " + mmBuffer[0] + " " + mmBuffer[1] + " " + mmBuffer[2]);
                                    Message readMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_READ, mmBuffer[0], message[0], mmBuffer);
                                    readMsg.sendToTarget();
                                    break;
                                }
                            }
                        } else {
                            Log.d(TAG, "Error #" + mmBuffer[0]);
                        }

                    } else {
                        Log.d(TAG, "Nothing Avaliable");

                    }

                    //Wait for 10 seconds
                    try {Thread.sleep(readPeriod);}
                    catch(InterruptedException ex) {Thread.currentThread().interrupt();}

                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
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
        //visible();
        ((ViewGroup) lv.getParent()).removeView(lv);
        pairedDevices.clear();

        BA.startDiscovery();
        pairedDevices.addAll(BA.getBondedDevices());
        ArrayList list = new ArrayList();

        for(BluetoothDevice bt : pairedDevices) {
            list.add(bt.getName());
        }

        Toast.makeText(getApplicationContext(), "Showing Paired Devices",Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new ArrayAdapter(mainScreen.this, android.R.layout.simple_list_item_1, pairedDevices);
        lv.setAdapter(adapter);

        //AlertDialog.Builder builder = new AlertDialog.Builder(mainScreen.this);
        AlertDialog.Builder builder = new AlertDialog.Builder(mainScreen.this);
        //builder.setCancelable(true);

        //Clicking connect twice kills app, parent needs to be removed

        builder.setView(lv);
        dialog = builder.create();
        dialog.show();
    }

    private void populateGraph(LineChart mainGraph) {
        /*for (int i = 0; i < points.length; i++) {
            points[i] = new DataPoint(calendar.getTime(), 20 + (Math.random()*10+1));
            calendar.add(Calendar.MINUTE, 1);
        }
        */
        /*
        String str = "";//you need to retrieve this string from shared preferences.
        Type type = new TypeToken<ArrayList<Integer>>() { }.getType();
        List<Integer> placeholder = new Gson().fromJson(str, type);
        if (placeholder != null) {
            temps = placeholder;
        }

        String str2 = "";//you need to retrieve this string from shared preferences.
        Type type2 = new TypeToken<ArrayList<Integer>>() { }.getType();
        List<Integer> placeholder2 = new Gson().fromJson(str, type);
        if (placeholder != null) {
            humidity = placeholder2;
        }*/
    }
}