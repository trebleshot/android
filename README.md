![Build Status](https://github.com/trebleshot/android/actions/workflows/android-test.yml/badge.svg)
[![Translation Status](https://hosted.weblate.org/widgets/trebleshot/-/svg-badge.svg)](https://hosted.weblate.org/engage/trebleshot/)

# TrebleShot - FOSS File Sharing App for Android

Send and receive files securely without the internet. Got a question? Check the
[FAQ](https://github.com/genonbeta/TrebleShot/blob/master/FAQ.md) page or join
[the discussion group](https://t.me/trebleshot) on Telegram.

## Get it

[<img src="https://f-droid.org/badge/get-it-on.png" width="230">](https://f-droid.org/packages/org.monora.uprotocol.client.android/)
[<img src="assets/google-play-badge.png" width="230">](https://play.google.com/store/apps/details?id=org.monora.uprotocol.client.android)

An alpha version of the desktop version is also in the works. You can find it
[here](https://github.com/genonbeta/TrebleShot-Desktop).

## Main Features

* Secure; connections to other clients are encrypted using TLSv1.2 
* Share media files, apps, files & folders, plain texts, and URLs
* Works without the internet; set up a hotspot and you are good to go
* Share between multiple devices
* Send and receive locally using a web browser
* Uses **uprotocol**, and open content-sharing protocol

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

[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot1.png" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot1.png)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot2.png" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot2.png)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot3.png" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot3.png)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot4.png" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot4.png)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot5.png" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot5.png)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot6.png" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot6.png)
[<img src="https://github.com/trebleshot/assets/blob/main/screenshots/android/shot7.png" width=160>](https://github.com/trebleshot/assets/blob/main/screenshots/android/shot7.png)

### Web Interface

[![Web Interface](https://github.com/trebleshot/assets/blob/main/screenshots/android/web1.png)](https://github.com/trebleshot/assets/blob/main/screenshots/android/web1.png)

## License

This app is licensed under GNU Public License version 2.0 or later version.
