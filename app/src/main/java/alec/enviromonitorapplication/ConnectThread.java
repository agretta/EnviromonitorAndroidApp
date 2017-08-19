package alec.enviromonitorapplication;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;

public class ConnectThread {
/*
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
        Intent intent = new Intent(this, TiltGraph.class);
        startActivity(intent);

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
*/
}
