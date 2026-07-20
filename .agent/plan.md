# Project Plan

SpeedWatch: An app to track network speed, log historical data, and verify ISP promises. Features real-time tracking, analytics, and notifications for speed drops.

## Project Brief

# SpeedWatch

## Features
* **Real-time Speed Tracking**: Monitor download and upload speeds using standard Android networking APIs.
* **Historical Analytics**: Store speed test results in a local Room database and visualize performance trends over time with interactive charts.
* **ISP Speed Verification**: Compare measured speeds against user-defined ISP promised speeds to ensure service quality.
* **Automated Alerts**: Send notifications if network performance falls significantly below the promised ISP speeds.
* **Background Monitoring**: Periodic speed checks using WorkManager to log performance throughout the day.
* **Full Edge-to-Edge UI**: A modern, immersive experience following Material Design 3 guidelines.

## Tech Stacks
* **Language**: Kotlin
* **UI**: Jetpack Compose with Material Design 3
* **Database**: Room Persistence Library
* **Concurrency**: Kotlin Coroutines and Flow
* **Background Processing**: WorkManager
* **Networking**: ConnectivityManager and OkHttp for throughput measurement
* **Architecture**: MVVM with Clean Architecture principles
* **Graphics**: Vico or similar Compose-compatible charting library

## UI Design Image
The app features a vibrant, energetic dashboard with a central speedometer gauge that animates during tests. A bottom navigation bar provides quick access to 'Dashboard', 'History', and 'Settings'. The History view uses sleek cards and line graphs to show speed fluctuations. The design is fully adaptive, utilizing a two-pane layout for tablets and foldables to show history details alongside the main list.

## Implementation Steps

### 1: Set up project scaffolding, Material 3 theme, and navigation.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Project builds
  - M3 theme with dynamic colors is applied
  - Bottom navigation is functional
- **StartTime:** 2026-07-13 16:31:23 NPT

### 2: Implement the Data Layer with Room for speed logs and ISP settings.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Room database is initialized
  - DAO for SpeedLog and IspSettings created
  - Unit tests pass

### 3: Implement core speed tracking logic and throughput measurement.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Download/Upload speed measurement working
  - WorkManager task for background monitoring implemented

### 4: Build the Dashboard UI with an animated speedometer gauge.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Dashboard displays real-time or last test speed
  - Animation is smooth
  - Manual test trigger works

### 5: Implement History and Analytics screens with charts.
- **Status:** PENDING
- **Acceptance Criteria:**
  - History list shows logged data
  - Chart displays trends correctly

### 6: Implement ISP verification notifications and Settings screen.
- **Status:** PENDING
- **Acceptance Criteria:**
  - User can set promised speeds
  - Notifications trigger when speed is low

### 7: Ensure Full Edge-to-Edge display and Adaptive/Large screen layouts.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Edge-to-edge implemented
  - Tablet layout (two-pane) functional

### 8: Perform final quality check and UI refinement.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Critic agent verifies app stability and UI quality
  - All features verified

