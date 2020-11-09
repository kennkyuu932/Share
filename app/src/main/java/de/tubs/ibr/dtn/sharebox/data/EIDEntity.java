package de.tubs.ibr.dtn.sharebox.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(primaryKeys = {"slack_workspace_id", "slack_user_id"})
public class EIDEntity {

    @NonNull
    @ColumnInfo(name = "slack_workspace_id")
    public String slackWorkspaceId;

    @NonNull
    @ColumnInfo(name = "slack_user_id")
    public String slackUserId;

    @ColumnInfo(name = "eid")
    public String Eid;

    @ColumnInfo(name = "slack_use_name")
    public String slackUseName;
}