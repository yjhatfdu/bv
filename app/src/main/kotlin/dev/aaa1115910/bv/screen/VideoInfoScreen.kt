package dev.aaa1115910.bv.screen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.transform.BlurTransformation
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.video.Dimension
import dev.aaa1115910.biliapi.entity.video.Tag
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.repositories.FavoriteRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.activities.video.TagActivity
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.component.UpIcon
import dev.aaa1115910.bv.component.buttons.FavoriteButton
import dev.aaa1115910.bv.component.videocard.VideosRow
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.repository.VideoListItem
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fDebug
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.focusedBorder
import dev.aaa1115910.bv.util.formatPubTimeString
import dev.aaa1115910.bv.util.launchPlayerActivity
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.video.VideoDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import kotlin.math.ceil

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoInfoScreen(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    videoInfoRepository: VideoInfoRepository = getKoin().get(),
    videoDetailViewModel: VideoDetailViewModel = koinViewModel(),
    userRepository: UserRepository = getKoin().get(),
    favoriteRepository: FavoriteRepository = getKoin().get(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val intent = (context as Activity).intent
    val logger = KotlinLogging.logger { }

    var showFollowButton by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }

    var lastPlayedCid by remember { mutableIntStateOf(0) }
    var lastPlayedTime by remember { mutableIntStateOf(0) }

    var tip by remember { mutableStateOf("Loading") }
    var fromSeason by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }

    val containsVerticalScreenVideo by remember {
        derivedStateOf {
            videoDetailViewModel.videoDetail?.pages?.any { it.dimension.isVertical } ?: false
                    || videoDetailViewModel.videoDetail?.ugcSeason?.sections?.any { section -> section.episodes.any { it.dimension!!.isVertical } } ?: false
        }
    }

    var favorited by remember { mutableStateOf(false) }
    val favoriteFolderMetadataList = remember { mutableStateListOf<FavoriteFolderMetadata>() }
    val videoInFavoriteFolderIds = remember { mutableStateListOf<Long>() }

    val setHistory = {
        logger.info { "play history: ${videoDetailViewModel.videoDetail?.history}" }
        lastPlayedCid = videoDetailViewModel.videoDetail?.history?.lastPlayedCid ?: 0
        lastPlayedTime = videoDetailViewModel.videoDetail?.history?.progress ?: 0
    }

    val updateHistory = {
        scope.launch(Dispatchers.IO) {
            runCatching {
                videoDetailViewModel.loadDetailOnlyUpdateHistory(videoDetailViewModel.videoDetail!!.aid)
            }
            setHistory()
        }
    }


    val updateFollowingState: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            val userMid = videoDetailViewModel.videoDetail?.author?.mid ?: -1
            logger.fInfo { "Checking is following user $userMid" }
            val success = userRepository.checkIsFollowing(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Following user result: $success" }
            showFollowButton = success != null
            isFollowing = success ?: false
        }
    }

    val addFollow: (afterModify: (success: Boolean) -> Unit) -> Unit = { afterModify ->
        scope.launch(Dispatchers.IO) {
            val userMid = videoDetailViewModel.videoDetail?.author?.mid ?: -1
            logger.fInfo { "Add follow to user $userMid" }
            val success = userRepository.followUser(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Add follow result: $success" }
            afterModify(success)
        }
    }

    val delFollow: (afterModify: (success: Boolean) -> Unit) -> Unit = { afterModify ->
        scope.launch(Dispatchers.IO) {
            val userMid = videoDetailViewModel.videoDetail?.author?.mid ?: -1
            logger.fInfo { "Del follow to user $userMid" }
            val success = userRepository.unfollowUser(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Del follow result: $success" }
            afterModify(success)
        }
    }

    val fetchFavoriteData: (Int) -> Unit = { avid ->
        scope.launch(Dispatchers.IO) {
            runCatching {
                val favoriteFolderMetadataListResult =
                    favoriteRepository.getAllFavoriteFolderMetadataList(
                        mid = Prefs.uid,
                        rid = avid,
                        preferApiType = Prefs.apiType
                    )
                favoriteFolderMetadataList.swapList(favoriteFolderMetadataListResult)

                val videoInFavoriteFolderIdsResult = favoriteFolderMetadataListResult
                    .filter { it.videoInThisFav }
                videoInFavoriteFolderIds.swapList(videoInFavoriteFolderIdsResult.map { it.id })

                logger.fDebug { "Update favoriteFolders: ${favoriteFolderMetadataList.map { it.title }}" }
                logger.fDebug { "Update videoInFavoriteFolderIds: ${videoInFavoriteFolderIdsResult.map { it.title }}" }
            }
        }
    }

    val updateVideoFavoriteData: (List<Long>) -> Unit = { folderIds ->
        scope.launch(Dispatchers.IO) {
            runCatching {
                require(favoriteFolderMetadataList.isNotEmpty()) { "Not found favorite folder" }
                require(videoDetailViewModel.videoDetail?.aid != null) { "Video info is null" }
                logger.info { "Update video av${videoDetailViewModel.videoDetail?.aid} to favorite folder $folderIds" }

                favoriteRepository.updateVideoToFavoriteFolder(
                    aid = videoDetailViewModel.videoDetail!!.aid,
                    addMediaIds = folderIds,
                    delMediaIds = favoriteFolderMetadataList.map { it.id } - folderIds.toSet()
                )
            }.onFailure {
                logger.fInfo { "Update video to favorite folder failed: ${it.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    it.message ?: "unknown error".toast(context)
                }
            }.onSuccess {
                logger.fInfo { "Update video to favorite folder success" }
                videoInFavoriteFolderIds.swapList(folderIds)
            }
        }
    }

    val addVideoToDefaultFavoriteFolder: () -> Unit = {
        runCatching {
            val defaultFavoriteFolder =
                favoriteFolderMetadataList.firstOrNull { it.title == "默认收藏夹" }
            require(defaultFavoriteFolder != null) { "Not found default favorite folder" }
            updateVideoFavoriteData(listOf(defaultFavoriteFolder.id))
        }.onFailure {
            logger.fInfo { "Add video to default favorite folder failed: ${it.stackTraceToString()}" }
            it.message ?: "unknown error".toast(context)
        }
    }

    val updateVideoIsFavoured = {
        favorited = videoDetailViewModel.videoDetail?.userActions?.favorite ?: false
    }

    LaunchedEffect(Unit) {
        if (intent.hasExtra("aid")) {
            val aid = intent.getIntExtra("aid", 170001)
            fromSeason = intent.getBooleanExtra("fromSeason", false)
            //获取视频信息
            scope.launch(Dispatchers.IO) {
                runCatching {
                    videoDetailViewModel.loadDetail(aid)
                    updateVideoIsFavoured()
                    setHistory()
                    if (Prefs.isLogin) fetchFavoriteData(aid)

                    //如果是从剧集跳转过来的，就直接播放 P1
                    if (fromSeason) {
                        val playPart = videoDetailViewModel.videoDetail!!.pages.first()
                        launchPlayerActivity(
                            context = context,
                            avid = videoDetailViewModel.videoDetail!!.aid,
                            cid = playPart.cid,
                            title = videoDetailViewModel.videoDetail!!.title,
                            partTitle = videoDetailViewModel.videoDetail!!.pages.find { it.cid == playPart.cid }!!.title,
                            played = if (playPart.cid == lastPlayedCid) lastPlayedTime * 1000 else 0,
                            fromSeason = true,
                            isVerticalVideo = containsVerticalScreenVideo
                        )
                        context.finish()
                    } else if (videoDetailViewModel.videoDetail?.ugcSeason != null) {
                        //如果不是剧集，则设置分p数据，以便播放器读取（合集）
                        val partVideoList =
                            videoDetailViewModel.videoDetail!!.ugcSeason!!.sections[0].episodes.mapIndexed { index, episode ->
                                VideoListItem(
                                    aid = episode.aid,
                                    cid = episode.cid,
                                    title = episode.title,
                                    index = index,
                                    isEpisode = false
                                )
                            }
                        videoInfoRepository.videoList.clear()
                        videoInfoRepository.videoList.addAll(partVideoList)
                    } else {
                        //如果不是剧集，则设置分p数据，以便播放器读取（分P）
                        val partVideoList =
                            videoDetailViewModel.videoDetail!!.pages.mapIndexed { index, videoPage ->
                                VideoListItem(
                                    aid = aid,
                                    cid = videoPage.cid,
                                    title = videoPage.title,
                                    index = index,
                                    isEpisode = false
                                )
                            }
                        videoInfoRepository.videoList.clear()
                        videoInfoRepository.videoList.addAll(partVideoList)
                    }
                }.onFailure {
                    tip = it.localizedMessage ?: "未知错误"
                    logger.fInfo { "Get video info failed: ${it.stackTraceToString()}" }
                }
            }
        }
    }

    LaunchedEffect(videoDetailViewModel.videoDetail) {
        //如果是从剧集页跳转回来的，那就不需要再跳转到剧集页了
        if (fromSeason) return@LaunchedEffect

        videoDetailViewModel.videoDetail?.let {
            if (it.redirectToEp) {
                runCatching {
                    logger.fInfo { "Redirect to ep ${it.epid}" }
                    SeasonInfoActivity.actionStart(context, it.epid)
                    context.finish()
                }.onFailure {
                    logger.fWarn { "Redirect failed: ${it.stackTraceToString()}" }
                }
            } else {
                logger.fInfo { "No redirection required" }
            }

            if (!fromSeason) updateFollowingState()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                paused = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                // 如果 pause==true 那可能是从播放页返回回来的，此时更新历史记录
                if (paused) updateHistory()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (videoDetailViewModel.videoDetail == null || videoDetailViewModel.videoDetail?.redirectToEp == true || fromSeason) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = tip
            )
        }
    } else {
        Scaffold(
            containerColor = Color.Black
        ) { innerPadding ->
            Box(
                modifier.padding(innerPadding)
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(if (videoDetailViewModel.videoDetail?.ugcSeason != null) videoDetailViewModel.videoDetail!!.ugcSeason!!.cover else videoDetailViewModel.videoDetail!!.cover)
                            .transformations(BlurTransformation(LocalContext.current, 20f, 5f))
                            .build()
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    alpha = 0.6f
                )
                TvLazyColumn(
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (containsVerticalScreenVideo) {
                                ArgueTip(text = stringResource(R.string.video_info_argue_tip_vertical_screen))
                            }
                            if (videoDetailViewModel.videoDetail?.argueTip != null) {
                                ArgueTip(text = videoDetailViewModel.videoDetail!!.argueTip!!)
                            }
                        }
                    }
                    item {
                        VideoInfoData(
                            videoDetail = videoDetailViewModel.videoDetail!!,
                            showFollowButton = showFollowButton,
                            isFollowing = isFollowing,
                            tags = videoDetailViewModel.videoDetail!!.tags,
                            isFavorite = favorited,
                            userFavoriteFolders = favoriteFolderMetadataList,
                            favoriteFolderIds = videoInFavoriteFolderIds,
                            onClickCover = {
                                logger.fInfo { "Click video cover" }
                                launchPlayerActivity(
                                    context = context,
                                    avid = videoDetailViewModel.videoDetail!!.aid,
                                    cid = videoDetailViewModel.videoDetail!!.pages.first().cid,
                                    title = videoDetailViewModel.videoDetail!!.title,
                                    partTitle = videoDetailViewModel.videoDetail!!.pages.first().title,
                                    played = if (videoDetailViewModel.videoDetail!!.cid == lastPlayedCid) lastPlayedTime * 1000 else 0,
                                    fromSeason = false,
                                    isVerticalVideo = containsVerticalScreenVideo
                                )
                            },
                            onClickUp = {
                                UpInfoActivity.actionStart(
                                    context,
                                    mid = videoDetailViewModel.videoDetail!!.author.mid,
                                    name = videoDetailViewModel.videoDetail!!.author.name
                                )
                            },
                            onAddFollow = {
                                addFollow {
                                    updateFollowingState()
                                }
                            },
                            onDelFollow = {
                                delFollow {
                                    updateFollowingState()
                                }
                            },
                            onClickTip = { tag ->
                                TagActivity.actionStart(
                                    context = context,
                                    tagId = tag.id,
                                    tagName = tag.name
                                )
                            },
                            onAddToDefaultFavoriteFolder = {
                                addVideoToDefaultFavoriteFolder()
                                favorited = true
                            },
                            onUpdateFavoriteFolders = {
                                updateVideoFavoriteData(it)
                                favorited = it.isNotEmpty()
                                videoInFavoriteFolderIds.swapList(it)
                            }
                        )
                    }
                    item {
                        VideoDescription(
                            description = videoDetailViewModel.videoDetail?.description ?: "no desc"
                        )
                    }
                    if (videoDetailViewModel.videoDetail?.ugcSeason == null) {
                        item {
                            VideoPartRow(
                                pages = videoDetailViewModel.videoDetail?.pages ?: emptyList(),
                                lastPlayedCid = lastPlayedCid,
                                lastPlayedTime = lastPlayedTime,
                                enablePartListDialog =
                                (videoDetailViewModel.videoDetail?.pages?.size ?: 0) > 5,
                                onClick = { cid ->
                                    logger.fInfo { "Click video part: [av:${videoDetailViewModel.videoDetail?.aid}, bv:${videoDetailViewModel.videoDetail?.bvid}, cid:$cid]" }
                                    launchPlayerActivity(
                                        context = context,
                                        avid = videoDetailViewModel.videoDetail!!.aid,
                                        cid = cid,
                                        title = videoDetailViewModel.videoDetail!!.title,
                                        partTitle = videoDetailViewModel.videoDetail!!.pages.find { it.cid == cid }!!.title,
                                        played = if (cid == lastPlayedCid) lastPlayedTime * 1000 else 0,
                                        fromSeason = false,
                                        isVerticalVideo = containsVerticalScreenVideo
                                    )
                                }
                            )
                        }
                    } else {
                        item {
                            VideoUgcSeasonRow(
                                episodes = videoDetailViewModel.videoDetail?.ugcSeason?.sections?.get(
                                    0
                                )?.episodes
                                    ?: emptyList(),
                                lastPlayedCid = lastPlayedCid,
                                lastPlayedTime = lastPlayedTime,
                                enableUgcListDialog =
                                (videoDetailViewModel.videoDetail?.ugcSeason?.sections?.get(0)?.episodes?.size
                                    ?: 0) > 5,
                                onClick = { aid, cid ->
                                    logger.fInfo { "Click ugc season part: [av:${videoDetailViewModel.videoDetail?.aid}, bv:${videoDetailViewModel.videoDetail?.bvid}, cid:$cid]" }
                                    launchPlayerActivity(
                                        context = context,
                                        avid = aid,
                                        cid = cid,
                                        title = videoDetailViewModel.videoDetail!!.title,
                                        partTitle = videoDetailViewModel.videoDetail!!.ugcSeason!!.sections[0].episodes.find { it.cid == cid }!!.title,
                                        played = if (cid == lastPlayedCid) lastPlayedTime * 1000 else 0,
                                        fromSeason = false,
                                        isVerticalVideo = containsVerticalScreenVideo
                                    )
                                }
                            )
                        }
                    }
                    if (videoDetailViewModel.relatedVideos.isNotEmpty()) {
                        item {
                            VideosRow(
                                header = stringResource(R.string.video_info_related_video_title),
                                videos = videoDetailViewModel.relatedVideos,
                                showMore = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ArgueTip(
    modifier: Modifier = Modifier,
    text: String
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 50.dp),
        colors = NonInteractiveSurfaceDefaults.colors(
            containerColor = Color.Yellow.copy(alpha = 0.2f),
            contentColor = Color.Yellow
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = Color.Yellow
            )
            Text(text = text)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoInfoData(
    modifier: Modifier = Modifier,
    videoDetail: VideoDetail,
    showFollowButton: Boolean,
    isFollowing: Boolean,
    tags: List<Tag>,
    isFavorite: Boolean,
    userFavoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    favoriteFolderIds: List<Long> = emptyList(),
    onClickCover: () -> Unit,
    onClickUp: () -> Unit,
    onAddFollow: () -> Unit,
    onDelFollow: () -> Unit,
    onClickTip: (Tag) -> Unit,
    onAddToDefaultFavoriteFolder: () -> Unit,
    onUpdateFavoriteFolders: (List<Long>) -> Unit
) {
    val scope = rememberCoroutineScope()

    val localDensity = LocalDensity.current
    val focusRequester = remember { FocusRequester() }

    var heightIs by remember { mutableStateOf(0.dp) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus(scope)
    }

    Row(
        modifier = modifier
            .padding(horizontal = 50.dp, vertical = 16.dp),
    ) {
        AsyncImage(
            modifier = Modifier
                .focusRequester(focusRequester)
                .weight(3f)
                .aspectRatio(1.6f)
                .clip(MaterialTheme.shapes.large)
                .onGloballyPositioned { coordinates ->
                    heightIs = with(localDensity) { coordinates.size.height.toDp() }
                }
                .focusedBorder(MaterialTheme.shapes.large)
                .clickable { onClickCover() },
            model = if (videoDetail.ugcSeason != null) videoDetail.ugcSeason!!.cover else videoDetail.cover,
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .weight(7f)
                .height(heightIs),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = videoDetail.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UpButton(
                        name = videoDetail.author.name,
                        followed = isFollowing,
                        showFollowButton = showFollowButton,
                        onClickUp = onClickUp,
                        onAddFollow = onAddFollow,
                        onDelFollow = onDelFollow
                    )
                }

                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        R.string.video_info_time, videoDetail.publishDate.formatPubTimeString()
                    ),
                    maxLines = 1,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Box(modifier = Modifier.focusable(true)) {}
                    FavoriteButton(
                        isFavorite = isFavorite,
                        userFavoriteFolders = userFavoriteFolders,
                        favoriteFolderIds = favoriteFolderIds,
                        onAddToDefaultFavoriteFolder = onAddToDefaultFavoriteFolder,
                        onUpdateFavoriteFolders = onUpdateFavoriteFolders
                    )
                    Box(modifier = Modifier.focusable(true)) {}
                }
                Text(
                    text = "点赞：${videoDetail.stat.like}",
                    color = Color.White
                )
                Text(
                    text = "投币：${videoDetail.stat.coin}",
                    color = Color.White
                )
                Text(
                    text = "收藏：${videoDetail.stat.favorite}",
                    color = Color.White
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.video_info_tags),
                    color = Color.White
                )
                TvLazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tags) { tag ->
                        Surface(
                            modifier = Modifier,
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.2f),
                                pressedContainerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(width = 3.dp, color = Color.White),
                                    shape = MaterialTheme.shapes.small
                                )
                            ),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                            onClick = { onClickTip(tag) }
                        ) {
                            Text(
                                modifier = Modifier.padding(8.dp, 4.dp),
                                text = tag.name,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpButton(
    modifier: Modifier = Modifier,
    name: String,
    followed: Boolean,
    showFollowButton: Boolean = false,
    onClickUp: () -> Unit,
    onAddFollow: () -> Unit,
    onDelFollow: () -> Unit
) {
    val view = LocalView.current
    val isLogin by remember { mutableStateOf(if (!view.isInEditMode) Prefs.isLogin else true) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(Color.White.copy(alpha = 0.2f))
                .focusedBorder(MaterialTheme.shapes.small)
                .padding(4.dp)
                .clickable { onClickUp() }
        ) {
            UpIcon(color = Color.White)
            Text(text = name, color = Color.White)
        }
        AnimatedVisibility(visible = isLogin && showFollowButton) {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.White.copy(alpha = 0.2f))
                    .focusedBorder(MaterialTheme.shapes.small)
                    .padding(horizontal = 4.dp, vertical = 3.dp)
                    .clickable { if (followed) onDelFollow() else onAddFollow() }
                    .animateContentSize()
            ) {
                if (followed) {
                    Icon(
                        imageVector = Icons.Rounded.Done,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(text = stringResource(R.string.video_info_followed), color = Color.White)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(text = stringResource(R.string.video_info_follow), color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoDescription(
    modifier: Modifier = Modifier,
    description: String
) {
    var hasFocus by remember { mutableStateOf(false) }
    val titleColor = if (hasFocus) Color.White else Color.White.copy(alpha = 0.6f)
    val titleFontSize by animateFloatAsState(
        targetValue = if (hasFocus) 30f else 14f,
        label = "title font size"
    )
    var showDescriptionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(horizontal = 50.dp),
    ) {
        Text(
            text = stringResource(R.string.video_info_description_title),
            fontSize = titleFontSize.sp,
            color = titleColor
        )
        Box(
            modifier = Modifier
                .padding(top = 15.dp)
                .onFocusChanged { hasFocus = it.hasFocus }
                .clip(MaterialTheme.shapes.medium)
                .focusedBorder(MaterialTheme.shapes.medium)
                .padding(8.dp)
                .clickable {
                    showDescriptionDialog = true
                }
        ) {
            Text(
                text = description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
        }
    }

    VideoDescriptionDialog(
        show = showDescriptionDialog,
        onHideDialog = { showDescriptionDialog = false },
        description = description
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoDescriptionDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    description: String
) {
    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = {
                Text(
                    text = stringResource(R.string.video_info_description_title),
                    color = Color.White
                )
            },
            text = {
                TvLazyColumn {
                    item {
                        Text(text = description)
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPartButton(
    modifier: Modifier = Modifier,
    index: Int,
    title: String,
    duration: Int,
    played: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            pressedContainerColor = MaterialTheme.colorScheme.primary
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 3.dp, color = Color.White),
                shape = MaterialTheme.shapes.large
            )
        ),
        onClick = { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(200.dp, 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.2f))
                    .fillMaxHeight()
                    .fillMaxWidth(if (played < 0) 1f else (played / duration.toFloat()))
            ) {}
            Text(
                modifier = Modifier
                    .padding(8.dp),
                text = "P$index $title",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoPartRowButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(64.dp),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            pressedContainerColor = MaterialTheme.colorScheme.primary
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 3.dp, color = Color.White),
                shape = MaterialTheme.shapes.medium
            )
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .size(48.dp)
                    .rotate(90f),
                imageVector = Icons.Rounded.ViewModule,
                contentDescription = null
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPartRow(
    modifier: Modifier = Modifier,
    pages: List<VideoPage>,
    lastPlayedCid: Int = 0,
    lastPlayedTime: Int = 0,
    enablePartListDialog: Boolean = false,
    onClick: (cid: Int) -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    var showPartListDialog by remember { mutableStateOf(false) }
    val titleColor = if (hasFocus) Color.White else Color.White.copy(alpha = 0.6f)
    val titleFontSize by animateFloatAsState(
        targetValue = if (hasFocus) 30f else 14f,
        label = "title font size"
    )

    Column(
        modifier = modifier
            .padding(start = 50.dp)
            .onFocusChanged { hasFocus = it.hasFocus },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.video_info_part_row_title),
            fontSize = titleFontSize.sp,
            color = titleColor
        )

        TvLazyRow(
            modifier = Modifier.padding(top = 15.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (enablePartListDialog) {
                item {
                    VideoPartRowButton(
                        onClick = { showPartListDialog = true }
                    )
                }
            }
            itemsIndexed(items = pages, key = { _, page -> page.cid }) { index, page ->
                VideoPartButton(
                    index = index + 1,
                    title = page.title,
                    played = if (page.cid == lastPlayedCid) lastPlayedTime else 0,
                    duration = page.duration,
                    onClick = { onClick(page.cid) }
                )
            }
        }
    }

    VideoPartListDialog(
        show = showPartListDialog,
        onHideDialog = { showPartListDialog = false },
        pages = pages,
        title = "分 P 列表",
        onClick = onClick
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoUgcSeasonRow(
    modifier: Modifier = Modifier,
    episodes: List<Episode>,
    lastPlayedCid: Int = 0,
    lastPlayedTime: Int = 0,
    enableUgcListDialog: Boolean = false,
    onClick: (avid: Int, cid: Int) -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    var showUgcListDialog by remember { mutableStateOf(false) }
    val titleColor = if (hasFocus) Color.White else Color.White.copy(alpha = 0.6f)
    val titleFontSize by animateFloatAsState(
        targetValue = if (hasFocus) 30f else 14f,
        label = "title font size"
    )

    Column(
        modifier = modifier
            .padding(start = 50.dp)
            .onFocusChanged { hasFocus = it.hasFocus },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.video_info_ugc_season_row_title),
            fontSize = titleFontSize.sp,
            color = titleColor
        )

        TvLazyRow(
            modifier = Modifier.padding(top = 15.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (enableUgcListDialog) {
                item {
                    VideoPartRowButton(
                        onClick = { showUgcListDialog = true }
                    )
                }
            }
            itemsIndexed(items = episodes) { index, episode ->
                VideoPartButton(
                    index = index + 1,
                    title = episode.title,
                    played = if (episode.cid == lastPlayedCid) lastPlayedTime else 0,
                    duration = episode.duration,
                    onClick = { onClick(episode.aid, episode.cid) }
                )
            }
        }
    }

    VideoUgcListDialog(
        show = showUgcListDialog,
        onHideDialog = { showUgcListDialog = false },
        episodes = episodes,
        title = "合集列表",
        onClick = onClick
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoPartListDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    pages: List<VideoPage>,
    onHideDialog: () -> Unit,
    onClick: (cid: Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabCount by remember { mutableIntStateOf(ceil(pages.size / 20.0).toInt()) }
    val selectedVideoPart = remember { mutableStateListOf<VideoPage>() }

    val tabRowFocusRequester = remember { FocusRequester() }
    val videoListFocusRequester = remember { FocusRequester() }
    val listState = rememberTvLazyGridState()

    LaunchedEffect(selectedTabIndex) {
        val fromIndex = selectedTabIndex * 20
        var toIndex = (selectedTabIndex + 1) * 20
        if (toIndex >= pages.size) {
            toIndex = pages.size
        }
        selectedVideoPart.swapList(pages.subList(fromIndex, toIndex))
    }

    LaunchedEffect(show) {
        if (show && tabCount > 1) tabRowFocusRequester.requestFocus(scope)
        if (show && tabCount == 1) videoListFocusRequester.requestFocus(scope)
    }

    if (show) {
        AlertDialog(
            modifier = modifier,
            title = { Text(text = title) },
            onDismissRequest = { onHideDialog() },
            confirmButton = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Column(
                    modifier = Modifier.size(600.dp, 330.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabRow(
                        modifier = Modifier
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    scope.launch(Dispatchers.Main) {
                                        listState.scrollToItem(0)
                                    }
                                }
                            },
                        selectedTabIndex = selectedTabIndex,
                        separator = { Spacer(modifier = Modifier.width(12.dp)) },
                    ) {
                        for (i in 0 until tabCount) {
                            Tab(
                                modifier = if (i == 0) Modifier.focusRequester(
                                    tabRowFocusRequester
                                ) else Modifier,
                                selected = i == selectedTabIndex,
                                onFocus = { selectedTabIndex = i },
                            ) {
                                Text(
                                    text = "P${i * 20 + 1}-${(i + 1) * 20}",
                                    fontSize = 12.sp,
                                    color = LocalContentColor.current,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                                )
                            }
                        }
                    }

                    TvLazyVerticalGrid(
                        state = listState,
                        columns = TvGridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = selectedVideoPart,
                            key = { _, video -> video.cid }
                        ) { index, page ->
                            val buttonModifier =
                                if (index == 0) Modifier.focusRequester(videoListFocusRequester) else Modifier

                            VideoPartButton(
                                modifier = buttonModifier,
                                index = index + 1,
                                title = page.title,
                                played = 0,
                                duration = page.duration,
                                onClick = { onClick(page.cid) }
                            )
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoUgcListDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    episodes: List<Episode>,
    onHideDialog: () -> Unit,
    onClick: (avid: Int, cid: Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabCount by remember { mutableIntStateOf(ceil(episodes.size / 20.0).toInt()) }
    val selectedVideoPart = remember { mutableStateListOf<Episode>() }

    val tabRowFocusRequester = remember { FocusRequester() }
    val videoListFocusRequester = remember { FocusRequester() }
    val listState = rememberTvLazyGridState()

    LaunchedEffect(selectedTabIndex) {
        val fromIndex = selectedTabIndex * 20
        var toIndex = (selectedTabIndex + 1) * 20
        if (toIndex >= episodes.size) {
            toIndex = episodes.size
        }
        selectedVideoPart.swapList(episodes.subList(fromIndex, toIndex))
    }

    LaunchedEffect(show) {
        if (show && tabCount > 1) tabRowFocusRequester.requestFocus(scope)
        if (show && tabCount == 1) videoListFocusRequester.requestFocus(scope)
    }

    if (show) {
        AlertDialog(
            modifier = modifier,
            title = { Text(text = title) },
            onDismissRequest = { onHideDialog() },
            confirmButton = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Column(
                    modifier = Modifier.size(600.dp, 330.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabRow(
                        modifier = Modifier
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    scope.launch(Dispatchers.Main) {
                                        listState.scrollToItem(0)
                                    }
                                }
                            },
                        selectedTabIndex = selectedTabIndex,
                        separator = { Spacer(modifier = Modifier.width(12.dp)) },
                    ) {
                        for (i in 0 until tabCount) {
                            Tab(
                                modifier = if (i == 0) Modifier.focusRequester(
                                    tabRowFocusRequester
                                ) else Modifier,
                                selected = i == selectedTabIndex,
                                onFocus = { selectedTabIndex = i },
                            ) {
                                Text(
                                    text = "P${i * 20 + 1}-${(i + 1) * 20}",
                                    fontSize = 12.sp,
                                    color = LocalContentColor.current,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                                )
                            }
                        }
                    }

                    TvLazyVerticalGrid(
                        state = listState,
                        columns = TvGridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = selectedVideoPart,
                            key = { _, video -> video.cid }
                        ) { index, episode ->
                            val buttonModifier =
                                if (index == 0) Modifier.focusRequester(videoListFocusRequester) else Modifier

                            VideoPartButton(
                                modifier = buttonModifier,
                                index = index + 1,
                                title = episode.title,
                                played = 0,
                                duration = episode.duration,
                                onClick = { onClick(episode.aid, episode.cid) }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun VideoPartButtonShortTextPreview() {
    BVTheme {
        VideoPartButton(
            index = 2,
            title = "这是一段短文字",
            duration = 100,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun VideoPartButtonLongTextPreview() {
    BVTheme {
        VideoPartButton(
            index = 2,
            title = "这可能是我这辈子距离梅西最近的一次",
            played = 23333,
            duration = 100,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun VideoPartRowPreview() {
    val pages = remember { mutableStateListOf<VideoPage>() }
    for (i in 0..10) {
        pages.add(
            VideoPage(
                cid = 1000 + i,
                index = i,
                title = "这可能是我这辈子距离梅西最近的一次",
                duration = 10,
                dimension = Dimension(0, 0)
            )
        )
    }
    BVTheme {
        VideoPartRow(pages = pages, onClick = {})
    }
}

@Preview
@Composable
fun VideoDescriptionPreview() {
    BVTheme {
        VideoDescription(description = "12435678")
    }
}

@Preview
@Composable
private fun UpButtonPreview() {
    var followed by remember { mutableStateOf(false) }
    BVTheme {
        UpButton(
            name = "12435678",
            followed = followed,
            onClickUp = { followed = !followed },
            onAddFollow = {},
            onDelFollow = {}
        )
    }
}