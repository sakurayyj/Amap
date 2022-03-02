package com.example.amaptest2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.amap.api.services.core.PoiItem;

import java.util.List;

public class PoiSearch_adapter extends BaseAdapter {
    private List<PoiItem> poiItems;// poi数据
    private LayoutInflater mInflater;

    public PoiSearch_adapter(Context context, List<PoiItem> list) {
        poiItems = list;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return poiItems.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View cv, ViewGroup parent) {
        ViewHolder holder;
        if (cv == null) {
            cv = mInflater.inflate(R.layout.item_poi_search, parent, false);
            holder = new ViewHolder(cv);
            cv.setTag(holder);
        } else holder = (ViewHolder) cv.getTag();

        PoiItem item = poiItems.get(position);
        holder.title.setText(item.getTitle());
        holder.des.setText(item.getSnippet());
        if(item.getDistance()<=1000){
            holder.dis.setText("距离"+String.valueOf(item.getDistance())+"米");
        }else
        {
            holder.dis.setText("距离>"+String.valueOf(item.getDistance()/1000)+"千米");
        }


        return cv;
    }

    private class ViewHolder {
        TextView title, des,dis;

        ViewHolder(View view) {
            title = (TextView) view.findViewById(R.id.poi_text);
            des = (TextView) view.findViewById(R.id.poi_des);
            dis = view.findViewById(R.id.poi_distance);
        }
    }
}
