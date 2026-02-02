package com.captchaalarm

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.captchaalarm.data.AlarmDatabase
import com.captchaalarm.data.AlarmEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var addFab: FloatingActionButton
    private lateinit var adapter: AlarmAdapter

    private val dao by lazy { AlarmDatabase.getInstance(this).alarmDao() }

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.alarmRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        addFab = findViewById(R.id.addAlarmFab)

        adapter = AlarmAdapter(
            alarms = mutableListOf(),
            onToggle = { alarm, enabled -> toggleAlarm(alarm, enabled) },
            onClick = { alarm -> showAlarmDialog(alarm) },
            onLongClick = { alarm -> confirmDelete(alarm) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        addFab.setOnClickListener {
            requestPermissionsAndShowDialog(null)
        }

        requestNotificationPermission()
        loadAlarms()
    }

    override fun onResume() {
        super.onResume()
        loadAlarms()
    }

    private fun loadAlarms() {
        thread {
            val alarms = dao.getAll()
            runOnUiThread {
                adapter.updateAlarms(alarms)
                emptyText.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (alarms.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        thread {
            val updated = alarm.copy(isEnabled = enabled)
            dao.update(updated)
            if (enabled) {
                AlarmScheduler.schedule(this, updated)
            } else {
                AlarmScheduler.cancel(this, alarm.id)
            }
            runOnUiThread { loadAlarms() }
        }
    }

    private fun confirmDelete(alarm: AlarmEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Alarm")
            .setMessage("Delete this alarm?")
            .setPositiveButton("Delete") { _, _ ->
                thread {
                    AlarmScheduler.cancel(this, alarm.id)
                    dao.delete(alarm)
                    runOnUiThread { loadAlarms() }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestPermissionsAndShowDialog(existingAlarm: AlarmEntity?) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                Toast.makeText(this, "Please allow exact alarms, then try again", Toast.LENGTH_LONG).show()
                return
            }
        }
        showAlarmDialog(existingAlarm)
    }

    private fun showAlarmDialog(existingAlarm: AlarmEntity?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_alarm, null)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.dialogTimePicker)
        val labelInput = dialogView.findViewById<TextInputEditText>(R.id.dialogLabelInput)

        val dayToggles = mapOf(
            1 to dialogView.findViewById<ToggleButton>(R.id.toggleSun),
            2 to dialogView.findViewById<ToggleButton>(R.id.toggleMon),
            3 to dialogView.findViewById<ToggleButton>(R.id.toggleTue),
            4 to dialogView.findViewById<ToggleButton>(R.id.toggleWed),
            5 to dialogView.findViewById<ToggleButton>(R.id.toggleThu),
            6 to dialogView.findViewById<ToggleButton>(R.id.toggleFri),
            7 to dialogView.findViewById<ToggleButton>(R.id.toggleSat)
        )

        timePicker.setIs24HourView(false)

        if (existingAlarm != null) {
            timePicker.hour = existingAlarm.hour
            timePicker.minute = existingAlarm.minute
            labelInput.setText(existingAlarm.label)
            val selectedDays = existingAlarm.daysOfWeek
                .split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
            dayToggles.forEach { (day, toggle) ->
                toggle.isChecked = day in selectedDays
            }
        }

        val title = if (existingAlarm != null) "Edit Alarm" else "New Alarm"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val hour = timePicker.hour
                val minute = timePicker.minute
                val label = labelInput.text?.toString()?.trim() ?: ""
                val days = dayToggles
                    .filter { it.value.isChecked }
                    .keys
                    .sorted()
                    .joinToString(",")

                thread {
                    if (existingAlarm != null) {
                        val updated = existingAlarm.copy(
                            hour = hour, minute = minute,
                            label = label, daysOfWeek = days,
                            isEnabled = true
                        )
                        dao.update(updated)
                        AlarmScheduler.cancel(this, existingAlarm.id)
                        AlarmScheduler.schedule(this, updated)
                    } else {
                        val newAlarm = AlarmEntity(
                            hour = hour, minute = minute,
                            label = label, daysOfWeek = days,
                            isEnabled = true
                        )
                        val id = dao.insert(newAlarm).toInt()
                        val saved = newAlarm.copy(id = id)
                        AlarmScheduler.schedule(this, saved)
                    }
                    runOnUiThread { loadAlarms() }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }
}
