package uk.co.gusward.example.multiplatform.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.gusward.blueprint.compose.preview.preview.BlueprintPreview

@Composable
fun ExampleComplexDashboardComponent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Header Area (Nested Rows and Alignment)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFD1E4FF), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "A",
                        fontSize = 30.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Good Morning,",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Alex Johnson",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
            )
        }

        // 2. Featured Card (Weights and FillMaxWidth)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFF2D3142), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Total Balance",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$12,450.00",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Grid of Stats (Nested Rows/Columns with Weights)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Income",
                value = "$4,200",
                color = Color(0xFFB9FBC0)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Expenses",
                value = "$1,850",
                color = Color(0xFFFFCFD2)
            )
        }

        // 4. Recent Activity (Deep Nesting)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Recent Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            repeat(2) {
                ActivityItem()
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, color: Color) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActivityItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFE8EAF6), CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Grocery Store", fontWeight = FontWeight.SemiBold)
            Text(text = "Today, 10:45 AM", fontSize = 12.sp, color = Color.Gray)
        }
        Text(text = "-$45.00", fontWeight = FontWeight.Bold, color = Color.Red)
    }
}

@Composable
fun ExampleComplexDashboardComponentNormalPreview() {
    ExampleComplexDashboardComponent()
}

@Composable
fun ExampleComplexDashboardComponentPreview() {
    BlueprintPreview {
        ExampleComplexDashboardComponent()
    }
}
