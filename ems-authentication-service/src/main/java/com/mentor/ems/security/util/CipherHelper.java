package com.mentor.ems.security.util;

import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import com.mentor.ems.security.exceptions.SecurityException;

public class CipherHelper {

	private CipherHelper() {}

	public static Cipher getCipherObject(String transformation, Key keyObj, byte[] iv, int mode) throws SecurityException {
		try {
		Cipher cipher = Cipher.getInstance(transformation);
			if(iv == null) 
				cipher.init(mode, keyObj);
			else {
				IvParameterSpec ivSpec = new IvParameterSpec(iv);
				cipher.init(mode, keyObj, ivSpec);
			}
			return cipher;
		} catch(GeneralSecurityException e) {
			throw new SecurityException("GeneralSecurityException occurred in CipherHelper - getCipherObject method",e);
		}
	}
}
