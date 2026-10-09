package com.bizzeh.synthkit.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.ui.MinTouchTarget

@Composable
fun ProjectListScreen(
    projects: List<StoredProject>?,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onRename: (String, String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.projects_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onCreate, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.project_new))
            }
        }
        when {
            projects == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            projects.isEmpty() -> Text(stringResource(R.string.projects_empty), style = MaterialTheme.typography.bodyLarge)
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(projects, key = { it.project.id }) { stored ->
                    ProjectRow(
                        project = stored.project,
                        onOpen = { onOpen(stored.project.id) },
                        onRename = { renaming = stored.project.id },
                        onDuplicate = { onDuplicate(stored.project.id) },
                        onDelete = { deleting = stored.project.id },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val byId = { id: String? -> projects?.firstOrNull { it.project.id == id }?.project }
    byId(renaming)?.let { project ->
        RenameDialog(
            initial = project.name,
            onSave = { name ->
                onRename(project.id, name)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    byId(deleting)?.let { project ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text(stringResource(R.string.project_delete_confirm, project.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(project.id)
                    deleting = null
                }) { Text(stringResource(R.string.project_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ProjectRow(
    project: Project,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(project.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.project_summary,
                    project.tempoBpm,
                    pluralStringResource(R.plurals.track_count, project.tracks.size, project.tracks.size),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.project_actions, project.name))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.project_rename)) }, onClick = {
                    menu = false
                    onRename()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.project_duplicate)) }, onClick = {
                    menu = false
                    onDuplicate()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.project_delete)) }, onClick = {
                    menu = false
                    onDelete()
                })
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.project_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(Project.MAX_NAME_LENGTH) },
                singleLine = true,
                label = { Text(stringResource(R.string.project_name)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = ProjectNames.clean(name) != null) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
