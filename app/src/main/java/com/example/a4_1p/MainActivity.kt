package com.example.a4_1p

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.a4_1p.data.EventDatabase
import com.example.a4_1p.data.EventEntity
import com.example.a4_1p.ui.theme._41PTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class PlannerEvent(
    val id: Int,
    val title: String,
    val category: String,
    val date: String,
    val location: String,
    val notes: String,
)

private fun EventEntity.toPlannerEvent(): PlannerEvent = PlannerEvent(
    id = id,
    title = title,
    category = category,
    date = date,
    location = location,
    notes = notes,
)

private fun PlannerEvent.toEventEntity(): EventEntity = EventEntity(
    id,
    title,
    category,
    date,
    location,
    notes,
)

private enum class PlannerDestination(val route: String, val label: String, val iconText: String) {
    EventList("event_list", "Event List", "L"),
    AddEvent("add_event", "Add Event", "A"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _41PTheme {
                EventPlannerApp()
            }
        }
    }
}

@Composable
fun EventPlannerApp() {
    val context = LocalContext.current
    val eventDao = remember(context) { EventDatabase.getInstance(context).eventDao() }
    val events = remember { mutableStateListOf<PlannerEvent>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var editingEventId by remember { mutableStateOf<Int?>(null) }

    fun clearForm() {
        title = ""
        category = ""
        date = ""
        location = ""
        notes = ""
        editingEventId = null
    }

    fun editEvent(event: PlannerEvent) {
        editingEventId = event.id
        title = event.title
        category = event.category
        date = event.date
        location = event.location
        notes = event.notes
    }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    suspend fun loadEventsFromDb() {
        val loadedEvents = withContext(Dispatchers.IO) {
            eventDao.getAll().map { it.toPlannerEvent() }
        }
        events.clear()
        events.addAll(loadedEvents)
    }

    fun parseDateInput(rawDate: String): LocalDate? {
        val datePart = rawDate.trim().split(" ").firstOrNull().orEmpty()
        return try {
            LocalDate.parse(datePart)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun saveEvent(): Boolean {
        if (title.isBlank() || date.isBlank()) {
            showMessage("Title and Date are required.")
            return false
        }

        if (editingEventId == null) {
            val selectedDate = parseDateInput(date)
            if (selectedDate == null) {
                showMessage("Enter Date as YYYY-MM-DD (optionally followed by time).")
                return false
            }
            if (selectedDate.isBefore(LocalDate.now())) {
                showMessage("New events cannot use a past date.")
                return false
            }
        }

        if (category.isBlank() || location.isBlank()) {
            showMessage("Category and Location are required.")
            return false
        }

        val event = PlannerEvent(
            id = editingEventId ?: 0,
            title = title.trim(),
            category = category.trim(),
            date = date.trim(),
            location = location.trim(),
            notes = notes.trim(),
        )

        scope.launch {
            if (editingEventId == null) {
                withContext(Dispatchers.IO) {
                    eventDao.insert(event.toEventEntity())
                }
            } else {
                withContext(Dispatchers.IO) {
                    eventDao.update(event.toEventEntity())
                }
            }
            loadEventsFromDb()
            clearForm()
        }
        return true
    }

    LaunchedEffect(Unit) {
        loadEventsFromDb()
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: PlannerDestination.EventList.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                PlannerDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                            }
                        },
                        icon = { Text(destination.iconText) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PlannerDestination.EventList.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(PlannerDestination.EventList.route) {
                EventListScreen(
                    events = events,
                    onEdit = { event ->
                        editEvent(event)
                        navController.navigate(PlannerDestination.AddEvent.route)
                    },
                    onDelete = { event ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                eventDao.delete(event.toEventEntity())
                            }
                            events.remove(event)
                            if (editingEventId == event.id) {
                                clearForm()
                            }
                            showMessage("Event deleted successfully.")
                        }
                    },
                )
            }
            composable(PlannerDestination.AddEvent.route) {
                AddEventScreen(
                    title = title,
                    category = category,
                    date = date,
                    location = location,
                    notes = notes,
                    editingEventId = editingEventId,
                    onTitleChange = { title = it },
                    onCategoryChange = { category = it },
                    onDateChange = { date = it },
                    onLocationChange = { location = it },
                    onNotesChange = { notes = it },
                    onSave = {
                        val saved = saveEvent()
                        if (saved) {
                            navController.navigate(PlannerDestination.EventList.route) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onCancelEdit = {
                        clearForm()
                        navController.navigate(PlannerDestination.EventList.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun EventListScreen(
    events: List<PlannerEvent>,
    onEdit: (PlannerEvent) -> Unit,
    onDelete: (PlannerEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Upcoming schedule",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (events.isEmpty()) {
            Text(
                text = "No events yet. Use Add Event to create one.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(events, key = { it.id }) { event ->
                    EventCard(event = event, onEdit = { onEdit(event) }, onDelete = { onDelete(event) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventScreen(
    title: String,
    category: String,
    date: String,
    location: String,
    notes: String,
    editingEventId: Int?,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Column(
        modifier = Modifier
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
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Event title") },
            singleLine = true,
        )

        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Category") },
            singleLine = true,
        )

        OutlinedTextField(
            value = date,
            onValueChange = onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Date & time") },
            placeholder = { Text("e.g. 2026-05-03 14:00") },
            singleLine = true,
        )

        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Location") },
            singleLine = true,
        )

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes") },
            minLines = 2,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Text(if (editingEventId == null) "Add event" else "Update event")
            }

            if (editingEventId != null) {
                Button(onClick = onCancelEdit, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
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

@Preview(showBackground = true)
@Composable
fun EventPlannerPreview() {
    _41PTheme {
        EventPlannerApp()
    }
}
