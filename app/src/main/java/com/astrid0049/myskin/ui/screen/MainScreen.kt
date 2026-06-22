package com.astrid0049.myskin.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.astrid0049.myskin.model.Skincare
import com.astrid0049.myskin.network.ApiStatus
import com.astrid0049.myskin.network.SkincareApi
import com.astrid0049.myskin.ui.theme.MySkinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "MySkin")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        ScreenContent(Modifier.padding(innerPadding))
    }
}

@Composable
fun ScreenContent(modifier: Modifier = Modifier) {
    val viewModel: MainViewModel = viewModel()
    val data by viewModel.data
    val apiStatus by viewModel.status.collectAsState()

    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        when (apiStatus) {
            ApiStatus.LOADING -> {
                androidx.compose.material3.CircularProgressIndicator()
            }
            ApiStatus.FAILED -> {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(text = "Failed to synchronize routing pipeline.", color = Color.Red)
                    androidx.compose.material3.Button(
                        onClick = { viewModel.retrieveData("") },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
            ApiStatus.SUCCESS -> {
                if (data.isEmpty()) {
                    Text(text = "No skincare data available.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        columns = GridCells.Fixed(2),
                    ) {
                        items(data) { ListItem(skincare = it) }
                    }
                }
            }
        }
    }
}

@Composable
fun ListItem(skincare: Skincare) {
    Box(
        modifier = Modifier.padding(4.dp).border(1.dp, Color.Gray),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(SkincareApi.getSkincareUrl(skincare.imageId))
                .crossfade(enable = true)
                .build(),
            contentDescription = skincare.nama,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(4.dp)
                .background(Color(red = 0f, green = 0f, blue = 0f, alpha = 0.5f))
                .padding(4.dp)
        ) {
            Text(
                text = skincare.nama,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = skincare.brand,
                fontSize = 14.sp,
                color = Color.White
            )
            if (skincare.mine == 1) {
                Text(
                    text = "Mine",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun MainScreenPreview() {
    MySkinTheme {
        MainScreen()
    }
}