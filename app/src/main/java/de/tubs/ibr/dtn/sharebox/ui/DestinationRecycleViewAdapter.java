package de.tubs.ibr.dtn.sharebox.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.tubs.ibr.dtn.sharebox.R;
import de.tubs.ibr.dtn.sharebox.ui.DestinationViewHolder;
import de.tubs.ibr.dtn.sharebox.ui.DestinationRowData;

public class DestinationRecycleViewAdapter extends RecyclerView.Adapter<DestinationViewHolder> {
    protected List<DestinationRowData> list;
    protected Context activityContext;

    public DestinationRecycleViewAdapter(List<DestinationRowData> list, Context activityContext) {
        this.list = list;
        this.activityContext = activityContext;
    }

    @NonNull
    @Override
    public DestinationViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View inflate = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.select_destination_list_row, viewGroup, false);
        final DestinationViewHolder vh = new DestinationViewHolder(inflate);
        // click event
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onViewClick(v, vh.getAdapterPosition());
            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull DestinationViewHolder holder, int position) {
        holder.teamIdView.setText(list.get(position).getTeamId());
        holder.userIdView.setText(list.get(position).getUserId());
        holder.eidView.setText(list.get(position).getEid());
        holder.realNameView.setText(list.get(position).getRealName());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    void onViewClick(View v, int position) {

    }
}
