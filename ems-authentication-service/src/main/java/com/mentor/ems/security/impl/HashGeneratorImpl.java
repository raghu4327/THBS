package com.mentor.ems.security.impl;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import org.apache.log4j.Logger;

import com.mentor.ems.security.exceptions.SecurityException;
import com.mentor.ems.security.spi.HashGenerator;
import com.mentor.ems.security.util.SecurityConstants;
import com.mentor.ems.security.util.Util;

public class HashGeneratorImpl implements HashGenerator {
	
	private static final Logger Log = Logger.getLogger(HashGeneratorImpl.class);

	@Override
	public String generateSHA2Hash(String data) throws SecurityException {
		return Util.encodeBase32(calculateHash(data.getBytes(), SecurityConstants.SHA2_HASH_ALGO));
	}

	@Override
	public byte[] generateSHA2Hash(byte[] data) throws SecurityException {
		return calculateHash(data, SecurityConstants.SHA2_HASH_ALGO);
	}

	@Override
	public String generateSHAHash(String data) throws SecurityException {
		return	Util.encodeBase32(calculateHash(data.getBytes(), SecurityConstants.SHA_HASH_ALGO));
	}

	@Override
	public byte[] generateSHAHash(byte[] data) throws SecurityException {
		return calculateHash(data, SecurityConstants.SHA2_HASH_ALGO);
	}

	@Override
	public String generateHash(String data, String algorithm) throws SecurityException {
		return Util.encodeBase32(calculateHash(data.getBytes(), algorithm));
	}

	@Override
	public byte[] generateHash(byte[] data, String algorithm) throws SecurityException {
		return calculateHash(data, algorithm);
	}
	
	private byte[] calculateHash(byte[] data, String algorithm) throws SecurityException {
		try {
			if(Util.length(data) == 0) {
				Log.error("Data for HASH generation is null");
				throw new SecurityException("Data is null"); 
			}
			
			if(Util.length(algorithm) == 0) {
				Log.error("Algorithm for HASH generation is null");
				throw new SecurityException("Algorithm is null"); 
			}
				
			MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
			return messageDigest.digest(data);
			
		} catch (GeneralSecurityException e) {
			Log.error("Error generating HASH.", e);
			throw new SecurityException(e.getMessage());
		}        
	}

}
