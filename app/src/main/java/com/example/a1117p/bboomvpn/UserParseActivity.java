package com.example.a1117p.bboomvpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;


public class UserParseActivity extends Activity {


    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_parse);
        final EditText editText = findViewById(R.id.find_url);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    final String url = editText.getText().toString();


                    if (url.contains("profile/home.nhn") || (url.contains("postNo=") && url.contains("boardNo="))) {
                        dialog = ProgressDialog.show(UserParseActivity.this, "기다려주세요", "유저정보를 불러오는중입니다.", true, false);
                        dialog.setCancelable(true);
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                finish();
                            }
                        });
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String nick = "";
                                int UserNo = 0;
                                try {
                                    Document document = Jsoup.connect(url).get();
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    dialog.dismiss();
                                    Elements elements = document.select(".nick");
                                    nick = elements.text();
                                    if (url.contains("profile/home.nhn")) {
                                        String[] strings = url.split("userNo=");
                                        int i, len = strings[1].length();
                                        char ch;
                                        for (i = 0; i < len; i++) {
                                            ch = strings[1].charAt(i);
                                            if (ch < '0' || ch > '9')
                                                break;

                                            UserNo = UserNo * 10 + ch - '0';
                                        }
                                    } else
                                        UserNo = Integer.parseInt(elements.attr("href").replace("/profile/home.nhn?userNo=", ""));

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                final String finalNick = nick;
                                final int finalUserNo = UserNo;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AlertDialog.Builder dialog = new AlertDialog.Builder(UserParseActivity.this);
                                        dialog.setMessage(finalNick + "님을 차단하시겠습니까?")
                                                .setTitle("유저 차단")
                                                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        HashMap<Integer, String> hashMap = MySharedPreferences.getHashmap();
                                                        if (!hashMap.containsKey(finalUserNo)) {
                                                            hashMap.put(finalUserNo, finalNick);
                                                            MySharedPreferences.commit_Block_usr(hashMap);
                                                        }
                                                        finish();
                                                    }
                                                })
                                                .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                    }
                                                })
                                                .create().show();
                                    }
                                });

                            }
                        }).start();
                    } else {
                        Toast.makeText(UserParseActivity.this, "잘못된 입력입니다.", Toast.LENGTH_LONG).show();
                        editText.setText("");
                    }
                    handled = true;
                }
                return handled;
            }
        });
        findViewById(R.id.find_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                final String url = editText.getText().toString();


                if (url.contains("profile/home.nhn") || (url.contains("postNo=") && url.contains("boardNo="))) {
                    dialog = ProgressDialog.show(UserParseActivity.this, "기다려주세요", "유저정보를 불러오는중입니다.", true, false);
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            finish();
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String nick = "";
                            int UserNo = 0;
                            try {
                                Document document = Jsoup.connect(url).get();
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                dialog.dismiss();
                                Elements elements = document.select(".nick");
                                nick = elements.text();
                                if (url.contains("profile/home.nhn")) {
                                    String[] strings = url.split("userNo=");
                                    int i, len = strings[1].length();
                                    char ch;
                                    for (i = 0; i < len; i++) {
                                        ch = strings[1].charAt(i);
                                        if (ch < '0' || ch > '9')
                                            break;

                                        UserNo = UserNo * 10 + ch - '0';
                                    }
                                } else
                                    UserNo = Integer.parseInt(elements.attr("href").replace("/profile/home.nhn?userNo=", ""));

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            final String finalNick = nick;
                            final int finalUserNo = UserNo;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder dialog = new AlertDialog.Builder(UserParseActivity.this);
                                    dialog.setMessage(finalNick + "님을 차단하시겠습니까?")
                                            .setTitle("유저 차단")
                                            .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    HashMap<Integer, String> hashMap = MySharedPreferences.getHashmap();
                                                    if (!hashMap.containsKey(finalUserNo)) {
                                                        hashMap.put(finalUserNo, finalNick);
                                                        MySharedPreferences.commit_Block_usr(hashMap);
                                                    }
                                                    finish();
                                                }
                                            })
                                            .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                }
                                            })
                                            .create().show();
                                }
                            });
                        }
                    }).start();
                } else {
                    Toast.makeText(UserParseActivity.this, "잘못된 입력입니다.", Toast.LENGTH_LONG).show();
                    editText.setText("");
                }}
        });
    }







}
