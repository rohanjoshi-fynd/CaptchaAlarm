package com.captchaalarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.captchaalarm.data.AlarmEntity
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmAdapter(
    private var alarms: MutableList<AlarmEntity>,
    private val onToggle: (AlarmEntity, Boolean) -> Unit,
    private val onClick: (AlarmEntity) -> Unit,
    private val onLongClick: (AlarmEntity) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.alarmTimeText)
        val labelText: TextView = view.findViewById(R.id.alarmLabelText)
        val daysText: TextView = view.findViewById(R.id.alarmDaysText)
        val enableSwitch: SwitchMaterial = view.findViewById(R.id.alarmSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
        }
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        holder.timeText.text = timeFormat.format(cal.time)

        holder.labelText.text = alarm.label.ifBlank { "Alarm" }
        holder.daysText.text = formatDays(alarm.daysOfWeek)

        holder.enableSwitch.setOnCheckedChangeListener(null)
        holder.enableSwitch.isChecked = alarm.isEnabled
        holder.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggle(alarm, isChecked)
        }

        holder.itemView.alpha = if (alarm.isEnabled) 1.0f else 0.5f

        holder.itemView.setOnClickListener { onClick(alarm) }
        holder.itemView.setOnLongClickListener {
            onLongClick(alarm)
            true
        }
    }

    override fun getItemCount() = alarms.size

    fun updateAlarms(newAlarms: List<AlarmEntity>) {
        alarms.clear()
        alarms.addAll(newAlarms)
        notifyDataSetChanged()
    }

    private fun formatDays(daysOfWeek: String): String {
        if (daysOfWeek.isBlank()) return "One time"
        val dayNames = mapOf(
            1 to "Sun", 2 to "Mon", 3 to "Tue",
            4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat"
        )
        val days = daysOfWeek.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
        if (days.size == 7) return "Every day"
        if (days.sorted() == listOf(2, 3, 4, 5, 6)) return "Weekdays"
        if (days.sorted() == listOf(1, 7)) return "Weekends"
        return days.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }
}
