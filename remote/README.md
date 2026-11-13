# ZineROM Remote Control

The workflow is `.github/workflows/zinerom-remote-control.yml`.

## Before enabling manual runs
GitHub only exposes `workflow_dispatch` after the workflow exists on the repository's default branch. Merge the development branch's remote-control workflow to `main` after reviewing it.

## Inputs
- `publish_release`: when true, writes or replaces a release entry for the selected device and version.
- `release_version`, `previous_version`, `release_title`, `release_message`, `download_url`, `release_notes_url`: release metadata.
- `target_device`: shared target for the release and notification. `all` targets all subscribed ZineROM clients.
- `notification_type`: UPDATE, ANNOUNCEMENT, WARNING, MAINTENANCE, CUSTOM or NONE.
- `notification_title`, `notification_message`, `target_version`: notification content and optional version.
- `button_enabled`, `button_text`, `button_url`: optional action button metadata.
- With `notification_type=NONE`, no FCM request is made. With `publish_release=false`, the release feed is not changed.

## Required secrets
Create repository Actions secret `FCM_SERVICE_ACCOUNT_JSON` containing the Firebase service-account JSON for a service account with Firebase Cloud Messaging send permission. Do not commit this JSON or place it in the APK.

The Android app must also be configured with the Firebase Android project's `google-services.json` at `android-app/app/google-services.json`. This file contains client configuration, not the server private key. Do not add service-account credentials to the Android app.

## Topic routing
The app subscribes to `zinerom_all` and, where its model mapping is recognized, one device topic such as `zinerom_a52s-5g`. The workflow sends to the selected topic. Unknown device models receive only the all-devices topic.

## Delivery guarantees
A notification run fails if the server credential secret is missing or FCM rejects the request. The workflow does not report a push as sent unless the FCM HTTP v1 API accepts it. The metadata commit step runs only after successful validation and, when requested, successful FCM delivery.
