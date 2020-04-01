# TrebleShot libre software file and text sharing app
Send and receive files over available connections. [FAQ](https://github.com/genonbeta/TrebleShot/blob/master/FAQ.md).

## Get it on
[<img src="https://f-droid.org/badge/get-it-on.png" width="230">](https://f-droid.org/packages/com.genonbeta.TrebleShot/) [<img src="assets/google-play-badge.png" width="230">](https://play.google.com/store/apps/details?id=com.genonbeta.TrebleShot)

## Main features
* Pause and resume transfers
* Share large files
* Share all kinds of content, video, photo, music and app libraries
* No Internet connection is required: Set up a hotspot and connect to it using a QR code
* Share between multiple devices at the same time
* Exchange texts of any kind and save them to TrebleShot
* Share folders for backup and other purposes
* Light UI: Works faster than its rivals on low-end devices
* Speed-oriented: Minimal UI optimized for speed
* Advanced features: Network change handling, choose network based on measured speed

## Project status
[![Build Status](https://travis-ci.org/genonbeta/TrebleShot.svg)](https://travis-ci.org/genonbeta/TrebleShot)
[![Translation Status](https://hosted.weblate.org/widgets/trebleshot/-/svg-badge.svg)](https://hosted.weblate.org/engage/trebleshot/)

## Build from source
Run the following command. This will build `fossReliant` variant in debug mode.  
```sh
./gradlew -Dorg.gradle.jvmargs=-Xmx1536m assembleFossReliantDebug lintFossReliantDebug testFossReliantDebugUnitTest
``` 
If you are facing errors, checkout the latest stable release using its tag name. 

## Pull requests
Before making a pull request, please make sure your commits follow the requirements below.
* Make sure you are on the `dev` branch. `master` branch is updated before releasing a new stable release.  
* Open an issue regarding the pull request you are about to make if possible.
* Use spaces instead of tabs.
* Use LF (Unix and Linux) line separator.
* The right margin is limited to 120 chars and overflowing lines wrapped in a meaningful way. That is: 
    * logic statements are wrapped before `&&` or `||`.
    * method calls are wrapped after the most fitting parameter.
    * inner logic statements with parenthesis should be together if they can fit a single line.
* The opening braces `{` after the class and method declarations should come with the next line.
* When you reformat code, please make sure the previously untouched code does not change in an undefined way.
* Do not create simple methods for the sake of creating methods. For instance, do not a create a method 
that compares two booleans like `function isTrue(boolean a, bolean b) { return a == b; }`.
* If you plan ignore an exception make sure the parameter name in the `catch` block is *ignored*. This will silence the
warnings.
* Add `todo` and `fixme` comments when needed. In short, add `// todo: What to do` or `// fixme: What is the problem`
before the appropriate code. This will ensure they are not forgotten.
* Do not duplicate code blocks. Create methods or classes when needed.
* Variable names should follow the pattern below:
    * Constants are `SOME_VARIABLE`.
    * Global variables are `mSomeVariable` if not public or `someVariable` if public.
    * Local variables are `someVariable`.
* If blocks (do, while, for, if) will contain a single line, they can be free of braces. Also, make sure they are 
wrapped. 
* Headless braces `{}` can be used for scoping.
* When creating strings for localization, please follow the patterns suggested in 
[Language contribution](https://github.com/genonbeta/TrebleShot/wiki/Language-contribution) wiki.
* Other than that some other good practises that you can follow would be:
    * making sure your code backward compatible with the minimum SDK version defined in the main module.
    * committing your changes when you are about to change something unrelated.  
     

## Screenshots
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_1.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_1.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_2.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_2.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_3.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_3.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_4.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_4.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_5.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_5.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_6.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_6.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_7.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_7.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_8.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_8.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_9.png" width=160>](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_9.png)

## Localization
To translate TrebleShot, please read
[this wiki](https://github.com/genonbeta/TrebleShot/wiki/Language-contribution) first if you haven't
worked with Weblate, where you will find the [TrebleShot localization](https://hosted.weblate.org/engage/TrebleShot/).


[![Translation Status](https://hosted.weblate.org/widgets/trebleshot/-/multi-auto.svg)](https://hosted.weblate.org/engage/TrebleShot/)


This app is licensed under GNU Public License version 2.0 or later version.
