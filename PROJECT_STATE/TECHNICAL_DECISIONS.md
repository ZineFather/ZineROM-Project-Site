# Technical Decisions
- Keep the production static website intact; add the app under android-app/.
- Use Kotlin, Jetpack Compose, Material 3, WorkManager and OkHttp.
- Use WorkManager's unique periodic work with a 24-hour interval; Android may defer execution under battery constraints.
- The initial device catalog mirrors the website's current 11 device entries; unknown model codes and missing release data remain unspecified.
- Remote release metadata is hosted in the repository under remote/ and served through raw.githubusercontent.com.
- FCM service-account credentials must stay in GitHub Secrets and must never be included in the APK or repository.
- Work only on zinerom-info-development until a reviewed pull request is ready.
