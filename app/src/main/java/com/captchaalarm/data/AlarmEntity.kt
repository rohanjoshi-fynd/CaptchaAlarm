package com.captchaalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val daysOfWeek: String = "",
    val isEnabled: Boolean = true,
    val captchaEnabled: Boolean = true,
    val mathEnabled: Boolean = true,
    val scrambleEnabled: Boolean = true
)
