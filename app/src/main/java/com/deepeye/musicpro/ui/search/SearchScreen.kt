package com.deepeye.musicpro.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.search.SearchResultItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.ui.youtube.SmartTubeVideoCard
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import kotlinx.coroutines.flow.collectLatest

// ── Glassmorphism Palette ───────────────────────────────────────────────────
private val GlassContainer = Color(0xFF131722).copy(alpha = 0.85f)
private val NeonCyanAccent = Color(0xFF00E5FF)
private val GlassBorder = Color(0x22FFFFFF)
private val TextWhite = Color.White
private val TextSecondaryGlass = Color.White.copy(alpha = 0.65f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    windowSizeClass: WindowSizeClass,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val recent by viewModel.recentSearches.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasMoreResults by viewModel.hasMoreResults.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    // Mobile optimization: infinite scroll — fetch more as the user nears the end.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collectLatest { lastIndex ->
                val total = results.size
                if (total > 0 && lastIndex != null && lastIndex >= total - 4) {
                    viewModel.loadMore()
                }
            }
    }

    val hazeState = com.deepeye.musicpro.ui.LocalHazeState.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    tint = HazeTint(Color(0xFF1D1E26).copy(alpha = 0.4f)),
                                    blurRadius = 32.dp,
                                    noiseFactor = 0.05f
                                )
                            )
                        } else {
                            Modifier.background(Color(0xFF1D1E26).copy(alpha = 0.4f))
                        }
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Search",
                    style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        fontSize = 28.sp,
                    ),
                    color = TextWhite,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                when (windowSizeClass.widthSizeClass) {
                                    WindowWidthSizeClass.Medium -> Modifier.widthIn(max = 600.dp)
                                    WindowWidthSizeClass.Expanded -> Modifier.widthIn(max = 800.dp)
                                    else -> Modifier
                                },
                            ),
                    ) {
                        com.deepeye.musicpro.ui.components.premium.GlassOmnibox(
                            query = query,
                            onQueryChange = viewModel::onQueryChange,
                            onClear = { viewModel.onQueryChange("") },
                            placeholder = "Search local & YouTube Music",
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading && results.isNotEmpty(),
            onRefresh = { viewModel.refresh() },
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            SearchFilterBar(
                selected = selectedFilter,
                onSelected = viewModel::onFilterChange,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (query.isBlank()) {
                SearchSuggestionsSection(
                    suggestions = suggestions,
                    recentSearches = recent,
                    onSuggestionClick = viewModel::onQueryChange,
                )
            } else if (isLoading && results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyanAccent)
                }
            } else if (results.isEmpty()) {
                SearchEmptyState(query)
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 180.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(results, key = { it.id }) { item ->
                        if (item.videoId != null) {
                            SmartTubeVideoCard(
                                video = HomeVideoItem(
                                    id = item.videoId,
                                    title = item.title,
                                    channelName = item.artist ?: item.subtitle,
                                    thumbnailUrl = item.thumbnailUrl ?: "",
                                ),
                                onClick = {
                                    viewModel.playResult(item)
                                    onNavigateToNowPlaying()
                                },
                            )
                        } else {
                            SearchResultRow(
                                item = item,
                                onClick = {
                                    viewModel.playResult(item)
                                    onNavigateToNowPlaying()
                                },
                                onArtistClick = { item.artist?.let(onNavigateToArtist) },
                                onAddToQueue = { viewModel.addToQueue(item) },
                            )
                        }
                    }

                    if (hasMoreResults) {
                        item(key = "footer_loading") {
                            LoadingIndicatorRow(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    } else if (results.size >= 5) {
                        item(key = "footer_end") {
                            EndOfResultsRow(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Surface(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(32.dp),
        color = GlassContainer,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondaryGlass,
            )

            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                placeholder = {
                    Text(
                        text = "Search local & YouTube Music",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = TextSecondaryGlass,
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    color = TextWhite,
                ),
                colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = NeonCyanAccent,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                KeyboardActions(
                    onSearch = { focusManager.clearFocus() },
                ),
                singleLine = true,
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondaryGlass,
                    )
                }
            }
        }
    }
}

@Composable
fun SearchSuggestionsSection(
    suggestions: List<String>,
    recentSearches: List<String>,
    onSuggestionClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (recentSearches.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Searches",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                )
            }
            items(recentSearches, key = { it }) { recent ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassContainer)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .clickable { onSuggestionClick(recent) }
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Recent",
                        tint = TextSecondaryGlass,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = recent,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = TextWhite
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = "Search",
                        tint = TextSecondaryGlass.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            item {
                Text(
                    text = "Suggestions",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            item {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    suggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { onSuggestionClick(suggestion) },
                            label = {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    color = TextWhite,
                                )
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = GlassContainer,
                                labelColor = TextWhite,
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = GlassBorder,
                                borderWidth = 1.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(
    item: SearchResultItem,
    onClick: () -> Unit,
    onArtistClick: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GlassContainer)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GlassContainer)
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(18.dp)
                ),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                ),
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = TextSecondaryGlass,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onAddToQueue != null) {
            IconButton(
                onClick = onAddToQueue,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to queue",
                    tint = NeonCyanAccent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        if (item.artist != null) {
            IconButton(
                onClick = onArtistClick,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go to artist",
                    tint = TextSecondaryGlass,
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
fun SearchEmptyState(query: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextSecondaryGlass.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "No results for \"$query\"",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            color = TextWhite,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Try checking for typos or searching for something else.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = TextSecondaryGlass,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun LoadingIndicatorRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
            color = NeonCyanAccent,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Loading more...",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = TextSecondaryGlass,
        )
    }
}

@Composable
fun EndOfResultsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "You're all caught up",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = TextSecondaryGlass.copy(alpha = 0.7f),
        )
    }
}
