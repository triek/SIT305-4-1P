package com.example.a4_1p.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {EventEntity.class}, version = 1, exportSchema = false)
public abstract class EventDatabase extends RoomDatabase {
    private static volatile EventDatabase INSTANCE;

    public abstract EventDao eventDao();

    public static EventDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (EventDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            EventDatabase.class,
                            "event_planner.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
