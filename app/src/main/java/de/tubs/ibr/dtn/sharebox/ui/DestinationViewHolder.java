package de.tubs.ibr.dtn.sharebox.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import de.tubs.ibr.dtn.sharebox.R;

public class DestinationViewHolder extends RecyclerView.ViewHolder{
    public TextView teamIdView;
    public TextView userIdView;
    public TextView eidView;
    public TextView realNameView;
    public DestinationViewHolder(View itemView) {
        super(itemView);
        teamIdView = (TextView) itemView.findViewById(R.id.team_id);
        userIdView = (TextView) itemView.findViewById(R.id.user_id);
        eidView = (TextView) itemView.findViewById(R.id.eid);
        realNameView = (TextView) itemView.findViewById(R.id.real_name);
    }
}
