package de.tubs.ibr.dtn.sharebox.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface EIDDao {

    @Insert
    void insert(EIDEntity user);

    @Delete
    void delete(EIDEntity user);

    @Update
    void update(EIDEntity user);

    @Query("SELECT * FROM EIDEntity")
    List<EIDEntity> getAll();

    @Query("SELECT * FROM EIDEntity WHERE slack_workspace_id = (:slackWorkspaceId) AND slack_user_id = (:slackUserId)")
    List<EIDEntity> search(String slackWorkspaceId, String slackUserId);

    @Query("DELETE FROM EIDEntity")
    void deleteAll();

    @Query("SELECT * FROM EIDEntity WHERE eid = (:eid)")
    EIDEntity searchFromEid(String eid);

}
