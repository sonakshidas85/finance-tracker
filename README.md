# gremlin (with Jetpack Glance home-screen widgets)

A local-only, native Android budget tracker with two independently-driven periods: Weekly is a
direct, manually-entered spending allotment, while Monthly still works off your monthly salary and
a savings goal. Split each period's pool into spending categories, track what you've spent, and
see the same numbers live on two home-screen widgets (Weekly and Monthly), each resizable between
a compact and an expanded layout.

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (Android Studio bundles a compatible JDK; no separate install needed if you let Studio
  manage it)
- Android SDK Platform 34 + Build-Tools (Android Studio's SDK Manager will prompt to install
  these on first sync)
- No network access is required at runtime - the app makes no network calls - but Gradle does
  need network access once, to download dependencies from Google's Maven and Maven Central.

## Opening / building / running

This project ships without a materialized Gradle wrapper JAR (`gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.jar`) because this was produced in a sandboxed environment with no
access to fetch that binary. `gradle/wrapper/gradle-wrapper.properties` (which pins the Gradle
version, 8.4) is included, so either of the following will materialize the rest automatically:

**Option A - Android Studio (recommended):**
1. `File > Open...` and select the `BudgetTrackerWidget/` folder.
2. Android Studio detects the missing wrapper JAR and offers to regenerate it (or simply syncs
   using its own bundled Gradle) - accept the Gradle sync prompt.
3. Let Gradle sync finish (downloads AGP, Kotlin, Compose, Glance, DataStore, WorkManager, and
   kotlinx.serialization dependencies from Google's Maven / Maven Central).
4. Run the `app` configuration on a device or emulator (API 26+).

**Option B - command line, once you have a local Gradle install:**
```bash
cd BudgetTrackerWidget
gradle wrapper --gradle-version 8.4   # materializes gradlew/gradlew.bat/gradle-wrapper.jar
./gradlew assembleDebug
```
The resulting APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Installing on a device/emulator

```bash
./gradlew installDebug
```
or drag the built APK onto a running emulator, or use Android Studio's Run button with a
connected device/emulator selected (API 26+ required, matching `minSdk`).

## Exporting an installable APK

This sandbox has no Android SDK or network access to compile the project directly (see
"Opening / building / running" above), so a `.github/workflows/build-apk.yml` GitHub Actions
workflow is included to produce a real, installable APK without needing a local Android Studio
setup. Two ways to get an APK onto your phone, easiest first:

**(a) GitHub Actions (no local Android Studio needed):**
1. Push this project to a new GitHub repository.
2. Open the repo's **Actions** tab. You'll see the **"Build installable APK"** workflow - either
   run it manually (`Run workflow` button) or just push to `main`/`master` to trigger it
   automatically.
3. Once the run finishes with a green check, open the run's summary page.
4. Under **Artifacts**, download `budget-tracker-debug-apk` (a zip file).
5. Unzip it - inside is `app-debug.apk`.
6. Transfer that file to your phone any way you like (email it to yourself, upload to Drive/Dropbox
   and download on-device, or plug the phone in over USB and copy it across).
7. On the phone, tapping the APK to install it will prompt you to allow installs from that source
   the first time - Android calls this **"Install unknown apps"** and it's per-app (i.e. per
   file-manager/browser/mail-app you used to open the APK). You can also pre-enable it at
   **Settings -> Apps -> Special access -> Install unknown apps** -> pick the app you'll use to
   open the file -> toggle **Allow from this source**.
8. Tap the APK again to install, then open gremlin like any other app.

**(b) Android Studio, if you have it installed locally:**
1. Open the project (`File > Open...` -> select `BudgetTrackerWidget/`), let Gradle sync finish.
2. **Build** menu -> **Build App Bundle(s) / APK(s)** -> **Build APK(s)**.
3. Once it finishes, find the APK at `app/build/outputs/apk/debug/app-debug.apk`.
4. Transfer and install it on your phone the same way as step 6-8 above.

Either path produces a **debug-signed APK** - that's fine for sideloading onto your own device, but
if you ever wanted to distribute this beyond personal use you'd need to build a `release` variant
signed with your own proper signing key instead of the auto-generated debug key.

## Adding the widgets to a home screen

1. Install and open the app at least once (this seeds default data into DataStore so the widget
   never shows a blank/empty first-run state).
2. Long-press an empty area of the home screen -> **Widgets**.
3. Find **gremlin** in the widget picker - you'll see two entries, **gremlin:
   Weekly** and **gremlin: Monthly** (their labels come from
   `res/values/strings.xml` / `AndroidManifest.xml` receiver labels).
