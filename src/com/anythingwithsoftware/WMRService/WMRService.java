/* 
 * WMRService
 * Wraps together the WMR100 device listener, command parser, publication to MQTT broker in a command-runnable class
 *  
 * See the "LICENSE.txt" file for the full license terms and conditions governing this code.
 * */

package com.anythingwithsoftware.WMRService;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class WMRService {

	public static void main(String[] args) {
		WMRUtils.writeLogMessage("WMRService version %s started", WMR100Constants.VERSION);
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = WMRService.class.getClassLoader().getResourceAsStream("WMRService.properties");
			if (input == null) {
				WMRUtils.writeErrMessage("Unable to find WMRService.properties configuration file");
				return;
			}
			prop.load(input);
			int monitoringInterval = Integer
					.parseInt(prop.getProperty("monitoringInterval", WMR100Constants.DEFAULT_MONITORINGINTERVAL));
			int minCommandsInInterval = Integer
					.parseInt(prop.getProperty("minCommandsInInterval", WMR100Constants.DEFAULT_CMDSPERINTERVAL));

			WMR100Device device = new WMR100Device(prop);
			if (!device.initializeWMR())
				return;

			// data collection thread is now running
			int quietTimes = 0;
			while (!Thread.interrupted()) {
				DataCollector dc = device.getDataCollector();

				// if the data collection thread gets terminated
				// (exception/device error), try to restart it
				if (dc.getState() == Thread.State.TERMINATED) {
					device.deinitializeWMR();
					if (!device.initializeWMR())
						return;

				} else {
					DataQueue dq = dc.getData();
					if (dc.getCommandsReceived() < minCommandsInInterval) {
						WMRUtils.writeErrMessage("WMR100 has gone quiet");
						++quietTimes;
						if (quietTimes > 5) {
							WMRUtils.writeErrMessage("5 successive quiet failures: trying to restart WMR100");
							device.deinitializeWMR();
							device.initializeWMR();
						}
					} else {
						quietTimes = 0;
						String t = (new SimpleDateFormat("HH:mm:ss.SS")).format(new Date(dc.getLastDataReceived()));
						WMRUtils.writeLogMessage(
								"WMR100 last command received: %s, %d commands received, %d data updates sent", t,
								dc.getCommandsReceived(), dq.getMessageCount());
						dc.resetCommandCount();
						dq.resetMessageCount();
					}
					Thread.sleep(monitoringInterval * 1000);
				}
			}

			device.deinitializeWMR();

		} catch (Exception ex) {
			WMRUtils.writeErrMessage("WMRService exception: %s", ex.toString());
			ex.printStackTrace(System.err);
		}

	}

}
