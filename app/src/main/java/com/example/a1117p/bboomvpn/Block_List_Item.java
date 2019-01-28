package com.example.a1117p.bboomvpn;

public class Block_List_Item {
    private int userno;
    private String nick_name;
    Block_List_Item(int usr, String name){
        userno = usr;
        nick_name = name;
    }

    public int getUserno() {
        return userno;
    }

    public String getNick_name() {
        return nick_name;
    }
}
