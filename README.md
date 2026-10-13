# App Guide / アプリ図鑑

Remember what every app on your phone does. An offline "Play Store for the apps you already have".
スマホの中のアプリを、ひと目で。Free forever · no ads · no login · no tracking.

## Features
- Lists every app that shows in your launcher: icon, name, install / update date, app size
- Starts each app in Android's own category (games, photo, maps…), then you pick your own: Health, Work, Photo, Tools, Learning, Games, Travel, Money, Social, Other, or make new categories
- Filter by category, search names, packages and your notes, sort A–Z or by recently updated
- Detail page: your own note "what this app does", Open app, App info
- Optional **Fetch info**: only when you tap it, reads the app's public Google Play page and saves the short description and up to 4 screenshots on the phone, so it works offline afterwards. **Fetch all** does every app with a progress bar and a Stop button
- In-app English / 日本語 switch

## Permissions
- `INTERNET` — used **only** by the Fetch buttons, to read public Google Play pages. Nothing about you is sent.
- Package visibility uses a `<queries>` launcher intent instead of `QUERY_ALL_PACKAGES`: the app only needs the apps you can open from your home screen, and this keeps it Play-policy friendly if it is ever published there.

All data (notes, categories, fetched text and images) is in the app's private storage. No backups, no analytics.

## Install (sideload)
Download `app-guide.apk` from the [Free Apps site](https://gutsmcd-cmd.github.io/free-apps/) and open it.

## Build
Kotlin, no AndroidX, minSdk 26, target 37.
```
./gradlew lint test assembleRelease
```
Release signing reads a git-ignored `keystore.properties`.

MIT License.
