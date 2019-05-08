package com.example.dan87.livestreamom;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.vikramezhil.droidspeech.DroidSpeech;
import com.vikramezhil.droidspeech.OnDSListener;
import com.vikramezhil.droidspeech.OnDSPermissionsListener;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity implements View.OnClickListener,
        GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener
        ,AdapterView.OnItemClickListener, OnDSListener, OnDSPermissionsListener {

    //<editor-fold defaultstate="collapsed" desc="Variables">

    //<editor-fold defaultstate="collapsed" desc="LiveStream Variables">
    private ImageView imgView, imgView1;
    private Button mBtnStart;
    private SocketServer socketServer;
    ShowImage showImage;
    PassedObject passedObject;
    Bitmap bmp;
    MainActivity main = this;
    private GestureDetector gestureDetector;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Bluetooth Variables">
    private static final String TAG = "MainActivity";

    BluetoothAdapter mBluetoothAdapter;

    BluetoothConnectionService mBluetoothConnection;

    Button btnFindUnpairedDevices;
    Button btnStartConnection;

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothDevice mBTDevice;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    public DeviceListAdapter mDeviceListAdapter;

    ListView lvNewDevices;

    private boolean registeredBrReg1;
    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                }
                //case2: creating a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="SpeechToText Variables">

    private DroidSpeech droidSpeech;
    private boolean stoppedFromOnPause = false;

    public volatile int robotOptions = -1;

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Sensor Variables">

    SensorManager sensorManager;

    Sensor gyroscope;
    Sensor accelerometer;

    CommandSender commandSender;

    //</editor-fold>

    //</editor-fold>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //<editor-fold defaultstate="collapsed" desc="LiveStream Stuff">
        imgView = findViewById(R.id.imageView);
        imgView.setRotation(90);
        imgView1 = findViewById(R.id.imageView1);
        imgView1.setRotation(90);
        mBtnStart = findViewById(R.id.startServer);
        mBtnStart.setOnClickListener(this);
        showImage = new ShowImage();
        passedObject = new PassedObject();
        gestureDetector = new GestureDetector(this, this);

        //Start SocketServer - Thread
        socketServer = new SocketServer(passedObject, showImage, main);
        socketServer.start();

        //Start AsyncTask
        showImage.execute();
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Bluetooth Stuff">
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Button btnONOFF = findViewById(R.id.btnONOFF);
        if (mBluetoothAdapter.isEnabled()) {
            btnONOFF.setVisibility(View.GONE);
        }
        lvNewDevices = findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        btnFindUnpairedDevices = findViewById(R.id.btnFindUnpairedDevices);
        btnStartConnection = findViewById(R.id.btnStartConnection);

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        lvNewDevices.setOnItemClickListener(MainActivity.this);


        btnONOFF.setOnClickListener(view -> {
            Log.d(TAG, "onClick: enabling/disabling bluetooth.");
            if (!mBluetoothAdapter.isEnabled()) {
                enableDisableBT();
                btnONOFF.setVisibility(View.GONE);
            }
        });

        btnStartConnection.setOnClickListener(view -> {
            startConnection();
            btnStartConnection.setVisibility(View.GONE);
            lvNewDevices.setVisibility(View.GONE);
        });
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Sensor Stuff">
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="SpeechToText Stuff">
        // Initializing the droid speech and setting the listener
        requestMicrophonePermission();
        droidSpeech = new DroidSpeech(this, null);
        droidSpeech.setOnDroidSpeechListener(this);

        //</editor-fold>
    }

    //<editor-fold defaultstate="collapsed" desc="LiveStream Stuff">
    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {
        if (mBtnStart.getText().equals("Start")) {
            mBtnStart.setText("Stop");

            commandSender.start();

            droidSpeech.startDroidSpeechRecognition();

            //Make button "GONE"
            mBtnStart.setVisibility(View.GONE);

            //Start SocketServer communication
            socketServer.runningServer = true;
            socketServer.runningThread = true;

            //Get Bitmap without blocking UI thread
            showImage.runningAsync = true;

        } else {
            mBtnStart.setText("Start");

            //Stop SocketServer communication
            socketServer.runningThread = false;
            socketServer.runningServer = false;

            //Stop the AsyncTask
            showImage.runningAsync = false;
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (mBtnStart.getVisibility() == View.VISIBLE) {
            mBtnStart.setVisibility(View.GONE);
        } else {
            mBtnStart.setVisibility(View.VISIBLE);
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @SuppressLint("StaticFieldLeak")
    class ShowImage extends AsyncTask<String, String, String> {
        int ok = 1;
        boolean runningAsync = false;

        @Override
        protected String doInBackground(String... strings) {
            while (true) {
                if (runningAsync) {
                    synchronized (this) {
                        while (passedObject.frameSize() == 0 || passedObject.lenSize() == 0) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        bmp = passedObject.receive();
                        publishProgress();
                        ok = 1;
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            imgView.setImageBitmap(Bitmap.createScaledBitmap(bmp, imgView.getWidth(), imgView.getHeight(), false));
            imgView1.setImageBitmap(Bitmap.createScaledBitmap(bmp, imgView1.getWidth(), imgView1.getHeight(), false));
        }
    }

    public void showToast(final String toast) {
        runOnUiThread(() -> {
            mBtnStart.setText("Start");

            //Stop the AsyncTask
            showImage.runningAsync = false;

            Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
        });
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Bluetooth Stuff">
    //create method for starting connection
    //***remember the conncction will fail and app will crash if you haven't paired first
    public void startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    /**
     * starting chat service method
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device, uuid);
        commandSender = new CommandSender(sensorManager,gyroscope,accelerometer,mBluetoothConnection,this);
    }

    public void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
            registeredBrReg1 = true;
        } else {
            registeredBrReg1 = false;
        }

    }

    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if (!mBluetoothAdapter.isDiscovering()) {

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        btnFindUnpairedDevices.setVisibility(View.GONE);
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     * <p>
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();

            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        droidSpeech.closeDroidSpeechOperations();

        if (mBtnStart.getText().equals("Stop")) {
            writeOnBluetooth("a");
        }
        if (registeredBrReg1) {
            unregisterReceiver(mBroadcastReceiver1);
        }
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
        //mBluetoothAdapter.cancelDiscovery();
    }

    @Override
    protected void onPause() {
        if (mBtnStart.getText().equals("Stop")) {
            writeOnBluetooth("a");
        }

        droidSpeech.closeDroidSpeechOperations();
        stoppedFromOnPause = true;

        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBtnStart.getText().equals("Stop")) {
            writeOnBluetooth("a");
        }
        super.onStop();
    }

    private void writeOnBluetooth(String msg) {
        byte[] bytes = msg.getBytes(Charset.defaultCharset());
        mBluetoothConnection.write(bytes);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="SpeechToText Stuff">


    @Override
    protected void onResume() {
        super.onResume();
        if(stoppedFromOnPause && mBtnStart.getText().equals("Stop")){
            droidSpeech.startDroidSpeechRecognition();
            stoppedFromOnPause = false;
        }
    }

    @Override
    public void onDroidSpeechSupportedLanguages(String currentSpeechLanguage, List<String> supportedSpeechLanguages)
    {
    }

    @Override
    public void onDroidSpeechRmsChanged(float rmsChangedValue)
    {
    }

    @Override
    public void onDroidSpeechLiveResult(String liveSpeechResult)
    {
    }

    @Override
    public void onDroidSpeechFinalResult(String finalSpeechResult) {
        switch (finalSpeechResult) {
            case "stop":
                robotOptions = 0;
                break;
            case "start":
                robotOptions = 1;
                break;
            case "faster":
                robotOptions = 2;
                break;
            case "slower":
                robotOptions = 3;
                break;
            case "rotation faster":
                robotOptions = 4;
                break;
            case "rotation slower":
                robotOptions = 5;
                break;
            case "dance":
                robotOptions = 6;
                break;
            default:
                robotOptions = -1;
                break;
        }
        System.out.println("robotOptions "+robotOptions);
    }

    @Override
    public void onDroidSpeechClosedByUser()
    {
    }

    @Override
    public void onDroidSpeechError(String errorMsg)
    {
        System.out.println("on err");
        droidSpeech.closeDroidSpeechOperations();
    }

    @Override
    public void onDroidSpeechAudioPermissionStatus(boolean audioPermissionGiven, String errorMsgIfAny)
    {
        System.out.println("on perm");
        if(audioPermissionGiven)
        {
            droidSpeech.startDroidSpeechRecognition();
        }
        else
        {
            if(errorMsgIfAny != null)
            {
                // Permissions error
                Toast.makeText(this, errorMsgIfAny, Toast.LENGTH_LONG).show();
            }

            droidSpeech.closeDroidSpeechOperations();
        }
    }

    private void requestMicrophonePermission() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.RECORD_AUDIO");
        if (permissionCheck != 0) {
            this.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1002); //Any number
        }
    }

    //</editor-fold>
}

