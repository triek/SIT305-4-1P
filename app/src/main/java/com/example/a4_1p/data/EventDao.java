package com.example.a4_1p.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT * FROM events ORDER BY date ASC, id ASC")
    List<EventEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(EventEntity event);

    @Update
    void update(EventEntity event);

    @Delete
    void delete(EventEntity event);
}
