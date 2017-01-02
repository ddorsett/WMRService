/* 
 * WMR100Constants
 *  
 * See the "LICENSE.txt" file for the full license terms and conditions governing this code.
 * */
package com.anythingwithsoftware.WMRService;

public interface WMR100Constants {
	static final String VERSION = "0.3.1 (2-Jan-2017)";

	// USB communications
	static final int DEFAULT_STATION_VENDOR = 0x0FDE;
	static final int DEFAULT_STATION_PRODUCT = 0xFFFFCA01;  //weird sign extension thing...
	static final String DEFAULT_MONITORINGINTERVAL = "30";
	static final String DEFAULT_CMDSPERINTERVAL = "5";
	static final int PACKET_LENGTH = 9;
	static final int MAX_PACKET = 25;
	static final byte[] STATION_INITIALIZATION = { (byte) 0x00,
			(byte) 0x20, (byte) 0x00, (byte) 0x08, (byte) 0x01, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00 };

	// sensor types
	static final byte SENSOR_TIMESTAMP = 0x60;
	static final byte SENSOR_TEMP = 0x42;
	static final byte SENSOR_WATERTEMP = 0x44;
	static final byte SENSOR_WIND = 0x48;
	static final byte SENSOR_PRESSURE = 0x46;
	static final byte SENSOR_RAIN = 0x41;
	static final byte SENSOR_UV = 0x47;

	// item names
	static final String ITEM_TEMPERATURE = "temperature"; 						// degC
	static final String ITEM_HUMIDITY = "humidity"; 							// %
	static final String ITEM_HEATINDEX = "temperatureHeatIndex";				// degC
	static final String ITEM_DEWPOINT = "temperatureDewPoint"; 					// degC
	static final String ITEM_WINDCHILL = "temperatureWindChill"; 				// degC
	static final String ITEM_TEMPERATURE_BATTERY = "temperatureBattery";		// 1=good
	static final String ITEM_WIND_DIRECTION = "windDirection";					// degrees (0-360)
	static final String ITEM_WIND_COMPASSDIRECTION = "windCompassDirection";	// 3 letter compass direction
	static final String ITEM_WIND_GUST = "windGust";							// mps
	static final String ITEM_WIND_SPEED = "windSpeed";							// mps
	static final String ITEM_WIND_BEAUFORTSCALE= "windBeaufortScale";			// string Beaufort scale
	static final String ITEM_WIND_BATTERY = "windBattery";						// 1=good
	static final String ITEM_PRESSURE = "pressure";								// mbar (hPa)
	static final String ITEM_RAIN_RATE = "rainRate";							// in/hr
	static final String ITEM_RAIN_LASTHOUR = "rainLastHour";					// in
	static final String ITEM_RAIN_LAST24HOURS = "rainLast24Hours";				// in
	static final String ITEM_RAIN_BATTERY = "rainBattery";						// 1=good
	static final String ITEM_UVINDEX = "UVIndex";								// 0-2/3-5/6-7/8-10/>10
	static final String ITEM_UVDESCRIPTION = "UVDescription";					// "Low", "Medium", "High", "Very High", "Extremely High"
	static final String ITEM_UV_BATTERY = "UVBattery";							// 1= good
	static final String ITEM_RFSIGNAL = "RFSignal";								// "Weak" "Strong" "Inactive"
	static final String ITEM_STATIONPOWER = "stationPower";						// 1 = on power
	static final String ITEM_STATIONBATTERY = "stationBattery";					// 1 = on battery

	// units
	static final String[] COMPASS_DIRECTION = { "N", "NNE", "NE", "ENE",
			"E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW",
			"NNW" };
	static final String[] UV_DESCRIPTION = { "Low", "Medium", "High",
			"Very High", "Extremely High" };

}