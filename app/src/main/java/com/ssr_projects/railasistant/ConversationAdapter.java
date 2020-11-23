package com.ssr_projects.railasistant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class ConversationAdapter extends BaseAdapter {

    private Activity activity;
    private ArrayList<HashMap> dataList;

    public ConversationAdapter(ArrayList<HashMap> dataList, Activity activity) {
        this.dataList = dataList;
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int i) {
        return dataList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        final HashMap map = dataList.get(i);

        LayoutInflater inflater = activity.getLayoutInflater();

        if(map.get("POSITION").toString().contains("LEFT")){
            view = inflater.inflate(R.layout.left_chat, null);
        }

        else if(map.get("POSITION").toString().contains("RIGHT")){
            view = inflater.inflate(R.layout.right_chat, null);
        }

        if(map.get("TYPE")!=null){
            if(map.get("TYPE").toString().contains("IMAGE"));
        }

        TextView convoText = view.findViewById(R.id.convo_text);
        convoText.setText(map.get("TEXT").toString());

        return view;
    }

}
