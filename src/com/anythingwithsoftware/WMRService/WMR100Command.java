/* 
 * WMR100Command
 * Class that represents a parsed WMR100 command to pass along specific item values to the DataQueue
 *  
 * See the "LICENSE.txt" file for the full license terms and conditions governing this code.
 * */
package com.anythingwithsoftware.WMRService;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * represents a WMR command 
 *
 */
public class WMR100Command {

	private byte[] data = new byte[WMR100Constants.MAX_PACKET];
	private int cmdLen = 0;
	private boolean valid = true;
	private long timestamp = 0;

	public WMR100Command(byte[] cmd, int len) {
		System.arraycopy(cmd, 0, data, 0, len);
		cmdLen = len;
		timestamp = System.currentTimeMillis();
		validate();
	}

	/**
	 * set the command internal data
	 * @param cmd command bytes
	 * @param len command length
	 */
	public void setData(byte[] cmd, int len) {
		System.arraycopy(cmd, 0, data, 0, cmd.length);
		cmdLen = len;
		validate();
	}

	/**
	 * @return raw command data
	 */
	public byte[] getRawData() {
		return data;
	}

	/**
	 * @return length of command
	 */
	public int rawDataLength() { return cmdLen - 2; }

	/**
	 * @return timestamp (msec) command was received
	 */
	public long getTimestamp() { return timestamp; }

	/**
	 * @return true of command is valid for WMR100
	 */
	public boolean isValid() { return valid; }

	/**
	 * @return validate the command for WMR100
	 */
	public boolean validate() {
		valid = true;
		if (cmdLen == 0) return true; // clear
		if (cmdLen < 4) { valid = false; return false; } // must have 1 byte cmd, 2 byte checksum

		// checksum
		long checkSum = 0;
		for (int i = 0; i < cmdLen - 2; i++) {
			checkSum = checkSum + Byte.toUnsignedInt(data[i]);
		}
		long frameVal = Byte.toUnsignedInt(data[cmdLen-2]) + (Byte.toUnsignedInt(data[cmdLen-1]) << 8);
		valid = (checkSum == frameVal); 
		if (!valid) { 
			WMRUtils.writeErrMessage("Checksum error- Calculated: %d, frame val: %d, sensor %02x", checkSum, frameVal, data[1]);
		} else {
			// now check for expected command data sizes
			switch (data[1]) {
			case WMR100Constants.SENSOR_TIMESTAMP:
				valid = (cmdLen == 12);
				break;
			case WMR100Constants.SENSOR_TEMP:
				valid = (cmdLen == 12);
				break;
			case WMR100Constants.SENSOR_WATERTEMP:
				valid = (cmdLen == 7);
				break;
			case WMR100Constants.SENSOR_WIND:
				valid = (cmdLen == 11);
				break;
			case WMR100Constants.SENSOR_PRESSURE:
				valid = (cmdLen == 8);
				break;
			case WMR100Constants.SENSOR_RAIN:
				valid = (cmdLen == 17);
				break;
			case WMR100Constants.SENSOR_UV:
				valid = (cmdLen == 6);
				break;
			default:
				WMRUtils.writeErrMessage("Unexpected sensor %02x", data[1]);
				valid = false;
				break;
			}
			if (!valid)
				WMRUtils.writeErrMessage("Unexpected command length: %d for sensor %02x", cmdLen, data[1]);

		}		
		return valid;
	}

	/**
	 * @return sensor type (WMRConstants.SENSOR_ constants)
	 */
	public byte getSensor() {
		return (valid ? data[1] : 0);
	}

