package com.mentor.ems.security.spi;

import com.mentor.ems.security.exceptions.SecurityException;

public interface HashGenerator {

	String generateSHA2Hash(String data) throws SecurityException;
	
	byte[] generateSHA2Hash(byte[] data) throws SecurityException;
	
	String generateSHAHash(String data) throws SecurityException;
	
	byte[] generateSHAHash(byte[] data) throws SecurityException;
	
	String generateHash(String data, String algorithm) throws SecurityException;
	
	byte[] generateHash(byte[] data, String algorithm) throws SecurityException;
}
