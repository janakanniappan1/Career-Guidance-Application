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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompleteProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val tokens = LocalCGATokens.current
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var email by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
    var department by remember(currentUser) { mutableStateOf(currentUser?.department ?: "") }
    var yearOfStudy by remember(currentUser) { mutableStateOf(currentUser?.yearOfStudy ?: "") }
    var careerInterest by remember(currentUser) { mutableStateOf(currentUser?.careerInterest ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var departmentError by remember { mutableStateOf<String?>(null) }
    var yearError by remember { mutableStateOf<String?>(null) }
    var interestError by remember { mutableStateOf<String?>(null) }

    val commonDepts = listOf(
        "Computer Science & Engineering",
        "Information Technology",
        "Electronics & Communication",
        "Data Science & AI",
        "Mechanical Engineering"
    )

    val commonYears = listOf(
        "1st Year",
        "2nd Year",
        "3rd Year",
        "4th Year"
    )

    val commonInterests = listOf(
        "AI / Machine Learning",
        "Full Stack Development",
        "Cloud & DevOps",
        "Cybersecurity",
        "Data Science"
    )

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Complete Your Profile",
                onBackClick = null,
                testTag = "complete_profile_top_bar"
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .testTag("complete_profile_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            TagPill(
                text = "ACADEMIC ONBOARDING",
                backgroundColor = CGAColors.LightGreenIconBg,
                textColor = CGAColors.Primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Welcome, ${name.ifBlank { "Student" }}!",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Please verify your information to unlock tailored career recommendations and placement roadmaps.",
                color = tokens.textMuted,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Full Name *
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = null
                },
                label = { Text("Full Name *", fontSize = 13.sp) },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = Color(0xFFDC2626), fontSize = 11.sp) } },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = if (name.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                },
                modifier = Modifier.fillMaxWidth().testTag("complete_profile_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = tokens.surface,
                    unfocusedContainerColor = tokens.surface,
                    focusedBorderColor = CGAColors.ActionBlue,
                    focusedLabelColor = CGAColors.ActionBlue,
                    unfocusedBorderColor = tokens.borderMain
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email (read-only verification indicator)
            if (email.isNotBlank()) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { },
                    enabled = false,
                    readOnly = true,
                    label = { Text("Registered Email Address", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = tokens.textMuted)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("complete_profile_email_display"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = tokens.surface.copy(alpha = 0.6f),
                        disabledBorderColor = tokens.borderMain,
                        disabledTextColor = tokens.textHeading
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Department *
            OutlinedTextField(
                value = department,
                onValueChange = {
                    department = it
                    if (it.isNotBlank()) departmentError = null
                },
                label = { Text("Department / Branch *", fontSize = 13.sp) },
                isError = departmentError != null,
                supportingText = departmentError?.let { { Text(it, color = Color(0xFFDC2626), fontSize = 11.sp) } },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = if (department.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                },
                modifier = Modifier.fillMaxWidth().testTag("complete_profile_department_input"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = tokens.surface,
                    unfocusedContainerColor = tokens.surface,
                    focusedBorderColor = CGAColors.ActionBlue,
                    focusedLabelColor = CGAColors.ActionBlue,
                    unfocusedBorderColor = tokens.borderMain
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Department chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                commonDepts.forEach { dept ->
                    val isSelected = department.equals(dept, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) tokens.infoContainer else tokens.surface)
                            .border(1.dp, if (isSelected) CGAColors.ActionBlue else tokens.borderMain, RoundedCornerShape(12.dp))
                            .clickable {
                                department = dept
                                departmentError = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = dept,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CGAColors.ActionBlue else tokens.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Year of Study *
            OutlinedTextField(
                value = yearOfStudy,
                onValueChange = {
                    yearOfStudy = it
                    if (it.isNotBlank()) yearError = null
                },
                label = { Text("Year of Study * (e.g. 1st Year, 2nd Year)", fontSize = 13.sp) },
                isError = yearError != null,
                supportingText = yearError?.let { { Text(it, color = Color(0xFFDC2626), fontSize = 11.sp) } },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.School, contentDescription = null, tint = if (yearOfStudy.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                },
                modifier = Modifier.fillMaxWidth().testTag("complete_profile_year_input"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = tokens.surface,
                    unfocusedContainerColor = tokens.surface,
                    focusedBorderColor = CGAColors.ActionBlue,
                    focusedLabelColor = CGAColors.ActionBlue,
                    unfocusedBorderColor = tokens.borderMain
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Year chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commonYears.forEach { yr ->
                    val isSelected = yearOfStudy.equals(yr, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) tokens.infoContainer else tokens.surface)
                            .border(1.dp, if (isSelected) CGAColors.ActionBlue else tokens.borderMain, RoundedCornerShape(12.dp))
                            .clickable {
                                yearOfStudy = yr
                                yearError = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = yr,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CGAColors.ActionBlue else tokens.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Career Interest *
            OutlinedTextField(
                value = careerInterest,
                onValueChange = {
                    careerInterest = it
                    if (it.isNotBlank()) interestError = null
                },
                label = { Text("Career Interest *", fontSize = 13.sp) },
                isError = interestError != null,
                supportingText = interestError?.let { { Text(it, color = Color(0xFFDC2626), fontSize = 11.sp) } },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = if (careerInterest.isNotEmpty()) CGAColors.ElectricPurple else tokens.textMuted)
                },
                modifier = Modifier.fillMaxWidth().testTag("complete_profile_interest_input"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = tokens.surface,
                    unfocusedContainerColor = tokens.surface,
                    focusedBorderColor = CGAColors.ElectricPurple,
                    focusedLabelColor = CGAColors.ElectricPurple,
                    unfocusedBorderColor = tokens.borderMain
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Career Interest chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                commonInterests.forEach { interest ->
                    val isSelected = careerInterest.equals(interest, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) tokens.purpleContainer else tokens.surface)
                            .border(1.dp, if (isSelected) CGAColors.ElectricPurple else tokens.borderMain, RoundedCornerShape(12.dp))
                            .clickable {
                                careerInterest = interest
                                interestError = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = interest,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CGAColors.ElectricPurple else tokens.textMuted
                        )
                    }
                }
            }

            if (!uiState.error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.error ?: "",
                    color = Color(0xFFDC2626),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            CgaPrimaryButton(
                text = if (uiState.isLoading) "SAVING DETAILS..." else "SAVE & CONTINUE TO DASHBOARD",
                enabled = !uiState.isLoading,
                onClick = {
                    var hasError = false
                    if (name.isBlank()) {
                        nameError = "Please enter your name."
                        hasError = true
                    }
                    if (department.isBlank()) {
                        departmentError = "Please select or enter your department."
                        hasError = true
                    }
                    if (yearOfStudy.isBlank()) {
                        yearError = "Please select or enter your year of study."
                        hasError = true
                    }
                    if (careerInterest.isBlank()) {
                        interestError = "Please specify your career interest."
                        hasError = true
                    }
                    if (!hasError) {
                        authViewModel.completeProfile(
                            name = name.trim(),
                            department = department.trim(),
                            year = yearOfStudy.trim(),
                            careerInterest = careerInterest.trim()
                        ) {
                            onNavigateToDashboard()
                        }
                    }
                },
                testTag = "complete_profile_submit_btn"
            )

            Spacer(modifier = Modifier.height(12.dp))

            CgaSecondaryButton(
                text = "USE RECOMMENDED DEFAULTS",
                onClick = {
                    authViewModel.completeProfile(
                        name = name.trim().ifBlank { "Student" },
                        department = "Computer Science & Engineering",
                        year = "1st Year",
                        careerInterest = "Full Stack Development"
                    ) {
                        onNavigateToDashboard()
                    }
                },
                testTag = "complete_profile_skip_btn"
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

