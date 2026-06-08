package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SettingsManager
import com.example.data.TTSManager
import com.example.data.Word
import com.example.ui.WordViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var ttsManager: TTSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ttsManager = TTSManager(this)

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val viewModel: WordViewModel = viewModel()
                
                // Permission states
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasNotificationPermission = isGranted
                    if (!isGranted) {
                        Toast.makeText(
                            context,
                            "Ekranda so'zlarni ko'rish uchun bildirishnomalarga ruxsat bering.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // Request permission on start
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                MainScreen(
                    viewModel = viewModel,
                    ttsManager = ttsManager,
                    hasPermission = hasNotificationPermission,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}

@Composable
fun MainScreen(
    viewModel: WordViewModel,
    ttsManager: TTSManager,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    val allWords by viewModel.allWords.collectAsState()
    
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                tonalElevation = 12.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.School, contentDescription = "Practice Tab") },
                    label = { Text("Amaliyot") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Words Tab") },
                    label = { Text("So'zlar") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings Tab") },
                    label = { Text("Sozlamalar") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission warning if not granted
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRequestPermission() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Permission warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Bildirishnomalarga ruxsat berilmagan. Ruxsat berish uchun bosing.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "Tab Transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> PracticeTab(allWords, viewModel, ttsManager)
                    1 -> WordsListTab(allWords, viewModel, ttsManager)
                    2 -> SettingsTab(allWords.size, viewModel, ttsManager)
                }
            }
        }
    }
}

