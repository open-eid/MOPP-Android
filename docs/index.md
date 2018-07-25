# MOPP Android Documentation

Android application that allows signing containers with Mobile-ID and ID-card 
and encrypting/decrypting containers with ID-card.

## Application

Application is developed using single-activity approach using 
[Conductor](https://github.com/bluelinelabs/Conductor) as navigator.

Architecture is based on MVI (Model-View-Intent) paradigm and relies heavily on 
[RxJava](https://github.com/ReactiveX/RxJava)

Code is organized in packages by feature:

* **crypto**
  * **create** - Encrypt/decrypt
  * **home** - Crypto tab in home view
* **eid** - My eID tab in home view
* **main**
  * **about** - Info screen in main menu
  * **diagnostics** - Diagnostics screen in main menu
  * **home** - Home screen
  * **settings** - Settings screen in main menu
* **signature**
  * **create** - Handles opening signature container files
  * **home** - Signature tab in home view
  * **list** - Recent documents tab in main menu
  * **update** - View/sign container documents

## Modules

Several modules are being developed along with the mobile application that can be used separately.

### sign-lib

High-level support for signing and parsing containers.

[View JavaDoc]({{ site.baseurl }}{% link /sign-lib/javadoc/release/index.html %})

**Key classes:**

* [SignLib]({{ site.baseurl }}{% link /sign-lib/javadoc/release/ee/ria/DigiDoc/sign/SignLib.html %})
* [SignedContainer]({{ site.baseurl }}{% link /sign-lib/javadoc/release/ee/ria/DigiDoc/sign/SignedContainer.html %})
* [DataFile]({{ site.baseurl }}{% link /sign-lib/javadoc/release/ee/ria/DigiDoc/sign/DataFile.html %})
* [Signature]({{ site.baseurl }}{% link /sign-lib/javadoc/release/ee/ria/DigiDoc/sign/Signature.html %})
* [SignatureStatus]({{ site.baseurl }}{% link /sign-lib/javadoc/release/ee/ria/DigiDoc/sign/SignatureStatus.html %})

### crypto-lib

High-level support for encryption/decryption.

Ability to query for recipients from [ldap.sk.ee](https://www.sk.ee/en/repository/ldap).

Decryption needs ID-card.

[View JavaDoc]({{ site.baseurl }}{% link /crypto-lib/javadoc/release/index.html %})

**Key classes:**

* [CryptoContainer]({{ site.baseurl }}{% link /crypto-lib/javadoc/release/ee/ria/DigiDoc/crypto/CryptoContainer.html %})
* [RecipientRepository]({{ site.baseurl }}{% link /crypto-lib/javadoc/release/ee/ria/DigiDoc/crypto/RecipientRepository.html %})
* [DecryptToken]({{ site.baseurl }}{% link /crypto-lib/javadoc/release/ee/ria/DigiDoc/crypto/DecryptToken.html %})

### id-card-lib

Java API for communicating with ID-cards (APDU commands for EstEID v. 3.5 and EstEID v. 3.4).

[View JavaDoc]({{ site.baseurl }}{% link /id-card-lib/javadoc/release/index.html %})

**Key classes:**

* [Token]({{ site.baseurl }}{% link /id-card-lib/javadoc/release/ee/ria/DigiDoc/idcard/Token.html %})
* [PersonalData]({{ site.baseurl }}{% link /id-card-lib/javadoc/release/ee/ria/DigiDoc/idcard/PersonalData.html %})
* [CertificateType]({{ site.baseurl }}{% link /id-card-lib/javadoc/release/ee/ria/DigiDoc/idcard/CertificateType.html %})
* [CodeType]({{ site.baseurl }}{% link /id-card-lib/javadoc/release/ee/ria/DigiDoc/idcard/CodeType.html %})

### smart-card-reader-lib

High level API for communicating with supported smart card readers in Android.

Currently supported smart card readers:

* [ACR38U-ND PocketMate Smart Card Reader](https://www.acs.com.hk/en/products/228/acr38u-nd-pocketmate-smart-card-reader-micro-usb)
* [Identiv uTrust SmartFold SCR3500 Family](https://www.identiv.com/products/smart-card-readers/contact/scr3500)

[View JavaDoc]({{ site.baseurl }}{% link /smart-card-reader-lib/javadoc/release/index.html %})

**Key classes:**

* [SmartCardReaderManager]({{ site.baseurl }}{% link /smart-card-reader-lib/javadoc/release/ee/ria/DigiDoc/smartcardreader/SmartCardReaderManager.html %})
* [SmartCardReader]({{ site.baseurl }}{% link /smart-card-reader-lib/javadoc/release/ee/ria/DigiDoc/smartcardreader/SmartCardReader.html %})
* [SmartCardReaderStatus]({{ site.baseurl }}{% link /smart-card-reader-lib/javadoc/release/ee/ria/DigiDoc/smartcardreader/SmartCardReaderStatus.html %})

### mobile-id-lib

Android service for signing with Mobile-ID.

[View JavaDoc]({{ site.baseurl }}{% link /mobile-id-lib/javadoc/release/index.html %})

**Key classes:**

* [MobileSignService]({{ site.baseurl }}{% link /mobile-id-lib/javadoc/release/ee/ria/DigiDoc/mobileid/service/MobileSignService.html %})

### core-lib

Shared classes for other libraries.

[View JavaDoc]({{ site.baseurl }}{% link /core-lib/javadoc/release/index.html %})

**Key classes:**

* [Certificate]({{ site.baseurl }}{% link /core-lib/javadoc/release/ee/ria/DigiDoc/core/Certificate.html %})
* [EIDType]({{ site.baseurl }}{% link /core-lib/javadoc/release/ee/ria/DigiDoc/core/EIDType.html %})
