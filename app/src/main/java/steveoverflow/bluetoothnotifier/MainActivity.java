package steveoverflow.bluetoothnotifier;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    BluetoothAdapter    btAdapter = null;
    BluetoothSocket     socket = null;
    BluetoothDevice     clock = null;
    OutputStream        btStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("BTMsg"));

        ToggleButton bluetoothToggle = (ToggleButton) findViewById(R.id.toggleBluetooth);
        bluetoothToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = ((ToggleButton) v).isChecked();

                if(on){
                    connectBluetooth();
                }else{
                    disconnectBluetooth();
                }
            }
        });

        ToggleButton hangoutsToggle = (ToggleButton) findViewById(R.id.toggleHangouts);
        hangoutsToggle.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(btStream == null){
                    return;
                }

                boolean on = ((ToggleButton) v).isChecked();

                if(on){
                    writeStream("+h");
                }else{
                    writeStream("-h");
                }
            }
        });

        ToggleButton facebookToggle = (ToggleButton) findViewById(R.id.toggleFacebook);
        facebookToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btStream == null){
                    return;
                }

                boolean on = ((ToggleButton) v).isChecked();

                if(on){
                    writeStream("+f");
                }else{
                    writeStream("-f");
                }
            }
        });

        ToggleButton otherToggle = (ToggleButton) findViewById(R.id.toggleOther);
        otherToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btStream == null){
                    return;
                }

                boolean on = ((ToggleButton) v).isChecked();

                if(on){
                    writeStream("+o");
                }else{
                    writeStream("-o");
                }
            }
        });

        Button setTimeButton = (Button) findViewById(R.id.setTime);
        setTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setArduinoTime();
            }
        });
    }

    private void setArduinoTime(){
        byte second, minute, hour, dayOfWeek, dayOfMonth, month, year;

        Calendar now = Calendar.getInstance();
        second      = (byte) now.get(Calendar.SECOND);
        minute      = (byte) now.get(Calendar.MINUTE);
        hour        = (byte) now.get(Calendar.HOUR_OF_DAY);
        dayOfWeek   = (byte) now.get(Calendar.DAY_OF_WEEK);
        dayOfMonth  = (byte) now.get(Calendar.DAY_OF_MONTH);
        month       = (byte) (now.get(Calendar.MONTH) + 1);
        year        = (byte) (now.get(Calendar.YEAR)-2000);

        byte[] byteArray = new byte[8];

        byteArray[0] = (byte) '@';
        byteArray[1] = second;
        byteArray[2] = minute;
        byteArray[3] = hour;
        byteArray[4] = dayOfWeek;
        byteArray[5] = dayOfMonth;
        byteArray[6] = month;
        byteArray[7] = year;

        try{
            btStream.write(byteArray);
        }catch(IOException e){
            disconnectBluetooth();
        }
    }

    private void writeStream(String message){
        try{
            btStream.write(message.getBytes());
        }catch(IOException e){
            disconnectBluetooth();
        }
    }

    protected BroadcastReceiver onNotice = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(btStream==null){
                return;
            }
            String pack = intent.getStringExtra("package");
            String mode = intent.getStringExtra("mode");

            if(pack.equalsIgnoreCase("com.google.android.talk")){
                writeStream((mode+"h"));
            }
            if(pack.equalsIgnoreCase("com.facebook.katana")){
                writeStream((mode+"f"));
            }
            //com.google.android.calendar
            if(pack.equalsIgnoreCase("com.google.android.calendar")){
                writeStream((mode+"o"));
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void connectBluetooth(){
        btAdapter = BluetoothAdapter.getDefaultAdapter();;

        if(!btAdapter.isEnabled()){
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if(pairedDevices.size()>0){
            for(BluetoothDevice device: pairedDevices){
                if(device.getName().equals("HC-05")){
                    clock = device;
                    break;
                }
            }
        }

        BluetoothSocket socket;

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID

        try {
            socket = clock.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            btStream = socket.getOutputStream();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnectBluetooth(){
        if(btStream != null){
            try{
                btStream.close();
            }catch(Exception e){}
            btStream = null;
        }

        if(socket != null){
            try{
                socket.close();
            }catch(Exception e){}
            socket = null;
        }

        ToggleButton bluetoothToggle = (ToggleButton) findViewById(R.id.toggleBluetooth);
        bluetoothToggle.setChecked(false);
    }
}