@Composable
fun PracticeTab(
    words: List<Word>,
    viewModel: WordViewModel,
    ttsManager: TTSManager
) {
    var currentIndex by remember { mutableStateOf(0) }
    var revealState by remember { mutableStateOf(false) }

    LaunchedEffect(words) {
        if (words.isNotEmpty() && currentIndex >= words.size) {
            currentIndex = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SO'ZLARNI O'RGANISH",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (words.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hozircha so'zlar mavjud emas.\n'So'zlar' bo'limidan yangilarini qo'shing.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val word = words.getOrNull(currentIndex)
            if (word != null) {
                // Flashcard panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Word Count indicator
                        Text(
                            text = "${currentIndex + 1} / ${words.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Center Words
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = word.english,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 36.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            // Pronounce English Button
                            Button(
                                onClick = { ttsManager.speakEnglish(word.english) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak", modifier = Modifier.size(16.dp))
                                    Text("Talaffuz (EN)")
                                }
                            }

                            if (word.exampleEnglish.isNotEmpty()) {
                                Text(
                                    text = "\"${word.exampleEnglish}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(modifier = Modifier.width(60.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            if (revealState) {
                                // Translations
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "UZ",
                                                color = MaterialTheme.colorScheme.tertiary,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = word.uzbek,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "RU",
                                                color = MaterialTheme.colorScheme.secondary,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = word.russian,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        IconButton(onClick = { ttsManager.speakRussian(word.russian) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak RU", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { revealState = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = "Reveal", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tarjimani ko'rish")
                                }
                            }
                        }

                        // Bottom Navigation row of indices
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        currentIndex--
                                        revealState = false
                                    } else {
                                        currentIndex = words.size - 1
                                        revealState = false
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Orqaga")
                            }

                            TextButton(
                                onClick = {
                                    if (currentIndex < words.size - 1) {
                                        currentIndex++
                                        revealState = false
                                    } else {
                                        currentIndex = 0
                                        revealState = false
                                    }
                                }
                            ) {
                                Text("Keyingisi")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WordsListTab(
    words: List<Word>,
    viewModel: WordViewModel,
    ttsManager: TTSManager
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isAddDialogVisible by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<Word?>(null) }

    // Add Word States
    var engValue by remember { mutableStateOf("") }
    var uzbValue by remember { mutableStateOf("") }
    var rusValue by remember { mutableStateOf("") }
    var engEx by remember { mutableStateOf("") }
    var uzbEx by remember { mutableStateOf("") }
    var rusEx by remember { mutableStateOf("") }

    val filteredList = words.filter {
        it.english.contains(searchQuery, ignoreCase = true) ||
                it.uzbek.contains(searchQuery, ignoreCase = true) ||
                it.russian.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SO'ZLAR OMBORI",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Qidiruv (EN | RU | UZ)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Mos so'zlar topilmadi." else "So'zlar yo'q.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { word ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingWord = word
                                    engValue = word.english
                                    uzbValue = word.uzbek
                                    rusValue = word.russian
                                    engEx = word.exampleEnglish
                                    uzbEx = word.exampleUzbek
                                    rusEx = word.exampleRussian
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = word.english,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { ttsManager.speakEnglish(word.english) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = "Speech", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { viewModel.deleteWord(word) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "UZ: ${word.uzbek}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(text = "•", color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(
                                        text = "RU: ${word.russian}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                if (word.shownCount > 0) {
                                    Text(
                                        text = "Ekranlarda ko'rsatilgan: ${word.shownCount} marta",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                editingWord = null
                engValue = ""
                uzbValue = ""
                rusValue = ""
                engEx = ""
                uzbEx = ""
                rusEx = ""
                isAddDialogVisible = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Word")
        }

        // Add/Edit Word Dialog
        if (isAddDialogVisible || editingWord != null) {
            AlertDialog(
                onDismissRequest = {
                    isAddDialogVisible = false
                    editingWord = null
                },
                title = {
                    Text(text = if (editingWord != null) "So'zni tahrirlash" else "Yangi so'z qo'shish")
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = engValue,
                            onValueChange = { engValue = it },
                            label = { Text("English Word") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uzbValue,
                            onValueChange = { uzbValue = it },
                            label = { Text("O'zbekcha tarjimasi") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = rusValue,
                            onValueChange = { rusValue = it },
                            label = { Text("Русский перевод") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = engEx,
                            onValueChange = { engEx = it },
                            label = { Text("Example sentence (EN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (engValue.isBlank() || uzbValue.isBlank() || rusValue.isBlank()) {
                                Toast.makeText(context, "Barcha tillarda tarjimalarini kiriting!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val activeWord = editingWord
                            if (activeWord != null) {
                                // Update
                                viewModel.updateWord(
                                    activeWord.copy(
                                        english = engValue.trim(),
                                        uzbek = uzbValue.trim(),
                                        russian = rusValue.trim(),
                                        exampleEnglish = engEx.trim()
                                    )
                                )
                                editingWord = null
                            } else {
                                // Add
                                viewModel.insertWord(
                                    Word(
                                        english = engValue.trim(),
                                        uzbek = uzbValue.trim(),
                                        russian = rusValue.trim(),
                                        exampleEnglish = engEx.trim()
                                    )
                                )
                                isAddDialogVisible = false
                            }
                        }
                    ) {
                        Text("Saqlash")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        isAddDialogVisible = false
                        editingWord = null
                    }) {
                        Text("Bekor qilish")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsTab(
    totalWords: Int,
    viewModel: WordViewModel,
    ttsManager: TTSManager
) {
    val context = LocalContext.current
    val isEnabled by viewModel.isLockEnabled.collectAsState()
    val selectedInterval by viewModel.intervalMinutes.collectAsState()
    
    val intervals = listOf(
        Pair(1, "1 daqiqa (Test)"),
        Pair(5, "5 daqiqa"),
        Pair(15, "15 daqiqa"),
        Pair(30, "30 daqiqa"),
        Pair(60, "1 soat"),
        Pair(180, "3 soat")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "SOZLAMALAR",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.primary
        )

        // General Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Faol lug'at bazasi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$totalWords ta so'z",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Stats icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Toggle Feature option
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bloklangan ekranga chiqarish",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Ekran yonganda so'zlarni chiqarib turadi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { viewModel.toggleLockScreen(it) }
                )
            }
        }

        // Interval selection
        if (isEnabled) {
            Text(
                text = "Vaqt intervalini sozlash",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    intervals.forEach { (minutes, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateInterval(minutes) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                            RadioButton(
                                selected = selectedInterval == minutes,
                                onClick = { viewModel.updateInterval(minutes) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Instant tester
        Button(
            onClick = {
                viewModel.forceShowNextImmediate()
                Toast.makeText(
                    context,
                    "Kutish boshlandi! Telefoningiz ekranini bloklab ko'ring. 2-3 soniyada notification keladi.",
                    Toast.LENGTH_LONG
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Test now")
            Spacer(modifier = Modifier.width(8.dp))
            Text("O'sha zahoti test qilish (2 soniya)")
        }
    }
}
