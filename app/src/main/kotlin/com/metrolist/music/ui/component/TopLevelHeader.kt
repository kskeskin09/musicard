/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.constants.TopLevelHeaderHeight

/**
 * Header of the main tabs: the page name in large type with a round profile picture on the
 * right, and a search pill under it. It sits below the status bar and has the page background.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopLevelHeader(
    title: String,
    profileImageUrl: String?,
    showBadge: Boolean,
    onProfileClick: () -> Unit,
    onProfileLongClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    profileCrop: ProfileCrop = ProfileCrop(),
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TopLevelHeaderHeight)
                .padding(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(56.dp),
        ) {
            Text(
                text = title,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
            Spacer(Modifier.width(4.dp))
            BadgedBox(badge = { if (showBadge) Badge() }) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .combinedClickable(onClick = onProfileClick, onLongClick = onProfileLongClick),
                ) {
                    if (profileImageUrl != null) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = stringResource(R.string.account),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp).profileCrop(profileCrop),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = stringResource(R.string.account),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 18.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.search_hint_everything),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
