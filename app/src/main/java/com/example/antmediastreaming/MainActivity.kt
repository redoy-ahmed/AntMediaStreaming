package com.example.antmediastreaming

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                ActivityGrid(activities = createActivities())
            }
        }
    }

    private fun createActivities(): List<ActivityLink> {
        return listOf(
            ActivityLink(Intent(this, PublishActivity::class.java), "Publish"),
            ActivityLink(Intent(this, PlayActivity::class.java), "Play"),
        )
    }

    data class ActivityLink(val intent: Intent, val label: String)

    @Composable
    fun ActivityGrid(activities: List<ActivityLink>) {
        val context = LocalContext.current
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(activities) { activity ->
                ActivityButton(label = activity.label) {
                    context.startActivity(activity.intent)
                }
            }
        }
    }

    @Composable
    fun ActivityButton(label: String, onClick: () -> Unit) {
        Text(
            text = label,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onClick() }
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    @Composable
    fun AppTheme(content: @Composable () -> Unit) {
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                content()
            }
        }
    }
}
