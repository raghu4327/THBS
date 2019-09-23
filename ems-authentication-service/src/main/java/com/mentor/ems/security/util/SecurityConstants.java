package com.mentor.ems.security.util;

public interface SecurityConstants {
	
	String SHA2_HASH_ALGO = "SHA-256";
	
	String SHA_HASH_ALGO = "SHA";
	
	String MD5_HASH = "MD5";
	
	String BC_SEC_PROVIDER = "BC";
	
	String RANDOM_KEY_ALGO = "SHA1PRNG";
	
	int RANDOM_BYTES_LENGTH = 16;
	
	int CRYPTO_BLOCK_SIZE = 128;
	
	String AES_ALGO = "AES";
	
	int AES_KEY_SIZE = 256;
	
	byte[] PBE_SALT = new byte[] {21, 97, -103, 108, 7, -62, -123, 30, -30, 9, 10, -4, 64, -121, -83, 54};
	
	int PBE_ITERATIONS = 1024;
	
	int KEY_BYTES_LENGTH = 32;
	
	int SHA2_BYTES_LENGTH = 32;
	
	int IV_BYTES_LENGTH = 16;
	
	String ALGO_TRANSFOMRATION_CBC = "AES/CBC/PKCS5Padding";
	
	String ALGO_TRANSFOMRATION_ECB = "AES/ECB/PKCS5Padding";
	
	String ALGO_TRANSFOMRATION_ECB_NOPADDING = "AES/ECB/NOPadding";
	
	String UNAUTORIZED_ERROR_CODE = "401";
	
	String HTTP_OK = "200";
	
}
