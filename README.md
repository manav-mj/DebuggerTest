# Test Android Debugger using JDI
### What it does
This android debugger attaches to a waiting android virtual machine and watches the mouseX variable displaying the old and new value of the variable in the console.

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
12. type `adb forward tcp:7777 jdwp:[last pid in above command output]
13. now open the java application in your preffered editor and open `FieldMonitor.java` 
change the 
`PKG_NAME` to the package name of your android app
`CLASS_NAME` to the class containing mouseX

14. now run the java application
