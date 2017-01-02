/* 
 * WMR100Device
 * Talking to a WMR100 attached as a USB device through the HID interface  
 *  
 * See the "LICENSE.txt" file for the full license terms and conditions governing this code.
 * 
 * */

package com.anythingwithsoftware.WMRService;

import java.util.Properties;
import org.hid4java.HidDevice;
import org.hid4java.HidException;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesListener;
import org.hid4java.event.HidServicesEvent;

public class WMR100Device implements HidServicesListener {
	private HidServices hidServices;
	private HidDevice wmr = null;
	private DataCollector wmrDataCollector = null;
	private DataQueue wmrDataItemQueue = null;
	private Properties props = null;

	public WMR100Device(Properties props) {
		this.props = props;
	}

	public boolean initializeWMR() {
		WMRUtils.writeLogMessage("Initializing WMR");

		try {
			// find the WMR as a HID device
			hidServices = HidManager.getHidServices();
			hidServices.addHidServicesListener(this);
			wmr = hidServices.getHidDevice(WMR100Constants.DEFAULT_STATION_VENDOR, WMR100Constants.DEFAULT_STATION_PRODUCT, null);

			if (wmr != null) {

				// Send the initialize message to WMR
				if (wmr.write(WMR100Constants.STATION_INITIALIZATION, WMR100Constants.STATION_INITIALIZATION.length, (byte) 0) == -1) {
					WMRUtils.writeErrMessage("WMR initialization error: %s", wmr.getLastErrorMessage());
				}

				// initialize the item data queue
				wmrDataItemQueue = new DataQueue(props);

				// start data collector in a separate thread
				wmrDataCollector = new DataCollector(wmr, wmrDataItemQueue);
				// note: this thread state is monitored in WMRService.main() 
				wmrDataCollector.start();
				return true;

			} else {

				WMRUtils.writeErrMessage("WMR100 not found! Attached devices: ");
				for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
					System.err.println(hidDevice.toString());
				}
				return false;

			}
		} catch (HidException e) {
			WMRUtils.writeErrMessage("HID exception: %s", e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public void deinitializeWMR() {
		WMRUtils.writeLogMessage("Deinitializing WMR");

		if (wmrDataCollector.getState() != Thread.State.TERMINATED) {
			wmrDataCollector.interrupt();
			try {
				wmrDataCollector.join(1500);
			} catch (InterruptedException e) {
				WMRUtils.writeErrMessage("Unable to interrupt data collector");
			}
		}
		if (wmr != null && wmr.isOpen()) {
			try {
				wmr.close();
				WMRUtils.writeLogMessage("WMR100 device closed.");
			} catch (Exception e) {
				WMRUtils.writeErrMessage("Could not properly close WMR device");
			}
		}
	}

	public DataCollector getDataCollector() {
		return wmrDataCollector;
	}

	// *********************************************************************************************
	// *********************************************************************************************
	// handle HID events

	@Override
	public void hidDeviceAttached(HidServicesEvent event) {
		if (event.getHidDevice().getVendorId() == WMR100Constants.DEFAULT_STATION_VENDOR && event.getHidDevice().getProductId() == WMR100Constants.DEFAULT_STATION_PRODUCT) {
			initializeWMR();
		}
	}

	@Override
	public void hidDeviceDetached(HidServicesEvent event) {
		if (event.getHidDevice().getVendorId() == WMR100Constants.DEFAULT_STATION_VENDOR && event.getHidDevice().getProductId() == WMR100Constants.DEFAULT_STATION_PRODUCT) {
			deinitializeWMR();
		}
	}

	@Override
	public void hidFailure(HidServicesEvent event) {
		WMRUtils.writeErrMessage("HID failure: ", event.toString());
		deinitializeWMR();
		initializeWMR();
	}

}
