# MOPP Android Documentation

Android application that allows signing containers with Mobile-ID and ID-card 
and encrypting/decrypting containers with ID-card.

## Modules

Several modules are being developed along with the mobile application that can be used separately.

### sign-lib

High-level support for signing and parsing containers.

* {{ site.baseurl }}

### crypto-lib

High-level support for encryption/decryption.

Ability to query for recipients from ldap.sk.ee.

Decryption needs ID-card.

### id-card-lib

Java API for communicating with ID-cards (APDU commands for EstEID v. 3.5 and EstEID v. 3.4).

### smart-card-reader-lib

High level API for communicating with supported smart card readers in Android.

Currently supported smart card readers:

* [ACR38U-ND PocketMate Smart Card Reader](https://www.acs.com.hk/en/products/228/acr38u-nd-pocketmate-smart-card-reader-micro-usb)
* [Identiv uTrust SmartFold SCR3500 Family](https://www.identiv.com/products/smart-card-readers/contact/scr3500)

### mobile-id-lib

Android service for signing with Mobile-ID.

### core-lib

Shared classes for other libraries.
