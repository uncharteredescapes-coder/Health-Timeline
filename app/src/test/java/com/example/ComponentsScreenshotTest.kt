package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.components.BadgeType
import com.example.ui.components.DesignPrimaryButton
import com.example.ui.components.GroundingNote
import com.example.ui.components.StatusBadge
import com.example.ui.components.SummaryBlock
import com.example.ui.theme.HealthTimelineTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ComponentsScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testComponents_lightMode() {
        composeTestRule.setContent {
            HealthTimelineTheme {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusBadge(text = "NORMAL", type = BadgeType.OK)
                    StatusBadge(text = "HIGH", type = BadgeType.WATCH)
                    StatusBadge(text = "CRITICAL", type = BadgeType.ATTN)
                    SummaryBlock(
                        eyebrowTitle = "Clinical Findings",
                        body = "HbA1c level is 6.8% showing a downward trend."
                    )
                    GroundingNote(
                        text = "AI extraction requires human clinical verification."
                    )
                    DesignPrimaryButton(text = "Confirm & Save", onClick = {})
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/components_light.png")
    }

    @Test
    fun testBengaliLocalizationRendering() {
        composeTestRule.setContent {
            HealthTimelineTheme {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusBadge(text = "স্বাভাবিক", type = BadgeType.OK)
                    StatusBadge(text = "সতর্কতা", type = BadgeType.WATCH)
                    SummaryBlock(
                        eyebrowTitle = "ক্লিনিক্যাল ফলাফল ও সারসংক্ষেপ",
                        body = "ল্যাব টেস্টের ফলাফলে উন্নতির ধারা লক্ষ্য করা গেছে।"
                    )
                    GroundingNote(
                        text = "এই ফলাফল আপনার সংরক্ষিত রিপোর্ট থেকে সংগৃহীত। ডাক্তারের পরামর্শের বিকল্প নয়।"
                    )
                    DesignPrimaryButton(text = "সংরক্ষণ করুন", onClick = {})
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/components_bengali.png")
    }
}
