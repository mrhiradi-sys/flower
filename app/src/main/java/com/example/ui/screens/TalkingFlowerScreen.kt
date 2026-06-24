package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.components.TalkingFlowerView
import com.example.ui.viewmodel.FlowerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkingFlowerScreen(
    viewModel: FlowerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe data from ViewModel
    val phrases by viewModel.phrases.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val lastSpokenPhrase by viewModel.lastSpokenPhrase.collectAsStateWithLifecycle()
    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()

    val autoTalkInterval by viewModel.autoTalkInterval.collectAsStateWithLifecycle()
    val flowerColor by viewModel.flowerColor.collectAsStateWithLifecycle()
    val voicePitch by viewModel.voicePitch.collectAsStateWithLifecycle()
    val voiceSpeed by viewModel.voiceSpeed.collectAsStateWithLifecycle()

    val geminiBrainEnabled by viewModel.geminiBrainEnabled.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val geminiTheme by viewModel.geminiTheme.collectAsStateWithLifecycle()
    val isGeneratingAI by viewModel.isGeneratingAI.collectAsStateWithLifecycle()

    // Screen Tabs
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Settings", "My Library", "AI Brain", "Logs")

    // Input States
    var customPhraseText by remember { mutableStateOf("") }
    var inputApiKeyText by remember { mutableStateOf(geminiApiKey) }

    // Sync input key text with model key
    LaunchedEffect(geminiApiKey) {
        inputApiKeyText = geminiApiKey
    }

    // Post Notification Permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleBackgroundService()
        } else {
            Toast.makeText(context, "Notification permission is required for background speech!", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Flower Icon",
                            tint = Color(0xFFFFEB3B),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Talking Flower",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Status Badge
                    val badgeColor = if (isServiceActive) Color(0xFF4CAF50) else Color(0xFF757575)
                    val statusText = if (isServiceActive) "Active Every ${autoTalkInterval}m" else "Off-duty"
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- TOP PORTION: The Animated Talking Flower View ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TalkingFlowerView(
                    isSpeaking = isSpeaking,
                    phraseText = if (isGeneratingAI) "Generating witty thought..." else lastSpokenPhrase,
                    flowerColorHex = flowerColor,
                    onTap = {
                        viewModel.triggerSpeech()
                    }
                )

                // Quick tap floating action helper
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .testTag("tap_flower_badge")
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { viewModel.triggerSpeech() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Speak Now",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tap to Talk!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // --- MID PORTION: Tabs Selection ---
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // --- BOTTOM PORTION: Dynamic Tab Content Area ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (selectedTab) {
                    0 -> { // Settings Tab
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                // Background Mode Switch
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Keep Speaking in Background",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = "Talks every 5 minutes (or selected interval) even when app is closed!",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = isServiceActive,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    // Request POST_NOTIFICATIONS permission on SDK 33+
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        val permissionCheck = ContextCompat.checkSelfPermission(
                                                            context, Manifest.permission.POST_NOTIFICATIONS
                                                        )
                                                        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        } else {
                                                            viewModel.toggleBackgroundService()
                                                        }
                                                    } else {
                                                        viewModel.toggleBackgroundService()
                                                    }
                                                } else {
                                                    viewModel.toggleBackgroundService()
                                                }
                                            },
                                            modifier = Modifier.testTag("background_switch")
                                        )
                                    }
                                }
                            }

                            item {
                                // Auto Talk Interval Select
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Auto-Talk Interval",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Choose how frequently I chime in!",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        val intervals = listOf(1, 2, 5, 10, 30)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            intervals.forEach { mins ->
                                                val isSelected = autoTalkInterval == mins
                                                Button(
                                                    onClick = { viewModel.setAutoTalkInterval(mins) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = "${mins}m",
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                // Color Selector
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "My Flower Color",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        val colors = listOf(
                                            Pair("#FFEB3B", "Gold Yellow"),
                                            Pair("#E91E63", "Rose Pink"),
                                            Pair("#00BCD4", "Ocean Cyan"),
                                            Pair("#9C27B0", "Royal Purple"),
                                            Pair("#4CAF50", "Garden Green")
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            colors.forEach { (hex, name) ->
                                                val isSelected = flowerColor == hex
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                                        .border(
                                                            width = if (isSelected) 3.dp else 1.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                            shape = CircleShape
                                                        )
                                                        .clickable {
                                                            viewModel.setFlowerColor(hex)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = if (hex == "#FFEB3B") Color.Black else Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                // Voice controls sliders
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Voice Pitch",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Slider(
                                            value = voicePitch,
                                            onValueChange = { viewModel.setVoicePitch(it) },
                                            valueRange = 0.6f..2.2f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Sullen Deep", fontSize = 11.sp, color = Color.Gray)
                                            Text("Default", fontSize = 11.sp, color = Color.Gray)
                                            Text("Cute Squeaky", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = "Voice Speed",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Slider(
                                            value = voiceSpeed,
                                            onValueChange = { viewModel.setVoiceSpeed(it) },
                                            valueRange = 0.5f..1.8f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Slow Poke", fontSize = 11.sp, color = Color.Gray)
                                            Text("Normal", fontSize = 11.sp, color = Color.Gray)
                                            Text("Fast Chatter", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // Library Tab
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Add custom phrase row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customPhraseText,
                                    onValueChange = { customPhraseText = it },
                                    label = { Text("Teach me a new phrase...") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("phrase_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (customPhraseText.isNotBlank()) {
                                            viewModel.addCustomPhrase(customPhraseText)
                                            customPhraseText = ""
                                            Toast.makeText(context, "Added new phrase!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("add_phrase_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Phrase")
                                }
                            }

                            // Phrases list
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(phrases) { phrase ->
                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (phrase.isCustom) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = phrase.isEnabled,
                                                onCheckedChange = { checked ->
                                                    viewModel.togglePhrase(phrase.id, checked)
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = phrase.text,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = if (phrase.isCustom) "User phrase" else "Original phrase",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            if (phrase.isCustom) {
                                                IconButton(
                                                    onClick = { viewModel.deletePhrase(phrase.id) }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // AI Brain Tab (Gemini config)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Activate Gemini AI Brain",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = "Let the flower generate infinite witty, clever, or funny comments dynamically!",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Switch(
                                            checked = geminiBrainEnabled,
                                            onCheckedChange = { viewModel.setGeminiBrainEnabled(it) },
                                            modifier = Modifier.testTag("gemini_switch")
                                        )
                                    }
                                }
                            }

                            if (geminiBrainEnabled) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Configure API Key",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            OutlinedTextField(
                                                value = inputApiKeyText,
                                                onValueChange = {
                                                    inputApiKeyText = it
                                                    viewModel.setGeminiApiKey(it)
                                                },
                                                label = { Text("Gemini API Key") },
                                                placeholder = { Text("Enter your API key...") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp)
                                            )

                                            // Quick Helper to inject AI Studio automatic key if present
                                            val buildConfigKey = BuildConfig.GEMINI_API_KEY
                                            if (buildConfigKey.isNotEmpty() && buildConfigKey != "MY_GEMINI_API_KEY") {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                    onClick = {
                                                        inputApiKeyText = buildConfigKey
                                                        viewModel.setGeminiApiKey(buildConfigKey)
                                                        Toast.makeText(context, "Loaded project secret key!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondary
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Use secret key")
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Use Built-in Studio Secret")
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Secrets are safe. Offline local speech operates whenever Gemini is off or key is empty.",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "AI Personality Theme",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            val themes = listOf(
                                                Pair("motivational", "Silly Motivational"),
                                                Pair("sarcastic", "Witty Sarcastic"),
                                                Pair("pirate", "Garden Pirate"),
                                                Pair("sleepy", "Yawny Sleepy"),
                                                Pair("energetic", "Mega Excited"),
                                                Pair("shakespeare", "Shakespearean")
                                            )

                                            themes.forEach { (themeId, themeName) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { viewModel.setGeminiTheme(themeId) }
                                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = geminiTheme == themeId,
                                                        onClick = { viewModel.setGeminiTheme(themeId) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(text = themeName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> { // Logs Tab (History list)
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "What I've Said",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { viewModel.clearLogs() },
                                    modifier = Modifier.testTag("clear_history_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear Log")
                                }
                            }

                            if (logs.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.List,
                                            contentDescription = "Empty",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Silence is golden... for now!",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(logs) { log ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Log",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "\"${log.text}\"",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = dateFormat.format(Date(log.timestamp)),
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
