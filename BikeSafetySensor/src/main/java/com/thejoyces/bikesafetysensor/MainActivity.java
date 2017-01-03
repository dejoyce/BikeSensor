package com.thejoyces.bikesafetysensor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    Button startButton, sendButton, clearButton, stopButton;
    TextView textView;
    EditText editText;
    EditText editPhoneNumber;
    UsbManager usbManager;
    Button buttonStatus;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    private LocationManager locationMangaer = null;
    private LocationListener locationListener = null;
    private Boolean flag = false;

    private Boolean connected = false;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                textviewAppendData(textView, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                textviewAppendData(textView, e.getStackTrace().toString());
            }


        }
    };
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.

                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            textviewAppendData(textView, "Serial Connection Opened!");

                            connected = true;

                            updateStatus("Connected to Arduino");
                            Toast.makeText(getApplicationContext(), "Connected to Arduino",
                                    Toast.LENGTH_LONG).show();


                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                            connected = false;
                            textviewAppendData(textView, "Port not Opened!");
                            updateStatus("Port not Opened!");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                        connected = false;
                        textviewAppendData(textView, "Port is NULL");
                        updateStatus("Port is NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                    connected = false;
                    textviewAppendData(textView, "Perm not granted!");
                    updateStatus("Perm not granted!");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                textviewAppendData(textView, "UsbManager.ACTION_USB_DEVICE_ATTACHED)");
                initializeUSBDevices(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                textviewAppendData(textView, "UsbManager.ACTION_USB_DEVICE_DETACHED)");
                closeSerialPort(stopButton);

            }
        }

        ;
    };

    public void initializeUSBDevices(View view) {

       // textviewAppendData(textView, "initializeUSBDevices");
        Log.d("SERIAL", "initializeUSBDevices");

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                textviewAppendData(textView, "initializeUSBDevices - deviceId" + device.getDeviceName() + "   " + device.getManufacturerName() + "   " + device.getDeviceId());
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                    textviewAppendData(textView, "Found Arduino Vendor ID");
                    updateStatus("Found Arduino USB Board");
                } else {
                    connection = null;
                    device = null;
                    textviewAppendData(textView, "Not an Arduino Vendor");
                }
                if (!keep)
                    break;
            }
        } else {
            textviewAppendData(textView, "initializeUSBDevices - No USB Devices");
            updateStatus("No USB Devices");
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    public void getGPSLocationAndSendText( View view )
    {
        flag = displayGpsStatus();
        if (flag) {
            textviewAppendData(textView, "Cannot get the GPS coordinates - Move a bit");
            // editLocation.setText("Please!! move your device to"+
            //          " see the changes in coordinates."+"\nWait..");

            // pb.setVisibility(View.VISIBLE);
            locationListener = new MyLocationListener();

            try {
                locationMangaer.requestLocationUpdates(LocationManager
                        .GPS_PROVIDER, 5000, 10, locationListener);
            } catch (Exception ex) {
                alertbox("Gps Status!!", "Your GPS is: OFF" + ex.getMessage());
            }


        } else {
            alertbox("Gps Status!!", "Your GPS is: OFF");
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(editPhoneNumber.getText().toString(), null, "sms message", null, null);
            Toast.makeText(getApplicationContext(), "SMS Sent!",
                    Toast.LENGTH_LONG).show();
            textviewAppendData(textView, "SMS Sent!");
        } catch (Exception ex) {
            ex.printStackTrace();

            textviewAppendData(textView, ex.getMessage());
            alertbox("cannot send sms", ex.getMessage());
        }
    }

    public void sendDataViaSerialPort(View view) {
        String string = editText.getText().toString();

        try {
            serialPort.write(string.getBytes());
            textviewAppendData(textView, "Serial Data Sent: " + string + "");
        }
        catch( Exception ex)
        {
            textviewAppendData(textView, "Error sending data: " + ex.getMessage());
        }
    }

    public void closeSerialPort(View view) {
        try {
            serialPort.close();
            textviewAppendData(textView, "Serial Connection Closed!");
        }
        catch( Exception ex)
        {
            textviewAppendData(textView, "Error Closing Serial Connection" + ex.getMessage());
        }
    }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    private void textviewAppendData(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        Calendar c = Calendar.getInstance();
        final String formattedDate = df.format(c.getTime());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.setMovementMethod(new ScrollingMovementMethod());

                String message = formattedDate + ": " + ftext.toString() + "\n";
                ftv.setText(message + ftv.getText());
             }
        });
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    /*----Method to Check GPS is enable or disable ----- */
    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;

        } else {
            return false;
        }
    }

    protected void alertbox(String title, String mymessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your Device's GPS is Disable")
                .setCancelable(false)
                .setTitle("** Gps Status **")
                .setPositiveButton("Gps On",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // finish the current activity
                                // AlertBoxAdvance.this.finish();
                                Intent myIntent = new Intent(
                                        Settings.ACTION_SECURITY_SETTINGS);
                                startActivity(myIntent);
                                dialog.cancel();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // cancel the dialog box
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      // setContentView(R.layout.layout);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        buttonStatus = (Button) findViewById(R.id.buttonStatus);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        editText = (EditText) findViewById(R.id.editText);
        editPhoneNumber = (EditText) findViewById(R.id.editPhoneNumber);

        buttonStatus.requestFocus();


        textView = (TextView) findViewById(R.id.textView);
        textView.setText("");
        // textView.setMovementMethod(new ScrollingMovementMethod());

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        locationMangaer = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        editPhoneNumber.setText("0876238219");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();


        //private final int interval = 10000; // 1 Second
         final Handler handler = new Handler();
         Runnable runnable = new Runnable(){
            public void run() {
                initializeUSBDevices( null );

                handler.postDelayed(this, 10000);
            }
        };

        handler.postAtTime(runnable, System.currentTimeMillis()+2000);
        handler.postDelayed(runnable, 2000);



    /*    Timer timerLoopScan = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                initializeUSBDevices( null );
            });
        }
    };
    timerLoopScan.scheduleAtFixedRate(timerTask, 0, scanPeriod);
    */
    }

    public void updateStatus(String message)
    {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        Calendar c = Calendar.getInstance();
        final String formattedDate = df.format(c.getTime());

        this.buttonStatus.setText("Status: " + message + "(" + formattedDate + ")");
    }

    /*----------Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {

            Toast.makeText(getBaseContext(), "Location changed : Lat: " +
                            loc.getLatitude() + " Lng: " + loc.getLongitude(),
                    Toast.LENGTH_SHORT).show();

            String longitude = "Longitude: " + loc.getLongitude();

            textviewAppendData(textView, "longitude " + longitude);

            String latitude = "Latitude: " + loc.getLatitude();
            textviewAppendData(textView, "Latitude " + latitude);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider,
                                    int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
    }
}

