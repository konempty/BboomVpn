package com.example.a1117p.bboomvpn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class User_List_Item {
    private int user_no;
    private String nickName;
    private Bitmap pf_img;

    User_List_Item(int user, String nick, String pf, final Context context) {
        user_no = user;
        nickName = nick;

        if (pf.indexOf('/') == 0)
            pf = "http://m.bboom.naver.com" + pf;
        final String finalPf = pf;
        new Thread(new Runnable() {
            @Override
            public void run() {
                BboomCache bboomCache = new BboomCache(context);
                if ((pf_img = bboomCache.Read(finalPf)) == null) {
                    try {
                        URL url1 = new URL(finalPf);
                        HttpURLConnection connection = (HttpURLConnection) url1.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream is = connection.getInputStream();
                        pf_img = BitmapFactory.decodeStream(is);
                        bboomCache.Write(finalPf,pf_img);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }

    public Bitmap getPf_img() {
        return pf_img;
    }

    public int getUser_no() {
        return user_no;
    }

    public String getNickName() {
        return nickName;
    }
}
