package yos.music.player.ui.widgets.basic

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.PersonCropCircle
import yos.music.player.R
import yos.music.player.code.utils.others.Vibrator
import yos.music.player.data.NormalSaver

private val profilePictureUri = mutableStateOf(NormalSaver.readData("profile_picture_uri", ""))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileButton(onClick: () -> Unit) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            profilePictureUri.value = uri.toString()
            NormalSaver.saveData("profile_picture_uri", profilePictureUri.value)
        } catch (_: SecurityException) {
            // The provider did not offer durable access, so don't save a URI that will later break.
        }
    }
    val modifier = Modifier
        .size(if (profilePictureUri.value.isEmpty()) 30.dp else 32.dp)
        .clip(CircleShape)
        .combinedClickable(
            onClick = onClick,
            onLongClick = {
                Vibrator.longClick(context)
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )

    if (profilePictureUri.value.isEmpty()) {
        Box(
            modifier = modifier
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                .border(0.6.dp, Color.White.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CupertinoIcons.Default.PersonCropCircle,
                contentDescription = stringResource(R.string.profile_picture),
                modifier = Modifier.size(20.dp),
                tint = Color.White.copy(alpha = 0.85f)
            )
        }
    } else {
        AsyncImage(
            model = profilePictureUri.value,
            contentDescription = stringResource(R.string.profile_picture),
            contentScale = ContentScale.Crop,
            modifier = modifier.border(0.6.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )
    }
}
