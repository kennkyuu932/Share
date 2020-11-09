package de.tubs.ibr.dtn.sharebox.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = EIDEntity.class, version = 1, exportSchema = false)
public abstract class EIDDatabase extends RoomDatabase {
    public abstract EIDDao eiddao();
}
