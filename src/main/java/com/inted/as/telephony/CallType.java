package com.inted.as.telephony;

import java.util.HashMap;
import java.util.Map;

public enum CallType {

	VOICE_CALL(0), VIDEO_CALL(1), LEGACY_SMS(2), INSTANT_MESSAGE(3), FILE_TRANSFER(4), CONTENT_SHARE(
			5), LOCATION_EXCHANGE(6);

	private int value;

	private CallType(int value) {
		this.value = value;
	}

	// Mapping CallType to CallType Id
	private static final Map<Integer, CallType> map = new HashMap<Integer, CallType>();
	static {
		for (CallType idType : CallType.values())
			map.put(idType.value, idType);
	}

	/**
	 * Get CallType from value
	 * 
	 * @param value
	 *            Value
	 * @return CallType
	 */
	public static CallType getEValue(int value) {
		return map.get(value);
	}
}
