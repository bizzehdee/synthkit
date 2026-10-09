package com.bizzeh.synthkit.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.instruments.Family
import com.bizzeh.synthkit.ui.MinTouchTarget
import com.bizzeh.synthkit.ui.studio.PanelIconButton
import com.bizzeh.synthkit.ui.studio.raised
import com.bizzeh.synthkit.ui.theme.StudioTheme
import com.bizzeh.synthkit.ui.theme.familyColor

private const val PREVIEW_TRACKS = 4

@Composable
fun ProjectListScreen(
    projects: List<StoredProject>?,
    familyOf: (Track) -> Family?,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onRename: (String, String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = StudioTheme.palette
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BrandMark()
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = p.text,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .heightIn(min = MinTouchTarget)
                    .background(p.amber, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClick = onCreate)
                    .padding(horizontal = 18.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = p.onLit)
                Text(stringResource(R.string.project_new), style = MaterialTheme.typography.labelLarge, color = p.onLit)
            }
        }
        when {
            projects == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = p.amber)
            }
            projects.isEmpty() -> Text(stringResource(R.string.projects_empty), style = MaterialTheme.typography.bodyLarge, color = p.muted)
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 230.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(projects, key = { it.project.id }) { stored ->
                    ProjectCard(
                        project = stored.project,
                        familyOf = familyOf,
                        onOpen = { onOpen(stored.project.id) },
                        onRename = { renaming = stored.project.id },
                        onDuplicate = { onDuplicate(stored.project.id) },
                        onDelete = { deleting = stored.project.id },
                    )
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

/** The 2 x 2 pad mark from the launcher icon. */
@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier.size(32.dp).background(Color(0xFF2E2A85), RoundedCornerShape(8.dp)).padding(6.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(listOf(0xFFFFB870, 0xFF8B7CFF), listOf(0xFFFFB870, 0xFFFFB870)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { color ->
                        Box(Modifier.size(8.5.dp).background(Color(color), RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    familyOf: (Track) -> Family?,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val p = StudioTheme.palette
    var menu by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .raised(p, 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(14.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Bottom),
            modifier = Modifier.fillMaxWidth().aspectRatio(3f),
        ) {
            project.tracks.take(PREVIEW_TRACKS).forEach { track ->
                NotePreview(
                    track = track,
                    loopBars = project.loopBars,
                    color = familyOf(track)?.let(::familyColor) ?: p.muted,
                    barLines = false,
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(
                        R.string.project_meta,
                        project.tempoBpm,
                        pluralStringResource(R.plurals.track_count, project.tracks.size, project.tracks.size),
                        if (project.loopBars == 0) stringResource(R.string.project_no_loop)
                        else pluralStringResource(R.plurals.bar_count, project.loopBars, project.loopBars),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = p.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                PanelIconButton(Icons.Filled.MoreVert, stringResource(R.string.project_actions, project.name), { menu = true }, framed = false)
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
