package com.mentor.ems.security.impl;

import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.mentor.ems.security.exceptions.SecurityException;
import com.mentor.ems.security.spi.SecurityProvider;
import com.mentor.ems.security.util.CipherHelper;
import com.mentor.ems.security.util.SecurityConstants;
import com.mentor.ems.security.util.Util;

public class SecurityProviderImpl implements SecurityProvider {
	
	private static final Logger Log = Logger.getLogger(SecurityProviderImpl.class);
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Override
	public String encrypt(String plainData, String key) throws SecurityException {
		SecretKeySpec secretKey = getSecretKeySpec(key);
		Cipher cipher = CipherHelper.getCipherObject
				(SecurityConstants.ALGO_TRANSFOMRATION_ECB, secretKey , null, Cipher.ENCRYPT_MODE);
		byte[] encBytes;
		try {
			encBytes = cipher.doFinal(plainData.getBytes());
			return Util.encodeBase32(encBytes);
			
		} catch (IllegalBlockSizeException e) {
			Log.error("Error in crypto operations.", e);
			throw new SecurityException("Error in crypto operation.", e);
		} catch (BadPaddingException e) {
			Log.error("Error in crypto operations.", e);
			throw new SecurityException("Error in crypto operation.", e);
		}
	}

	@Override
	public String decrypt(String encData, String key) throws SecurityException {
		SecretKeySpec secretKey = getSecretKeySpec(key);
		Cipher cipher = CipherHelper.getCipherObject
				(SecurityConstants.ALGO_TRANSFOMRATION_ECB, secretKey , null, Cipher.DECRYPT_MODE);
		
		byte[] decBytes;
		try {
			decBytes = cipher.doFinal(Util.decodeBase32(encData));
			return new String(decBytes);
			
		} catch (IllegalBlockSizeException e) {
			Log.error("Error in crypto operations.", e);
			throw new SecurityException("Error in crypto operation.", e);
		} catch (BadPaddingException e) {
			Log.error("Error in crypto operations.", e);
			throw new SecurityException("Error in crypto operation.", e);
		}		
	}
	
	private SecretKeySpec getSecretKeySpec(String key) {
		byte[] keyBytes = Util.decodeBase32(key);
		
		if(keyBytes.length != 32) {
			Log.debug("Key length : " + keyBytes.length);
		}
		return new SecretKeySpec(keyBytes, "AES");
	}
}
