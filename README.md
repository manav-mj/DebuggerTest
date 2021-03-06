# Test Android Debugger using JDI
### Purpose
This android debugger attaches to a waiting android virtual machine and watches the mouseX variable displaying the old and new value of the variable in the console. This Debugger is made as a part of GSoC'18 as a basic prototype for [Android Debugger for PDE](https://summerofcode.withgoogle.com/projects/#5029506465660928)

### Dependencies
1. adb tools and jdk pre installed

### How to use
1. first install an android application (must use mouseX variable of p5.js)
[[example PDE Sketch](https://github.com/manav-mj/DebuggerTest/blob/master/androidsketch/androidsketch.pde)]
2. Enable developer options in handset
3. Goto `Developer options` in handset
4. Click `Select debug app`
5. Select your installed debuggable application
6. enable `Wait for debugger`
7. now run the android app
8. a dialogue showing waiting for debugger will be displayed do not close
9. open terminal or cmd in your debugger machine
10. type `adb devices`
11. type `adb jdwp` and press ctrl/command + c
12. type `adb forward tcp:7777 jdwp:[last pid in above command output]`
13. now open the java application in your preffered editor and open `FieldMonitor.java` 
change the 
`PKG_NAME` to the package name of your android app
`CLASS_NAME` to the class containing mouseX

14. now run the java application

### Output Screenshot
![capture](https://user-images.githubusercontent.com/22222147/40159836-a1ff5214-59c8-11e8-90fc-01cbb2972274.PNG)
