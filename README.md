# react-native-yyamap
AMap (Gaode map) sdk for react-native

## install

`npm install react-native-yyamap --save`

### iOS

1. Open your project in XCode, right click on `Libraries` and click `Add
   Files to "Your Project Name"` Look under `node_modules/react-native-yyamap` and add `RCTAMap.xcodeproj`.
2. Add `libRCTAMap.a` to `Build Phases -> Link Binary With Libraries.
3. Click on `RCTAMap.xcodeproj` in `Libraries` and go the `Build
   Settings` tab. Double click the text to the right of `Header Search
   Paths` and verify that it has `$(SRCROOT)/../react-native/React` - if they
   aren't, then add them. This is so XCode is able to find the headers that
   the `RCTAMap` source files are referring to by pointing to the
   header files installed within the `react-native` `node_modules`
   directory.
4. Add `node_modules/react-native-yymap/RCTAMap/RCTAMap/AMap/MAMapkit.framework` and `MAMapkit.framework/AMap.bundle` to your project.
5. Set your project's framework Search Paths to include `$(PROJECT_DIR)/../node_modules/react-native-yyamap/ios/RCTAMap/RCTAMap/AMap`.
6. Set your project's Header Search paths to include `$(SRCROOT)/../node_modules/react-native-yyamap/ios/RCTAMap/RCTAMap`.
4. Whenever you want to use it within React code now you can: `var MapView =
   require('react-native-yyamap');`


### android

1. in `android/settings.gradle`
  
  ```
  include ':app', ':react-native-amap'
  project(':react-native-amap').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-yyamap/android/react-native-amap')
  ```
    
2. in `android/app/build.gradle` add:

  ```
  dependencies {
    ...
    compile project(':react-native-amap')
  }
  ```
3. in `MainActivity.java` add
**Newer versions of React Native**
      ```
    ...
    import com.yiyang.reactnativeamap.ReactMapPackage; // <--- This!
    ...
    public class MainActivity extends ReactActivity {

     @Override
     protected String getMainComponentName() {
         return "sample";
     }

     @Override
     protected boolean getUseDeveloperSupport() {
         return BuildConfig.DEBUG;
     }

     @Override
     protected List<ReactPackage> getPackages() {
       return Arrays.<ReactPackage>asList(
         new MainReactPackage()
         new ReactMapPackage() // <---- and This!
       );
     }
   }
   ```

    **Older versions of React Native**
   ```
   ...
   import com.yiyang.reactnativeamap.ReactMapPackage; // <--- This!
   ...
   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       mReactRootView = new ReactRootView(this);

       mReactInstanceManager = ReactInstanceManager.builder()
               .setApplication(getApplication())
               .setBundleAssetName("index.android.bundle")
               .setJSMainModuleName("index.android")
               .addPackage(new MainReactPackage())
               .addPackage(new ReactMapPackage()) // <---- and This!
               .setUseDeveloperSupport(BuildConfig.DEBUG)
               .setInitialLifecycleState(LifecycleState.RESUMED)
               .build();

       mReactRootView.startReactApplication(mReactInstanceManager, "MyApp", null);

       setContentView(mReactRootView);
   }
   ```
4. specify your Gaode Maps API Key in your `AndroidManifest.xml`:

  ```xml
  <application
    android:allowBackup="true"
    android:label="@string/app_name"
    android:icon="@mipmap/ic_launcher"
    android:theme="@style/AppTheme">
      <!-- You will only need to add this meta-data tag, but make sure it's a child of application -->
      <meta-data
        android:name="com.amap.api.v2.apikey"
        android:value="{{Your Gaode maps API Key Here}}"/>
  </application>
  ```    

## usage

```
...
import MapView from 'react-native-yyamap';

...
render() {
  <MapView
    style={{flex: 1, width: 300}}
    annotations={[{latitude: 39.832136, longitude: 116.34095, title: "start", subtile: "hello", image: require('./amap_start.png')}, {latitude: 39.902136, longitude: 116.44095, title: "end", subtile: "hello", image: require('./amap_end.png')}]}
    overlays={[{coordinates: [{latitude: 39.832136, longitude: 116.34095}, {latitude: 39.832136, longitude: 116.42095}, {latitude: 39.902136, longitude: 116.42095}, {latitude: 39.902136, longitude: 116.44095}], strokeColor: '#666666', lineWidth: 3}]}
  />
}
