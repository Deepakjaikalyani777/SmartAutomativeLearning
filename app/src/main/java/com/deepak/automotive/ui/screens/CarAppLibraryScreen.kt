package com.deepak.automotive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.CodeBlock
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors

private const val MAX_DEPTH = 5

/** A pretend host that renders our "templates" so you can see the screen stack and limits. */
@Composable
fun CarAppLibraryScreen() {
    val stack = remember { mutableStateListOf("Nearby chargers") }
    val top by remember { androidx.compose.runtime.derivedStateOf { stack.last() } }

    Panel("Host screen (what the car renders) · stack depth ${stack.size}/$MAX_DEPTH") {
        AnimatedContent(stack.size to top, transitionSpec = {
            if (targetState.first > initialState.first) slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            else slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
        }, label = "screen") { (_, title) ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CarColors.SurfaceHigh).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (stack.size > 1) Text("←", color = CarColors.Cyan, style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.clickable { stack.removeAt(stack.lastIndex) })
                    Text(title, style = MaterialTheme.typography.titleLarge, color = CarColors.TextPrimary)
                }
                (1..6).forEach { i ->  // ListTemplate: hosts show at most ~6 rows while driving
                    Text("Row $i · tap to push detail", color = CarColors.TextPrimary,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CarColors.Surface)
                            .clickable(enabled = stack.size < MAX_DEPTH) { stack.add("$title › $i") }.padding(12.dp))
                }
                if (stack.size >= MAX_DEPTH) Text("Task depth limit reached: host refuses further push()", color = CarColors.Red,
                    fontWeight = FontWeight.Bold)
            }
        }
        CarButton("Reset", accent = CarColors.Pink) { stack.clear(); stack.add("Nearby chargers") }
    }
    Panel("Lifecycle") {
        CodeBlock(
            """
            Host (Android Auto / AAOS)
              └─ binds ▶ CarAppService            (manifest: category CHARGING)
                   ├─ createHostValidator()        ← which hosts may bind (security!)
                   └─ onCreateSession() ▶ Session  (lifecycle = car screen on/off)
                        └─ onCreateScreen() ▶ Screen (stack managed by ScreenManager)
                             └─ onGetTemplate() ▶ ListTemplate / PaneTemplate / NavigationTemplate
            invalidate() → host calls onGetTemplate() again (refresh is quota-limited)
            """
        )
    }
}
