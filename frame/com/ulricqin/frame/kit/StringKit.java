package com.ulricqin.frame.kit;

import java.util.UUID;

public class StringKit {

	public static boolean isBlank(String val) {
		if (val == null || val.equals("") || val.trim().equals("")) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isNotBlank(String val) {
		return !isBlank(val);
	}

	public static String firstCharToLowerCase(String str) {
		Character firstChar = str.charAt(0);
		String tail = str.substring(1);
		str = Character.toLowerCase(firstChar) + tail;
		return str;
	}

	public static String firstCharToUpperCase(String str) {
		Character firstChar = str.charAt(0);
		String tail = str.substring(1);
		str = Character.toUpperCase(firstChar) + tail;
		return str;
	}
	
	public static String randomStr32() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
