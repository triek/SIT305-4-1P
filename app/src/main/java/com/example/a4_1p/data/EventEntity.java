package com.example.a4_1p.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public String category;
    public String date;
    public String location;
    public String notes;

    public EventEntity(int id, String title, String category, String date, String location, String notes) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.date = date;
        this.location = location;
        this.notes = notes;
    }
}
