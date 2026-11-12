package yos.music.player.ui.widgets.basic

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.navigationBarsHeight
import yos.music.player.ui.theme.primaryDark
import yos.music.player.ui.theme.withNight

@Stable
data class NavItem(val label: String, val iconResId: Int)

@Composable
fun BottomNavigator(
    nowLabel: () -> String,
    onLabelChange: (String) -> Unit,
    items: List<NavItem>,
    modifier: Modifier
) =
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsHeight(66.dp)
            .background(Color(0xF2FFFFFF) withNight Color(0xF20C0D14))
    ) {
        // Subtle top hairline highlight
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.6.dp)
                .background(Color.Black.copy(alpha = 0.05f) withNight Color.White.copy(alpha = 0.08f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { item ->
                NavigatorItem(item, nowLabel, onLabelChange)
            }
        }
    }

@Composable
fun RowScope.NavigatorItem(
    item: NavItem,
    nowLabel: () -> String,
    onLabelChange: (String) -> Unit
) =
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onLabelChange(item.label)
            }
    ) {
        val isSelected = remember(item) {
            derivedStateOf {
                nowLabel() == item.label
            }
        }
        val iconColor = animateColorAsState(
            if (isSelected.value) {
                Color.Black withNight Color.White
            } else {
                Color.Gray withNight Color(0xFF70717E)
            }
        )

        Icon(
            painterResource(item.iconResId),
            contentDescription = null,
            tint = iconColor.value,
            modifier = Modifier
                .size(22.dp)
                .padding(bottom = 2.dp)
        )
        Text(
            item.label,
            color = iconColor.value,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
            fontWeight = if (isSelected.value) FontWeight.SemiBold else FontWeight.Medium
        )
        if (isSelected.value) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(width = 12.dp, height = 2.dp)
                    .background(primaryDark, CircleShape)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
