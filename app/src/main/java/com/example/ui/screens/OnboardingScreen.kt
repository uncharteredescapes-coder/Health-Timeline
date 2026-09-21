package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HealthViewModel
import com.example.ui.components.DesignOutlineButton
import com.example.ui.components.DesignPrimaryButton
import com.example.ui.components.DesignTextButton
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(onFinish: () -> Unit, viewModel: HealthViewModel = viewModel()) {
    var step by remember { mutableIntStateOf(0) }
    var language by remember { mutableStateOf("en") }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    val selectedConditions = remember { mutableStateListOf<String>() }

    val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .padding(horizontal = 18.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        if (step < 3) {
            // 3-segment progress bar: 4dp height, 4dp radius, 6dp spacing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { i ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = if (i <= step) Primary else Line
                    ) {}
                }
            }
        }

        when (step) {
            0 -> LanguageStep(
                selectedLang = language,
                onSelect = { language = it },
                onContinue = { step = 1 },
                t = t
            )
            1 -> BasicInfoStep(
                name = name,
                onNameChange = { name = it },
                age = age,
                onAgeChange = { age = it },
                gender = gender,
                onGenderSelect = { gender = it },
                onBack = { step = 0 },
                onNext = { step = 2 },
                t = t
            )
            2 -> ConditionsStep(
                selectedConditions = selectedConditions,
                onToggle = { cond ->
                    if (cond == "none") {
                        if (selectedConditions.contains("none")) selectedConditions.clear()
                        else {
                            selectedConditions.clear()
                            selectedConditions.add("none")
                        }
                    } else {
                        selectedConditions.remove("none")
                        if (selectedConditions.contains(cond)) selectedConditions.remove(cond)
                        else selectedConditions.add(cond)
                    }
                },
                onBack = { step = 1 },
                onFinish = {
                    viewModel.savePatient(name.ifBlank { "Ahmed Karim" }, age.toIntOrNull() ?: 52, gender.ifBlank { "male" }, language)
                    selectedConditions.filter { it != "none" }.forEach {
                        viewModel.addCondition(it)
                    }
                    step = 3
                },
                t = t
            )
            3 -> SetupDoneStep(
                onGoTimeline = onFinish,
                t = t
            )
        }
    }
}

