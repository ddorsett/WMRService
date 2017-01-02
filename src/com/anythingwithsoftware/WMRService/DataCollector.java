/* 
 * DataCollector
 * Runnable thread that collects data from WMR100 through an USB/HID connection and passes frames to be parsed by the WMR100Command class
 *  
 * See the "LICENSE.txt" file for the full license terms and conditions governing this code.
 * */
package com.anythingwithsoftware.WMRService;

import java.util.Arrays;
import org.hid4java.HidDevice;

public class DataCollector extends Thread {
	private HidDevice wmr;
	private DataQueue queue;
	private long lastDataReceived = 0;
	private long commandCnt = 0;

	public DataCollector(HidDevice WMR, DataQueue q) {
		wmr = WMR;
		queue = q;
	}

	public long getLastDataReceived() { return lastDataReceived; }
	public long getCommandsReceived() { return commandCnt; }
	public void resetCommandCount() { commandCnt = 0; }
	public DataQueue getData() { return queue; }

	@Override
	public void run() {
		byte[] data = new byte[WMR100Constants.MAX_PACKET];
		int datalen = 0;
		byte rawdata[] = new byte[WMR100Constants.PACKET_LENGTH];

		// do this until we're interrupted or we have a device error
		WMRUtils.writeLogMessage("Starting data collection");
		boolean deviceErr = false;
		while (!deviceErr)
		{
			try {
				// read a frame, don't wait too long
				Arrays.fill(rawdata, (byte)0);
				int val = wmr.read(rawdata, 1000);
				switch (val) {
				case -1:
					WMRUtils.writeErrMessage("WMR read error: %s", wmr.getLastErrorMessage());
					deviceErr = true;
					break;
				case 0:
					break;
				default:
					lastDataReceived = System.currentTimeMillis();
					int len = rawdata[0];
					//WMRUtils.writeLogMessage(" frame len %d, raw [%02x %02x %02x %02x %02x %02x %02x %02x]", len, rawdata[0], rawdata[1], rawdata[2], rawdata[3], rawdata[4],rawdata[5],rawdata[6],rawdata[7],rawdata[8]);
					if (len > 7) {
						WMRUtils.writeErrMessage("Bad frame size!");
						break;
					}
					for (int i = 0; i < len; i++) {
						data[datalen++] = rawdata[1+i]; 
						// completed a command?
						if (datalen > 1 && data[datalen-1] == (byte)0xFF && data[datalen-2] == (byte)0xFF) {
							// skip empty/malformed commands
							if (datalen > 3) {
								WMR100Command cmd = new WMR100Command(data, datalen-2);
								++commandCnt;
								// unpack all the item values from the command and add them to the data queue
								cmd.updateDataQueue(queue);
							}
							Arrays.fill(data, (byte)0);
							datalen = 0;
						}
					}
				}
			} catch (Exception e) {
				WMRUtils.writeErrMessage("DataCollector exception: %s", e.getMessage());
				e.printStackTrace(System.err);
				break;
			} 
			if (Thread.interrupted()) {
				break;
			}
		}
		WMRUtils.writeLogMessage("Stopping data collection");
	}

}
