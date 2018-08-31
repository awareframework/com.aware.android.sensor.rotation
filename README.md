# AWARE Rotation

[![jitpack-badge](https://jitpack.io/v/awareframework/com.aware.android.sensor.rotation.svg)](https://jitpack.io/#awareframework/com.aware.android.sensor.rotation)

The rotation sensor measures the orientation of the device as a combination of an angle and an axis, in which the device has rotated through an angle θ around an axis (x, y, or z). The rotational vector sensor is particularly versatile and can be used for a wide range of motion-related tasks, such as detecting gestures, monitoring angular change, and monitoring relative orientation changes. For example, the rotational vector sensor is ideal if you are developing a game, an augmented reality application, a 2-dimensional or 3-dimensional compass, or a camera stabilization app.

![Sensor axes](http://www.awareframework.com/wp-content/uploads/2013/05/axis_globe.png)

Three elements of the rotation vector are expressed as follows:

+ x*sin(θ/2)
+ y*sin(θ/2)
+ z*sin(θ/2)

Where the **magnitude** of the rotation vector is equal to **sin(θ/2)**, and the direction of the rotation vector is equal to the direction of the axis of rotation. The three elements of the rotation vector are equal to the last three components of a unit quaternion (cos(θ/2), x*sin(θ/2), y*sin(θ/2), z*sin(θ/2)). Elements of the rotation vector are unitless. The x, y, and z axes are defined in the same way as the acceleration sensor. The reference coordinate system is defined as a direct orthonormal basis, seen above.

This coordinate system has the following characteristics:

+ X is defined as the vector product Y x Z. It is tangential to the ground at the device’s current location and points approximately East;
+ Y is tangential to the ground at the device’s current location and points toward the geomagnetic North Pole;
+ Z points toward the sky and is perpendicular to the ground plane.

For more information, check the official [Android’s Sensor Coordinate System][3] documentation.

## Public functions

### RotationSensor

+ `start(context: Context, config: RotationSensor.Config?)`: Starts the rotation sensor with the optional configuration.
+ `stop(context: Context)`: Stops the service.
+ `currentInterval`: Data collection rate per second. (e.g. 5 samples per second)

### RotationSensor.Config

Class to hold the configuration of the sensor.

#### Fields

+ `sensorObserver: RotationSensor.Observer`: Callback for live data updates.
+ `interval: Int`: Data samples to collect per second. (default = 5)
+ `period: Float`: Period to save data in minutes. (default = 1)
+ `threshold: Double`: If set, do not record consecutive points if change in value is less than the set value.
+ `enabled: Boolean` Sensor is enabled or not. (default = `false`)
+ `debug: Boolean` enable/disable logging to `Logcat`. (default = `false`)
+ `label: String` Label for the data. (default = "")
+ `deviceId: String` Id of the device that will be associated with the events and the sensor. (default = "")
+ `dbEncryptionKey` Encryption key for the database. (default = `null`)
+ `dbType: Engine` Which db engine to use for saving data. (default = `Engine.DatabaseType.NONE`)
+ `dbPath: String` Path of the database. (default = "aware_rotation")
+ `dbHost: String` Host for syncing the database. (default = `null`)

## Broadcasts

### Fired Broadcasts

+ `RotationSensor.ACTION_AWARE_ROTATION` fired when rotation saved data to db after the period ends.

### Received Broadcasts

+ `RotationSensor.ACTION_AWARE_ROTATION_START`: received broadcast to start the sensor.
+ `RotationSensor.ACTION_AWARE_ROTATION_STOP`: received broadcast to stop the sensor.
+ `RotationSensor.ACTION_AWARE_ROTATION_SYNC`: received broadcast to send sync attempt to the host.
+ `RotationSensor.ACTION_AWARE_ROTATION_SET_LABEL`: received broadcast to set the data label. Label is expected in the `RotationSensor.EXTRA_LABEL` field of the intent extras.

## Data Representations

### Rotation Sensor

Contains the hardware sensor capabilities in the mobile device.

| Field      | Type   | Description                                                     |
| ---------- | ------ | --------------------------------------------------------------- |
| maxRange   | Float  | Maximum sensor value possible                                   |
| minDelay   | Float  | Minimum sampling delay in microseconds                          |
| name       | String | Sensor’s name                                                  |
| power      | Float  | Sensor’s power drain in mA                                     |
| resolution | Float  | Sensor’s resolution in sensor’s units                         |
| type       | String | Sensor’s type                                                  |
| vendor     | String | Sensor’s vendor                                                |
| version    | String | Sensor’s version                                               |
| deviceId   | String | AWARE device UUID                                               |
| label      | String | Customizable label. Useful for data calibration or traceability |
| timestamp  | Long   | unixtime milliseconds since 1970                                |
| timezone   | Int    | [Raw timezone offset][1] of the device                          |
| os         | String | Operating system of the device (ex. android)                    |

### Rotation Data

Contains the raw sensor data.

| Field     | Type   | Description                                                     |
| --------- | ------ | --------------------------------------------------------------- |
| x         | Float  | the rotation vector component along the x axis, x*sin(θ/2)      |
| y         | Float  | the rotation vector component along the y axis, y*sin(θ/2)      |
| z         | Float  | the rotation vector component along the z axis, z*sin(θ/2)      |
| w         | Float  | cos(θ/2) (optional, manufacturer dependent)                     |
| accuracy  | Int    | Sensor’s accuracy level (see [SensorManager][2])               |
| label     | String | Customizable label. Useful for data calibration or traceability |
| deviceId  | String | AWARE device UUID                                               |
| label     | String | Customizable label. Useful for data calibration or traceability |
| timestamp | Long   | unixtime milliseconds since 1970                                |
| timezone  | Int    | [Raw timezone offset][1] of the device                          |
| os        | String | Operating system of the device (ex. android)                    |

## Example usage

```kotlin
// To start the service.
RotationSensor.start(appContext, RotationSensor.Config().apply {
    sensorObserver = object : RotationSensor.Observer {
        override fun onDataChanged(data: RotationData) {
            // your code here...
        }
    }
    dbType = Engine.DatabaseType.ROOM
    debug = true
    // more configuration...
})

// To stop the service
RotationSensor.stop(appContext)
```

## License

Copyright (c) 2018 AWARE Mobile Context Instrumentation Middleware/Framework (http://www.awareframework.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[1]: https://developer.android.com/reference/java/util/TimeZone#getRawOffset()
[2]: http://developer.android.com/reference/android/hardware/SensorManager.html
[3]: http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords