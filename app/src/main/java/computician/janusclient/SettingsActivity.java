package computician.janusclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;

import computician.janusclient.R;


import android.view.KeyEvent;

import android.widget.EditText;


public class SettingsActivity extends AppCompatActivity {

    public static final String IPTVACCESS_IP = "access_ip";
    public static final String IPTVACCESS_PORT = "access_port";
    public static final String SETTING_CONFIG = "config";

    private EditText mEditaccessIp;
    private EditText mEditaccessPort;

    private SharedPreferences mPre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activety_setting);

        mEditaccessIp = (EditText)findViewById(R.id.edit_access_ip);
        mEditaccessPort = (EditText)findViewById(R.id.edit_access_port);

        mPre = getSharedPreferences(SETTING_CONFIG, MODE_PRIVATE);

        mEditaccessIp.setText(mPre.getString(IPTVACCESS_IP, "10.0.3.115"));
        mEditaccessPort.setText(mPre.getString(IPTVACCESS_PORT, "8188"));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            mPre.edit().putString(IPTVACCESS_PORT, mEditaccessPort.getText().toString()).apply();
            mPre.edit().putString(IPTVACCESS_IP, mEditaccessIp.getText().toString()).apply();
            mPre.edit().apply();

//            Intent intent = new Intent();
//            intent.setClass(this, JanusActivity.class);
//            startActivity(intent);

            finish();
        }

        return super.onKeyDown(keyCode, event);
    }

}