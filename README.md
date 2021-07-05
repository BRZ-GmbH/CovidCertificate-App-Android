# Grüner Pass App - Android

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://github.com/BRZ-GmbH/CovidCertificate-App-Android/blob/main/LICENSE)

This project is released by the [Bundesrechenzentrum GmbH](https://www.brz.gv.at/).

It is based on the open source work of the Swiss [Federal Office of Information Technology, Systems and Telecommunication FOITT](https://github.com/admin-ch/CovidCertificate-App-Android)

## Grüner Pass App

Grüner Pass is the official app for storing and presenting COVID certificates issued in Austria.
The certificates are kept and checked locally on the user's phone.

<a href='https://play.google.com/store/apps/details?id=at.gv.brz.wallet'>
<img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="20%"/>
</a>

<p align="center">
<img src="wallet/src/main/play/listings/en-US/graphics/phone-screenshots/EN_01.png" width="20%">
<img src="wallet/src/main/play/listings/en-US/graphics/phone-screenshots/EN_02.png" width="20%">
<img src="wallet/src/main/play/listings/en-US/graphics/phone-screenshots/EN_03.png" width="20%">
<img src="wallet/src/main/play/listings/en-US/graphics/phone-screenshots/EN_04.png" width="20%">
</p>

## Contribution Guide

This project is truly open-source and we welcome any feedback on the code regarding both the implementation and security aspects.

Bugs or potential problems should be reported using Github issues.
We welcome all pull requests that improve the quality of the source code.
Please note that the app will be available with approved translations in English, German.

## Repositories

* Android App: [CovidCertificate-App-Android](https://github.com/BRZ-GmbH/CovidCertificate-App-Android)
* Android SDK: [CovidCertificate-SDK-Android](https://github.com/BRZ-GmbH/CovidCertificate-SDK-Android)
* iOS App: [CovidCertificate-App-iOS](https://github.com/BRZ-GmbH/CovidCertificate-App-iOS)
* iOS SDK: [CovidCertificate-SDK-iOS](https://github.com/BRZ-GmbH/CovidCertificate-SDK-iOS)

## Installation and Building

Make sure to properly check out the submodule: `git submodule update --init`.

The project can be opened with Android Studio 4.1.2 or later.
Alternatively, you can build the respective apps using Gradle:
```sh
$ ./gradlew wallet:assembleProdRelease
```
Note that in order for that to work, you must have set up your own keystore.

The APK is generated under `app/build/outputs/apk/prod/release/app-prod-release.apk` where `app` is one of: `verifier`, `wallet`.

## Reproducible builds

To verify that the app distributed on the Play Store was built by the source code published here, please see the instructions
in [REPRODUCIBLE_BUILDS.md](REPRODUCIBLE_BUILDS.md).

## License

This project is licensed under the terms of the MPL 2 license. See the [LICENSE](LICENSE) file for details.
