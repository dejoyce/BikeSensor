package com.thejoyces.bikesafetysensor;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    public final String ACTION_USB_PERMISSION = "com.thejoyces.bikesafetysensor.USB_PERMISSION";
    Button startButton, sendButton, clearButton, stopButton;
    TextView textView;
    EditText editTextSerialCommandToSend;
    EditText editPhoneNumber;
    TextView textViewNumVehiclesUnder15;
    TextView numTimesBikeFallen;
    UsbManager usbManager;
    Button buttonStatus;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    RadioButton radioButtonSoundBuzzer;
    RadioButton radioButtonLightLED;
    RadioButton radioButtonDisplayLCD;
    RadioButton radioButtonSendSerialCommand;
    RadioButton radioButtonSendSMSTest;
    ViewFlipper vf;
    ImageButton buttonFlipper;

    int numVehiclesWithin15 = 0;
    int numTimeBikeFallen = 0;

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
                textviewAppendData(textView, "Received Data: " + data);

                if( data.startsWith("2"))
                {
                    numTimeBikeFallen++;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textviewAppendData(textView, "Tilt Sensor Activated");
                            numTimesBikeFallen.setText(numTimeBikeFallen + " times the bicycle has fallen");
                            getGPSLocationAndSendText(null);
                        }
                    });
                }
                else if( data.startsWith("3"))
                {
                    numVehiclesWithin15++;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textviewAppendData(textView, "Vehicle passed within 1.5m");
                            textViewNumVehiclesUnder15.setText(numVehiclesWithin15 + " Vehicles passed within 1.5meters");
                        }
                    });
                }
            } catch (UnsupportedEncodingException e) {
                textviewAppendData(textView, "onReceivedData: " + e.getMessage() );
            }
            catch( Exception e )
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                sw.toString(); // stack trace as a string

                textviewAppendData(textView, "onReceivedData: " + e.getMessage()  + sw.toString() );
            }

        }
    };
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                buttonStatus.setBackgroundColor(android.graphics.Color.RED);

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

                            buttonStatus.setBackgroundColor(Color.GREEN);
                        } else {
                            connected = false;
                            textviewAppendData(textView, "Port not Opened!");
                            updateStatus("Port not Opened!");
                        }
                    } else {
                        connected = false;
                        textviewAppendData(textView, "Port is NULL");
                        updateStatus("Port is NULL");
                    }
                } else {
                    connected = false;
                    textviewAppendData(textView, "Perm not granted!");
                    updateStatus("Perm not granted!");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                textviewAppendData(textView, "USB Cable plugged in");
                initializeUSBDevices(startButton);

             /*   final Handler handler = new Handler();
                Runnable runnable = new Runnable(){
                    public void run() {
                        initializeUSBDevices( null );

                      //  handler.postDelayed(this, 10000);
                    }
                };

                handler.postAtTime(runnable, System.currentTimeMillis()+6000);
                handler.postDelayed(runnable, 6000);
*/

            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                textviewAppendData(textView, "USB Cable unplugged");
                closeSerialPort(stopButton);

            }
        }
        catch( Exception e )
        {
            textviewAppendData(textView, "BroadcastReceiver: " + e.getMessage());
        }
        }

        ;
    };



    public void initializeUSBDevices(View view) {
try {
    Log.d("SERIAL", "initializeUSBDevices");

    if (connected == true) {
        textviewAppendData(textView, "Arduino already Connected");
        return;
    }

    updateStatus("Trying to connect to Ardunio");

    buttonStatus.setBackgroundColor(android.graphics.Color.RED);

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
                textviewAppendData(textView, "Found Arduino Board");
                updateStatus("Found Arduino Board");

                buttonStatus.setBackgroundColor(Color.GREEN);
            } else {
                connection = null;
                device = null;
                textviewAppendData(textView, "Not an Arduino Vendor");
            }
            if (!keep)
                break;

        }
    } else {
        textviewAppendData(textView, "No USB Devices");
        updateStatus("No USB Devices");
        buttonStatus.setBackgroundColor(android.graphics.Color.RED);

    }
}
catch( Exception ex)
{
    textviewAppendData(textView, "initializeUSBDevices " + ex.getMessage());
}
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;






    public void getGPSLocationAndSendText(View view) {

        textviewAppendData(textView, "Requesting GPS Location");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //alertbox("Gps Status!!", "No permission to get GPS");
            textviewAppendData(textView, "No permission to get GPS");
            return;
        }

        Location locationGPS = locationMangaer.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationMangaer.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        String logString;

        Location locationToUse = locationNet;
        if ( 0 < GPSLocationTime - NetLocationTime ) {
            locationToUse = locationGPS;
            logString = "GPS Loc ";
        }
        else
        {
            logString = "Network Loc ";
            //textviewAppendData(textView, "Using Network Location");
        }

        if( locationToUse == null )
        {
            textviewAppendData(textView, "Cannot get location");
            return;
        }

        textviewAppendData(textView, logString + locationToUse.getLatitude() + ", " + locationToUse.getLongitude());

        try {
            SmsManager smsManager = SmsManager.getDefault();

           // String msg = "Warning Alert from Bob while out cycling, his location is.\ncomgooglemaps://?center=" + locationToUse.getLatitude() + "," + locationToUse.getLongitude() + "&zoom=14&views=traffic&mapmode=standard&views=traffic";
            String msg = "Warning Alert from Bob while out cycling, his location is.\nhttp://maps.google.com/?q=" + locationToUse.getLatitude() + "," + locationToUse.getLongitude();

            smsManager.sendTextMessage(editPhoneNumber.getText().toString(), null, msg, null, null);
              Toast.makeText(getApplicationContext(), "SMS Sent to " + editPhoneNumber.getText().toString(),
                    Toast.LENGTH_SHORT).show();
            textviewAppendData(textView, "SMS Sent to " + editPhoneNumber.getText().toString());
        } catch (Exception ex) {
            ex.printStackTrace();

            textviewAppendData(textView, ex.getMessage());
        }
    }

    public void sendDataViaSerialPort(View view) {

        String dataToSend = "";
        if( radioButtonSoundBuzzer.isChecked())
        {
            dataToSend = "a";
        }
        else if( radioButtonLightLED.isChecked())
        {
            dataToSend = "b";
        }
        else if( radioButtonDisplayLCD.isChecked())
        {
            dataToSend = "c";
        }
        else if( radioButtonSendSerialCommand.isChecked())
        {
            dataToSend = editTextSerialCommandToSend.getText().toString();
        }
        else if( radioButtonSendSMSTest.isChecked())
        {
            getGPSLocationAndSendText( null );
            return;
        }

        try {
            if( serialPort != null ) {
                serialPort.write(dataToSend.toString().getBytes());

                textviewAppendData(textView, "Serial Data Sent: " + dataToSend + "");
            }
            else
            {
                textviewAppendData(textView, "Cannot send data - serial port not connected");
            }
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
            connected = false;

            updateStatus("Trying to connect to Ardunio");
            buttonStatus.setBackgroundColor(android.graphics.Color.RED);
        }
        catch( Exception ex)
        {
            textviewAppendData(textView, "Error Closing Serial Connection" + ex.getMessage());
        }
    }

    public void onViewFlip(View view) {
            vf.showNext();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      // setContentView(R.layout.layout);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        buttonStatus = (Button) findViewById(R.id.buttonStatus);
        sendButton = (Button) findViewById(R.id.buttonTest);
        clearButton = (Button) findViewById(R.id.buttonClear);
        editTextSerialCommandToSend = (EditText) findViewById(R.id.editTextSerialCommandToSend);
        editPhoneNumber = (EditText) findViewById(R.id.editPhoneNumber);
        textViewNumVehiclesUnder15 = (TextView) findViewById(R.id.textViewNumVehiclesUnder15);
        numTimesBikeFallen = (TextView) findViewById(R.id.numTimesBikeFallen);
        radioButtonSoundBuzzer = (RadioButton) findViewById(R.id.radioButtonSoundBuzzer);
        radioButtonLightLED = (RadioButton) findViewById(R.id.radioButtonLightLED);
        radioButtonDisplayLCD = (RadioButton) findViewById(R.id.radioButtonDisplayLCD);
        radioButtonSendSerialCommand = (RadioButton) findViewById(R.id.radioButtonSendSerialCommand);
        radioButtonSendSMSTest = (RadioButton) findViewById(R.id.radioButtonSendTestSMS);
        vf = (ViewFlipper) findViewById( R.id.viewFlipper );
        buttonFlipper = (ImageButton) findViewById( R.id.imageButtonFlipper );

        buttonStatus.requestFocus();

        textView = (TextView) findViewById(R.id.textView);
        textView.setText("");

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        locationMangaer = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        editPhoneNumber.setText("0876238219");

        initializeUSBDevices( null );
    }

    public void updateStatus(String message)
    {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        Calendar c = Calendar.getInstance();
        final String formattedDate = df.format(c.getTime());

       // this.buttonStatus.setText("Status: " + message + "(" + formattedDate + ")");
        this.buttonStatus.setText("Status: " + message );
    }
}

