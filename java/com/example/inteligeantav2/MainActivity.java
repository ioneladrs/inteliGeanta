package com.example.inteligeantav2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemClickListener,
        View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQ_ENABLE_BT = 10;
    public static final int BT_BOUNDED = 21;
    public static final int BT_SEARCH = 22;
    public static final int REQUEST_CODE_LOC = 1;

    private RelativeLayout frameMessage, communicationFrame;
    private LinearLayout frameControl;

    private Button btnEnableSearch, BtDisconnect;
    private Switch controlBluetooth;
    private ProgressBar searchProgress;
    private ListView bluetoothDevices;

    private BluetoothAdapter bluetoothAdapter;
    private BtListAdapter listAdapter;
    private ArrayList<BluetoothDevice> bluetoothDevicess;

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        frameMessage = findViewById(R.id.frame_message);
        frameControl = findViewById(R.id.frame_control);
        communicationFrame = findViewById(R.id.CommunicationFrame);

        searchProgress = findViewById(R.id.search_progress);
        btnEnableSearch = findViewById(R.id.btn_enable_search);
        controlBluetooth = findViewById(R.id.control_bluetooth);
        bluetoothDevices = findViewById(R.id.bluetooth_devices);
        BtDisconnect = findViewById(R.id.bt_disconnect);
        //notFound = findViewById(R.id.not_found);


        controlBluetooth.setOnCheckedChangeListener(this);
        btnEnableSearch.setOnClickListener(this);
        bluetoothDevices.setOnItemClickListener(this);
        BtDisconnect.setOnClickListener(this);

        bluetoothDevicess = new ArrayList<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: " + getString(R.string.bluetooth_not_supported));
            finish();

            if (bluetoothAdapter.isEnabled()) {
                showFrameControl();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        if (connectThread != null) {
            connectThread.cancel();
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        int id = item.getItemId();

        if (id == R.id.edit_option) {
        } else if (id == R.id.bag_location) {
        } else if (id == R.id.open_bag) {
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    @Override
    public void onClick(View v) {
        if (v.equals(btnEnableSearch)) {
            enableSearch();
        } else if (v.equals(BtDisconnect)){
            if(connectedThread != null) {
                connectedThread.cancel();
            }
            if(connectThread != null){
                connectThread.cancel();
            }
            showFrameControl();
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(bluetoothDevicess)) {
            BluetoothDevice device = bluetoothDevicess.get(position);
            if (device != null) {
                connectThread = new ConnectThread(device);
                connectThread.start();
            }
        }
    }
    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        }
    };

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (buttonView.equals(controlBluetooth)) {
            enableBt(isChecked);
        }
        if (!isChecked) {
            showFrameMessage();
            setListAdapter(BT_BOUNDED);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK && bluetoothAdapter.isEnabled()) {
                showFrameControl();
                setListAdapter(BT_BOUNDED);
            } else {
                enableBt(true); // here I should change for my application specifically
            }
        }
    }

    private void showFrameMessage() {
        frameMessage.setVisibility(View.VISIBLE);
        frameControl.setVisibility(View.GONE);
        communicationFrame.setVisibility(View.GONE);
    }

    private void showFrameControl() {
        frameMessage.setVisibility(View.GONE);
        frameControl.setVisibility(View.VISIBLE);
        communicationFrame.setVisibility(View.GONE);
    }
    private void showFrameCommunication() {
        frameMessage.setVisibility(View.GONE);
        frameControl.setVisibility(View.GONE);
        communicationFrame.setVisibility(View.VISIBLE);
    }
    private void enableBt(boolean flag) {
        if (flag) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE_BT);
        } else {
            bluetoothAdapter.disable();
        }
    }

    private void setListAdapter(int type) {
        bluetoothDevicess.clear();
        int iconType = R.drawable.bluetooth_searching;
        switch (type) {
            case BT_BOUNDED:
                bluetoothDevicess = getBoundedBtDevices();
                iconType = R.drawable.bluetooth_searching;
                break;
            case BT_SEARCH:
                iconType = R.drawable.bluetooth_devices;
                break;
        }
        listAdapter = new BtListAdapter(this, bluetoothDevicess, iconType);
        bluetoothDevices.setAdapter(listAdapter);
    }

    private ArrayList<BluetoothDevice> getBoundedBtDevices() {
        Set<BluetoothDevice> deviceSet = bluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> tempArrayList = new ArrayList<>();

        if (deviceSet.size() > 0) {
            for (BluetoothDevice device : deviceSet) {
                tempArrayList.add(device);
            }
        }
        return tempArrayList;
    }

    private void enableSearch() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        } else {
            accessLocationPermission();
            bluetoothAdapter.startDiscovery();
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    btnEnableSearch.setText(R.string.stop_search);
                    searchProgress.setVisibility(View.VISIBLE);
                    setListAdapter(BT_SEARCH);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    btnEnableSearch.setText(R.string.start_search);
                    searchProgress.setVisibility(View.GONE);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        bluetoothDevicess.add(device);
                        listAdapter.notifyDataSetChanged();
                    }
                    break;
                //default:
                //notFound.setVisibility(View.VISIBLE);
                //break;
            }
        }
    };

    private void accessLocationPermission() {
        int accessCoarseLocation = this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        int accessFineLocation = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        List<String> listRequestPermsission = new ArrayList<String>();

        if (accessCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermsission.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (accessFineLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermsission.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!listRequestPermsission.isEmpty()) {
            String[] strRequestPermission = listRequestPermsission.toArray(new String[listRequestPermsission.size()]);
            this.requestPermissions(strRequestPermission, REQUEST_CODE_LOC);
        }
    }

    public class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket = null;
        private boolean success = false;

        public ConnectThread(BluetoothDevice device) {
            try {
                Method method = device.getClass().getMethod("createRfcommSocketToServiceRecord", new Class[]{int.class});
                bluetoothSocket = (BluetoothSocket) method.invoke(device, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                bluetoothSocket.connect();
                success = true;

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Nu se poate stabili conexiunea", Toast.LENGTH_SHORT).show();
                    }
                });
                cancel();
            }
            if (success) {
                connectedThread = new ConnectedThread(bluetoothSocket);
                connectedThread.start();
                showFrameCommunication();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showFrameCommunication();
                    }
                });
            }
        }
        public boolean isConnect(){
            return bluetoothSocket.isConnected();
        }
        public void cancel(){
            try{
                bluetoothSocket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

        public class ConnectedThread extends Thread {
            private InputStream inputStream;
            private OutputStream outputStream;

            public ConnectedThread(BluetoothSocket bluetoothSocket) {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = bluetoothSocket.getInputStream();
                    outputStream = bluetoothSocket.getOutputStream();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.inputStream = inputStream;
                this.outputStream = outputStream;
            }

            @Override
            public void run() {

            }

            public void write(String command) {
                byte[] bytes = command.getBytes();
                if (outputStream != null) {
                    try {
                        outputStream.write(bytes);
                        outputStream.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void cancel() {

                try {
                    inputStream.close();
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

