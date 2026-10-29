package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CareerCard
import com.example.ui.components.CgaBottomNavigation
import com.example.ui.components.CgaTopBar
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.CareerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    careerViewModel: CareerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToRoadmap: (String) -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCareerDetails: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Tech", "Data", "Core", "Business")
    val careersList by careerViewModel.allCareers.collectAsState()
    val tokens = LocalCGATokens.current

    val filteredCareers = remember(searchQuery, selectedCategory, careersList) {
        careersList.filter { career ->
            val matchesQuery = career.title.contains(searchQuery, ignoreCase = true) ||
                    career.recommendedSkills.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesCategory = when (selectedCategory) {
                "Tech" -> career.careerId in listOf("aiml_engineer", "software_dev", "mobile_dev", "fullstack_dev")
                "Data" -> career.careerId in listOf("data_scientist", "data_analyst", "aiml_engineer")
                "Core" -> career.careerId in listOf("cloud_engineer", "devops_engineer", "cybersecurity_analyst")
                "Business" -> career.careerId in listOf("uiux_designer", "product_manager")
                else -> true
            }
            matchesQuery && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Explore Careers",
                onBackClick = onNavigateBack,
                testTag = "explore_top_bar"
            )
        },
        bottomBar = {
            CgaBottomNavigation(
                currentRoute = "explore",
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> onNavigateToHome()
                        "roadmap" -> onNavigateToRoadmap("")
                        "practice" -> onNavigateToPractice()
                        "explore" -> { /* Current */ }
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .testTag("explore_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar with Icon & 16dp rounded corners
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search careers, skills, or tech roles...",
                            fontSize = 13.sp,
                            color = tokens.textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (searchQuery.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tokens.surface,
                        unfocusedContainerColor = tokens.surface,
                        focusedBorderColor = CGAColors.ActionBlue,
                        focusedLabelColor = CGAColors.ActionBlue,
                        unfocusedBorderColor = tokens.borderMain
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_bar")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Pills (All, Tech, Data, Core, Business)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = category == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) tokens.infoContainer else tokens.surface)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CGAColors.ActionBlue else tokens.borderMain,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("filter_chip_$category")
                        ) {
                            Text(
                                text = category.uppercase(),
                                color = if (isSelected) CGAColors.ActionBlue else tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Career Profiles (${filteredCareers.size})",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            items(filteredCareers, key = { it.careerId }) { career ->
                val icon = when (career.careerId) {
                    "aiml_engineer" -> Icons.Default.AutoAwesome
                    "data_scientist", "data_analyst" -> Icons.Default.DataUsage
                    "software_dev", "fullstack_dev" -> Icons.Default.Code
                    "cloud_engineer", "devops_engineer" -> Icons.Default.Cloud
                    "cybersecurity_analyst" -> Icons.Default.Security
                    else -> Icons.Default.Work
                }

                CareerCard(
                    title = career.title,
                    description = career.reasonForRecommendation,
                    matchPercentage = career.matchPercentage,
                    salaryRange = "₹7 - ₹22 LPA",
                    icon = icon,
                    onViewRoadmap = { onNavigateToRoadmap(career.careerId) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
