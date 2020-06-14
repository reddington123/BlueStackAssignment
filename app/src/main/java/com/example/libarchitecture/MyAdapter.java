package com.example.libarchitecture;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private String[][] mDataset;
    public Context context;

    public  class MyViewHolder extends RecyclerView.ViewHolder {
        //private  ImageView imageView;
        // each data item is just a string in this case
        public TextView libNameTv;
        public TextView libAbiTv;
        public MyViewHolder(View view) {
            super(view);
            libNameTv = (TextView)view.findViewById(R.id.lib_name);
            libAbiTv = view.findViewById(R.id.lib_abi);
        }
    }

    public MyAdapter(String[][] myDataset, MainActivity mainActivity) {
        mDataset = myDataset;
        context=mainActivity;
    }

    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        MyViewHolder vh = new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item, parent,false));
        return vh;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.libNameTv.setText(mDataset[position][0]);
        holder.libAbiTv.setText(mDataset[position][1]);

    }
    @Override
    public int getItemCount() {
        if(mDataset!=null)
            return mDataset.length;
        else
            return 0;
    }
}