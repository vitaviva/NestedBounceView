package com.vitavia.nestedbounceview.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private String[] datas = new String[100];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        initRecyclerView();
    }

    private void initRecyclerView() {
        for (int i = 0; i < datas.length; i++) {
            datas[i] = "NestedBounceView Sample Data " + i;
        }
        RecyclerView.Adapter adapter;
        recyclerView.setAdapter(adapter = new RecyclerView.Adapter<InnerViewHolder>() {
            @Override
            public InnerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new InnerViewHolder(View.inflate(MainActivity.this, R.layout.list_item, null));
            }

            @Override
            public void onBindViewHolder(InnerViewHolder holder, int position) {
                holder.tv.setText(datas[position]);
            }

            @Override
            public int getItemCount() {
                return datas.length;
            }
        });
        adapter.notifyDataSetChanged();

    }

    class InnerViewHolder extends RecyclerView.ViewHolder {

        TextView tv;

        public InnerViewHolder(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvName);
        }
    }

}
