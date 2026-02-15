package com.vmeasure.app.feature.lists

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.vmeasure.app.App
import com.vmeasure.app.data.repository.UserRepositoryImpl
import androidx.navigation.NavHostController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.clickable
import com.vmeasure.app.core.navigation.Routes
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    navController: NavHostController,
    onAddUser: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
//    val repo = remember(app) { UserRepositoryImpl(app.db.userDao(), app.db.sectionDao()) }

    val repo = remember(app) { UserRepositoryImpl(app.db) }

    val vm: ListsViewModel = viewModel(factory = ListsViewModelFactory(repo))

    val uiState by vm.uiState.collectAsState()
    val overrides by vm.overrides.collectAsState()

    val pagingItems = vm.usersPaging.collectAsLazyPagingItems()

    val lifecycleOwner = LocalLifecycleOwner.current

    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

// Current applied filters from VM:
    val appliedFilters = uiState.filters

// Draft filters for editing in sheet:
    var draftFilters by remember(showFilters) { mutableStateOf(appliedFilters) }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                pagingItems.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Refresh list when coming back from Add screen
    val refreshFlow = remember(navController) {
        navController.currentBackStackEntry?.savedStateHandle
            ?.getStateFlow("lists_refresh", false)
    }

    LaunchedEffect(refreshFlow) {
        refreshFlow?.collect { shouldRefresh ->
            if (shouldRefresh) {
                pagingItems.refresh()
                navController.currentBackStackEntry?.savedStateHandle?.set("lists_refresh", false)
            }
        }
    }

    // One-off events (share, errors)
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is ListsUiEvent.ShareText -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, ev.text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share customer"))
                }
                is ListsUiEvent.Error -> {
                    // keep it simple for now; later we’ll use SnackbarHost
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Top search row (like Image 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchText,
                    onValueChange = vm::onSearchChanged,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search...") },
                    leadingIcon = {
                        Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(0.dp))
                    }
                )

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
//                    onClick = vm::onFilterClicked,
                    onClick = {
                        draftFilters = appliedFilters
                        showFilters = true
                        vm.onFilterClicked()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Outlined.FilterList, contentDescription = "Filter")
                }
            }

            Divider()

            // List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.publicUserId }
                ) { index ->
                    val user = pagingItems[index] ?: return@items
                    val o = overrides[user.publicUserId]

                    val isPinned = o?.isPinned ?: user.isPinned
                    val isFav = o?.isFavorite ?: user.isFavorite

                    UserCard(
                        name = user.name,
                        tags = user.tags,
                        isPinned = isPinned,
                        isFavorite = isFav,
                        onTogglePinned = { vm.onTogglePinned(user) },
                        onToggleFavorite = { vm.onToggleFavorite(user) },
                        onDelete = { vm.onDeleteUser(user) },
                        onShare = { vm.onShareUser(user) },
                        onOpenDetails = { navController.navigate(Routes.details(user.publicUserId)) }
                    )
                }

                // loader at end (paging)
                item {
                    if (pagingItems.loadState.append is androidx.paging.LoadState.Loading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        // FAB (+)
        FloatingActionButton(
            onClick = onAddUser,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp),
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f) // 80% height
            ) {
                FiltersSheetContent(
                    filters = draftFilters,
                    onChange = { draftFilters = it },
                    onApply = {
                        vm.applyFilters(draftFilters)
                        showFilters = false
                    },
                    onReset = {
                        val reset = ListFilters()
                        draftFilters = reset
                        vm.applyFilters(reset)
                        showFilters = false
                    },
                    onClose = { showFilters = false }
                )
            }
        }
    }

}

@Composable
private fun UserCard(
    name: String,
    tags: List<String>,
    isPinned: Boolean,
    isFavorite: Boolean,
    onTogglePinned: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onOpenDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .clickable { onOpenDetails() },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(onClick = onTogglePinned) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin"
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favourite"
                    )
                }

                UserMenu(onDelete = onDelete, onShare = onShare)
            }

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRowChips(tags = tags)
            }
        }
    }
}

@Composable
private fun UserMenu(
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Menu")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = { expanded = false; onShare() }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}

@Composable
private fun FlowRowChips(tags: List<String>) {
    // Lightweight manual wrap (no external library)
    // Simple approach: show up to 3 chips per row, then wrap
    val rows = tags.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tag ->
                    AssistChip(
                        onClick = { },
                        label = { Text(tag) }
                    )
                }
            }
        }
    }
}