@Composable
fun LanguageStep(
    selectedLang: String,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
    t: (String, String) -> String
) {
    Text(
        text = t("Choose your language", "আপনার ভাষা বেছে নিন"),
        style = OnboardingTitleStyle,
        color = Ink,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    Text(
        text = t("You can change this anytime from your profile.", "আপনি যেকোনো সময় প্রোফাইল থেকে এটি পরিবর্তন করতে পারবেন।"),
        style = MaterialTheme.typography.bodyMedium,
        color = InkSoft,
        modifier = Modifier.padding(bottom = 24.dp)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LanguageCard(
            name = "English",
            sub = "Continue in English",
            selected = selectedLang == "en",
            onClick = { onSelect("en") }
        )
        LanguageCard(
            name = "বাংলা",
            sub = "বাংলায় চালিয়ে যান",
            selected = selectedLang == "bn",
            onClick = { onSelect("bn") }
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    DesignPrimaryButton(
        text = t("Continue", "চালিয়ে যান"),
        onClick = onContinue
    )
}

@Composable
fun LanguageCard(name: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.6.dp, if (selected) Primary else Line),
        color = if (selected) PrimaryTint else Surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = name, style = GreetingNameStyle, fontSize = 19.sp, color = Ink)
                Text(text = sub, style = MaterialTheme.typography.bodySmall, color = InkSoft, modifier = Modifier.padding(top = 2.dp))
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) Primary else Color.Transparent)
                    .border(1.6.dp, if (selected) Primary else Line, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun BasicInfoStep(
    name: String, onNameChange: (String) -> Unit,
    age: String, onAgeChange: (String) -> Unit,
    gender: String, onGenderSelect: (String) -> Unit,
    onBack: () -> Unit, onNext: () -> Unit,
    t: (String, String) -> String
) {
    Text(
        text = t("Basic info", "প্রাথমিক তথ্য"),
        style = OnboardingTitleStyle,
        color = Ink,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    Text(
        text = t("This helps us personalize your timeline.", "এটি আপনার টাইমলাইন ব্যক্তিগতকরণে সাহায্য করবে।"),
        style = MaterialTheme.typography.bodyMedium,
        color = InkSoft,
        modifier = Modifier.padding(bottom = 24.dp)
    )

    Text(
        text = t("Full name", "পূর্ণ নাম"),
        style = MaterialTheme.typography.labelLarge,
        color = InkSoft,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text(t("e.g. Ahmed Karim", "যেমন: আহমেদ করিম"), color = InkSoft.copy(alpha = 0.5f)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            focusedBorderColor = Primary,
            unfocusedBorderColor = Line,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface
        )
    )

    Text(
        text = t("Age", "বয়স"),
        style = MaterialTheme.typography.labelLarge,
        color = InkSoft,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    OutlinedTextField(
        value = age,
        onValueChange = onAgeChange,
        placeholder = { Text(t("e.g. 52", "যেমন: ৫২"), color = InkSoft.copy(alpha = 0.5f)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            focusedBorderColor = Primary,
            unfocusedBorderColor = Line,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface
        )
    )

    Text(
        text = t("Gender", "লিঙ্গ"),
        style = MaterialTheme.typography.labelLarge,
        color = InkSoft,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GenderOption(t("Male", "পুরুষ"), gender == "male", onClick = { onGenderSelect("male") }, modifier = Modifier.weight(1f))
        GenderOption(t("Female", "মহিলা"), gender == "female", onClick = { onGenderSelect("female") }, modifier = Modifier.weight(1f))
        GenderOption(t("Other", "অন্যান্য"), gender == "other", onClick = { onGenderSelect("other") }, modifier = Modifier.weight(1f))
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .height(48.dp)
                .width(88.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.4.dp, Primary)
        ) {
            Text(t("Back", "পেছনে"), color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Button(
            onClick = onNext,
            enabled = name.isNotBlank(),
            modifier = Modifier
                .height(48.dp)
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                disabledContainerColor = Primary.copy(alpha = 0.4f)
            )
        ) {
            Text(t("Continue", "চালিয়ে যান"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun GenderOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.4.dp, if (selected) Primary else Line),
        color = if (selected) PrimaryTint else Color.Transparent,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Primary else InkSoft
            )
        }
    }
}

@Composable
fun ConditionsStep(
    selectedConditions: List<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    t: (String, String) -> String
) {
    Text(
        text = t("Any conditions to track?", "ট্র্যাক করার মতো কোনো রোগ আছে কি?"),
        style = OnboardingTitleStyle,
        color = Ink,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    Text(
        text = t("Optional — select any that apply. You can change this anytime.", "ঐচ্ছিক — যা প্রযোজ্য তা নির্বাচন করুন। আপনি যেকোনো সময় পরিবর্তন করতে পারবেন।"),
        style = MaterialTheme.typography.bodyMedium,
        color = InkSoft,
        modifier = Modifier.padding(bottom = 24.dp)
    )

    val conditions = listOf(
        "diabetes" to t("Diabetes", "ডায়াবেটিস"),
        "hypertension" to t("Hypertension", "উচ্চ রক্তচাপ"),
        "cholesterol" to t("High cholesterol", "উচ্চ কোলেস্টেরল"),
        "kidney" to t("Kidney disease", "কিডনি রোগ"),
        "heart" to t("Heart disease", "হৃদরোগ"),
        "none" to t("None of these", "এগুলোর কোনোটিই নয়")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        conditions.forEach { (key, label) ->
            TickRow(
                label = label,
                selected = selectedConditions.contains(key),
                onClick = { onToggle(key) }
            )
        }
    }

    Spacer(modifier = Modifier.height(18.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .height(48.dp)
                .width(88.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.4.dp, Primary)
        ) {
            Text(t("Back", "পেছনে"), color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Button(
            onClick = onFinish,
            modifier = Modifier
                .height(48.dp)
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text(t("Finish setup", "সেটআপ সম্পন্ন করুন"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
    DesignTextButton(
        text = t("Skip for now", "এখন এড়িয়ে যান"),
        onClick = onFinish,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun TickRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.4.dp, if (selected) Primary else Line),
        color = if (selected) PrimaryTint else Surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Ink)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) Primary else Color.Transparent)
                    .border(1.6.dp, if (selected) Primary else Line, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
fun SetupDoneStep(
    onGoTimeline: () -> Unit,
    t: (String, String) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(PrimaryTint),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                color = Primary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = t("You're all set", "আপনি প্রস্তুত"),
            style = OnboardingTitleStyle,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = t(
                "Your health timeline is ready. Start by scanning a report or adding a reading manually.",
                "আপনার স্বাস্থ্য টাইমলাইন প্রস্তুত। একটি রিপোর্ট স্ক্যান করে বা ম্যানুয়ালি একটি রিডিং যোগ করে শুরু করুন।"
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.padding(bottom = 26.dp)
        )
        DesignPrimaryButton(
            text = t("Go to my timeline", "আমার টাইমলাইনে যান"),
            onClick = onGoTimeline
        )
    }
}
