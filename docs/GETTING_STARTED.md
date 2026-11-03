# Getting started on your computer

## 1. Get the code

```sh
git clone https://github.com/Manojx2005/Tsuzuki.git
cd Tsuzuki
```

## 2. Install the tools

- **Android Studio**, latest stable. It includes the Android SDK and a JDK.
- On first open, let Android Studio sync Gradle. It downloads the SDK version set in
  `gradle/mihon.versions.toml` and all dependencies. The first sync takes a while.
- If you build from a terminal, use JDK 17 or newer (`java -version`).

## 3. Build and install on your phone

1. On the phone: Settings › About phone › tap **Build number** 7 times, then
   Settings › Developer options › turn on **USB debugging**. Connect by USB and accept the prompt.
2. In Android Studio, pick the **debug** build variant and press **Run** (▶).
   Or from a terminal:
   ```sh
   ./gradlew :app:installDebug
   ```
   The debug app is installed as `io.github.manojx2005.tsuzuki.dev` and can sit next to a release build.

Telemetry and the self-updater are off by default. No Firebase file is needed.

## 4. What to check first

These changes were written without compiling (the cloud session couldn't reach the Android SDK).
If the build fails, the likely places are:

| File | Change |
|---|---|
| `app/src/main/java/eu/kanade/presentation/theme/LnrTypography.kt` | New fonts and typography (uses `FontVariation` for variable fonts) |
| `app/src/main/java/eu/kanade/presentation/theme/colorscheme/LnrColourColorScheme.kt`, `LnrDotColorScheme.kt` | New colour schemes |
| `app/src/main/java/eu/kanade/presentation/theme/TachiyomiTheme.kt` | Passes the new typography to `MaterialExpressiveTheme` |
| `app/src/main/java/eu/kanade/domain/ui/model/AppTheme.kt`, `UiPreferences.kt` | `COLOUR` / `DOT` themes, Colour is the default |
| `app/src/main/res/font/lnr_*.ttf` | Bundled OFL fonts |

On the phone, check:

- [ ] App name shows **Tsuzuki**.
- [ ] Settings › Appearance lists **Colour** and **Dot** first. Switching changes colours and fonts.
- [ ] Headings use Instrument Serif in Colour and the dot-matrix font in Dot.
- [ ] Browse › Extensions can add LNReader plugins and open a novel (this part is Tsundoku's, unchanged).

## 5. Where things are

- Product and engineering plan: [`docs/PLAN.md`](PLAN.md)
- Screen designs, both themes, plus Motion: the "Tsuzuki App Screens" canvas
  (https://claude.ai/artifact/JupsRJRJ2GsXbLaXnmbjMC)
- Original Tsundoku readme: [`docs/TSUNDOKU_README.md`](TSUNDOKU_README.md)

## 6. Next steps (in order)

1. Build succeeds and the app runs on your phone. Fix any compile errors.
2. Theme building blocks in Compose: Colour theme taken from the book cover, dot progress bars and rings, tiles, nav bar, rail.
3. New bento Home screen, then restyled Library, Novel details, Reader and Settings.
4. Main transitions from the Motion page.
5. Basic tablet layout, then release v0.1.

## Keeping up with Tsundoku

```sh
git remote add upstream https://github.com/tsundoku-otaku/tsundoku.git
git fetch upstream
git merge upstream/main
```
