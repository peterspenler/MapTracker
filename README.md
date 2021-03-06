# Map Tracker
The goal of this app is to facilitate experiments in wireless indoor location by recording locations (fixed landmarks) and the time that a participant reached that location.

Indoor location with wireless can be approached by using Bluetooth Low Energy (BLE) beacons along with a scanner.
BLE beacons are small battery powered devices that emit a BLE advertising frame, providing the listening device with details about the device and a signal strength.

In our experiments we have placed fixed scanners on the walls in the machine learning research lab.
If a person holds a BLE beacon we can use the scanners as well as the signal strength to locate them given we know the position of the scanners (principles of a triangle: lateration).
In order to validate our methods we need to have ground truths (or actual positions) to compare to our estimations.
This is where this application comes in to play.

## Requirements
The app must
  - Work on devices (Nexus 5?) we have in the lab
  - Be configurable
  - Be able to display an image of map and interactive buttons that report the current time to a database (SQLITE)

### Activities
Suggested Activities
- Home screen (likely only will launch the config screen)
- Configuration screen for the experiment
- Experiment screen

#### Configuration Screen
The configuration screen should have fields for the following information
- Experiment Name
- Configuration File (used to load the map, landmark data)
  - Allow this to be a url (if starts with http:// or https://)
- Results Server
- Beacon Label
- Beacon Height
- Start Experiment Button 
  - The app should check if the experiment has been run yet by searching for the files of a previous run of the same name and give and force the user to fix it before moving on.
All fields are required.

#### Experiment Screen
The experiment screen will be the most challenging part.
The experiment screen will need to be zoom-able, pan-able, and clickable.
Think Google Maps.
You must be able to load the image and place nodes for landmark data from the external storage of the device.
The landmarks should appear as white dots that the user can click on to indicate their current position.
The device should not start "recording" until the user clicks the first position.
The menu bar should have a way to end the experiment which will return to the home screen after doing required cleanup (uploading the data ect...)

### Sqlite
You should utilize a sqlite database for each experiment individually, consider naming the databases with <experiment>.db
Times should be stored in RFC3339 datetime: `yyyy-MM-dd'T'HH:mm:ssZ` Z indicates UTC, which simplifies processing. So you should convert local times to UTC.

### Recording
The device must record the following information:
- The current position and timestamp once pressed
- The accelerometer data plus the timestamp of that data (use TYPE_LINEAR_ACCELERATION)
To faciliate this information you will need two tables:

`position_log (string datetime, real x, real y)`

`acceleration_log (string datetime, real x_acc, real y_acc, real z_acc)`

When the user taps on a landmark it should add an entry to `position_log`. When Android provides an acceleration sensor insert into the `acceleration_log` table.
### On Closing
We are going to likely dump all the experiment data to a file, or set of files on the external storage.
Prefix all files with the experiment name followed by the beacon label.
Like: `experiment1_beaconA_<positions|acceleration>.csv`
Dump the two databases to a CSV file with the headings from the database.

### Upload Server
On completion the data should be uploaded to the ResultsServer address, i.e. `"http://example.com:<port>/results"` make this dynamic as per the configuration file. Use a HTTP post.

JSON Format:
```
{
  "Experiment Name": "<>",
  "SubmissionDatetime": "<Rfc3339datetime>",
  "Configuration File": "<>",
  "Beacon Label": "<>",
  "Beacon Height": <number in metres>,
  "PositionLog": [
  {
    "Datetime": "<RFC3339 Datetime>",
    "Data": [<xreal>, <yreal>]
  },
  {
    "Datetime": "<RFC3339 Datetime>",
    "Data": [<xreal>, <yreal>]
  }],
  "AccelerometerData": [
  {
    "Datetime": "<RFC3339 Datatime>",
    "Data": [<xacc>, <yacc>, <zacc>]
  },
  {
    "Datetime": "<RFC3339 Datatime>",
    "Data": [<xacc>, <yacc>, <zacc>]
  }]
}
```

After posting to the server, the server should respond with a 

```
{
  "Success": true
}
// or
{
  "Success": false,
  "Error": "any error"
}
```

Which should be handled on the app side.

We would like to put these back into CSV format once they get to the server but the other data (Experiment Name, Configuration File, ...) wont be able to be put into the same CSV file, for that problem simply export that section as JSON to a different file. 
This means we will have 3 files for each run.
`experiment1_beaconA_<positions|acceleration|config>.csv`
The config file will have
```
{
  "SubmissionDatetime": "<Rfc3339datetime>",
  "ReceiveDatetime": "<Rfc3339datetime>"
  "Experiment Name": "<>",
  "Configuration File": "<>",
  "Beacon Label": "<>",
  "Beacon Height": <number in metres>
}
```

This may be redunant but should be easy to implement and easier to machine-read from a Python environment.

#### Security of server
At this step we need to ensure the user doesn't upload a file that tricks our server into saving a file to a directory outside our application. 
It possible that the user can upload a experiment called "../../../../etc/passwd" and prevents us from accessing our server.
If you have time ensure the user cannot save files with ".." in them.
I'm not sure if stripping ".." is sufficient.
It is acceptable to make the server and client not accept any experiments or beacon names with non alphanumeric characters [A-Za-z0-9]+.
Please also ensure the user doesn't upload a file with a blank field.
So needless to say, the same confirmation of fields should be done on the client app and the server side.

### Configuration File
The configuration file is a JSON file with the following schema

```
{
  "Title": "<experiment config name>",
  "ImagePath": "<path>",
  "Landmarks": [
    {
      "Label": "<label>",
      "XDisplayLoc", <xloc in pixels>
      "YDisplayLoc", <yloc in pixels>
      "XLoc": <xloc in metres>,
      "YLoc": <yloc in metres>
    },
    {
      "Label": "<label2>",
      "XDisplayLoc", <xloc in pixels>
      "YDisplayLoc", <yloc in pixels>
      "XLoc": <xloc in metres>,
      "YLoc": <yloc in metres>
    }
  ]
}
```

- ImagePath should be able to be a URL instead of a file, similar with the configuration file, (to make things easier);
so you should design the app to use a file relative to the external storage if the first characters in the value string of that field if it starts with `file://`.
- The ImagePath and Configuration File can be simple HTTP GET documents so we can host them on any webserver.
- The XDisplayLoc and XLoc reflect that the map may not be to scale. 
- Important: The XLoc and YLoc should be the values recorded not the pixel location.
- he pixel locations should be relative to the image not the display, for obvious reasons.
- The landmarks can be placed in their location based on any coordinate system (often people use 0,0 as either the bottom left or top left), make 0,0 the top left.
