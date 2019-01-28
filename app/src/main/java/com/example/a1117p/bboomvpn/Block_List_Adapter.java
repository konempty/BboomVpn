package com.example.a1117p.bboomvpn;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class Block_List_Adapter extends BaseAdapter {
    private ArrayList<Block_List_Item> block_list_items = new ArrayList<>();
    HashMap<Integer, String> hashMap;

    Block_List_Adapter() {
        hashMap = MySharedPreferences.getHashmap();
        if (hashMap.size() > 0) {
            Integer[] keys = hashMap.keySet().toArray(new Integer[1]);
            for (int tmp : keys)
                block_list_items.add(new Block_List_Item(tmp, hashMap.get(tmp)));
        }
    }

    @Override
    public int getCount() {
        return block_list_items.size();
    }

    @Override
    public Object getItem(int i) {
        return block_list_items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final Context context = parent.getContext();
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.block_list_view, parent, false);
        }
        Button button = convertView.findViewById(R.id.cancel);
        TextView textView = convertView.findViewById(R.id.name);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(context).setMessage(block_list_items.get(position).getNick_name() + "님의 차단이 취소되었습니다. 브라우저를 종료후 다시시작하면 적용됩니다.")
                        .setNeutralButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).create().show();
                hashMap.remove(block_list_items.get(position).getUserno());
                MySharedPreferences.commit_Block_usr(hashMap);
                block_list_items.remove(position);
                notifyDataSetChanged();

            }
        });
        textView.setText(block_list_items.get(position).getNick_name());
        return convertView;
    }

}
