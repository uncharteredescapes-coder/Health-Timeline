# Health Timeline — Personal Health Record & Analytics

**Personal Health Timeline** turns scattered medical information — lab reports, photos of reports, doctor visits, medications, and vitals — into a structured, longitudinal health record. Built for patients and caregivers, it provides actionable insights and clinician-ready summaries.

## 🌟 Vision
We believe health records should belong to the patient. This app is designed to help users track chronic conditions like diabetes and hypertension without manual typing, using AI to extract data directly from physical reports.

## ✨ Key Features

### 📸 Smart Report Capture
- **CameraX Scanning**: Integrated document scanner for high-quality capture of physical lab reports.
- **AI-Powered OCR**: Uses **Gemini 3.5 Flash** to extract structured data (test names, values, units, reference ranges, and lab metadata) automatically.
- **Verification Flow**: A dedicated review screen allows users to confirm AI extractions, highlighting low-confidence results for safety.

### 📈 Clinical Trend Analytics
- **Sparklines & Delta Tracking**: View how metrics like HbA1c or Blood Pressure change over time with elegant visual trends.
- **Automatic Status Zoning**: Values outside reference ranges are visually flagged as "high" or "low" for immediate comprehension.

### 💊 Medication & Vitals Tracking
- **Adherence Logs**: Simple, effective medication logging with adherence percentage tracking.
- **Vitals Logging**: Quick-log interface for Blood Pressure, Glucose, and Weight.

### 📋 Clinician-Ready Summaries
- **AI Visit Summary**: Generates a structured one-page summary for doctor appointments, tracing every claim back to verified records.
- **Multilingual Support**: First-class support for **English** and **Bengali** interfaces.

## 🛠 Tech Stack

- **UI**: Jetpack Compose with Material 3 (Material You)
- **Language**: Kotlin
- **Persistence**: Room Database (Offline-first architecture)
- **AI**: Gemini API (REST)
- **Networking**: Retrofit 2 + OkHttp 4
- **Hardware**: CameraX API for document scanning
- **Dependency Management**: Gradle Version Catalogs (libs.versions.toml)

## 🏗 Project Structure

```text
app/src/main/java/com/example/
├── ai/            # Gemini API services and prompt logic
├── data/          # Room entities, DAOs, and Repositories
├── ui/
│   ├── components/ # Reusable UI atoms (Sparklines, Row items)
│   ├── screens/    # Feature-specific screens (Home, Capture, Summary)
│   ├── theme/      # Material 3 color schemes and typography
│   └── HealthViewModel.kt # Unified state management
```

## 🔒 Data Security & AI Safety

- **Privacy-First**: All medical data is stored locally in an encrypted database.
- **No Diagnostics**: The app identifies "out of range" values but never provides medical verdicts or diagnoses.
- **Source-Grounded AI**: Every AI-generated statement in the "Visit Summary" is grounded in the user's uploaded records to prevent hallucinations.

## 🚀 Getting Started

1.  **Secrets**: Add your `GEMINI_API_KEY` to the Secrets panel in AI Studio.
2.  **Permissions**: The app will request Camera permission on the Capture screen.
3.  **Onboarding**: Choose your preferred language (English/Bengali) and set up your profile on first launch.

---
*Created with Google AI Studio Build*
