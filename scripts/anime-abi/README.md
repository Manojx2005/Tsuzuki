# Anime extension ABI check (AY6)

Anime extensions are compiled against aniyomi's `extensions-lib` stubs and link to whatever the app offers under
the same names. A renamed method, a changed signature or a class R8 strips becomes a `NoSuchMethodError` /
`NoClassDefFoundError` when a user opens that extension. These files pin what extensions expect and check it.

| File | What |
|---|---|
| `expected-lib14.txt`, `-lib16.txt`, `-lib17.txt` | Public/protected API of each stub jar, one `class#member descriptor` per line |
| `not-offered.txt` | Expected lines Tsuzuki deliberately doesn't offer, with the reason (prefix match) |
| `dump_api.py` | Makes an `expected-lib*.txt` from a stub `classes.jar` (uses `javap`; skips compiler-generated classes) |
| `check_apk.py` | Checks a built APK (use the minified **preview** one) still has every expected line, following supertypes |
| `app/src/test/.../AnimeExtensionAbiTest.kt` | The same check on the JVM test classpath, in the normal test run |

Pinned stubs (aniyomiorg/extensions-lib, built by JitPack, `classes.jar` from the AAR):

| Lib | Tag | Commit |
|---|---|---|
| 14 | `14` | `f33cc241dee8f7561b97f0aa0921c58f0f8a3b8c` |
| 16 | `v16` (same commit as `16`) | `a776b99df09003423eb13d9816323aaa09af7c24` |
| 17 | `v17` | `971ce96e76202bb35432fe71f92553e3ae40d01d` |

## Re-run after a dependency bump (or a change to `source-api` / `core/common`)

```sh
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:testDebugUnitTest --tests "*AnimeExtensionAbiTest"     # JVM check
./gradlew :app:assemblePreview
python -I scripts/anime-abi/check_apk.py app/build/outputs/apk/preview/app-arm64-v8a-preview.apk   # R8 check
```

Both list every miss. Fix a miss with a bridge in a Tsuzuki file (an overload with the old signature that calls the
new one, as `network/Requests.kt` does), or a keep rule in `app/proguard-rules.pro` if only R8 lost it; add to
`not-offered.txt` **and** `NOT_OFFERED` in the test only when it's deliberate, with the reason.

## Adding a new lib version

Download `https://jitpack.io/com/github/aniyomiorg/extensions-lib/<tag>/extensions-lib-<tag>.aar` into an empty
folder, take `classes.jar` out of it, then:

```sh
python -I scripts/anime-abi/dump_api.py <folder>/classes.jar scripts/anime-abi/expected-lib<N>.txt
```

and add a `lib <N> API is offered` test. The jar is only read (by `javap`); nothing from it runs.
