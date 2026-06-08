package com.example

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LockScreenWordActivity : ComponentActivity() {
    private lateinit var repository: WordRepository
    private lateinit var ttsManager: TTSManager
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Window to display when locked
        enableEdgeToEdge()
        setupLockscreenWindow()

        ttsManager = TTSManager(this)
        settingsManager = SettingsManager(this)
        
        val db = AppDatabase.getDatabase(this, CoroutineScope(Dispatchers.IO))
        repository = WordRepository(db.wordDao())

        val initialWordId = intent.getIntExtra("word_id", -1)

        setContent {
            MyApplicationTheme {
                LockScreenUI(
                    initialWordId = initialWordId,
                    repository = repository,
                    ttsManager = ttsManager,
                    settingsManager = settingsManager,
                    onClose = { finish() }
                )
            }
        }
    }

    private fun setupLockscreenWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        
        // Clear background behind full-screen window for crisp aesthetics
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LockScreenUI(
    initialWordId: Int,
    repository: WordRepository,
    ttsManager: TTSManager,
    settingsManager: SettingsManager,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentWord by remember { mutableStateOf<Word?>(null) }
    var revealTranslation by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch word from DB
    LaunchedEffect(initialWordId) {
        isLoading = true
        val id = if (initialWordId != -1) initialWordId else settingsManager.lastWordId
        val word = repository.getWordById(id) ?: repository.getRandomWord()
        currentWord = word
        isLoading = false
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 24.dp, vertical = 40.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App header with status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lockscreen mode",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LOCKSCREEN WORD LEARNER",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = "Eng – Rus – Uzb",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Main Word Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                currentWord?.let { word ->
                    WordFlashcard(
                        word = word,
                        revealTranslation = revealTranslation,
                        onRevealToggle = { revealTranslation = !revealTranslation },
                        onPronounce = { lang ->
                            if (lang == "EN") {
                                ttsManager.speakEnglish(word.english)
                            } else {
                                ttsManager.speakRussian(word.russian)
                            }
                        },
                        onToggleFavorite = {
                            coroutineScope.launch {
                                val updated = word.copy(isFavorite = !word.isFavorite)
                                repository.update(updated)
                                currentWord = updated
                            }
                        }
                    )
                } ?: Text(
                    text = "No words available. Launch the main app to load words.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Lower Navigation / Action Bars
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Next word button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            revealTranslation = false
                            val nextWord = repository.getRandomWord()
                            if (nextWord != null) {
                                currentWord = nextWord
                                repository.incrementShownCount(nextWord.id)
                                settingsManager.lastWordId = nextWord.id
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Next word"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Keyingi so'z")
                }

                // Close activity
                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Close"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Yopish")
                }
            }

            // Quick unlock hint
            Text(
                text = "Ekranni ochish uchun odatdagidek tepaga suring",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WordFlashcard(
    word: Word,
    revealTranslation: Boolean,
    onRevealToggle: () -> Unit,
    onPronounce: (String) -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp, max = 460.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Fav & Sounds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onPronounce("EN") }) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "English Pronunciation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "EN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(end = 8.dp)
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (word.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Word displays
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // English Display
                Text(
                    text = word.english,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                // Subtitle/Context (Example sentence)
                if (word.exampleEnglish.isNotEmpty()) {
                    Text(
                        text = "\"${word.exampleEnglish}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider line
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.width(120.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Translations Block
                if (revealTranslation) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Uzbek translation Row
                        TranslationRow(
                            label = "UZ",
                            text = word.uzbek,
                            example = word.exampleUzbek,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        // Russian translation Row
                        TranslationRow(
                            label = "RU",
                            text = word.russian,
                            example = word.exampleRussian,
                            color = MaterialTheme.colorScheme.secondary,
                            onSpeak = { onPronounce("RU") }
                        )
                    }
                } else {
                    // Tap to Reveal Box
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable { onRevealToggle() }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Reveal",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tarjimasini ko'rish",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Reveal and Hide buttons if revealed
            if (revealTranslation) {
                TextButton(
                    onClick = onRevealToggle,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "Hide", modifier = Modifier.size(16.dp))
                        Text("Yashirish")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Composable
fun TranslationRow(
    label: String,
    text: String,
    example: String,
    color: Color,
    onSpeak: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            onSpeak?.let {
                IconButton(onClick = it, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak Russian",
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        if (example.isNotEmpty()) {
            Text(
                text = example,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}
