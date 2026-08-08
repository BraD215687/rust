package com.vurnindustrys.vurn

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vurnindustrys.vurn.data.Scanner
import com.vurnindustrys.vurn.data.VurnDataClient
import com.vurnindustrys.vurn.model.StockPick
import com.vurnindustrys.vurn.ui.theme.VurnTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VurnTheme { VurnApp() } }
    }
}

@Composable
private fun VurnApp() {
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("Ready") }
    var error by remember { mutableStateOf<String?>(null) }
    var picks by remember { mutableStateOf<List<StockPick>>(emptyList()) }
    var showDataInfo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Surface {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("VURN", fontWeight = FontWeight.Black, fontSize = 25.sp, letterSpacing = 3.sp)
                        Text("INDUSTRYS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    TextButton(onClick = { showDataInfo = true }) { Text("NO-KEY DATA") }
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
                        scope.launch {
                            scanning = true
                            error = null
                            progress = "Starting scan…"
                            runCatching {
                                Scanner(VurnDataClient()).scan { done, total ->
                                    progress = "Scanning $done / $total"
                                }
                            }.onSuccess {
                                picks = it
                                progress = "Scan complete"
                                if (it.isEmpty()) error = "No qualifying sub-$5 setups were returned by the free feeds right now."
                            }.onFailure {
                                error = it.message ?: "Scan failed"
                            }
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
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Vurn Score", fontWeight = FontWeight.Bold)
                            Text("No account. No API key. No subscription.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("Vurn pulls free public price and headline data itself, filters sub-$5 names, and ranks up to 10 seven-day setups.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                itemsIndexed(picks) { index, pick -> PickCard(index + 1, pick) }
            }
        }
    }

    if (showDataInfo) {
        AlertDialog(
            onDismissRequest = { showDataInfo = false },
            title = { Text("Vurn data") },
            text = { Text("Vurn uses no user API keys. It reads free public market/chart endpoints and public news RSS feeds directly. Sources can change or be delayed, so every signal is treated as experimental.") },
            confirmButton = { Button(onClick = { showDataInfo = false }) { Text("GOT IT") } }
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
            Text("$${"%.2f".format(pick.price)}   ${if (pick.changePercent >= 0) "+" else ""}${"%.2f".format(pick.changePercent)}%")
            Text("${pick.confidence} confidence · ${pick.risk}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(pick.thesis)
            pick.catalysts.take(2).forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
