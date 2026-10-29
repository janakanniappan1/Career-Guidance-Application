package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.SkillChip
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.PracticeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortfolioScreen(
    practiceViewModel: PracticeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by practiceViewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newTech by remember { mutableStateOf("") }
    var newGithub by remember { mutableStateOf("") }
    val tokens = LocalCGATokens.current

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Project Portfolio",
                onBackClick = onNavigateBack,
                testTag = "portfolio_top_bar"
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .testTag("portfolio_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // GitHub Status Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.surface)
                        .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CGAColors.DarkHeading),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "GitHub",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GitHub Connected",
                                    color = tokens.textHeading,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "github.com/student",
                                    color = tokens.textMuted,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        TagPill(
                            text = "VERIFIED",
                            backgroundColor = tokens.successContainer,
                            textColor = tokens.onSuccessContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showcase Projects (${uiState.projects.size})",
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAddDialog = true }
                            .testTag("add_project_pill_btn")
                    ) {
                        TagPill(
                            text = "+ ADD NEW",
                            backgroundColor = tokens.infoContainer,
                            textColor = CGAColors.ActionBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (uiState.projects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.surface)
                            .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                            .padding(28.dp)
                            .testTag("empty_projects_card"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No projects yet",
                                color = tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add your capstone, hackathon, or personal GitHub projects to showcase in your portfolio.",
                                color = tokens.textBody,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            } else {
                items(uiState.projects, key = { it.id ?: it.title }) { project ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.surface)
                            .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                            .testTag("project_item_${project.id}")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = project.title,
                                    color = tokens.textHeading,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = CGAColors.Star,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${project.stars}",
                                        color = tokens.textHeading,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = project.description,
                                color = tokens.textBody,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                project.techStack.forEach { tech ->
                                    SkillChip(skill = tech, accentColor = CGAColors.Primary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                CgaPrimaryButton(
                    text = "ADD PORTFOLIO PROJECT",
                    onClick = { showAddDialog = true },
                    leadingIcon = Icons.Default.Add,
                    testTag = "open_add_project_dialog_btn"
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Add Placement Project",
                    fontWeight = FontWeight.ExtraBold,
                    color = tokens.textHeading
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Project Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CGAColors.ActionBlue,
                            focusedLabelColor = CGAColors.ActionBlue,
                            unfocusedBorderColor = tokens.borderMain
                        )
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Impact & Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CGAColors.ActionBlue,
                            focusedLabelColor = CGAColors.ActionBlue,
                            unfocusedBorderColor = tokens.borderMain
                        )
                    )
                    OutlinedTextField(
                        value = newTech,
                        onValueChange = { newTech = it },
                        label = { Text("Technologies (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CGAColors.ActionBlue,
                            focusedLabelColor = CGAColors.ActionBlue,
                            unfocusedBorderColor = tokens.borderMain
                        )
                    )
                    OutlinedTextField(
                        value = newGithub,
                        onValueChange = { newGithub = it },
                        label = { Text("GitHub Repo URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CGAColors.ActionBlue,
                            focusedLabelColor = CGAColors.ActionBlue,
                            unfocusedBorderColor = tokens.borderMain
                        )
                    )
                }
            },
            confirmButton = {
                CgaPrimaryButton(
                    text = "ADD PROJECT",
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            val techs = newTech.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            practiceViewModel.addProject(newTitle, newDesc, techs, newGithub)
                            showAddDialog = false
                            newTitle = ""
                            newDesc = ""
                            newTech = ""
                            newGithub = ""
                        }
                    }
                )
            },
            dismissButton = {
                CgaSecondaryButton(
                    text = "CANCEL",
                    onClick = { showAddDialog = false }
                )
            }
        )
    }
}
