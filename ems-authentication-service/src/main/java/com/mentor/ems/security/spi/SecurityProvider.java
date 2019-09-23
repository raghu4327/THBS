package com.mentor.ems.security.spi;

import com.mentor.ems.security.exceptions.SecurityException;

public interface SecurityProvider {
	
	public String encrypt(String plainData, String key) throws SecurityException;
	
	public String decrypt(String encData, String key) throws SecurityException;
	

}
