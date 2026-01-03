package com.nerdpace.statusholder.screen



// Main Screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.whatsappstatussaver.domain.model.MediaType
import com.whatsappstatussaver.domain.model.StatusMedia
import com.whatsappstatussaver.domain.model.WhatsAppSource
import com.whatsappstatussaver.presentation.model.MainUiState
import com.whatsappstatussaver.presentation.model.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onEvent: (UiEvent) -> Unit,
    onSourceChanged: (WhatsAppSource) -> Unit,
    onTabChanged: (MediaType) -> Unit,
    onScanClick: () -> Unit,
    onRequestSafAccess: () -> Intent,
    onSafAccessGranted: (android.net.Uri) -> Unit,
    onMediaClick: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // SAF launcher
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { onSafAccessGranted(it) }
    }

    // Handle events
    LaunchedEffect(Unit) {
        // Event handling would be here
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsApp Status Saver") },
                actions = {
                    IconButton(
                        onClick = onScanClick,
                        enabled = !uiState.isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan statuses"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Source selector
            SourceSelector(
                selectedSource = uiState.selectedSource,
                onSourceSelected = onSourceChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Tab selector
            TabRow(selectedTabIndex = if (uiState.selectedTab == MediaType.PHOTO) 0 else 1) {
                Tab(
                    selected = uiState.selectedTab == MediaType.PHOTO,
                    onClick = { onTabChanged(MediaType.PHOTO) },
                    text = { Text("Photos") }
                )
                Tab(
                    selected = uiState.selectedTab == MediaType.VIDEO,
                    onClick = { onTabChanged(MediaType.VIDEO) },
                    text = { Text("Videos") }
                )
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.needsSafAccess -> {
                        SafAccessRequired(
                            onGrantAccess = {
                                safLauncher.launch(null)
                            }
                        )
                    }
                    uiState.isLoading || uiState.isScanning -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.error != null -> {
                        ErrorState(
                            message = uiState.error,
                            onRetry = onScanClick
                        )
                    }
                    uiState.mediaList.isEmpty() -> {
                        EmptyState(
                            type = uiState.selectedTab,
                            onScan = onScanClick
                        )
                    }
                    else -> {
                        MediaGrid(
                            mediaList = uiState.mediaList,
                            onMediaClick = onMediaClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SourceSelector(
    selectedSource: WhatsAppSource,
    onSourceSelected: (WhatsAppSource) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SourceChip(
            label = "WhatsApp",
            isSelected = selectedSource == WhatsAppSource.NORMAL_WHATSAPP,
            onClick = { onSourceSelected(WhatsAppSource.NORMAL_WHATSAPP) },
            modifier = Modifier.weight(1f)
        )
        SourceChip(
            label = "Business",
            isSelected = selectedSource == WhatsAppSource.WHATSAPP_BUSINESS,
            onClick = { onSourceSelected(WhatsAppSource.WHATSAPP_BUSINESS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SourceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun MediaGrid(
    mediaList: List<StatusMedia>,
    onMediaClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(mediaList, key = { it.id }) { media ->
            MediaItem(
                media = media,
                onClick = { onMediaClick(media.id) }
            )
        }
    }
}

@Composable
fun MediaItem(
    media: StatusMedia,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(media.cachedUri ?: media.uri)
                .crossfade(true)
                .build(),
            contentDescription = media.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video indicator
        if (media.mediaType == MediaType.VIDEO) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = "VIDEO",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Saved indicator
        if (media.isSaved) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = "✓",
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun SafAccessRequired(onGrantAccess: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Storage Access Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "To view WhatsApp statuses, please grant access to the WhatsApp status folder.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGrantAccess) {
            Text("Grant Access")
        }
    }
}

@Composable
fun EmptyState(type: MediaType, onScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No ${type.name.lowercase()}s found",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the refresh button to scan for statuses",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

// Preview Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    media: StatusMedia?,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (media != null) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onSaveClick,
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving && !media.isSaved
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(if (media.isSaved) "Saved" else "Save")
                            }
                        }
                        OutlinedButton(
                            onClick = onShareClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (media != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(media.cachedUri ?: media.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = media.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}