4. Drag either one onto the home screen. It initially places at its small (~2x1 cell) size.
5. Long-press the placed widget and drag its resize handles to grow it - once it crosses the
   medium size threshold (~220dp x 120dp), it switches from the compact headline-only layout to
   the expanded layout showing the top 3 categories. Shrinking it back reverts to the compact
   layout. This is `SizeMode.Responsive` in action (see `widget/WeeklyBudgetWidget.kt` /
   `widget/MonthlyBudgetWidget.kt`).
6. Tap anywhere on either widget to open the app with the matching Weekly/Monthly tab
   pre-selected.

## Logging a spend from the Weekly widget

The Weekly widget (only - the Monthly widget stays exactly as described above: display-only,
tap-through-to-app) can log a spend without opening the full app:

- **Add spend popup.** Both the small and medium Weekly widget layouts have an "Add spend" / "+"
  entry point that opens a small floating popup (`QuickAddSpendActivity`) over the home screen - a
  dropdown of the current week's categories, a ₹-prefixed amount field, and Add/Cancel buttons -
  then closes immediately back to the home screen.
- **Why a popup instead of typing directly on the widget:** Android home-screen widgets (Jetpack
  Glance included, since Glance compiles down to `RemoteViews`) cannot host a real text input
  field - `EditText` isn't in the `RemoteViews` supported view set - so an inline amount field on
  the widget itself isn't possible. The popup Activity is how the widget offers a typed-amount
  entry point at all.
- **Weekly-only by design.** This entry point only exists for the Weekly widget/period, matching
  how weekly is the direct, frequently-topped-up manual allotment while monthly is a slower,
  salary-driven budget that doesn't need a fast top-up affordance.
- **App-lock interaction.** If "Require unlock to open app" is enabled, `QuickAddSpendActivity`
  requires the same `BiometricPrompt` check as the main app before showing any category names or
  letting you add a spend - otherwise the widget's "Add spend" popup would be a silent bypass
  around the app lock. Cancelling or failing that prompt just closes the popup (the widget itself
  stays on the home screen; tap it again to retry) rather than getting stuck on a locked screen.
