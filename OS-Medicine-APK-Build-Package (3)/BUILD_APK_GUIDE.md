# Build and install OS Medicine (Android APK)

This project already contains an Android app module and a GitHub Actions workflow at
`.github/workflows/build-apk.yml`. The workflow builds a **debug APK** that can be
installed on an Android phone for testing.

## Build the APK with GitHub Actions

1. Sign in to GitHub and create a new **private** repository, for example `os-medicine`.
2. Extract the project ZIP on your computer.
3. Upload the *contents* of the extracted project folder to the repository root.
   The `.github/workflows/build-apk.yml` file must be included. If uploading through
   the GitHub website, make sure hidden folders such as `.github` are included.
4. Open the repository's **Actions** tab.
5. Select **Build OS Medicine APK** in the left sidebar.
6. Click **Run workflow**, select the `main` (or `master`) branch, and run it.
7. Wait for the workflow to finish. Open the successful run and find **Artifacts**.
8. Download the `OS-Medicine-APK` artifact ZIP and extract it. It contains
   `app-debug.apk`.

## Install on your Android phone

1. Transfer `app-debug.apk` to your Android phone (USB, Drive, or another trusted method).
2. Open the APK on the phone.
3. If Android asks, allow your file manager/browser to **Install unknown apps**.
4. Confirm **Install**. Then open **OS Medicine**.

The debug APK is for testing and direct installation. It is not a Play Store release.

## Important project notes

- Android application ID: `com.aistudio.osmedicine.kxmpzq`.
- Minimum Android version: Android 7.0 (API 24).
- The current workflow runs `gradle assembleDebug` and uploads the resulting APK.
- The debug build uses the project's `debug.keystore` and `debugConfig` signing setup.
- Do not remove the debug signing configuration when building this debug APK.
- A Play Store release requires a separate release-signing key and a release build.
  Do not publish or share your private release keystore or passwords.

## If the build fails

Open the failed GitHub Actions run, expand the first red step, and copy the
error text. Common causes include a missing project file during upload, a Gradle
or Android SDK dependency issue, or a Firebase configuration mismatch. Share the
error log (not passwords or private signing keys) for diagnosis.
