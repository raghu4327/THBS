package com.mentor.ems.security.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;


public class Util {

	private Util() {}

	private static final char[] DIGITS = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	
	private static final String ALPHA_NUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	
	public static int length(String anyStr) {
		return anyStr == null ? 0 : anyStr.trim().length();
	}
	
	public static int length(byte[] anyBytes) {
		return anyBytes == null ? 0 : anyBytes.length;
	}
	
	public static byte[] decodeBase16(String data) {
		int k = 0;
		byte[] results = new byte[data.length() / 2];
		int i = 0;
		while( i < data.length()) {
			results[k] = (byte) (Character.digit(data.charAt(i++), 16) << 4);
			results[k] += (byte) (Character.digit(data.charAt(i++), 16));
			k++;
		}
		return results;
	}
	
	public static String encodeBase16(byte[] bytes) {
		char[] out = new char[bytes.length * 2]; 
		for (int i = 0; i < bytes.length; i++) {
			out[2*i] = DIGITS[bytes[i] < 0 ? 8 + (bytes[i] + 128) / 16 : bytes[i] / 16]; 
			out[2*i + 1] = DIGITS[bytes[i] < 0 ? (bytes[i] + 128) % 16 : bytes[i] % 16];
		}
		return new String(out); 
	}
	
	public static String encodeBase32(byte[] bytes) {
		Base32 base32Instance = new Base32();
		return base32Instance.encodeToString(bytes);
	}
	
	public static byte[] decodeBase32(byte[] bytes) {
		Base32 base32Instance = new Base32();				
		return base32Instance.decode(bytes);
	}
	
	public static byte[] decodeBase32(String data) {
		Base32 base32Instance = new Base32();				
		return base32Instance.decode(data);
	}
	
	
	
	public static String generateRandomString(int passwordLen){
		Random rnd = new Random();
		StringBuilder sb = new StringBuilder( passwordLen );
		for( int i = 0; i < passwordLen; i++ ) 
			sb.append( ALPHA_NUMERIC.charAt( rnd.nextInt(ALPHA_NUMERIC.length())));
		return sb.toString();
	}
	
	public static byte[] generateSecureRandomBytes() {
		try {
			// Create a secure random number generator instance
			SecureRandom sr = SecureRandom.getInstance(SecurityConstants.RANDOM_KEY_ALGO);
			Random r = new Random();
			long l = r.nextLong();
			sr.setSeed(l);
			byte[] bytes = new byte[SecurityConstants.RANDOM_BYTES_LENGTH];
			sr.nextBytes(bytes);
			
			return bytes;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Random Number Generation Error.",e);
		}
	}
	
	public static byte[] generateRandomAESKey(int bitSize) {
		try {
			KeyGenerator kgen = KeyGenerator.getInstance(SecurityConstants.AES_ALGO);
			kgen.init(bitSize);
			SecretKey key = kgen.generateKey();
			return key.getEncoded();
		}
		catch(NoSuchAlgorithmException ex) {
			throw new RuntimeException("Not able to Generate Key.",ex);
		}
	}
	
	public static String encodeBase64(byte[] bytes) {
		Base64 base64Instance = new Base64();
		return base64Instance.encodeToString(bytes);
	}
	
	public static byte[] decodeBase64(byte[] bytes) {				
		return Base64.decodeBase64(bytes);
	}
	
	public static byte[] decodeBase64(String data) {		
		return Base64.decodeBase64(data);
	}
	
	public static long getCurrentTimestamp () {
		return new Date().getTime();
	}
 	
		
}
