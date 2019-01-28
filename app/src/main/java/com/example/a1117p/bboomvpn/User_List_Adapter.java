package com.example.a1117p.bboomvpn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class User_List_Adapter extends BaseAdapter {
    private ArrayList<User_List_Item> user_list_items = new ArrayList<>();

    @Override
    public int getCount() {
        return user_list_items.size();
    }

    @Override
    public Object getItem(int i) {
        return user_list_items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    ImageView imageView;
    TextView nicknae_text;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.user_list_item_view, parent, false);
        }
        imageView = convertView.findViewById(R.id.pf_img);
        nicknae_text = convertView.findViewById(R.id.nick);
        imageView.setImageBitmap(user_list_items.get(position).getPf_img());
        nicknae_text.setText(user_list_items.get(position).getNickName());
        return convertView;
    }

    void addItem(int user, String nick, String pf,Context context) {
        user_list_items.add(new User_List_Item(user, nick, pf, context));
    }
}
