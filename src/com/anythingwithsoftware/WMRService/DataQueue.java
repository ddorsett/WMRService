/* 
 * DataQueue
 * Receives parsed WMR100 commands and passes them along.
 * Originally writeen against OpenHAB 1.x API as a binding, this version passes commands to MQTT with throttling to not
 * allow the MQTT subscribers to get overwhelmed.
 * The broker configuration (including the units to use for message payloads) are specified in the properties passed on
 * construction.  
 *  
 * See the "LICENSE.txt" file for the full license terms and conditions governing this code.
 * */
package com.anythingwithsoftware.WMRService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

// This version of DataQueue passes WMR data values to MQTT
public class DataQueue {
	private String MQTTrootTopic;
	private String MQTTbroker;
	private String MQTTclientId;
	private String MQTTuser;
	private String MQTTpassword;
	private long maxReportingRate;
	private int messageCnt = 0;
	public int getMessageCount() { return messageCnt; }
	public void resetMessageCount() { messageCnt = 0; }
	private Map<String, Long> lastMsgSent = new HashMap<String, Long>();

	public enum tempUnits {
		C, 
		F
	};
	private tempUnits tempUnit = tempUnits.C;
	public tempUnits getTempUnit() {
		return tempUnit;
	}

	public enum windSpeedUnits {
		MPS, 
		MPH,
		KT
	};
	private windSpeedUnits windSpeedUnit = windSpeedUnits.MPS;
	public windSpeedUnits getWindSpeedUnit() {
		return windSpeedUnit;
	}

	public enum pressureUnits {
		MBAR, 
		MMHG,
		INHG
	};
	private pressureUnits pressureUnit = pressureUnits.MBAR;
	public pressureUnits getPressureUnit() {
		return pressureUnit;
	}

	public enum rainUnits {
		IN, 
		MM
	};
	private rainUnits rainUnit = rainUnits.IN;
	public rainUnits getRainUnit() {
		return rainUnit;
	}

	public DataQueue(Properties props) {
		MQTTrootTopic = props.getProperty("MQTTrootTopic", "WMR100");
		MQTTclientId = props.getProperty("MQTTclientId", "WMR100");
		MQTTbroker = props.getProperty("MQTTbroker", "");
		MQTTuser= props.getProperty("MQTTuser", "");
		MQTTpassword = props.getProperty("MQTTpassword", "");
		if (MQTTbroker.length() == 0) 
			WMRUtils.writeErrMessage("MQTT broker not configured correctly");

		tempUnit = (props.getProperty("tempUnits","").compareToIgnoreCase("f") == 0 ? tempUnits.F : tempUnits.C);
		windSpeedUnit = (props.getProperty("windSpeedUnits","").compareToIgnoreCase("mph") == 0 ? windSpeedUnits.MPH : 
			(props.getProperty("windSpeedUnits","").compareToIgnoreCase("kt") == 0 ? windSpeedUnits.KT : windSpeedUnits.MPS));
		pressureUnit = (props.getProperty("pressureUnits","").compareToIgnoreCase("mmhg") == 0 ? pressureUnits.MMHG: 
			(props.getProperty("pressureUnits","").compareToIgnoreCase("inhg") == 0 ? pressureUnits.INHG : pressureUnits.MBAR));
		rainUnit = (props.getProperty("rainUnits","").compareToIgnoreCase("mm") == 0 ? rainUnits.MM : rainUnits.IN);

		maxReportingRate = Integer.parseInt(props.getProperty("maxReportingRate", "60")) * 1000;
	}



	public void addValue(String item, Object value) {
		if (MQTTbroker.length() == 0) {
			return;
		}

		long currentTime = (new Date()).getTime();
		Long lastTime = lastMsgSent.get(item);
		if (lastTime == null || currentTime > lastTime.longValue() + maxReportingRate) {
			try {
				MqttClient sampleClient = new MqttClient(MQTTbroker, MQTTclientId);
				MqttConnectOptions opt = new MqttConnectOptions();
				opt.setAutomaticReconnect(true);
				opt.setCleanSession(true);
				if (MQTTuser.length() > 0 && MQTTpassword.length() > 0) {
					opt.setUserName(MQTTuser);
					opt.setUserName(MQTTpassword);
				}
				sampleClient.connect();
				MqttMessage message = new MqttMessage();
				message.setPayload(value.toString().getBytes());
				message.setRetained(true);
				String fullTopic = String.format("%s/%s", MQTTrootTopic, item);
				sampleClient.publish(fullTopic, message);
				sampleClient.disconnect();
				++messageCnt;
				lastMsgSent.put(item, new Long(currentTime));
			} catch(MqttException me) {
				// see https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttException.html
				if (me.getReasonCode() == MqttException.REASON_CODE_CLIENT_EXCEPTION) {
					WMRUtils.writeErrMessage("MQTT client exception cause %s ", me.getCause().toString());
				} else if (me.getReasonCode() == MqttException.REASON_CODE_CONNECTION_LOST) {
					WMRUtils.writeErrMessage("MQTT connection lost exception cause %s ", me.getCause().toString());
				} else {
					WMRUtils.writeErrMessage("MQTT exception reason code %d", me.getReasonCode());
				}
			};
		}
	}

}
