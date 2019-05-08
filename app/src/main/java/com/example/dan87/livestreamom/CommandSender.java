package com.example.dan87.livestreamom;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import java.nio.charset.Charset;
import static java.lang.Math.abs;

public class CommandSender extends Thread{

    //<editor-fold defaultstate="collapsed" desc="Variables">

    //<editor-fold defaultstate="collapsed" desc="Sensor Variables">

    private SensorManager mSensorManager;
    private Sensor gyroscope, accelerometer;
    private MySensorListener msl;
    /*
    0 = repaus;
    1 = fata;
    2 = spate;
    3 = inclinare dreapta;
    4 = inclinare stanga;
    5 = rotatie dreapta;
    6 = rotatie stanga;
     */
    private int position = 0, i, oldPosition = -1;
    private static final double precision = 1.8;

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Bluetooth Variables">

    private BluetoothConnectionService mBluetoothConnection;

    //</editor-fold>

    private MainActivity mainActivity;
    private boolean started = false;

    //</editor-fold>

    CommandSender(SensorManager mSensorManager, Sensor gyroscope, Sensor accelerometer,
                  BluetoothConnectionService mBluetoothConnection,MainActivity mainActivity){
        this.mSensorManager = mSensorManager;
        this.gyroscope = gyroscope;
        this.accelerometer = accelerometer;
        this.mBluetoothConnection = mBluetoothConnection;
        this.mainActivity = mainActivity;
        msl = new MySensorListener();
    }

    public void run() {
        Looper.prepare();
        Handler handler = new Handler();
        mSensorManager.registerListener(msl, gyroscope,SensorManager.SENSOR_DELAY_UI, handler);
        mSensorManager.registerListener(msl, accelerometer,SensorManager.SENSOR_DELAY_UI, handler);
        Looper.loop();
    }

    //<editor-fold defaultstate="collapsed" desc="Bluetooth Stuff">

    private void writeOnBluetooth(String msg) {
        byte[] bytes = msg.getBytes(Charset.defaultCharset());
        mBluetoothConnection.write(bytes);
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Sensor Stuff">

    private class MySensorListener implements SensorEventListener {
        public void onAccuracyChanged (Sensor sensor, int accuracy) {
        }
        public void onSensorChanged(SensorEvent event) {
            if (started) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    //repaus
                    if ((abs(x) > 9 && abs(x) < 10.5) && (abs(y) > 0 && abs(y) < 2) && (abs(z) > 0 && abs(z) < 4) && position != 5 && position != 6 && position != 7) {
                        i++;
                        if (i == 5) {
                            position = 0;
                            i = 0;
                        }
                    }
                }

                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    // fata
                    if ((x > -precision && x < precision) && (y < -precision) && (z > -precision && z < precision) && position == 0) {
                        position = 1;
                    }

                    // spate
                    if ((x > -precision && x < precision) && (y > precision) && (z > -precision && z < precision) && position == 0) {
                        position = 2;
                    }

                    // miscare dreapta
                    if ((x > -precision && x < precision) && (y > -precision && y < precision) && (z < -precision) && position == 0) {
                        position = 3;
                    }

                    // miscare stanga
                    if ((x > -precision && x < precision) && (y > -precision && y < precision) && (z > precision) && position == 0) {
                        position = 4;
                    }

                    // rotatie dreapta
                    if ((x > precision) && (y > -precision && y < precision) && (z > -precision && z < precision) && (position == 0 || position == 6)) {
                        if (position == 6) {
                            position = 7;
                        } else {
                            position = 5;
                        }
                    }

                    // rotatie stanga
                    if ((x < -precision) && (y > -precision && y < precision) && (z > -precision && z < precision) && (position == 0 || position == 5)) {
                        if (position == 5) {
                            position = 7;
                        } else {
                            position = 6;
                        }
                    }

                    // nu se mai misca
                    if ((x > -precision && x < precision) && (y > -precision && y < precision) && (z > -precision && z < precision) && position == 7) {
                        position = 0;
                    }


                /*a = repaus
                b = fata
                c = spate
                d = miscare dreapta
                e = miscare stanga
                f = rotatie dreapta
                g = rotatie stanga*/

                    if (position != oldPosition) {
                        // send command to robot
                        oldPosition = position;
                        switch (position) {
                            case 0:
                                System.out.println("wrote a");
                                writeOnBluetooth("a");
                                break;
                            case 1:
                                System.out.println("wrote b");
                                writeOnBluetooth("b");
                                break;
                            case 2:
                                System.out.println("wrote c");
                                writeOnBluetooth("c");
                                break;
                            case 3:
                                System.out.println("wrote d");
                                writeOnBluetooth("d");
                                break;
                            case 4:
                                System.out.println("wrote e");
                                writeOnBluetooth("e");
                                break;
                            case 5:
                                System.out.println("wrote f");
                                writeOnBluetooth("f");
                                break;
                            case 6:
                                System.out.println("wrote g");
                                writeOnBluetooth("g");
                                break;
                            case 7:
                                System.out.println("wrote a");
                                writeOnBluetooth("a");
                                break;
                        }
                    }
                }
            }

            switch (mainActivity.robotOptions) {
                case 0:
                    System.out.println("wrote a");
                    writeOnBluetooth("a");
                    started = false;
                    mainActivity.robotOptions = -1;
                    break;
                case 1:
                    started = true;
                    mainActivity.robotOptions = -1;
                    break;
                case 2:
                    System.out.println("wrote h");
                    writeOnBluetooth("h");
                    mainActivity.robotOptions = -1;
                    break;
                case 3:
                    System.out.println("wrote i");
                    writeOnBluetooth("i");
                    mainActivity.robotOptions = -1;
                    break;
                case 4:
                    System.out.println("wrote j");
                    writeOnBluetooth("j");
                    mainActivity.robotOptions = -1;
                    break;
                case 5:
                    System.out.println("wrote k");
                    writeOnBluetooth("k");
                    mainActivity.robotOptions = -1;
                    break;
                case 6:
                    System.out.println("wrote l");
                    writeOnBluetooth("l");
                    mainActivity.robotOptions = -1;
                    break;
            }
        }
    }

    //</editor-fold>

}
