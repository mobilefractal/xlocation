# AirLocation
[![](https://jitpack.io/v/mobilefractal/xlocation.svg)](https://jitpack.io/#mumayank/AirLocation)

An android library to simplify the usage of Google Play services location APIs, to get the user's most precise live location via a callback!

Jump to [Setup](https://github.com/mobilefractal/xlocation/blob/master/README.md#setup "Setup") or [Usage](https://github.com/mumayank/AirLocation/blob/master/README.md#usage "Usage")

![alt text](https://github.com/mobilefractal/xlocation/blob/master/github_assets/image.png "Logo")

Features:
+ The location is precise up to 7 decimal places
+ Choose to get user's location just one-time, or continue getting live updates
+ The library takes care of a host of redundant checks and tasks like:
	+ Declaring the location permissions in the Manifest file
	+ Adding the Google Play services location APIs library dependency in Gradle
	+ Checking if Google Play services are available and up to date or not
		+ If not, requesting the user to update it, along with providing an option to do so
	+ Checking if location permissions are available or not
		+ If not, requesting the user at runtime to grant the permissions
		+ Also checking if the permissions are permanently disabled by the user
			+ If so, taking the user to the app's settings page and requesting to manually grant the permissions.
	+ Checking if the device hardware settings are optimized or not (GPS is on, Wifi is on, etc)
		+ If not, requesting the user to grant permission to change settings automatically
+ Uses only Google Play services location APIs internally - so you're in safe hands
+ Simple plug and play design
+ Extremely lightweight library (~50KB)
+ **Written in Kotlin (with full Java support)**
+ Android 10+ compatible (gets user's location via foreground location access, i.e., the activity requesting the location must be visible to the user to continue receiving location updates)
+ Takes care of the activity lifecycle

# Screenshots

|   |  |
| ------------- | ------------- |
| <img src="https://github.com/mumayank/AirLocation/blob/master/github_assets/s1.png" width="300">  | <img src="https://github.com/mumayank/AirLocation/blob/master/github_assets/s2.png" width="300">  |
| <img src="https://github.com/mumayank/AirLocation/blob/master/github_assets/s3.png" width="300">    | 

# Setup

Add this line in your root build.gradle at the end of repositories:

```gradle
    allprojects {
      repositories {
        ...
        maven { url 'https://jitpack.io' } // this line
      }
    }
```
Add this line in your app build.gradle:
```gradle
    dependencies {
      implementation 'com.github.mobilefractal:xlocation:LATEST_VERSION' // this line
    }
```
where LATEST_VERSION is [![](https://jitpack.io/v/mobilefractal/xlocation.svg)](https://jitpack.io/#mobilefractal/xlocation)

# Usage

1. Define `airLocation`
2. To start receiving live location, call `airLocation.start()`
3. Override `onActivityResult` and call `airLocation.onActivityResult()`
4. Override `onRequestPermissionsResult` and call `airLocation.onRequestPermissionsResult()`

Example:
```kotlin
    class MainActivity : AppCompatActivity() {

        private val airLocation = AirLocation(this, object : AirLocation.Callback {  

            override fun onSuccess(locations: ArrayList<Location>) {
                // do something 
                // the entire track is sent in locations
            }  

            override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {  
                // do something 
                // the reason for failure is given in locationFailedEnum
            }  

        })

        override fun onCreate(savedInstanceState: Bundle?) {
            ...
            airLocation.start() // CALL .start() WHEN YOU ARE READY TO RECEIVE LOCATION UPDATES
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            airLocation.onActivityResult(requestCode, resultCode, data) // ADD THIS LINE INSIDE onActivityResult
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            airLocation.onRequestPermissionsResult(requestCode, permissions, grantResults) // ADD THIS LINE INSIDE onRequestPermissionResult
        }

    }
```

## Thank you!
