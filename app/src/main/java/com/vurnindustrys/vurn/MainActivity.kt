package com.vurnindustrys.vurn

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurnindustrys.vurn.data.FinnhubClient
import com.vurnindustrys.vurn.data.Scanner
import com.vurnindustrys.vurn.model.StockPick
import com.vurnindustrys.vurn.ui.theme.VurnTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VurnTheme { VurnApp(this) } }
    }
}

@Composable
private fun VurnApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("vurn", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString("finnhub", "") ?: "") }
    var showSettings by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("Ready") }
    var error by remember { mutableStateOf<String?>(null) }
    var picks by remember { mutableStateOf<List<StockPick>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Surface {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("VURN", fontWeight = FontWeight.Black, fontSize = 25.sp, letterSpacing = 3.sp)
                        Text("INDUSTRYS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    TextButton(onClick = { showSettings = true }) { Text("DATA KEY") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("7-DAY HUNTER", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Text("Top penny-stock setups", fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("Experimental signal scanner — not financial advice.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (apiKey.isBlank()) showSettings = true else scope.launch {
                            scanning = true
                            error = null
                            progress = "Starting scan…"
                            runCatching { Scanner(FinnhubClient(apiKey)).scan { done, total -> progress = "Scanning $done / $total" } }
                                .onSuccess { picks = it; progress = "Scan complete" }
                                .onFailure { error = it.message ?: "Scan failed" }
                            scanning = false
                        }
                    },
                    enabled = !scanning,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (scanning) progress else "SCAN MARKET NOW") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }

            if (picks.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Vurn Score", fontWeight = FontWeight.Bold)
                            Text("Add your free Finnhub key, then scan. Vurn filters live sub-$5 names and ranks up to 10 setups using price action plus recent headline catalysts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                itemsIndexed(picks) { index, pick -> PickCard(index + 1, pick) }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Free market data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Paste a free Finnhub API key. It stays on this phone.")
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it.trim() },
                        label = { Text("Finnhub API key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit().putString("finnhub", apiKey).apply()
                    showSettings = false
                }) { Text("SAVE") }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
private fun PickCard(rank: Int, pick: StockPick) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#$rank", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(12.dp))
                Text(pick.symbol, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text("${pick.score}/100", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text("$${"%.2f".format(pick.price)}   ${if (pick.changePercent >= 0) "+" else ""}${"%.2f".format(pick.changePercent)}% today")
            Text("${pick.confidence} confidence · ${pick.risk}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(pick.thesis)
            pick.catalysts.take(2).forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
