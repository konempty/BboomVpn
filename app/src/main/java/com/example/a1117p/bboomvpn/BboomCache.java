package com.example.a1117p.bboomvpn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BboomCache {
    Context context;

    public BboomCache(Context co) {
        context = co;
    }

    public File getBboomCacheDir(Context context) {
        File cacheDir;
        cacheDir = new File(context.getCacheDir().getAbsolutePath(), "/bboomcache/");

        // if (!(cacheDir.exists()&&cacheDir.isDirectory())) {
        cacheDir.mkdirs();
        // }

        return cacheDir;
    }


    public void Write(String name, Bitmap bitmap) throws IOException {
        File cacheDir = getBboomCacheDir(context);
        File cacheFile = new File(cacheDir.getAbsolutePath() + "/" + name);
        if (!cacheFile.exists()) {
            cacheFile.getParentFile().mkdirs();
            cacheFile.createNewFile();
        }

        FileOutputStream out = new FileOutputStream(cacheFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();
        out.close();
    }

    public Bitmap Read(String name) {
        Bitmap bitmap = null;
        File cacheDir = getBboomCacheDir(context);
        File cacheFile = new File(cacheDir.getAbsolutePath() + "/" + name);
        if (cacheFile.exists()) {
            bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
        }
        return bitmap;
    }


}


