![EU Regional Development Fund](docs/images/EL_Regionaalarengu_Fond_horisontaalne-vaike.jpg)

* License: LGPL 2.1
* &copy; Estonian Information System Authority

# MOPP-Android

Android application that allows signing containers with ID-card, mobile-ID and Smart-ID.

Check the wiki get an overview of the project.

## Building and developing the project

You can build the project from the command line like any other Android project.
See [Build your app from the command line](https://developer.android.com/studio/build/building-cmdline).

```
./gradle clean fetchAndPackageDefaultConfiguration app:assemble
```

Or import project from Android Studio and run it from there.

> **NOTE**: In order for the build to succeed the Firebase Crashlytics plugin requires google-services.json
> config file to be present inside "app" folder root

* To validate signatures in offline mode, download latest TSL files:  
&nbsp;&nbsp; 1. https://ec.europa.eu/tools/lotl/eu-lotl.xml -> Rename file to "eu-lotl.xml"  
&nbsp;&nbsp; 2. https://sr.riik.ee/tsl/estonian-tsl.xml -> Rename file to "EE.xml"  
&nbsp;&nbsp; 3. Create an "assets" folder in "app/src/main"  
&nbsp;&nbsp; 4. Create a folder named "tslFiles" in "assets" and move TSL files there  

## Support
Official builds are provided through official distribution point [https://www.id.ee/en/article/install-id-software/](https://www.id.ee/en/article/install-id-software/). If you want support, you need to be using official builds. Contact our support via [www.id.ee](http://www.id.ee) for assistance.

Source code is provided on "as is" terms with no warranty (see license for more information). Do not file Github issues with generic support requests.
