package com.example.whatsappautoreply

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AutoReplyScreen()
                }
            }
        }
    }
}

@Composable
fun AutoReplyScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("AutoReplyPrefs", Context.MODE_PRIVATE) }

    var headerText by remember { mutableStateOf(sharedPreferences.getString("header", "WhatsApp Auto Reply") ?: "") }
    var replyMessage by remember { mutableStateOf(sharedPreferences.getString("message", "I am currently busy. I will reply to you later.") ?: "") }
    var isAutoReplyOn by remember { mutableStateOf(sharedPreferences.getBoolean("isOn", false)) }

    var hasNotificationAccess by remember { mutableStateOf(checkNotificationAccess(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = headerText.ifEmpty { "WhatsApp Auto Reply" }, 
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp, top = 24.dp)
        )

        OutlinedTextField(
            value = headerText,
            onValueChange = { headerText = it },
            label = { Text("App Header / Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = replyMessage,
            onValueChange = { replyMessage = it },
            label = { Text("Auto-Reply Message") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                sharedPreferences.edit()
                    .putString("header", headerText)
                    .putString("message", replyMessage)
                    .apply()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save Message")
        }

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isAutoReplyOn) "Auto Reply is ON" else "Auto Reply is OFF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = isAutoReplyOn,
                    onCheckedChange = { isOn ->
                        isAutoReplyOn = isOn
                        sharedPreferences.edit().putBoolean("isOn", isOn).apply()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (!hasNotificationAccess) {
            Text(
                text = "Notification access is required for the app to detect incoming messages.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Enable Notification Access")
            }
        } else {
            Text(
                text = "✓ Notification Access Granted",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }) {
                Text("Manage Notification Access")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun checkNotificationAccess(context: Context): Boolean {
    val componentNames = NotificationManagerCompat.getEnabledListenerPackages(context)
    return componentNames.contains(context.packageName)
}
