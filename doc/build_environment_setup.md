This guide describes how to setup a command line build environment for MOPP android application on an ubuntu linux system.

### System requirements

Following are the prerequisites to successfully follow this guide.

* Oracle JDK 8 or above
* JAVA_HOME environment variable is set and $JAVA_HOME/bin is included in PATH
* Git version control system
* Open Internet connection
* Target operating system for this guide is Ubuntu 14.04 LTS

## 1. Preparing build environment

### Setup android command line tools

**Step 1.1** Use **wget** to download android command line tools from google

```bash
wget https://dl.google.com/android/repository/tools_r25.2.3-linux.zip
```

**Step 1.2** Unzip it somewhere. For example you can place it on the root of your user directory. The location you use will be your **Android SDK** location where both android **SDK** and android **NDK** will be located after completing a couple of more steps

```bash
unzip tools_r25.2.3-linux.zip -d /home/username/android/sdk/
```

**Step 1.3** Configure **ANDROID_HOME** environment variable based on the location of the Android SDK. Additionally, consider adding **ANDROID_HOME/tools/bin** to your path

```bash
export ANDROID_HOME=/home/username/android/sdk
export PATH=${PATH}:$ANDROID_HOME/tools/bin
```

### Use the sdkmanager to download required packages

**Step 1.4** Since the downloaded command line tools do not include all the necessary tools and platforms, use the sdkmanager to download them. For more information on using sdkmanager see [sdkmanager user guide](https://developer.android.com/studio/command-line/sdkmanager.html) For the purposes of this guide you need *build-tools;25.0.2*, *platforms;android-25* and *ndk-bundle*

After executing the command and before download begins you need to accept a licence agreement. The download and install itself might take some time so be prepared to wait.
```bash
sdkmanager "build-tools;25.0.2"
sdkmanager "platforms;android-25"
sdkmanager "ndk-bundle"
```

When the required packages have been downloaded your $ANDROID_HOME directory should look something like this:

```bash
parallels@ubuntu:~/dev/sdk/android/test_setup$ ls
build-tools  licenses  ndk-bundle  platforms  tools
```

Also consider adding **ANDROID_HOME/build-tools/25.0.2** to path. This is useful when using apksigner to sign the .apk file after build.
```bash
export PATH=${PATH}:$ANDROID_HOME/build-tools/25.0.2
```

**Step 1.5** Configure the ANDROID_NDK_HOME environment variable

```bash
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk-bundle
```

## 2. Build MOPP-Android application

### Clone repository from github

```bash
git clone https://github.com/open-eid/MOPP-Android.git
cd MOPP-Android/
```

### Optionally assemble debug build
Use the gradle wrapper to execute debug build.

```bash
./gradlew assembleDebug
```

### Assemble release build and sign the resulting .apk
**Step 2.1** Use the gradle wrapper to execute debug build.

```bash
./gradlew assembleRelease
```

After successful build you should see the resulting debug and release *.apk files in *app/build/outputs/apk/*
```bash
parallels@ubuntu:~/dev/test/MOPP-Android/app/build/outputs/apk$ ls
app-debug.apk  app-release-unsigned.apk
```

**Step 2.2** Use [zipalign](https://developer.android.com/studio/command-line/zipalign.html) to align release .apk

zipalign ensures that all uncompressed data starts with a particular byte alignment relative to the start of the file, which may reduce the amount of RAM consumed by an app.
```bash
cd app/build/outputs/apk
zipalign -v -p 4 app-release-unsigned.apk app-release-unsigned-aligned.apk
```

**Step 2.3** Sign the aligned release apk with your private key using [apksigner](https://developer.android.com/studio/command-line/apksigner.html)
```bash
apksigner sign --ks my-release-key.jks --ks-pass pass:keystorepassword --out app-release.apk app-release-unsigned-aligned.apk
```

Above command uses keystore with name *my-release-key.jks* and password *keystorepassword* to sign the APK. See [apksigner user guide](https://developer.android.com/studio/command-line/apksigner.html) for other options. For example it is also possible to sign an APK file using separate private key and certificate files.

**Step 2.4** Verify the signed APK

```bash
apksigner verify -v --print-certs app-release.apk
```

You should see a similar output to this:
```bash
parallels@ubuntu:~/dev/test/MOPP-Android/app/build/outputs/apk$ apksigner verify -v --print-certs app-release.apk
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
Signer #1 certificate DN: CN=Firstname Lastname, OU=My organizational unit, O=My Organization name, L=My City, ST=My Province, C=EE
Signer #1 certificate SHA-256 digest: 6d14b931811fea0be2aaff108ead995c72c498034eb3c4449519a9b747dd67ae
Signer #1 certificate SHA-1 digest: cdcf34f822f4da1ff2c23f19e16d7b97ea925fb4
Signer #1 certificate MD5 digest: d94686609dc8f2ca8b1a5ea54e983f4a
Signer #1 key algorithm: RSA
Signer #1 key size (bits): 2048
Signer #1 public key SHA-256 digest: 1ca4544c70055a54bd72f5bb423af9d1b04a1b2684e734bca0c6d33feeb54c36
Signer #1 public key SHA-1 digest: f40a77b0fbdaf180a579e14acda5db9007314ad4
Signer #1 public key MD5 digest: 596016a0983e8291c348487dc41a7ee5
WARNING: META-INF/LICENSE not protected by signature. Unauthorized modifications to this JAR entry will not be detected. Delete or move the entry outside of META-INF/.
WARNING: META-INF/services/com.fasterxml.jackson.core.JsonFactory not protected by signature. Unauthorized modifications to this JAR entry will not be detected. Delete or move the entry outside of META-INF/.
```

### Generate keystore with keytool (if you don't have one already)

Optionally if you don't yet have a private key you can generate one using keytool (part of JDK in $JAVA_HOME/bin). Google recommends that the certificate be valid for 25 years. This might be a bit of an overkill but it is **important that the validity period is at least as long as the expected lifetime of the application** because all updates to the application must be signed with the same key. Otherwise updating the app will not be possible.
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000
```
This example prompts you for passwords for the keystore and key, and to provide the Distinguished Name fields for your key. It then generates the keystore as a file called my-release-key.jks, saving it in the current directory. The keystore contains a single key that is valid for 10,000 days.