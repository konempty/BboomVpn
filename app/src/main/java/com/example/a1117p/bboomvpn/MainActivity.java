package com.example.a1117p.bboomvpn;

import android.content.Intent;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {
    Intent service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = VpnService.prepare(MainActivity.this);
                if (intent != null) {
                    startActivityForResult(intent, 0);
                } else {
                    onActivityResult(0, RESULT_OK, null);
                }
            }
        });
        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (VPNService.isRunning) {
                    service.putExtra("start", false);
                    startService(service);
                }
            }
        });
        findViewById(R.id.tobboom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("http://m.bboom.naver.com/");
                Intent it  = new Intent(Intent.ACTION_VIEW,uri);
                startActivity(it);
            }
        });
        findViewById(R.id.add_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, UserParseActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.block_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Block_List_Activity.class);
                startActivity(intent);
            }
        });
        MySharedPreferences.init(this);
        ToggleButton mSwitch = findViewById(R.id.mov_switch);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MySharedPreferences.setBlock_Mov(b);
            }
        });
        mSwitch.setChecked(MySharedPreferences.getBlock_Mov());

        ToggleButton cSwitch = findViewById(R.id.comment_switch);
        cSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MySharedPreferences.setCommentFilter(b);
            }
        });
        cSwitch.setChecked(MySharedPreferences.getCommentFilter());
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK && !VPNService.isRunning) {
            service = getServiceIntent();
            service.putExtra("start", true);
            startService(service);
        }
    }

    private Intent getServiceIntent() {
        return new Intent(this, VPNService.class);
    }

}
