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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.astrid0049.myskin.database.SkincareDao
import com.astrid0049.myskin.model.Skincare
import com.astrid0049.myskin.network.ApiStatus
import com.astrid0049.myskin.network.SkincareApi
import com.astrid0049.myskin.ui.theme.MySkinTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    dao: SkincareDao,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Skincare")
            }
        }
    ) { innerPadding ->
        ScreenContent(
            viewModel = viewModel,
            dao = dao,
            modifier = Modifier.padding(innerPadding)
        )

        if (showDialog) {
            AddSkincareDialog(
                onDismiss = { showDialog = false },
                onConfirm = { nama, brand, bitmap ->
                    viewModel.postNewData(nama, brand, bitmap, dao)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun AddSkincareDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Bitmap) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            if (uri != null) {
                bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Skincare") },
        text = {
            Column {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Skincare Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val options = CropImageContractOptions(
                            uri = null,
                            cropImageOptions = CropImageOptions(
                                guidelines = CropImageView.Guidelines.ON,
                                fixAspectRatio = true,
                                aspectRatioX = 1,
                                aspectRatioY = 1
                            )
                        )
                        cropLauncher.launch(options)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (bitmap == null) "Pick & Crop Photo" else "Change Photo")
                }
                
                bitmap?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = it,
                        contentDescription = "Selected Image",
                        modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotBlank() && brand.isNotBlank() && bitmap != null) {
                        onConfirm(nama, brand, bitmap!!)
                    }
                },
                enabled = nama.isNotBlank() && brand.isNotBlank() && bitmap != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ScreenContent(
    viewModel: MainViewModel,
    dao: SkincareDao,
    modifier: Modifier = Modifier
) {
    val data by viewModel.data
    val apiStatus by viewModel.status.collectAsState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (apiStatus) {
            ApiStatus.LOADING -> {
                androidx.compose.material3.CircularProgressIndicator()
            }
            ApiStatus.FAILED -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Failed to synchronize routing pipeline.", color = Color.Red)
                    androidx.compose.material3.Button(
                        onClick = { viewModel.retrieveData("", dao) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListItem(skincare: Skincare) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .border(1.dp, Color.Gray),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(SkincareApi.getSkincareUrl(skincare.imageId))
                .crossfade(true)
                .build(),
            contentDescription = skincare.nama,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
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
        val mockItems = listOf(
            Skincare(id = "1", nama = "Hydrating Cleanser", brand = "CeraVe", imageId = "sample1", mine = 1),
            Skincare(id = "2", nama = "Retinol Serum", brand = "The Ordinary", imageId = "sample2", mine = 0)
        )

        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text(text = "MySkin") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }
        ) { innerPadding ->
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(4.dp),
                columns = GridCells.Fixed(2),
            ) {
                items(mockItems) { ListItem(skincare = it) }
            }
        }
    }
}