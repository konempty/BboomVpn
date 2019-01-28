package com.example.a1117p.bboomvpn;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;

public class MySharedPreferences {
    private static SharedPreferences preference;
    private static SharedPreferences.Editor editor;
    private static Gson gson;
    static void init(Context context) {
        preference = context.getSharedPreferences("Bboom", Context.MODE_PRIVATE);
        editor = preference.edit();
        gson = new Gson();
    }
    static public HashMap<Integer, String> getHashmap(){
        String hash_str = preference.getString("Block list", "");
        HashMap<Integer, String> hashMap;
        if (hash_str.length() > 0) {
            Type type = new TypeToken<HashMap<Integer, String>>() {
            }.getType();
            hashMap = gson.fromJson(hash_str, type);
        } else {
            hashMap = new HashMap<>();
        }
        return hashMap;
    }
    static void commit_Block_usr(HashMap<Integer, String> hashMap){
        String hashMapString = gson.toJson(hashMap);
        editor.putString("Block list", hashMapString);
        editor.commit();
    }
    static void setBlock_Mov(boolean b){
        editor.putBoolean("Block_mov",b);
        editor.commit();
    }
    public static boolean getBlock_Mov(){
        return preference.getBoolean("Block_mov",false);
    }
}