- The popup reuses the exact same rollover-check-then-write logic
  (`BudgetRepository.applyWeeklySpend`) and the exact same widget-refresh mechanism
  (`BudgetApplication.refreshWeeklyWidget`, wrapping the same `GlanceAppWidget.updateAll` call the
  app's always-alive DataStore observer already uses) as the rest of the app, so a spend logged
  right at a week boundary lands in the correct week and the widget updates immediately either way.

## Project layout

```
BudgetTrackerWidget/
  settings.gradle.kts, build.gradle.kts, gradle.properties
  gradle/wrapper/gradle-wrapper.properties
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/res/...
    src/main/java/com/budgettracker/app/
      BudgetApplication.kt        - app-open rollover check, midnight worker scheduling,
                                     always-alive DataStore observer that pushes widget updates
      MainActivity.kt             - single Activity host, reads widget tap intent extra
      QuickAddSpendActivity.kt    - small floating popup the Weekly widget launches for its
                                     "Add spend" entry point (see "Logging a spend..." above)
      data/                       - pure model, pure calculations, pure period stamps, pure
                                     currency formatting, + the DataStore-backed repository
      ui/                         - Compose screens, components, theme, MainViewModel,
                                     BiometricGate.kt (shared app-lock helper)
      widget/                     - both GlanceAppWidgets, their receivers, shared content
      work/                       - self-rescheduling WorkManager rollover worker + scheduler
  verification/                   - standalone kotlinc/kotlin scratch harness (see below)
```

## Data model / calculations

The two periods are driven differently:

- **Weekly** is a direct, manually-entered spending allotment: `BudgetState.weeklyAllotment`
  (defaults to ₹3,000, editable via a single ₹-prefixed numeric field on the Weekly tab, same
  reject-negative validation as the salary field). The weekly pool categories allocate against is
  simply `weeklyAllotment` itself - there is no derivation from `monthlySalary` at all anymore.
- **Monthly** is unchanged: `monthlyIncome = monthlySalary`,
  `monthlySavings = monthlyIncome * (savingsGoalPercent / 100)`,
  `monthlyPool = monthlyIncome - monthlySavings`, still driven by the salary input + savings-goal
  slider on the Monthly tab.
- For both periods, `categoryAllocation = periodPool * (category.percent / 100)` and
  `categoryRemaining = categoryAllocation - category.spent`, and the "Left to allocate" footer is
  `periodPool - sum(categoryAllocation)`.

Default seeded categories differ per period (first launch / "Clear all data"), each starting at
`spent = 0.0`:

- **Weekly:** Groceries (50%), Fun / discretionary (30%), Other (20%).
- **Monthly:** Rent (40%), Groceries (15%), Transport (8%), Fun / discretionary (10%), Other (5%),
  Cat supplies (3%), Credit Card Payments (12%), Utility bill (7%).

Users can still add their own custom categories on either tab via the "Add category" button.

## Verification harness

`verification/` contains plain `.kt` copies of the three fully-pure files (`BudgetModels.kt`,
`BudgetCalculations.kt`, `PeriodStamps.kt`, `CurrencyFormat.kt` - none have Android imports in the
real app either) plus `Main.kt`, which asserts ~65 cases: currency formatting/rounding/negative
display, monthly income/savings/pool/allocation/remaining math for several salary/percent
combinations, weekly allocation/remaining math against the direct `weeklyAllotment` pool (new
3-category weekly seed and new 8-category monthly seed), weekly "Saved" (allotment - spent,
including an over-spent/negative case), monthly Income/Spent/Savings stat values, progress-fraction
clamping, input clamping (including the new `clampWeeklyAllotment`), top-3-by-allocation ordering,
ISO week stamps for several known reference dates (including an ISO week-year rollover and a
53-week year), month stamp zero-padding, `monthLabelFromStamp`, and a hand-inlined rollover
simulation.

Run it with:
```bash
cd BudgetTrackerWidget/verification
kotlinc BudgetModels.kt BudgetCalculations.kt PeriodStamps.kt CurrencyFormat.kt Main.kt -include-runtime -d verify.jar
kotlin -classpath verify.jar MainKt
```
(or `kotlinc *.kt -script` / any equivalent invocation). See the final report for pass/fail
results - this sandbox had no `kotlinc`/network access to install one, so every case was instead
hand-traced against independently computed reference values (Python's `datetime.isocalendar()`
for the ISO week-stamp logic, plain arithmetic for the budget math).

## Security

This app is local-only and has no server or login, but it still holds sensitive personal data
(salary, spending habits) on a device that could be lost, borrowed, or picked up unlocked, so it's
hardened against local/physical threats rather than network ones:

- **Encrypted storage at rest.** The single JSON blob DataStore persists (`BudgetState` - salary,
  savings goal, categories, spend) is encrypted with AES-256-GCM using a key that lives in the
  Android Keystore (provisioned via `androidx.security.crypto.MasterKey`), never in app memory or
  on disk in plaintext. This protects against another app on the device reading the file directly,
  or someone pulling the file off an unrooted device (e.g. via a non-privileged `adb backup`/file
  browser) and reading it as plain text - the Keystore key itself does not leave secure hardware on
  supported devices.
- **Backups disabled.** `android:allowBackup="false"` stops Android's automatic cloud/device-transfer
  backup from copying this file to a backup store outside our control. The tradeoff: switching
  phones no longer restores your budget data automatically - you'd need to manually recreate it (or
  extend the app yourself with an explicit export/import feature) on the new device.
- **Screenshots/screen recording blocked.** `FLAG_SECURE` on the main window means the app's
  content is blanked out in screenshots, the recent-apps thumbnail, and screen recordings/casting -
  so budget figures can't leak that way if someone else picks up or screen-shares your phone.
- **Optional app-lock.** Settings has a "Require unlock to open app" toggle (off by default) that
  gates the whole UI behind a `BiometricPrompt` accepting either a biometric (fingerprint/face) or
  your device PIN/pattern/password. It's off by default so the app stays frictionless for anyone
  who doesn't want it; if your device has no screen lock configured at all, the toggle is disabled
  with an explanation, since there'd be nothing to authenticate against. The same gate (factored
  into a shared `showBiometricGate` helper) also protects `QuickAddSpendActivity`, the Weekly
  widget's "Add spend" popup - otherwise that popup would be a way to view category names and log
  spends without ever unlocking the app.
- **Widget masking.** Home-screen widgets render even when the app itself is locked (and even on
  the lock screen, depending on launcher/OS settings) - app-lock only gates the app's own UI, not
  the widgets. The "Mask amounts on home-screen widgets" toggle (off by default) replaces real ₹
  figures on both widgets with a `₹••••` placeholder while keeping category names and progress bars
  visible, for anyone who doesn't want salary/spend numbers visible at a glance without unlocking.
- **Release-build obfuscation.** Release builds enable R8 minification and resource shrinking
  (`isMinifyEnabled` / `isShrinkResources`), renaming and stripping unused code/resources to raise
  the bar on casually reverse-engineering the APK. `proguard-rules.pro` carries explicit keep rules
  for the `kotlinx.serialization` generated serializers so this doesn't silently break JSON
  (de)serialization at runtime (debug builds are unaffected, matching the existing build config).

