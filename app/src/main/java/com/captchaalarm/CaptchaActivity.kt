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
import android.widget.LinearLayout
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
    private lateinit var progressDots: LinearLayout
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View

    private var currentStageIndex = 0
    private var correctAnswer = ""

    private var captchaEnabled = true
    private var mathEnabled = true
    private var scrambleEnabled = true

    private lateinit var enabledChallenges: List<ChallengeType>

    enum class ChallengeType {
        CAPTCHA, MATH, SCRAMBLE
    }

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

        captchaEnabled = intent.getBooleanExtra("CAPTCHA_ENABLED", true)
        mathEnabled = intent.getBooleanExtra("MATH_ENABLED", true)
        scrambleEnabled = intent.getBooleanExtra("SCRAMBLE_ENABLED", true)

        enabledChallenges = buildList {
            if (captchaEnabled) add(ChallengeType.CAPTCHA)
            if (mathEnabled) add(ChallengeType.MATH)
            if (scrambleEnabled) add(ChallengeType.SCRAMBLE)
        }

        if (enabledChallenges.isEmpty()) {
            enabledChallenges = listOf(ChallengeType.CAPTCHA)
        }

        stageText = findViewById(R.id.stageText)
        instructionText = findViewById(R.id.instructionText)
        problemText = findViewById(R.id.problemText)
        captchaImage = findViewById(R.id.captchaImage)
        answerInput = findViewById(R.id.answerInput)
        submitButton = findViewById(R.id.submitButton)
        progressDots = findViewById(R.id.progressDots)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)

        setupProgressDots()
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

    private fun setupProgressDots() {
        val totalChallenges = enabledChallenges.size
        dot1.visibility = if (totalChallenges >= 1) View.VISIBLE else View.GONE
        dot2.visibility = if (totalChallenges >= 2) View.VISIBLE else View.GONE
        dot3.visibility = if (totalChallenges >= 3) View.VISIBLE else View.GONE
    }

    private fun setupStage() {
        val totalChallenges = enabledChallenges.size
        stageText.text = "Stage ${currentStageIndex + 1} of $totalChallenges"
        updateDots()
        answerInput.text.clear()

        when (enabledChallenges[currentStageIndex]) {
            ChallengeType.CAPTCHA -> setupDistortedTextCaptcha()
            ChallengeType.MATH -> setupMultiplicationChallenge()
            ChallengeType.SCRAMBLE -> setupScrambledWord()
        }

        answerInput.requestFocus()
    }

    private fun updateDots() {
        val dots = listOf(dot1, dot2, dot3)
        for (i in dots.indices) {
            if (i >= enabledChallenges.size) {
                dots[i].visibility = View.GONE
                continue
            }
            dots[i].setBackgroundResource(
                when {
                    currentStageIndex > i -> R.drawable.dot_completed
                    currentStageIndex == i -> R.drawable.dot_active
                    else -> R.drawable.dot_inactive
                }
            )
        }
    }

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

    private fun setupScrambledWord() {
        instructionText.text = "Unscramble this word:"
        answerInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        answerInput.hint = "Unscrambled word"

        captchaImage.visibility = View.GONE
        problemText.visibility = View.VISIBLE

        val word = words[Random.nextInt(words.size)]
        correctAnswer = word

        var scrambled = word.toList().shuffled().joinToString("")
        while (scrambled == word) {
            scrambled = word.toList().shuffled().joinToString("")
        }
        problemText.text = scrambled
    }

    private fun checkAnswer() {
        val userAnswer = answerInput.text.toString().trim().uppercase()

        if (userAnswer.isEmpty()) {
            Toast.makeText(this, "Enter your answer", Toast.LENGTH_SHORT).show()
            return
        }

        if (userAnswer == correctAnswer.uppercase()) {
            if (currentStageIndex < enabledChallenges.size - 1) {
                currentStageIndex++
                Toast.makeText(this, "Correct! Next challenge...", Toast.LENGTH_SHORT).show()
                setupStage()
            } else {
                dismissAlarm()
            }
        } else {
            shakeView()
            Toast.makeText(this, "Wrong! Try again.", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, "Solve all challenges to dismiss!", Toast.LENGTH_SHORT).show()
    }
}