	/**
	 * Unpack all the possible item values from the WMR command and put them into the data queue
	 * Note that this doesn't care whether the items are defined in configuration
	 * @param cmd
	 */
	// this is the previous value used for wind chill calculations (only refreshed when temperatures are reported)
	static double lastWindSpeed = 0;  // in mps!
	public void updateDataQueue(DataQueue queue) {
		if (isValid() == false) return;
		// WMRUtils.writeLogMessage("Parsing command into item values: %s", toString());
		double val;
		switch (getSensor()) {
		case WMR100Constants.SENSOR_PRESSURE:
			val = (256*(0x0F & data[3])) + WMRUtils.getInt(data[2]);
			if (queue.getPressureUnit() == DataQueue.pressureUnits.MMHG)
				val = val * 0.7500615613;
			if (queue.getPressureUnit() == DataQueue.pressureUnits.INHG)
				val = val * 0.029529983071;
			queue.addValue(WMR100Constants.ITEM_PRESSURE, new Double(val));
			break;
		case WMR100Constants.SENSOR_RAIN:
			val = ((256*WMRUtils.getInt(data[3])) + WMRUtils.getInt(data[2])) / 100.0;
			if (queue.getRainUnit() == DataQueue.rainUnits.MM)
				val = val * 2.54;
			queue.addValue(WMR100Constants.ITEM_RAIN_RATE, new Double(val));
			val = ((256*WMRUtils.getInt(data[5])) + WMRUtils.getInt(data[4])) / 100.0;
			if (queue.getRainUnit() == DataQueue.rainUnits.MM)
				val = val * 2.54;
			queue.addValue(WMR100Constants.ITEM_RAIN_LASTHOUR, new Double(val));
			val = ((256*WMRUtils.getInt(data[7]))+WMRUtils.getInt(data[6])) / 100.0;
			if (queue.getRainUnit() == DataQueue.rainUnits.MM)
				val = val * 2.54;
			queue.addValue(WMR100Constants.ITEM_RAIN_LAST24HOURS, new Double(val));
			queue.addValue(WMR100Constants.ITEM_RAIN_BATTERY, new Boolean(WMRUtils.isBatteryOn(data[0])));
			break;
		case WMR100Constants.SENSOR_TEMP:
			// temp sensors are multi-channel
			val = ((256*(0x0F&data[4])) + WMRUtils.getInt(data[3])) / 10.0;
			if ((data[4] & 0x80) != 0) val *= -1;
			int rh = WMRUtils.getInt(data[5]);
			queue.addValue(String.format("%s/%d", WMR100Constants.ITEM_HUMIDITY, (data[2] & 0x0F)), new Integer(rh));
			double val2 = WMRUtils.heatIndex(val, rh);
			if (queue.getTempUnit() == DataQueue.tempUnits.F)
				val2 = (val2* 9.0 / 5.0) + 32.0;
			queue.addValue(String.format("%s/%d", WMR100Constants.ITEM_HEATINDEX, (data[2] & 0x0F)), new Double(val2));
			val2 = WMRUtils.windChill(val, lastWindSpeed);
			if (queue.getTempUnit() == DataQueue.tempUnits.F)
				val2 = (val2* 9.0 / 5.0) + 32.0;
			queue.addValue(String.format("%s/%d", WMR100Constants.ITEM_WINDCHILL, (data[2] & 0x0F)), new Double(val2));
			val2 = WMRUtils.dewPoint(val, rh);
			if (queue.getTempUnit() == DataQueue.tempUnits.F)
				val2 = (val2* 9.0 / 5.0) + 32.0;
			queue.addValue(String.format("%s/%d", WMR100Constants.ITEM_DEWPOINT, (data[2] & 0x0F)), new Double(val2));
			if (queue.getTempUnit() == DataQueue.tempUnits.F)
				val = (val * 9.0 / 5.0) + 32.0;
			queue.addValue(String.format("%s/%d", WMR100Constants.ITEM_TEMPERATURE, (data[2] & 0x0F)), new Double(val));
			queue.addValue(String.format("%s/%d", WMR100Constants.ITEM_TEMPERATURE_BATTERY, (data[2] & 0x0F)), new Boolean(WMRUtils.isBatteryOn(data[0])));
			break;
		case WMR100Constants.SENSOR_WATERTEMP:
			// temp sensors are multi-channel (TMWR800 only 1,2 or 3)
			val = ((256*(0x0F&data[4])) + WMRUtils.getInt(data[3])) / 10.0;
			if ((data[4] & 0x80) != 0) val *= -1;
			if (queue.getTempUnit() == DataQueue.tempUnits.F)
				val = (val * 9.0 / 5.0) + 32.0;
			queue.addValue(String.format("%s/%d", WMR100Constants.ITEM_TEMPERATURE, (data[2] & 0x0F)), new Double(val));
			queue.addValue(String.format("%s/%d", WMR100Constants.ITEM_TEMPERATURE_BATTERY, (data[2] & 0x0F)), new Boolean(WMRUtils.isBatteryOn(data[0])));
			break;
		case WMR100Constants.SENSOR_TIMESTAMP:
			queue.addValue(WMR100Constants.ITEM_RFSIGNAL, WMRUtils.getStationRF(data[0]));
			queue.addValue(WMR100Constants.ITEM_STATIONPOWER, new Boolean(WMRUtils.isStationPowered(data[0])));
			queue.addValue(WMR100Constants.ITEM_STATIONBATTERY, new Boolean(WMRUtils.isBatteryOn(data[0])));
			break;
		case WMR100Constants.SENSOR_UV:
			queue.addValue(WMR100Constants.ITEM_UVINDEX, new Integer(WMRUtils.getInt(data[3])));
			queue.addValue(WMR100Constants.ITEM_UVDESCRIPTION, WMRUtils.getUV(WMRUtils.getInt(data[3])));
			queue.addValue(WMR100Constants.ITEM_UV_BATTERY, new Boolean(WMRUtils.isBatteryOn(data[0])));
			break;
		case WMR100Constants.SENSOR_WIND:
			int deg = (data[2] * 360)/16;
			queue.addValue(WMR100Constants.ITEM_WIND_DIRECTION, new Integer(deg));
			queue.addValue(WMR100Constants.ITEM_WIND_COMPASSDIRECTION, WMRUtils.getCompass(deg));

			double wind = ((256*(0x0F&data[5])) + WMRUtils.getInt(data[4])) / 10.0;
			if (queue.getWindSpeedUnit() == DataQueue.windSpeedUnits.MPH)
				wind = wind * 2.23694;
			if (queue.getWindSpeedUnit() == DataQueue.windSpeedUnits.KT)
				wind = wind * 1.94384;
			queue.addValue(WMR100Constants.ITEM_WIND_GUST, new Double(wind));

			wind = ((16*WMRUtils.getInt(data[6])) + ((0xF0&data[5])>>4)) / 10.0;
			lastWindSpeed = wind;
			queue.addValue(WMR100Constants.ITEM_WIND_BEAUFORTSCALE, WMRUtils.getBeaufortScale(wind));
			if (queue.getWindSpeedUnit() == DataQueue.windSpeedUnits.MPH)
				wind = wind * 2.23694;
			if (queue.getWindSpeedUnit() == DataQueue.windSpeedUnits.KT)
				wind = wind * 1.94384;
			queue.addValue(WMR100Constants.ITEM_WIND_SPEED, new Double(wind));
			queue.addValue(WMR100Constants.ITEM_WIND_BATTERY, new Boolean(WMRUtils.isBatteryOn(data[0])));
			break;
		}
	}

	@Override
	public String toString() {
		String t = (new SimpleDateFormat( "HH:mm:ss.SS" )).format(new Date(timestamp));
		return String.format("%s: valid: %b len: %d [%s]", t, valid, cmdLen, WMRUtils.byteArrayToHex(data));
	}

	public void logCommand() {
		System.out.println(toString());
	}
}
