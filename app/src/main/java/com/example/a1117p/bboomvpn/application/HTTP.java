package com.example.a1117p.bboomvpn.application;

import android.util.Pair;

import java.util.List;

/**
 * Created by Lipi on 2017. 3. 28..
 */

public class HTTP implements IApplication {
    private List<Pair<String, String>> httpHeaders;
    private byte[] body;

    HTTP(List<Pair<String, String>> httpHeaders, byte[] body) {
        this.httpHeaders = httpHeaders;
        this.body = body;
    }

    public List<Pair<String, String>> getHttpHeaders() {
        return httpHeaders;
    }

    public byte[] getBody() {
        return body;
    }
}
