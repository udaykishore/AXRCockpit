# AXRCockpit-Android

AXRCockpit is an Android application that allows you to remotely control and mirror the screen of other Android devices (Targets) over ADB. It's designed to provide a simple interface for managing and interacting with multiple remote devices.

## Features

*   **Screen Mirroring**: View the screen of the target device in real-time.
*   **Remote Control**: Send ADB commands to the target device.
*   **Multi-Device Dashboard**: Easily switch between different pre-configured target devices.
*   **Device-Specific Controls**: The app comes with built-in control layouts for different types of devices, such as "Galaxy XR" and "Aura". Each layout has a specific set of buttons that execute hardcoded ADB commands tailored for that device.
*   **Wireless ADB Connection**: Connect to target devices over Wi-Fi, with support for ADB pairing.

## How it Works

The application uses the `scrcpy` library for screen mirroring and establishes an ADB (Android Debug Bridge) connection to the target devices. The main screen of the app presents a dashboard where you can select the target device you want to control.

Once a device is selected, a dedicated control screen is displayed. This screen contains a set of buttons, each programmed to send a specific ADB command to the target device.

### Control Configuration

Please note that the button configurations are **built into the application** and are not user-configurable through external files. The controls are defined for the following device profiles:

*   **Galaxy XR**: Controls for Samsung Galaxy devices.
*   **Aura**: Controls for Aura devices.

The specific commands for each button are hardcoded in the source code within `GalaxyFeatures.kt` and `AuraFeatures.kt`.

## Installation

1.  Clone this repository.
2.  Open the project in Android Studio.
3.  Build and run the `app` module on your control device.

## Usage

1.  Ensure that the target device has "Wireless debugging" enabled in its Developer Options.
2.  Use the pairing feature in the app to establish a secure connection for the first time.
3.  Once connected, the target device will appear in the dashboard.
4.  Select the device from the dashboard to open its control screen.
5.  Use the on-screen buttons to send commands and interact with the target device.

## Acknowledgments

This project utilizes the `scrcpy` library for its screen mirroring capabilities. A huge thank you to the developers of `scrcpy` for their incredible work.

*   **scrcpy GitHub Repository**: https://github.com/Genymobile/scrcpy
