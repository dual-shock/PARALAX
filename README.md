<h1 align="center">P A R A L A X</h1>

<p align="center">
  <img src="icons/Icon.png" width="128" height="128">
</p>

Paralax is a mobile application focused on computing the **viability of launching rockets** for a given location in the near future, as well as **simulating a rocket launch with current and future weather conditions**. Built upon 4 different weather API sources, the app fetches and parses ground level and atmospheric weather data used for these calculations and simulations, and processes the data entirely on-device without creating any dupllicate data files / copies.

## Quick start

### Prerequisites:
_note: extra build tools need to be added to the SDK to build this app_
1. Android Studio
2. The SDK Requires the Android NDK and CMake 3.18.1+ build tools installed, instructions on how to do this are [below](<PLACEHOLDER>) or in the [android docs](https://developer.android.com/studio/projects/install-ndk)
3. For examinators, a copy of the provided zip file with the repository inside, for others, you can ```git clone <PLACEHOLDER>``` to clone the repository
4. On startup, the app needs a temporary internet connection to cache the current weather data before use
5. _(opt.) A pre-compiled dev release is also available on the [release page](<PLACEHOLDER>)_

### Building and running
_note: At minimum, the emulator or physical device needs to be running Android 10 or later (sdk 29)_
1. Open the project in Android Studio
2. Enable the NDK (side by side) and CMake build tools
3. Run a Gradle sync to fetch the required imports to build the project
4. Build the application
5. Run on an emulator or physical device
6. _(opt.) If you have any issues building the application or running it correctly on your device, you can also install the pre-compiled .apk from the releases page mentioned above on your device that's guaranteed to work_

## Main Features (and how they work)

### Viability calculation based on weather data
<details>
<summary style="font-style:italic">Paralax fetches data from various weather sources and uses this data to determine how viable a time window is for launching a rocket</summary>
By fetching parameters for wind speeds at different altitudes, cloud coverage at different altitudes, fog, dew, precipitation, air pressure, temperature and humidity, Paralax determines an overall viability score based on a weighted composition of all these values for a specific time and location. The app is shipped with a default set of target values, but all these are editable by the user in the Settings screen. Note that for some values, like wind speed, fog, or precipitation, exceeding the limits set in settings will set the time window's viability to a hard 0.  
</details>

### Trajectory simulation
<details>
<summary style="font-style:italic">Paralax simulates and displays rocket trajectories based on live atmospheric data from GRIB, Interpol, and their averages</summary>
When the user has selected an hour to explore further, they can go to the trajectory simulation screen and see a render of a simulated launch trajectory based on live atmospheric data. Each data source and their average provide wind speed and direction data for multiple atmospheric layers and subsequently multiple altitudes. The rocket parameters used for the simulation can be saved as configuration models in the simulation setup screen, allowing you to test and save multiple rocket models in the simulator.  
</details>

## External libraries

### ecCodes
C library used for decoding GRIB2 weather data files from MET Norway.

### OpenJPEG
Image codec library required by ecCodes for JPEG2000 compressed GRIB data.

### Mapbox
Provides interactive maps and location search functionality.

### KTOR
Handles asynchronous HTTP requests to fetch weather data from APIs.

### Room
Local database for storing launch sites and cached weather locations.

### DataStore
Persists user preferences and settings.

### Navigation Compose
Handles screen navigation within the app.


## Setting up SDK
As mentioned previously, to build this project you need to add some extra build tools to your Android studio SDK. To do this, simply: 
1. Navigate to `tools` > `SDK Manager`
2. Go to `build tools`
3. Check the `Show packages` box in the bottom right
4. Check the boxes for `NDK (side by side)` , if you want to use a specific version make sure its above 28.2+
5. Expand the dropdown for `CMake` and check the box for `CMake 3.18.1`
6. Apply the changes and follow the instructions to install the extra build tools

## Warnings

### Mapbox Composable Warnings
Mapbox uses custom composable annotations, to fit the functions we wanted into the maps we had to work around these, which makes the build produce `Calling a Mapbox Style Composable composable function where a UI Composable composable was expected` warnings, these arent errors or anything and are completely safe. 

### Deprecated PointAnnotation
The Mapbox SDK marks `PointAnnotation` as deprecated (to be removed in a future major release). The current implementation still works and will continue to work until a Mapbox SDK upgrade requires migration to the newer annotation API, which we dont need to yet. 
