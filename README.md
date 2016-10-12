# Object Tracking System using Google Tango Device - Honours Project 2016-2017


## To-do

### Manual Tracking (to be done by 3rd October 2pm latest)

- [X] Add full functionality to record button
    - [X] When Record button is pressed, the button changes to Stop button
    - [X] The surface view is recorded until the user presses Stop
    - [X] The record time changes during recording
- [X] Add full functionality to stop button
    - [X] When Stop button is pressed, recording ends
    - [X] Recording is saved to file system with default name "VID_DATETIME.mp4"
- [X] Add full touch functionality to surface view **only** when user is recording
    - [X] When user touches screen during recording, the touched location is overlayed with a circle
    - [X] The touch coordinates as well as timestamp of touch is saved in a YML format, shown below.
- [ ] Give user an option to change circle appearance
    - [x] Allow user to change colour of circle
    - [ ] Allow user to change radius of circle (*possibly*)

### Additions to manual tracking (to be done by 17th October) 


- [ ] Add framestamp functionality
    - [ ] Add frame number to YML file when user clicks to track object
    - [ ] Add current frame number to corner of surface view when user has tapped screen. Research some API's to find frame information
    - [ ] Add the frame number to the final recording for testing (this is used to ensure the frame/timestamp correspondence is accurate). Look into AndroidFFMpeg for this.
    - [X] Store milliseconds in the YML timestamp for more accuracy
- [ ] Make the UI layout for camera full screen 
    - [ ] Make the toolbars transparent (i.e. no black frame), so that the optimal resolution isn't wonky.
 

### Changes/implementations to be made but are currently not prioritized

- [ ] Add full functionality to flash button
    - [ ] Listen to users changes and store users preference
    - [ ] Embed the actual flashlight into the application
- [ ] Add runtime permissions for devices >= Lollipop (https://developer.android.com/training/permissions/requesting.html)
    - [ ] Request permissions at runtime for Camera
    - [ ] Request permissions at runtime for Storage
- [ ] Create own filepicker for app (http://stackoverflow.com/questions/18097763/how-to-open-the-my-files-folder-in-android-programatically-using-intent)

## Project Summary

### Overall Goals
The overall goal of this project is to evaluate the possibilites and limitations of a Google Tango tablet for imaging and tracking of small animals such as insects. The resulting analysis should consist of a program which camn be used to extract useful data for tracking.
The final thesis will include:
- A technical evaluation of the Tango for tracking purposes
- A recording/tracking application prototype description (this will also be implemented)
- An evaluation of this application.


### External Libraries
This project will make use of a few external libraries:
- Google Tango SDK
- OpenCV4android SDK
- Color Picker by QuadFlask on GitHub. Imported as "color-picker.aar"

### Sources
- Google Tango Docs: <https://developers.google.com/tango/>
- OpenCV4android Docs: <http://opencv.org/platforms/android.html> 

## Code Documentation

### Coding Convention

This summary discusses coding conventions used throughout `XML` and `Java` code.

#### ID Names
ID's are named using underscore convention, for example, `@+id/start_tracking_button`, in XML, or `R.id.start_tracking_button` in Java.

#### Layout files
All layout files in the layout folders discussed below are named using underscore convention, for example `activity_main.xml`, or `R.layout.activity_main.xml` in Java.

#### Drawable files
- All drawable files in the `drawables` folder are named using underscore convention, for example, `ic_preferences.png`. 
- All icons begin with the ic_ prefix, e.g. `ic_viewrecording.png`
- All drawables used for styling purposes are prefixed with the widget they are meant to style, such as `button_preferences.xml`
- If a drawable styles multiple widgets, it is prefixed with `widget`, e.g. `widget_gradient.xml`

#### Strings
All string resources in `strings.xml` are named using underscore convention, for example, `@string/start_tracking`, or `R.string.start_tracking` in Java.

#### Dimensions
All dimension values in `dimens.xml`are named using underscore convention, for example `@dimen/activity_horizontal_margin`

#### Colours
All colours in `colours.xml` are named using camelCase convention, for example `@color/colorAccent`

#### Styles
All styles in `styles.xml` are named using underscore convention, e.g. `@style/start_tracking_button`

#### Menus
All menu XML files in `res/menu` folder are named using underscore convention, e.g. `flash_menu.xml`
All ID's within the XML file are also named using underscore convention, e.g. `android:id="@+id/flash_auto"`.


### Layout folders

Currently, my layouts are organized into 3 different folders. This overview gives a brief summary of the purpose of each of the folders.

#### layout
The layout directory is the default layout file. When layouts are being inflated, this folder will be searched last for the layout, because it is less "specific" than the other two folders. Currently, this folder is used for mobile layouts, and tablet layouts have its own layout folders, discussed next.

#### layout-sw600dp
The sw600dp suffix stands for "smallest width 600dp". From Android Documentation describing smallest width: 
> The Smallest-width qualifier allows you to target screens that have a certain minimum width given in dp. For example, the typical 7" tablet has a minimum width of 600 dp, so if you want your UI to have two panes on those screens (but a single list on smaller screens), you can use the same two layouts from the previous section for single and two-pane layouts, but instead of the large size qualifier, use sw600dp to indicate the two-pane layout is for screens on which the smallest-width is 600 dp:

When inflating a layout, if the device is a tablet, then this folder (or layout-sw600dp-land) is searched first, depending on the orientation. If the **tablet layout is portrait**, this layout is searched first, otherwise if the **tablet layout is landscape**, layout-sw600dp-land is searched first. The general layout folder above is searched after, ***only *** if the layout needing inflated is not in any of the sw600dp folders, as the sw600dp folders are more specific, so they are always searched first.

#### layout-sw600dp-land
As `layout-sw600dp`, but the difference is this folder stores landscape layouts and the former stores portrait layouts, for **tablets**.


### Drawable folders
Currently only hosting one drawable folder, but likely in future will also split drawables. This section remains as a placeholder.


## Misc

### YAML Format
The YAML format should follow something along the lines of (assuming 30 FPS):

```
framenumber: 90
    timestamp: 00:00:03
    x: 256
    y: 181
framenumber: 180    
    timestamp: 00:00:06
    x: 280 
    y: 193
...
...
...

```
    
    










