package com.captchaalarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class CaptchaActivity : AppCompatActivity() {

    private lateinit var stageText: TextView
    private lateinit var instructionText: TextView
    private lateinit var problemText: TextView
    private lateinit var captchaImage: ImageView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View

    private var currentStage = 1
    private var correctAnswer = ""

    private val words = listOf(
        "PLANET", "BRIDGE", "CASTLE", "DRAGON", "FOREST",
        "GARDEN", "HAMMER", "JUNGLE", "KNIGHT", "LEMON",
        "MONKEY", "ORANGE", "PENCIL", "ROCKET", "SILVER",
        "THUNDER", "VIOLET", "WINDOW", "FALCON", "GUITAR",
        "BREEZE", "CANDLE", "DAGGER", "EMPIRE", "FROZEN",
        "GLOBAL", "HARBOR", "INSECT", "JIGSAW", "KERNEL"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowFlags()
        setContentView(R.layout.activity_captcha)

        stageText = findViewById(R.id.stageText)
        instructionText = findViewById(R.id.instructionText)
        problemText = findViewById(R.id.problemText)
        captchaImage = findViewById(R.id.captchaImage)
        answerInput = findViewById(R.id.answerInput)
        submitButton = findViewById(R.id.submitButton)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)

        setupStage()

        submitButton.setOnClickListener { checkAnswer() }
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupStage() {
        stageText.text = "Stage $currentStage of 3"
        updateDots()
        answerInput.text.clear()

        when (currentStage) {
            1 -> setupDistortedTextCaptcha()
            2 -> setupMultiplicationChallenge()
            3 -> setupScrambledWord()
        }

        answerInput.requestFocus()
    }

    private fun updateDots() {
        dot1.setBackgroundResource(
            when {
                currentStage > 1 -> R.drawable.dot_completed
                currentStage == 1 -> R.drawable.dot_active
                else -> R.drawable.dot_inactive
            }
        )
        dot2.setBackgroundResource(
            when {
                currentStage > 2 -> R.drawable.dot_completed
                currentStage == 2 -> R.drawable.dot_active
                else -> R.drawable.dot_inactive
            }
        )
        dot3.setBackgroundResource(
            when {
                currentStage > 3 -> R.drawable.dot_completed
                currentStage == 3 -> R.drawable.dot_active
                else -> R.drawable.dot_inactive
            }
        )
    }

    // --- Stage 1: Distorted Text CAPTCHA ---

    private fun setupDistortedTextCaptcha() {
        instructionText.text = "Type the distorted text below:"
        answerInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        answerInput.hint = "Type text here"

        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val captchaText = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        correctAnswer = captchaText

        val bitmap = generateDistortedBitmap(captchaText)
        captchaImage.setImageBitmap(bitmap)
        captchaImage.visibility = View.VISIBLE
        problemText.visibility = View.GONE
    }

    private fun generateDistortedBitmap(text: String): Bitmap {
        val width = 560
        val height = 160
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background with noise
        canvas.drawColor(Color.rgb(240, 240, 240))
        val noisePaint = Paint()
        for (i in 0..800) {
            noisePaint.color = Color.rgb(
                Random.nextInt(150, 220),
                Random.nextInt(150, 220),
                Random.nextInt(150, 220)
            )
            canvas.drawCircle(
                Random.nextFloat() * width,
                Random.nextFloat() * height,
                Random.nextFloat() * 3f + 1f,
                noisePaint
            )
        }

        // Random lines through the image
        val linePaint = Paint().apply {
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        for (i in 0..5) {
            linePaint.color = Color.rgb(
                Random.nextInt(100, 180),
                Random.nextInt(100, 180),
                Random.nextInt(100, 180)
            )
            canvas.drawLine(
                Random.nextFloat() * width, Random.nextFloat() * height,
                Random.nextFloat() * width, Random.nextFloat() * height,
                linePaint
            )
        }

        // Draw each character with individual distortion
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 64f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 1.5f
        }

        val charWidth = (width - 80f) / text.length
        for (i in text.indices) {
            textPaint.color = Color.rgb(
                Random.nextInt(0, 100),
                Random.nextInt(0, 100),
                Random.nextInt(0, 100)
            )
            textPaint.textSize = Random.nextFloat() * 16f + 52f

            canvas.save()
            val x = 40f + i * charWidth
            val y = height / 2f + Random.nextFloat() * 20f - 10f
            canvas.rotate(Random.nextFloat() * 30f - 15f, x, y)
            canvas.drawText(text[i].toString(), x, y + 10f, textPaint)
            canvas.restore()
        }

        // More noise on top
        for (i in 0..400) {
            noisePaint.color = Color.rgb(
                Random.nextInt(100, 200),
                Random.nextInt(100, 200),
                Random.nextInt(100, 200)
            )
            canvas.drawCircle(
                Random.nextFloat() * width,
                Random.nextFloat() * height,
                Random.nextFloat() * 2f,
                noisePaint
            )
        }

        return bitmap
    }

    // --- Stage 2: 2-digit x 2-digit Multiplication ---

    private fun setupMultiplicationChallenge() {
        instructionText.text = "Solve the multiplication:"
        answerInput.inputType = InputType.TYPE_CLASS_NUMBER
        answerInput.hint = "Your answer"

        captchaImage.visibility = View.GONE
        problemText.visibility = View.VISIBLE

        val a = Random.nextInt(11, 100)
        val b = Random.nextInt(11, 100)
        val answer = a * b
        correctAnswer = answer.toString()
        problemText.text = "$a × $b = ?"
    }

    // --- Stage 3: Scrambled Word ---

    private fun setupScrambledWord() {
        instructionText.text = "Unscramble this word:"
        answerInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        answerInput.hint = "Unscrambled word"

        captchaImage.visibility = View.GONE
        problemText.visibility = View.VISIBLE

        val word = words[Random.nextInt(words.size)]
        correctAnswer = word

        var scrambled = word.toList().shuffled().joinToString("")
        // Make sure it's actually scrambled
        while (scrambled == word) {
            scrambled = word.toList().shuffled().joinToString("")
        }
        problemText.text = scrambled
    }

    // --- Answer Checking ---

    private fun checkAnswer() {
        val userAnswer = answerInput.text.toString().trim().uppercase()

        if (userAnswer.isEmpty()) {
            Toast.makeText(this, "Enter your answer", Toast.LENGTH_SHORT).show()
            return
        }

        if (userAnswer == correctAnswer.uppercase()) {
            if (currentStage < 3) {
                currentStage++
                Toast.makeText(this, "Correct! Next challenge...", Toast.LENGTH_SHORT).show()
                setupStage()
            } else {
                dismissAlarm()
            }
        } else {
            shakeView()
            Toast.makeText(this, "Wrong! Try again.", Toast.LENGTH_SHORT).show()
            // Regenerate same stage type
            setupStage()
        }
    }

    private fun shakeView() {
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
        Toast.makeText(this, "Solve all 3 challenges to dismiss!", Toast.LENGTH_SHORT).show()
    }
}
