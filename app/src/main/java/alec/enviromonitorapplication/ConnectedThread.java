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
        // Keep listening to the InputStream until an exception occurs.
        while (mySocket.isConnected()) {
            try {
                byte[] message;
                int type = 0;
                if(readPeriod == 0) {
                    message = new byte[1];
                    message[0] = 'p';
                    Log.d(TAG, "P Message");
                }
                //Completely Empty data set
                /*else if(temps.size() == 1) {
                    message = new byte[1];
                    message[0] = 'a';
                    Log.d(TAG, "Getting all of the data");
                }
                //Reconnected after a break
                else if (temps.get(data.size()-1).getX() < (calendar.getTimeInMillis() - readPeriod)) {
                    Date d = new Date((long) temps.get(temps.size()-1).getX());
                    Log.d(TAG, d.toString());
                    message = new byte[1];
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

                if(0 != myInStream.available()){
                    numBytes = myInStream.read(myBuffer);
                    //Log.d(TAG, "Reading Message After");
                    Log.d(TAG, "Number of bytes: " + Integer.toString(numBytes));

                    String log = "Bytes Received:";
                    for(int i = 0; i < numBytes; i++) {
                        log += myBuffer[i] + " ";
                    }
                    Log.d(TAG, log);

                    //if myBuffer[0] is not 0, then an error occured
                    if(myBuffer[0] == 0) {

                        switch (message[0]) {
                            case 'p': {
                                byte b = myBuffer[2];
                                readPeriod = (((int)myBuffer[1]) << 8) + (b & 0xFF);
                                Log.d(TAG, "Test 1: " + readPeriod);
                                break;
                            }
                            case 'u': {
                                // Send the obtained bytes to the UI activity.
                                Log.d(TAG, "Update Test: " + " " + myBuffer[0] + " " + myBuffer[1] + " " + myBuffer[2]);
                                Message readMsg = myHandler.obtainMessage(MessageConstants.MESSAGE_READ, myBuffer[0], message[0], myBuffer);
                                readMsg.sendToTarget();
                                break;
                            }
                            case 'a': {

                            }
                            case 'r': {

                            }
                        }
                    } else {
                        Log.d(TAG, "Error #" + myBuffer[0]);
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