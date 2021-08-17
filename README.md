![Build Status](https://github.com/trebleshot/android/actions/workflows/android-test.yml/badge.svg)
[![Translation Status](https://hosted.weblate.org/widgets/trebleshot/-/svg-badge.svg)](https://hosted.weblate.org/engage/trebleshot/)

# TrebleShot - FOSS File & Text Sharing Tool for Android

Send and receive files over available connections. Got a question? Check the
[FAQ](https://github.com/genonbeta/TrebleShot/blob/master/FAQ.md) page.

## Get it

[<img src="https://f-droid.org/badge/get-it-on.png" width="230">](https://f-droid.org/packages/org.monora.uprotocol.client.android/)
[<img src="assets/google-play-badge.png" width="230">](https://play.google.com/store/apps/details?id=org.monora.uprotocol.client.android)

An alpha version of the desktop version is also in the works, which you can find 
[here](https://github.com/genonbeta/TrebleShot-Desktop).

## Main Features

* Share all kinds of content without size limitation:
  * Videos
  * Photos
  * Music
  * Apps
  * Files & Folders (preserves content structure)
  * Texts
* Works without the internet; set up a hotspot and send to the other person
* Share between multiple devices at the same time
* Minimal UI optimized for speed
* Recovers from errors after a failure

## Build from Source

Run the following command to build the `fossReliant` variant in debug mode on Unix-alike OSes.

```sh
./gradlew -Dorg.gradle.jvmargs=-Xmx1536m assembleFossReliantDebug \ 
      lintFossReliantDebug testFossReliantDebugUnitTest
```

## Localization

We are using Weblate to translate TrebleShot. Click [here](https://hosted.weblate.org/engage/TrebleShot/) to go to the
translation page.


[![Translation Status](https://hosted.weblate.org/widgets/trebleshot/-/multi-auto.svg)](https://hosted.weblate.org/engage/TrebleShot/)

## Screenshots

[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot1.webp" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot1.webp)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot2.webp" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot2.webp)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot3.webp" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot3.webp)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot4.webp" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot4.webp)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot5.webp" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot5.webp)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot6.webp" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot6.webp)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot7.webp" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot7.webp)

## License

This app is licensed under GNU Public License version 2.0 or later version.
