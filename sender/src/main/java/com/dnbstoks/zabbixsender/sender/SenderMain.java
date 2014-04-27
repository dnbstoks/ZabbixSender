package com.dnbstoks.zabbixsender.sender;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;


public class SenderMain extends Activity {

    private static final String ET_ITEM_VALUE1 = "ET_ITEM_VALUE1";
    private static final String ET_ITEM_VALUE2 = "ET_ITEM_VALUE2";
    private static final String CB_RANDOM_ITEM_VALUE1 = "CB_RANDOM_ITEM_VALUE1";
    private static final String CB_RANDOM_ITEM_VALUE2 = "CB_RANDOM_ITEM_VALUE2";
    private static final String CB_HOST_GROUP2 = "CB_HOST_GROUP2";

    private static final String ITEM_VALUE1_RANDOM = "ITEM_VALUE1_RANDOM";
    private static final String ITEM_VALUE2_RANDOM = "ITEM_VALUE2_RANDOM";
    private static final String GOING_UP1 = "GOING_UP1";
    private static final String GOING_UP2 = "GOING_UP2";

    private static final String ITEM_VALUE1 = "ITEM_VALUE1";
    private static final String ITEM_VALUE2 = "ITEM_VALUE2";
    public SharedPreferences sharedPrefs;
    private TextView tvStatus;
    private TextView tvTerminal_tx;
    private TextView tvTerminal_rx;
    private Button btSend;
    private CheckBox cbRandomItemValue1;
    private CheckBox cbRandomItemValue2;
    private CheckBox cbConstantSend;
    private CheckBox cbHost2Group;
    private ScrollView tvTerminalTXScroll;
    private ScrollView tvTerminalRXScroll;
    private ProgressBar prbSending;
    private EditText etItemValue1;
    private EditText etItemValue2;
    private boolean sendPressed = false;
    private boolean CommsCompleted = true;
    private boolean networkConnected = false;
    private boolean host2Enabled = false;
    private boolean constantSend = false;
    private boolean itemValue1IsRandom, itemValue2IsRandom, goingUp1, goingUp2;
    private double itemValue1random, itemValue2random;
    private double itemValue1MIN = -10;
    private double itemValue1MAX = 10;
    private double itemValue2MIN = -10;
    private double itemValue2MAX = 10;
    private Socket socket;
    private String TCPResponce = "oops, response stayed empty";
    private String Hostname1, Hostname2, itemKey1, itemKey2,
            itemValue1, itemValue2;
    private int ZABBIX_PORT, generatorPeriod, TCPInTimeout, TCPOutTimeout;
    private String ZABBIX_IP;

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(GOING_UP1, goingUp1);
        outState.putBoolean(GOING_UP2, goingUp2);
        outState.putString(ITEM_VALUE1, itemValue1);
        outState.putString(ITEM_VALUE2, itemValue2);
        Log.i("ZabbixSender", "onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender_main);

        etItemValue1 = (EditText) findViewById(R.id.etItemValue1);
        etItemValue2 = (EditText) findViewById(R.id.etItemValue2);

        if(savedInstanceState == null){
            goingUp1 = goingUp2= true;
            itemValue1 = itemValue2= "0";
            Log.i("ZabbixSender", "savedInstanceState == null");
        }else{
            itemValue1 = savedInstanceState.getString(ITEM_VALUE1);
            itemValue2 = savedInstanceState.getString(ITEM_VALUE2);
            goingUp1 = savedInstanceState.getBoolean(GOING_UP1);
            goingUp2 = savedInstanceState.getBoolean(GOING_UP2);
            etItemValue1.setText(savedInstanceState.getString(ITEM_VALUE1));
            etItemValue2.setText(savedInstanceState.getString(ITEM_VALUE2));
            Log.i("ZabbixSender", "savedInstanceState NOT null");
        }

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvTerminal_tx = (TextView) findViewById(R.id.tvTerminal_tx);
        tvTerminal_rx = (TextView) findViewById(R.id.tvTerminal_rx);
        btSend = (Button) findViewById(R.id.btSend);
        cbRandomItemValue1 = (CheckBox) findViewById(R.id.cbRandomItemValue1);
        cbRandomItemValue2 = (CheckBox) findViewById(R.id.cbRandomItemValue2);
        cbConstantSend = (CheckBox) findViewById(R.id.cbConstantSend);
        cbHost2Group = (CheckBox) findViewById(R.id.cbHost2Group );
        tvTerminalTXScroll = (ScrollView) findViewById(R.id.scrollView1);
        tvTerminalRXScroll = (ScrollView) findViewById(R.id.scrollView2);
        prbSending = (ProgressBar) findViewById(R.id.prbSending);
        prbSending.setVisibility(100);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        cbRandomItemValue1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (cbRandomItemValue1.isChecked()){
                    itemValue1IsRandom=true;
                    cbConstantSend.setEnabled(true);
                    if (!etItemValue1.getText().toString().trim().isEmpty()) {
                        itemValue1random = Double.parseDouble(etItemValue1.getText().toString().trim());
                    } else {
                        itemValue1random = 0;
                    }
                    if (!itemValue2IsRandom) new randomTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }else{
                    itemValue1IsRandom=false;
                    if (!itemValue2IsRandom) {
                        cbConstantSend.setChecked(false);
                        cbConstantSend.setEnabled(false);
                    }
                }
            }
        });
        cbRandomItemValue2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (cbRandomItemValue2.isChecked()){
                    itemValue2IsRandom=true;
                    cbConstantSend.setEnabled(true);
                    if (!etItemValue2.getText().toString().trim().isEmpty()) {
                        itemValue2random = Double.parseDouble(etItemValue2.getText().toString().trim());
                    } else {
                        itemValue2random = 0;
                    }
                    if (!itemValue1IsRandom) new randomTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }else{
                    itemValue2IsRandom=false;
                    if (!itemValue1IsRandom) {
                        cbConstantSend.setChecked(false);
                        cbConstantSend.setEnabled(false);
                    }
                }
            }
        });
        cbConstantSend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (cbConstantSend.isChecked()){
                    constantSend=true;
                }else{
                    constantSend=false;
                }
            }
        });
        cbHost2Group.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (cbHost2Group.isChecked()){
                    etItemValue2.setEnabled(true);
                    cbRandomItemValue2.setEnabled(true);
                    host2Enabled=true;
                }else{
                    etItemValue2.setEnabled(false);
                    cbRandomItemValue2.setEnabled(false);
                    cbRandomItemValue2.setChecked(false);
                    host2Enabled=false;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("ZabbixSender", "called onResume");
        getSettings();
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected() && networkInfo.isAvailable()){
            networkConnected = true;
            tvStatus.setText("Network connection is active");
        }else{
            networkConnected = false;
            warningMessage("There is no active network connection! Close the app, turn WiFi or Mobile Network and try again.");
            tvStatus.setText("No active network connection!");
        }

    }

    @Override
    protected void onPause() {
        if (sendPressed) SendSwitch();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (itemValue1IsRandom) cbRandomItemValue1.setChecked(false);
        if (itemValue2IsRandom) cbRandomItemValue2.setChecked(false);
        if (sendPressed) SendSwitch();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sender_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, Settings.class));
        }

        if (id == R.id.action_about_app) {
            openAbout();
        }

        if (id == R.id.action_about_sender) {
            startActivity(new Intent(this, AboutSender.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void getSettings(){
        String generatorPeriod_default = "200";
        generatorPeriod = Integer.parseInt(sharedPrefs.getString("settings_generatorPeriod", generatorPeriod_default).trim());
        if (generatorPeriod<50) generatorPeriod=50;
        String TCPInTimeoutDefault = "5000";
        TCPInTimeout = Integer.parseInt(sharedPrefs.getString("settings_TCPinTimeout", TCPInTimeoutDefault).trim());
        String TCPOutTimeoutDefault = "5000";
        TCPOutTimeout = Integer.parseInt(sharedPrefs.getString("settings_TCPoutTimeout", TCPOutTimeoutDefault).trim());
        String server_ip_deffault = "192.168.1.1";
        ZABBIX_IP = sharedPrefs.getString("settings_server_ip", server_ip_deffault).trim();
        String server_port_default = "10051";
        ZABBIX_PORT = Integer.parseInt(sharedPrefs.getString("settings_server_port", server_port_default).trim());
        String hostname1default = "hostname1";
        Hostname1 = sharedPrefs.getString("settings_hostname1", hostname1default).trim();
        boolean hostname2differ1 = sharedPrefs.getBoolean("settings_hostname2differ1", false);
        if (hostname2differ1) {
            String hostname2default = "hostname2";
            Hostname2 = sharedPrefs.getString("settings_hostname2", hostname2default).trim();
        }else{
            Hostname2 = Hostname1;
        }
        String itemKey1default = "itemkey1";
        itemKey1 = sharedPrefs.getString("settings_itemKey1", itemKey1default).trim();
        String itemKey2default = "itemkey2";
        itemKey2 = sharedPrefs.getString("settings_itemKey2", itemKey2default).trim();
    }

    private void SendRequest(){

        if (sendPressed && CommsCompleted && networkConnected) {
            CommsCompleted = false;
            prbSending.setVisibility(View.VISIBLE);
            new MyAsyncTask().execute();
        }
    }

    private void SendSwitch(){
        if (CommsCompleted){
            if (sendPressed) {
                sendPressed = false;
                btSend.setText(getString(R.string.btSendSend));
                tvStatus.setText("Sending Stopped");
            }else{
                sendPressed = true;
                btSend.setText(getString(R.string.btSendStop));
                tvStatus.setText("Sending...");
                SendRequest();
                if (!etItemValue1.getText().toString().trim().isEmpty()) {
                    itemValue1 = etItemValue1.getText().toString().trim();
                } else {
                    itemValue1 = "0";
                }
                if (!etItemValue2.getText().toString().trim().isEmpty()) {
                    itemValue2 = etItemValue2.getText().toString().trim();
                } else {
                    itemValue2 = "0";
                }
            }
        }
    }

    public void btSendClick(View view){
        SendSwitch();
    }

    private String buildJSonString(String host, String item, String value)
    {
        return 		  "{"
                + "\"request\":\"sender data\",\n"
                + "\"data\":[\n"
                +        "{\n"
                +                "\"host\":\"" + host + "\",\n"
                +                "\"key\":\"" + item + "\",\n"
                +                "\"value\":\"" + value.replace("\\", "\\\\") + "\"}\n"
                + "]}\n" ;
    }

    private String buildJSonString2(String host1, String item1, String value1, String host2, String item2, String value2)
    {
        return 		  "{"
                + "\"request\":\"sender data\",\n"
                + "\"data\":[\n"
                +        "{\n"
                +                "\"host\":\"" + host1 + "\",\n"
                +                "\"key\":\"" + item1 + "\",\n"
                +                "\"value\":\"" + value1.replace("\\", "\\\\") + "\"},\n"
                +        "{\n"
                +                "\"host\":\"" + host2 + "\",\n"
                +                "\"key\":\"" + item2 + "\",\n"
                +                "\"value\":\"" + value2.replace("\\", "\\\\") + "\"}\n"
                + "]}\n" ;
    }

    protected void writeMessage(OutputStream out, byte[] data) throws IOException {
        int length = data.length;

        out.write(new byte[] {
                'Z', 'B', 'X', 'D',
                '\1',
                (byte)(length & 0xFF),
                (byte)((length >> 8) & 0x00FF),
                (byte)((length >> 16) & 0x0000FF),
                (byte)((length >> 24) & 0x000000FF),
                '\0','\0','\0','\0'});

        out.write(data);
    }

    public void openAbout(){

        AlertDialog.Builder aboutApp = new AlertDialog.Builder(this);
        aboutApp
                .setMessage("The app is designed for testing the connection to Zabbix Server. " +
                        "Please see www.zabbix.org for more info about Zabbix")
                .setTitle("ZABBIX Sender")
                .setPositiveButton("Rate App",new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        Uri uriUrl = Uri.parse("http://superuser.com/");
                        startActivity(new Intent(Intent.ACTION_VIEW, uriUrl));
                    }
                })
                .setNegativeButton("Understood",new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .show();
    }

    public void warningMessage(String message){

        AlertDialog.Builder aboutApp = new AlertDialog.Builder(this);
        aboutApp
                .setMessage(message)
                .setTitle("Warning message")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .show();
    }

    private class randomTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            long startTime = System.currentTimeMillis();
            while (itemValue1IsRandom || itemValue2IsRandom) {
                if (System.currentTimeMillis() - startTime > generatorPeriod) {
                    startTime = System.currentTimeMillis();
                    if (itemValue1IsRandom) {
                        if (itemValue1random < itemValue1MIN) goingUp1 = true;
                        if (itemValue1random > itemValue1MAX) goingUp1 = false;
                        if (goingUp1) itemValue1random = itemValue1random + 0.1;
                        else itemValue1random = itemValue1random - 0.1;
                        itemValue1 = String.valueOf(round(itemValue1random, 2));
                    }
                    if (itemValue2IsRandom) {
                        if (itemValue2random < itemValue2MIN) goingUp2 = true;
                        if (itemValue2random > itemValue2MAX) goingUp2 = false;
                        if (goingUp2) itemValue2random = itemValue2random + 0.1;
                        else itemValue2random = itemValue2random - 0.1;
                        itemValue2 = String.valueOf(round(itemValue2random, 2));
                    }

                    publishProgress();
                }
            }
            return null;
        }
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            etItemValue1.setText(itemValue1);
            etItemValue2.setText(itemValue2);
            if (constantSend && sendPressed) SendRequest();
        }
    }

    private class MyAsyncTask extends AsyncTask<String, Void, String> {
        StringBuilder report = new StringBuilder();
        boolean connectionError = false;
        @Override
        protected String doInBackground(String... params) {
            String item1 = buildJSonString(Hostname1, itemKey1, itemValue1);
            String item2 = buildJSonString2(Hostname1, itemKey1, itemValue1, Hostname2, itemKey2, itemValue2);
            if (host2Enabled) report.append(item2);
            else report.append(item1);
            try {
                SocketAddress sockaddr = new InetSocketAddress(ZABBIX_IP, ZABBIX_PORT);
                socket = new Socket();
                socket.connect(sockaddr, TCPOutTimeout);
                DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
                writeMessage(outToServer, report.toString().getBytes());
                outToServer.flush();
                long startTime = System.currentTimeMillis();
                while (true) {
                    StringBuilder json = new StringBuilder();
                    if (socket.getInputStream().available() > 0) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String str;
                        while ((str = in.readLine()) != null) {
                            json.append(str).append("\n");
                        }
                        in.close();
                        TCPResponce = String.valueOf(json);
                        break;
                    } else {
                        if (System.currentTimeMillis() - startTime > TCPInTimeout) {
                            TCPResponce = "Zabbix not detected with IP " + ZABBIX_IP + " and Port " + String.valueOf(ZABBIX_PORT);
                            break;
                        }
                    }
                }
                socket.close();
            } catch (UnknownHostException e) {
                TCPResponce = "Zabbix not detected with IP " + ZABBIX_IP
                        + " and Port " + String.valueOf(ZABBIX_PORT)
                        + ". Service message: " + e.getMessage();
                connectionError = true;
            } catch (IOException e) {
                TCPResponce = "Zabbix not detected with IP " + ZABBIX_IP
                        + " and Port " + String.valueOf(ZABBIX_PORT)
                        + ". Service message: " + e.getMessage();
                connectionError = true;
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
            prbSending.setVisibility(View.INVISIBLE);
            CommsCompleted = true;

            tvTerminal_tx.setText("Sent to Zabbix with IP " + ZABBIX_IP + " and Port " + String.valueOf(ZABBIX_PORT)+"\n"+report);
            tvTerminalTXScroll.smoothScrollTo(0, tvTerminal_tx.getBottom());

            tvTerminal_rx.setText(TCPResponce);
            tvTerminalRXScroll.smoothScrollTo(0, tvTerminal_rx.getBottom());
            if (!constantSend || connectionError) SendSwitch();
        }
    }
}
