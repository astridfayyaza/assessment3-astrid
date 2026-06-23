package com.astrid0049.myskin.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.astrid0049.myskin.model.User

@Composable
fun ProfileMenu(
    user: User?,
    isLoggedIn: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(end = 16.dp)) {
        if (isLoggedIn && user != null) {
            AsyncImage(
                model = user.photoUrl.ifEmpty { Icons.Default.AccountCircle },
                contentDescription = "Profile Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { expanded = true }
            )
        } else {
            IconButton(onClick = onLogin) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Sign In Action",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            user?.let {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text = it.name, style = MaterialTheme.typography.bodyLarge)
                            Text(text = it.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    },
                    onClick = {}
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Log Out") },
                    onClick = {
                        expanded = false
                        onLogout()
                    }
                )
            }
        }
    }
}