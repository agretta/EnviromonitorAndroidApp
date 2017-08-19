package alec.enviromonitorapplication;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

/**
 * This class handles the communication between the connected bluetooth device and the application
 */

class ConnectedThread extends Thread {
    private final BluetoothSocket mySocket;
    private final InputStream myInStream;
    private final OutputStream myOutStream;
    private byte[] myBuffer;

    private Handler myHandler;
    private long readPeriod;
    private Calendar calendar = Calendar.getInstance();

    private List<EnvData> data;
    boolean newRun;

    private static final String TAG = "Debug";

    interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
    }

    ConnectedThread(Handler h, BluetoothSocket socket, long readPeriod, List<EnvData> data) {
        mySocket = socket;
        InputStream tmpInStream = null;
        OutputStream tmpOutStream = null;
        newRun = true;
        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpInStream = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating the input stream", e);
        }
        try {
            tmpOutStream = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating the output stream", e);
        }

        myInStream = tmpInStream;
        myOutStream = tmpOutStream;

        myHandler = h;
        this.readPeriod = readPeriod;
        this.data = data;
    }

    public void run() {
        myBuffer = new byte[1024];
        int numBytes;
        Looper.prepare();
        //Looper.loop();
        // Keep listening to the InputStream until an exception occurs
        boolean pause = true;
        while (mySocket.isConnected()) {
            calendar = Calendar.getInstance();
            try {
                byte[] message;
                int type = 0;
                if(readPeriod == 0) {
                    message = new byte[1];
                    message[0] = 'p';
                    Log.d(TAG, "P Message");
                }
                //Completely Empty data set
                /*else if(data.size() == 1) {
                    message = new byte[1];
                    message[0] = 'a';
                    Log.d(TAG, "Getting all of the data");
                }*/
                //Reconnected after a break
                /*else if (data.get(data.size()-1).getTime() < (calendar.getTimeInMillis() - readPeriod)) {
                    Date d = new Date(data.get(data.size()-1).getTime());
                    long diff = calendar.getTimeInMillis() - d.getTime();
                    int amt = (int)(diff/readPeriod);
                    Log.d(TAG, d.toString());
                    message = new byte[3];
                    message[0] = 'u';
                    message[1] = (byte)(amt>>8);
                    message[2] = (byte)(amt);
                    Log.d(TAG, "Reconnect Stuff");

                }*/
                //normal update
                if(newRun) {
                    message = new byte[1];
                    message[0] = 'l';
                    Log.d(TAG, "New Message");
                    newRun = false;
                }

                else {
                    message = new byte[3];
                    message[0] = 'u';
                    message[1] = 0;
                    message[2] = 1;
                    Log.d(TAG, "Writing " + message[0] + " " + message[1] + " " + message[2]);
                }

                write(message);

                long time = System.currentTimeMillis();
                long overTime = time += (long)1000;
                while((0 == myInStream.available()) && (overTime >= time)) {
                    time = System.currentTimeMillis();
                }

                if(0 != myInStream.available()){
                    numBytes = myInStream.read(myBuffer);
                    //Log.d(TAG, "Number of bytes: " + Integer.toString(numBytes));
                    String log = "Bytes: " + Integer.toString(numBytes) + ": [ ";
                    for(int i = 0; i < numBytes; i++) {
                        log += myBuffer[i] + " ";
                    }
                    log += "]";
                    Log.d(TAG, log);

                    //if myBuffer[0] is not 0, then an error occured
                    if(myBuffer[0] == 0) {
                        Message readMsg = myHandler.obtainMessage(MessageConstants.MESSAGE_READ, numBytes, message[0], myBuffer);
                        
                        readMsg.sendToTarget();
                    } else {
                        Log.d(TAG, "Error # " + myBuffer[0]);
                    }

                } else {
                    Log.d(TAG, "Nothing Avaliable");
                }

                try {Thread.sleep(readPeriod);}
                catch(InterruptedException ex) {Thread.currentThread().interrupt();}

            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                break;
            }
        }
    }

    // Call this from the main activity to send data to the remote device.
    private void write(byte[] bytes) {
        try {
            myOutStream.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when writing data", e);

            // Send a failure message back to the activity.
            Message writeErrorMsg = myHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            myHandler.sendMessage(writeErrorMsg);
            this.interrupt();
        }
    }
}