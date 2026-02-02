package com.captchaalarm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    fun getById(id: Int): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    fun getEnabled(): List<AlarmEntity>

    @Insert
    fun insert(alarm: AlarmEntity): Long

    @Update
    fun update(alarm: AlarmEntity)

    @Delete
    fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    fun deleteById(id: Int)
}
