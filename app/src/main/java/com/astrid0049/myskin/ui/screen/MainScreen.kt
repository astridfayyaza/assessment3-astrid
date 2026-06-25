package com.astrid0049.myskin.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.astrid0049.myskin.database.SkincareDao
import com.astrid0049.myskin.model.Skincare
import com.astrid0049.myskin.network.ApiStatus
import com.astrid0049.myskin.network.SkincareApi
import com.astrid0049.myskin.ui.theme.MySkinTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.astrid0049.myskin.ui.component.ProfileMenu
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
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSkincare by remember { mutableStateOf<Skincare?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val errorMessage by viewModel.errorMessage

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "MySkin")
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData(dao) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    ProfileMenu(
                        user = viewModel.currentUser.value,
                        isLoggedIn = viewModel.isLoggedIn.value,
                        onLogin = { viewModel.loginGoogle(context) },
                        onLogout = { viewModel.logout() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            if (viewModel.isLoggedIn.value) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Skincare")
                }
            }
        }
    ) { innerPadding ->
        ScreenContent(
            viewModel = viewModel,
            dao = dao,
            onDelete = { id -> viewModel.executeDelete(id, dao) },
            onEdit = { skincare -> editingSkincare = skincare },
            modifier = Modifier.padding(innerPadding)
        )

        if (showAddDialog) {
            AddSkincareDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { nama, brand, bitmap ->
                    viewModel.postNewData(nama, brand, bitmap, dao)
                    showAddDialog = false
                }
            )
        }

        editingSkincare?.let { skincare ->
            EditSkincareDialog(
                skincare = skincare,
                onDismiss = { editingSkincare = null },
                onConfirm = { nama, brand, bitmap ->
                    viewModel.updateExistingData(skincare.id, nama, brand, bitmap, dao)
                    editingSkincare = null
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
                bitmap = try {
                    if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainScreen", "Error decoding bitmap: ${e.message}")
                    null
                }
            }
        } else {
            Log.e("MainScreen", "Crop Error: ${result.error?.message}")
        }
    }

    val startCrop = {
        val options = CropImageContractOptions(
            uri = null,
            cropImageOptions = CropImageOptions(
                guidelines = CropImageView.Guidelines.ON,
                fixAspectRatio = true,
                aspectRatioX = 1,
                aspectRatioY = 1,
                imageSourceIncludeCamera = true,
                imageSourceIncludeGallery = true
            )
        )
        cropLauncher.launch(options)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCrop()
        } else {
            Log.e("MainScreen", "Camera permission denied")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Skincare") },
        text = {
            Column {
                Button(
                    onClick = {
                        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        )
                        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            startCrop()
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (bitmap == null) "Pick & Crop Photo" else "Change Photo")
                }

                bitmap?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .crossfade(true)
                            .build(),
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        },
                        contentDescription = "Selected Image",
                        modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
fun EditSkincareDialog(
    skincare: Skincare,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Bitmap?) -> Unit
) {
    var nama by remember { mutableStateOf(skincare.nama.replace("\"", "")) }
    var brand by remember { mutableStateOf(skincare.brand.replace("\"", "")) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            if (uri != null) {
                bitmap = try {
                    if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainScreen", "Error decoding bitmap: ${e.message}")
                    null
                }
            }
        }
    }

    val startCrop = {
        val options = CropImageContractOptions(
            uri = null,
            cropImageOptions = CropImageOptions(
                guidelines = CropImageView.Guidelines.ON,
                fixAspectRatio = true,
                aspectRatioX = 1,
                aspectRatioY = 1,
                imageSourceIncludeCamera = true,
                imageSourceIncludeGallery = true
            )
        )
        cropLauncher.launch(options)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCrop()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Skincare") },
        text = {
            Column {
                Button(
                    onClick = {
                        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        )
                        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            startCrop()
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (bitmap == null) "Change Photo (Optional)" else "Photo Selected")
                }

                if (bitmap != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(bitmap)
                            .crossfade(true)
                            .build(),
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        },
                        contentDescription = "New Image",
                        modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(SkincareApi.getSkincareUrl(skincare.imageId))
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        },
                        contentDescription = "Current Image",
                        modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotBlank() && brand.isNotBlank()) {
                        onConfirm(nama, brand, bitmap)
                    }
                },
                enabled = nama.isNotBlank() && brand.isNotBlank()
            ) {
                Text("Update")
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
    onDelete: (String) -> Unit,
    onEdit: (Skincare) -> Unit,
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
                CircularProgressIndicator()
            }
            ApiStatus.FAILED -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to connect to the internet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "Please check your connection and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.refreshData(dao) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
            ApiStatus.SUCCESS -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (viewModel.isLoggedIn.value) {
                        viewModel.currentUser.value?.let { user ->
                            Text(
                                text = "Welcome, ${user.name}!",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Please sign in to manage your skincare collection.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    if (data.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "No skincare data available.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(data) { skincare ->
                                ListItem(
                                    skincare = skincare,
                                    onDelete = onDelete,
                                    onEdit = onEdit,
                                    isLoggedIn = viewModel.isLoggedIn.value
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListItem(
    skincare: Skincare,
    onDelete: (String) -> Unit,
    onEdit: (Skincare) -> Unit,
    isLoggedIn: Boolean
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && isLoggedIn) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Skincare") },
            text = { Text("Are you sure you want to delete ${skincare.nama}?") },
            confirmButton = {
                Button(onClick = {
                    onDelete(skincare.id)
                    showDeleteConfirm = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isLoggedIn) onEdit(skincare) },
                onLongClick = { if (isLoggedIn) showDeleteConfirm = true }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(SkincareApi.getSkincareUrl(skincare.imageId))
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error loading image",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(48.dp)
                    )
                },
                contentDescription = skincare.nama,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(red = 0f, green = 0f, blue = 0f, alpha = 0.6f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = skincare.nama.replace("\"", ""),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        fontSize = 14.sp
                    )
                    Text(
                        text = skincare.brand.replace("\"", ""),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
                if (isLoggedIn) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
                items(mockItems) { ListItem(skincare = it, onDelete = {}, onEdit = {}, isLoggedIn = true) }
            }
        }
    }
}