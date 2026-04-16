package com.example.a4_1p

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.a4_1p.ui.theme._41PTheme
import org.json.JSONArray
import org.json.JSONObject

data class PlannerEvent(
    val id: Int,
    val title: String,
    val category: String,
    val date: String,
    val location: String,
    val notes: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _41PTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EventPlannerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventPlannerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val eventStorage = remember { EventStorage(context) }
    val events = remember { mutableStateListOf<PlannerEvent>() }

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var nextId by remember { mutableIntStateOf(1) }
    var editingEventId by remember { mutableStateOf<Int?>(null) }

    fun clearForm() {
        title = ""
        category = ""
        date = ""
        location = ""
        notes = ""
        editingEventId = null
    }

    LaunchedEffect(Unit) {
        val loadedEvents = eventStorage.loadEvents()
        events.addAll(loadedEvents)
        nextId = (loadedEvents.maxOfOrNull { it.id } ?: 0) + 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Personal Event Planner",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Event title") },
            singleLine = true,
        )

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Category") },
            singleLine = true,
        )

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Date & time") },
            placeholder = { Text("e.g. 2026-05-03 14:00") },
            singleLine = true,
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Location") },
            singleLine = true,
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes") },
            minLines = 2,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    if (title.isBlank() || category.isBlank() || date.isBlank() || location.isBlank()) {
                        return@Button
                    }

                    val event = PlannerEvent(
                        id = editingEventId ?: nextId,
                        title = title.trim(),
                        category = category.trim(),
                        date = date.trim(),
                        location = location.trim(),
                        notes = notes.trim(),
                    )

                    if (editingEventId == null) {
                        nextId += 1
                        events.add(event)
                    } else {
                        val index = events.indexOfFirst { it.id == editingEventId }
                        if (index != -1) {
                            events[index] = event
                        }
                    }

                    eventStorage.saveEvents(events)
                    clearForm()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (editingEventId == null) "Add event" else "Update event")
            }

            if (editingEventId != null) {
                Button(
                    onClick = { clearForm() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Upcoming schedule",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(events, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    onEdit = {
                        editingEventId = event.id
                        title = event.title
                        category = event.category
                        date = event.date
                        location = event.location
                        notes = event.notes
                    },
                    onDelete = {
                        events.remove(event)
                        eventStorage.saveEvents(events)
                        if (editingEventId == event.id) {
                            clearForm()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun EventCard(event: PlannerEvent, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Category: ${event.category}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Date: ${event.date}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Location: ${event.location}", style = MaterialTheme.typography.bodyMedium)
            if (event.notes.isNotBlank()) {
                Text(text = "Notes: ${event.notes}", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
            ) {
                Button(onClick = onEdit) {
                    Text("Edit")
                }
                Button(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

class EventStorage(context: Context) {
    private val preferences = context.getSharedPreferences("planner_storage", Context.MODE_PRIVATE)

    fun loadEvents(): List<PlannerEvent> {
        val raw = preferences.getString(KEY_EVENTS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    PlannerEvent(
                        id = obj.optInt("id", i + 1),
                        title = obj.optString("title"),
                        category = obj.optString("category"),
                        date = obj.optString("date"),
                        location = obj.optString("location"),
                        notes = obj.optString("notes"),
                    ),
                )
            }
        }
    }

    fun saveEvents(events: List<PlannerEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject()
                    .put("id", event.id)
                    .put("title", event.title)
                    .put("category", event.category)
                    .put("date", event.date)
                    .put("location", event.location)
                    .put("notes", event.notes),
            )
        }
        preferences.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    companion object {
        private const val KEY_EVENTS = "events"
    }
}

@Preview(showBackground = true)
@Composable
fun EventPlannerPreview() {
    _41PTheme {
        EventPlannerScreen()
    }
}
