<p align="center">
  <img alt="Files hero image" src="./.github/readme-images/ReadmeHero.png" />
</p>

# Sekia

**Sekia** is an opinionated, custom-built app that bridges the gap between your Windows PC and Android phone, offering seamless clipboard and notification sharing. Born out of the frustration with existing solutions, Sekia provides a straightforward, no-nonsense alternative for those who need more from their Phone Link experience.


## Features

- **Clipboard Sharing**: Easily share clipboard content between your Android device and Windows PC.
- **Media Control**: Control media playback from android.
- **File Sharing**: Share files between your devices easily.
- **Notification**: Allows toasting the notifications from your android in desktop.


## Limitations

- **Clipboard Sharing on Android 13+**:
   - Due to Android's restrictions, clipboard sharing is only possible via the share sheet after Android 13.
   - 
## Installation

### Android

1. **Download the APK**
2. **Install the App**:
   - You might need to enable installation from unknown sources in your device settings.
3. **Permissions**:
   - Allow restricted access from App Info (after trying to grant the notification access).
   - Grant notification permissions to enable notification mirroring.

### Windows

1. **Download the Windows App**: [Seki](https://github.com/shrimqy/Seki/).
2. **Install the Certificate**:
   - Manually install the provided certificate to ensure secure communication between devices. (I will shortly release it on windows store.)

## How to Use

1. **Set Up**:
   - Ensure both your Android device and Windows PC are connected to the same network.
   - Launch the app on your Windows PC.
   - Open app on your Android device and grant the necessary permissions, and try to add a new device (Pull to refresh if it doesn't show any devices).

2. **Clipboard Sharing**:
   - Copy content on the desktop and it will automatically sync with your android (That is if you have enabled it from the settings).
   - To share clipboard from android you will have to manually sent it through the sharesheet that shows after you perform a copy action).
3. **File Transfer**:
   - Use the share sheet from android/windows and select the app to share any files between the devices.

   

## Screenshots

![screenshots of app](./.github/readme-images/Screenshot.png)



## Roadmap

- Notification Actions.
- Explore Device Files directly.
- Emulate Android Device.

## Tech Stack

- **Android**: Kotlin, Jetpack Compose
- **Desktop**: [Seki](https://github.com/shrimqy/Seki) (WinUI 3, C#)
- **Network**: Mdns, Websockets.

