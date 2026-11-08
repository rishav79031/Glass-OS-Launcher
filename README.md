# Glass Launcher

An iPadOS-style "liquid glass" home screen for Android, tuned for the Samsung Galaxy Tab S9 FE+.
Kotlin + Jetpack Compose, Android 12+.

## What it does

- **Liquid Glass**: Dock, Search pill, widgets, menus, folders and Control Centre use translucent glass that blurs the
  wallpaper, bends it at the edges (a lens shader on Android 13+) and catches light along the rim.
- **iPhone layout on phones, iPad layout on tablets**: 4-column grid and 4-app Dock on phones; 5×6 / 6×5 (or 6×7 / 8×5
  with "More App Icons") and a Dock with recent apps on tablets.
- **Every app icon converted** to an iOS 26 appearance: Default, Dark, Clear, Tinted (8 colours), Automatic
  (follows dark mode), or Colour glass. All use the iOS continuous-corner shape.
- **Pages like iOS**: Today View (swipe right) → Home Screen pages → App Library (swipe left), with a "Search" capsule
  that turns into page dots while swiping.
- **Widgets**: Clock, Calendar, Up Next, Batteries and Suggestions.
- **Jiggle mode**: long-press empty space; drag apps to rearrange (rest at an edge to change page), drop on the Dock,
  or tap − to remove. Removed apps stay in the App Library.
- **Long-press menu**: the app's Quick Actions (shortcuts), Edit Home Screen, Share App, Add/Remove Dock, App Info,
  Remove App (iOS-style alert).
- **Notification badges** (red counts) and **Now Playing** in Control Centre.
- **Spotlight search**: swipe down; Suggestions, apps, and web search.
- **Control Centre**: swipe down from the top-right. Connectivity, Now Playing, Rotation Lock, Screen Mirroring, Focus,
  brightness and volume sliders, Flashlight, Timer, Calculator, Camera.
- **Setup that asks for each permission** with what it's for: Home role, notification access, calendar,
  modify system settings, Do Not Disturb. All optional, all available later in settings.
- **Adjustable Liquid Glass**: Clear or Tinted (like iOS 26.1), plus separate frost sliders for widgets/panels and
  the Dock/Search. Continuous ("squircle") corners on every card.
- **Wallpapers like iOS**: Collections (built-in gradients plus **your own images** via *Add*, stored at this
  screen's full resolution), Photo Shuffle (new photo on each unlock), Home Screen as Original / Blur / Gradient,
  Perspective Zoom, and "Set as Lock Screen & Home Screen". The wallpaper blurs as you swipe into Today View or the
  App Library. Apple's own wallpapers aren't bundled (they're Apple's copyright) — add images you have yourself.
- **Settings**: long-press empty space → **Settings**, or Control Centre → **Home Screen**.
- **Notification Centre** (swipe down from the top): Lock Screen-style clock with notifications shown as
  Count / Stack / List; tap to open, swipe left to clear.
- **Built-in widgets**: Weather (Open-Meteo, no account), Calendar (shows Google Calendar events), Photos
  (featured photo), Clock, Batteries, Up Next, Suggestions — pick them in the widget gallery in jiggle mode.
- **iOS animations**: the Home Screen zooms in when you unlock, and settles back when you leave an app.
- **Font**: Inter, the closest open-licence match to SF Pro (SF Pro may not be shipped outside Apple platforms).
  Licence in `FONT_LICENSE.txt`.

## Performance

- The wallpaper is blurred **once** into a small texture. Each glass surface just draws its slice, so there's no live blur while scrolling.
- Icons are rendered to bitmaps once per style and cached, so scrolling never re-draws vectors.
- Release build uses R8 minify + resource shrinking. The app asks for the panel's highest refresh rate (90 Hz on the Tab S9 FE+).

## Build

Needs the Android SDK (install it through Android Studio's setup wizard) and JDK 21 (set in `gradle.properties`).

```bash
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`. It's signed with the debug key, which is fine for
sideloading. Create a real signing key before publishing to the Play Store.

## Install on the tablet

1. Copy the APK to the tablet and open it. Allow "Install unknown apps" for your file manager when asked.
2. Or, with USB debugging on: `adb install -r app/build/outputs/apk/release/app-release.apk`
3. Open Glass Launcher, long-press empty space → **Set as default home screen**.
4. Optional, in the same panel: allow brightness & rotation control and Do Not Disturb control for the Control Centre.

## Limits (Android rules, not bugs)

- The Control Centre opens from the home screen only. Showing it over other apps needs an Accessibility Service.
- It can't replace Samsung's lock screen or notification shade.
- Wi-Fi and Bluetooth can't be toggled directly by apps on Android 10+, so those tiles open the system panels.
