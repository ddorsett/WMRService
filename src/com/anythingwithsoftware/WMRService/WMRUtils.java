/* 
 * WMRUtils
 * Various static utilities. 
 *  
 * See the "LICENSE.txt" file for the full license terms and conditions governing this code.
 */ 

package com.anythingwithsoftware.WMRService;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.anythingwithsoftware.WMRService.WMR100Constants;

/**
 * 
 */
public class WMRUtils {

	/**
	 * Converts an array of bytes to a human readable string.
	 * @param a
	 * @return
	 */
	public static String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for(byte b: a)
			sb.append(String.format("%02x ", b&0xff));
		return sb.toString();
	}

	/**
	 * Return integer value of byte.
	 * 
	 * @param value
	 *            byte value
	 * @return integer value
	 */
	public static int getInt(byte value) {
		return (value & 0xFF); // return bottom 8 bits
	}

	/**
	 * @param batteryByte
	 * @return true if battery is on/good
	 */
	public static boolean isBatteryOn(byte battery) {
		return ((battery & 0x40) == 0);
	}

	public static boolean isStationPowered(byte station) {
		return ((station & 0x80) == 0);
	}

	public static String getStationRF(byte station) {
		if ((station & 0x10) != 0) {
			return ((station & 0x08) != 0 ? "Strong" : "Weak" );
		}
		return "Inactive";
	}

	public static String getCompass(int degrees) {
		int index = (int)(((degrees > 348) ? (double)(degrees) : (double)(degrees + 11.24)) / 22.5);
		return (index >= 0 && index < 16) ? WMR100Constants.COMPASS_DIRECTION[index] : "Unknown";
	}

	public static String getUV(int uvCode) {
		int uvIndex = (uvCode >= 11 ? 4 : uvCode >= 8 ? 3 : uvCode >= 6 ? 2 : uvCode >= 3 ? 1 : 0);
		return (WMR100Constants.UV_DESCRIPTION[uvIndex]); 
	}

	public static Double heatIndex(double tempC, double rh) {
		// NOAA calculation (https://en.wikipedia.org/wiki/Heat_index) - in degF, convert back!!
		if (tempC >= 26.7) {
			double t = (tempC * 9.0 / 5.0) + 32.0;
			double HI = (-42.379 + (2.04901523 * t) + (10.14333127 * rh) - (0.22475541 * t * rh) - (.00683783 * Math.pow(t, 2)) 
					- (0.05481717 * Math.pow(rh,2)) + (0.00122874 * Math.pow(t,2) * rh) + (0.00085282 * t * Math.pow(rh, 2))
					- (0.00000199 * Math.pow(t, 2) * Math.pow(rh, 2)) );
			return new Double((HI - 32.0) * 5.0 / 9.0);
		}
		return new Double(Double.NaN);
	}

	// http://andrew.rsmas.miami.edu/bmcnoldy/Humidity.html
	public static double dewPoint(double tempC, double rh) {
		return ( 243.04 * (Math.log(rh/100.0) + ((17.625*tempC) / (243.04+tempC)))
				/ (17.625-Math.log(rh/100.0) - ((17.625*tempC) / (243.04+tempC) ))); 
	}

	// https://en.wikipedia.org/wiki/Wind_chill
	public static Double windChill(double tempC, double wind) {
		wind = wind * 3600.0 / 1000.0; // kph
		if (tempC <= 10 && wind >= 4.8 ) { 
			return new Double( 13.12 + (0.6215 * tempC) - (11.37 * Math.pow(wind, 0.16)) + (0.3965 * tempC * Math.pow(wind, 0.16)));
		}
		return new Double(Double.NaN);
	}

	public static String getBeaufortScale(double wind) {
		// https://en.wikipedia.org/wiki/Beaufort_scale (in mps)
		if (wind < 0.3) {
			return "Calm (force 0)";
		} else if (wind < 1.5) {
			return "Light Air (f1)";
		} else if (wind < 3.3) {
			return "Light Breeze (f2)";
		} else if (wind < 5.5) {
			return "Gentle Breeze (f3)";
		} else if (wind < 7.9) {
			return "Moderate Breeze (f4)";
		} else if (wind < 10.7) {
			return "Fresh Breeze (f5)";
		} else if (wind < 13.8) {
			return "Strong Breeze (f6)";
		} else if (wind < 17.1) {
			return "Near Gale (f7)";
		} else if (wind < 20.7) {
			return "Gale (f8)";
		} else if (wind < 24.4) {
			return "Strong Gale (f9)";
		} else if (wind < 28.4) {
			return "Storm (f10)";
		} else if (wind < 32.6) {
			return "Violent Storm (f11)";
		} else {
			return "Hurricane (f12)";
		}
	}

	public static void writeLogMessage(String format, Object... arguments) {
		String msg = String.format(format, arguments);
		System.out.println(String.format("%s - %s", new SimpleDateFormat("MM-dd-yy HH:mm.ss").format(new Date()), msg));
	}

	public static void writeErrMessage(String format, Object... arguments) {
		String msg = String.format(format, arguments);
		System.err.println(String.format("%s - %s", new SimpleDateFormat("MM-dd-yy HH:mm.ss").format(new Date()), msg));
	}
}