Be honest about the limits: none of this is designed to resist a fully rooted, forensically
instrumented attacker with physical possession of the device and time to work - that's a much
larger threat model than a personal local budgeting app needs to defend against.

## Assumptions & tradeoffs

- **Two widgets per period instead of one toggleable widget.** Per-widget Glance state toggling
  (to let a single placed widget switch between Weekly/Monthly) adds real complexity - extra tap
  targets, per-instance state persistence, more surface area for bugs - for a feature users can
  already get by placing two widgets side by side. Two simple, "dumb", always-correct widgets
  (`WeeklyBudgetWidgetReceiver`, `MonthlyBudgetWidgetReceiver`) are more robust than one cleverer
  one for this scope.

- **Self-rescheduling one-time WorkManager request instead of AlarmManager exact alarms** for the
  midnight rollover. A `PeriodicWorkRequest`'s 15-minute minimum interval can never land exactly
  on midnight and drifts over time. `AlarmManager` exact alarms need
  `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` permission handling on Android 12+, which is extra
  friction for a feature that doesn't need to-the-second precision - a few minutes of slack after
  midnight is fine, especially since the same rollover check also runs synchronously on app open
  and on every widget `provideGlance` call, catching staleness even if the device was off when the
  worker's delay elapsed.

- **Manual Indian digit-grouping instead of `NumberFormat.getCurrencyInstance(Locale("en","IN"))`.**
  ICU/CLDR grouping data for `en-IN` is inconsistent across Android API levels 26+ (some
  OEM/API combinations don't apply the 2-2-3 grouping correctly, or render the rupee symbol
  differently). The manual `CurrencyFormat.groupIndian()` function is deterministic on every API
  level. A `NumberFormat`-based version is kept (commented, unused) in `CurrencyFormat.kt` purely
  for reference.

- **Preferences DataStore + one JSON blob instead of Proto DataStore.** Proto DataStore needs a
  `.proto` schema and a codegen step; Preferences DataStore + `kotlinx.serialization` for a single
  `BudgetState` JSON string is simpler to evolve (add a field, bump nothing) and still fully type-
  safe at the Kotlin layer, at the cost of losing Proto's schema-migration tooling - an acceptable
  tradeoff for a single-module, local-only app with one serialized shape.

- **Currency rounding/negative-display choice.** Amounts round to the nearest whole rupee for
  display (`RoundingMode.HALF_UP` via `BigDecimal.valueOf(amount)`, not the raw `BigDecimal(Double)`
  constructor, to avoid binary-floating-point rounding surprises) rather than showing paise/decimals
  - every input field in the app already operates at whole-rupee granularity, so decimals would be
  visual noise. Negative amounts render as `-₹1,234` (minus sign before the rupee symbol), matching
  how Android's own currency formatters typically render negative currency.

- **Delete affordance: plain trailing `IconButton` (×) instead of `SwipeToDismissBox`.** Each
  category row already has a percent `Slider` and two text fields, all of which consume horizontal
  drag/tap gestures - layering swipe-to-dismiss over a slider is an easy-to-mis-trigger gesture
  conflict. A small, explicit icon button keeps the delete action unambiguous and keeps the row's
  gesture surface simple.

- **Dark mode in Glance widgets.** Glance's Material3 dynamic color (`GlanceTheme.colors`) would
  resolve to the Android 12+ wallpaper-derived dynamic palette, which would not reproduce this
  app's specific warm-off-white/emerald/coral palette. Instead, `widget/WidgetContent.kt` branches
  on the host `Context`'s night-mode configuration (`Configuration.UI_MODE_NIGHT_MASK`) and
  manually selects between the same Light/Dark `Color` constants the Compose theme uses
  (`ui/theme/Color.kt`), so both surfaces share one palette source of truth.

- **Fractional-width progress bars in Glance.** `GlanceModifier.fillMaxWidth()` is boolean-only (no
  `Modifier.fillMaxWidth(Float)` equivalent to Compose). `GlanceProgressBar` in
  `widget/WidgetContent.kt` instead splits a full-width `Row` into a filled and remainder `Row`
  sized via integer `defaultWeight()` on a 0-1000 scale, which gives sub-percent visual resolution
  without needing a fractional-width modifier.

- **Launcher icon.** A minimal hand-authored adaptive icon (flat warm-off-white background +
  simple emerald vector glyph) is included purely so the manifest's `android:icon`/`android:roundIcon`
  references resolve to real resources; it is not meant as a finished design asset.
