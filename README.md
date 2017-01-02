# WMRService

##Installation
1. Prerequisites:
    * Windows (Windows 7+ thought to work, Windows 10 tested).
    * WMR-100 or compatible device plugged in via USB to the Windows computer.
    * MQTT broker running and network accessible.
    * Java JRE 8 (1.8) or later.
1. Copy WMRService.jar and WMRService.properties to a folder.
    * The provided WMRService.jar file includes the JAR dependencies, nothing else to copy/install.
1. Edit WMRService.properties to provide MQTT broker connection information:
    * `MQTTbroker` tcp address for the MQTT broker, e.g. `tcp://mybroker:1883`
    * `MQTTuser` username if MQTT broker is using username/password authentication, blank otherwise
    * `MQTTpassword` password for MQTT broker authentication
1. Edit WMRService.properties to provide optional values for:
    * `MQTTrootTopic` root topic for MQTT messages from the WMRService, default value is `WMR100`
    * `MQTTclientId` MQTT client ID for publisher, default value is `WMR100`
    * `monitoringInterval` number of seconds between device monitoring output written to the console. This is also used to detect if the WMR has gone dead. Default value is `30`
    * `minCommandsInInterval` minimum number of commands expecting in a monitoring interval, used to determine if the WMR device is still alive and sending. Default is `5` (empirically appropriate for a monitoringInternval value of 30)
    * `maxReportingRate` number of seconds between sensor value messages (implements throttling). Default is 60, which means sensor values are sent no more frequently than every 60 seconds for each sensor. Setting to 0 removes throttling, meaning that as soon as the WMR reports a sensor value it is sent. There is no guarantee on when the WMR sends sensor values.
    * `tempUnits` units for temperature sensors, either F or C (case insensitive). Default is `C`
    * `windSpeedUnits` units for wind speed sensors, either mph, kt, or mps (case insensitive). Default is `mps`
    * `pressureUnits` units for pressure sensors, either mmhg, inhg, or mbar (case insensitive). Default is `mbar`
    * `rainUnits` units for rain sensors, either in or mm (case insensitive). Default is `in`
1. Start the service
    * Run the JAR file from the command line: `java -jar WMRService.jar`
    * Or install as a service using the [NSSM utility](https://nssm.cc/) to create a Windows service that runs automatically on startup
        * From the command line: `nssm.exe install WMRService` 
        * In the installation dialog:
            * set __Application path__ to the java.exe from the installed JRE
            * set __Startup directory__ to the directory with WMRService.jar
            * set __Arguments_ to `-jar WMRservice.jar`
            * on the __Details__ tab provide a nice Display name and Description
            * on the __I/O tab__ specify a file for Output and Error redirection (e.g. WMRService.out and WMRService.err)
            * on the __File rotation__ tab check Rotate Files and Rotate while service is running and use 86400 secs for rotation 1x per day		   

##Building
1. The 3 dependent JARs must be on the classpath
    * hid4java.jar- [hid4java](http://github.com/gary-rowe/hid4java) supports USB HID devices through a cross-platform API
    * jna-4.0.0.jar - [Java Native Access](https://github.com/java-native-access/jna) library required by hid4java
    * org.eclipse.paho.client.mqttv3_1.1.0.jar - [Eclipse Paho MQTT client](https://eclipse.org/paho/clients/java/)
    
