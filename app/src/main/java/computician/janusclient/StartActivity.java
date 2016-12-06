package computician.janusclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    public Button m_setbtn;
    public Button m_joinbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        m_setbtn = (Button)findViewById(R.id.set_btn);
        m_joinbtn=(Button)findViewById(R.id.joinroom_btn);


    }

    public void btn_joinroom(View v){

        SharedPreferences ps = getSharedPreferences(SettingsActivity.SETTING_CONFIG, MODE_PRIVATE);
        String ip = ps.getString(SettingsActivity.IPTVACCESS_IP, "");
        String port = ps.getString(SettingsActivity.IPTVACCESS_PORT, "");
        Intent intent = new Intent();

            intent.setClass(this, JanusActivity.class);

        startActivity(intent);
    }
    public void btn_setParam(View v){
        SharedPreferences ps = getSharedPreferences(SettingsActivity.SETTING_CONFIG, MODE_PRIVATE);
        String ip = ps.getString(SettingsActivity.IPTVACCESS_IP, "");
        String port = ps.getString(SettingsActivity.IPTVACCESS_PORT, "");
        Intent intent = new Intent();
        intent.setClass(this, SettingsActivity.class);


        startActivity(intent);
    }

}
