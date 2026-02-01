package com.captchaalarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class CaptchaActivity : AppCompatActivity() {

    private lateinit var problemText: TextView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var instructionText: TextView

    private var correctAnswer: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindowFlags()
        setContentView(R.layout.activity_captcha)

        problemText = findViewById(R.id.problemText)
        answerInput = findViewById(R.id.answerInput)
        submitButton = findViewById(R.id.submitButton)
        instructionText = findViewById(R.id.instructionText)

        generateNewProblem()

        submitButton.setOnClickListener {
            checkAnswer()
        }
    }

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    private fun generateNewProblem() {
        val operationType = Random.nextInt(3)

        val (problem, answer) = when (operationType) {
            0 -> {
                // Addition
                val a = Random.nextInt(10, 100)
                val b = Random.nextInt(10, 100)
                Pair("$a + $b = ?", a + b)
            }
            1 -> {
                // Subtraction (ensure positive result)
                val a = Random.nextInt(50, 200)
                val b = Random.nextInt(10, a)
                Pair("$a - $b = ?", a - b)
            }
            else -> {
                // Multiplication
                val a = Random.nextInt(2, 15)
                val b = Random.nextInt(2, 15)
                Pair("$a \u00d7 $b = ?", a * b)
            }
        }

        correctAnswer = answer
        problemText.text = problem
        answerInput.text.clear()
        answerInput.requestFocus()
    }

    private fun checkAnswer() {
        val userAnswer = answerInput.text.toString().trim()

        if (userAnswer.isEmpty()) {
            Toast.makeText(this, "Enter your answer", Toast.LENGTH_SHORT).show()
            return
        }

        val numericAnswer = userAnswer.toIntOrNull()
        if (numericAnswer == null) {
            Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show()
            return
        }

        if (numericAnswer == correctAnswer) {
            dismissAlarm()
        } else {
            // Wrong answer - shake animation and new problem
            shakeView()
            Toast.makeText(this, "Wrong! Try again.", Toast.LENGTH_SHORT).show()
            generateNewProblem()
        }
    }

    private fun shakeView() {
        val shake = AnimationUtils.loadAnimation(this, android.R.anim.fade_in).apply {
            duration = 50
            repeatCount = 5
            repeatMode = android.view.animation.Animation.REVERSE
        }
        // Simple manual shake using translate animation
        val shakeAnim = android.view.animation.TranslateAnimation(-10f, 10f, 0f, 0f).apply {
            duration = 50
            repeatCount = 5
            repeatMode = android.view.animation.Animation.REVERSE
        }
        answerInput.startAnimation(shakeAnim)
    }

    private fun dismissAlarm() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        startService(stopIntent)

        Toast.makeText(this, "Alarm dismissed!", Toast.LENGTH_SHORT).show()
        finish()
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        // Do nothing - prevent dismissing without solving captcha
        Toast.makeText(this, "Solve the captcha to dismiss the alarm!", Toast.LENGTH_SHORT).show()
    }
}
