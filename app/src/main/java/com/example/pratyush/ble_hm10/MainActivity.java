package com.example.pratyush.ble_hm10;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private boolean mScanning;
    private BluetoothGatt mGatt;
    List<BluetoothDevice> mDevices;
    ListAdapter mDevicesAdapter;
    ListView deviceList;
    public static String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public EditText messageEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported.",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        initializeBLE();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Failed to get adapter.",
                    Toast.LENGTH_SHORT).show();
            finish();
        }


        mDevices = new ArrayList<>();
//        mDevicesAdapter = new ArrayAdapter<BluetoothDevice>(MainActivity.this, android.R.layout.simple_list_item_1, mDevices);
//        deviceList.setAdapter(mDevicesAdapter);
        startScan(true);
        deviceList = (ListView) findViewById(R.id.device_list);
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final BluetoothDevice device = (BluetoothDevice) adapterView.getItemAtPosition(i);
                String msg = device.getAddress() + "\n" + device.getBluetoothClass().toString();
                stopScan();
                /*new AlertDialog.Builder(MainActivity.this)
                        .setTitle(device.getName())
                        .setMessage(msg)
                        .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //stopScan();
                                connect(device);
                            }
                        });*/
                connect(device);
            }
        });
        Button write_button = (Button) findViewById(R.id.send_message_button);
        write_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mGatt == null)
                    Log.i("gatt", "Not connected yet");
                sendMessage(mGatt);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        mBluetoothAdapter.cancelDiscovery();
        mGatt.disconnect();
        mGatt.close();
    }

    private void initializeBLE() {
        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            mBluetoothAdapter = manager.getAdapter();
        }
        //mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mScanning = false;
    }

    private void startScan(final boolean enable) {
        if (enable) {
            mDevices.clear();
            //deviceList.invalidateViews();
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver, discoverDevicesIntent);
        }
    }

    private void stopScan() {
        mBluetoothAdapter.cancelDiscovery();
    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null && action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("Device", "Name: "+device.getName()+" address: "+device.getAddress());
                //if (device != null)
                mDevices.add(device);
                deviceList.setAdapter(new ArrayAdapter<BluetoothDevice>(MainActivity.this, android.R.layout.simple_list_item_1, mDevices));

            }

        }
    };

    private void connect(BluetoothDevice device) {
        mGatt = device.connectGatt(this, false, mCallback);
        /*final BluetoothDevice device1 = device;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (device1 != null) {
                    mGatt = device1.connectGatt(getApplicationContext(),
                            false, mCallback);
                    stopScan();
                }
            }
        });*/
    }

    private void sendMessage(BluetoothGatt gatt) {
        EditText messageEditText = (EditText) findViewById(R.id.message_edit_text);
        BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
        if (characteristic == null) {
            Log.i("Characteristic", "Unable to find echo characteristic.");
            try
            {
                mGatt.disconnect();
                mGatt.close();
            }
            catch(Exception e)
            {
                Log.i("Disconnect", "Problem disconnecting.");
            }
            return;
        }

        String message = messageEditText.getText().toString();
        Log.i("send", "Sending message: " + message);

        byte[] messageBytes = bytesFromString(message);
        if (messageBytes.length == 0 || messageBytes.length > 20) {
            Log.i("Message", "Message too long or null");
            return;
        }

        characteristic.setValue(messageBytes);

        boolean success = mGatt.writeCharacteristic(characteristic);
        if (success) {
            Log.i("write", "Wrote successfully");
        } else {
            Log.i("write", "Failed to write data");
        }


        boolean success2 = characteristic.setValue(bytesFromString(CHARACTERISTIC_UUID));
        if(success2)
            Log.i("write", "changed back");
    }

    public static byte[] bytesFromString(String string) {
        byte[] stringBytes = new byte[0];
        try {
            stringBytes = string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("Exception", "Failed to convert message string to byte array");
        }

        return stringBytes;
    }

    public BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("Success", "Connected to HM-10 successfully.");
                    final BluetoothGatt mGatt = gatt;
                    Handler handler;
                    handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mGatt.discoverServices();
                        }
                    });

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    try {
                        //onDestroy();
                        Log.i("no_conn", "Connection unsuccessful with status" + status);
                        //mGatt.disconnect();
                        mGatt.close();
                    } catch (Exception e) {
                        Log.i("Exception", "Exception caught: "+e);
                    }
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i("Not success", "Device service discovery unsuccessful, status " + status);
                return;
            }
            BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));

            if(service == null)
                Log.i("Service", "No such service");
            else
                Log.i("found", "Matching service found");

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));

            if(characteristic == null)
                Log.i("Characteristic", "No such characteristic");
            else
                Log.i("found", "Characteristic found");

            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            enableCharacteristicNotification(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("Write successful", "Characteristic written successfully");
            } else {
                Log.i("Unsuccessful", "Characteristic write unsuccessful, status: " + status);
                //mGatt.disconnect();
                mGatt.close();
            }
        }

        private void enableCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            boolean characteristicWriteSuccess = gatt.setCharacteristicNotification(characteristic, true);
            if (characteristicWriteSuccess) {
                Log.i("Notification success", "Characteristic notification set successfully for " + characteristic.getUuid().toString());
            } else {
                Log.i("Notification failure.", "Characteristic notification set failure for " + characteristic.getUuid().toString());
            }
        }
    };

}
