package com.mentor.ems.security.exceptions;

@SuppressWarnings("serial")
public class SecurityException extends Exception {

	public SecurityException(){
		super();
	}
	
	public SecurityException(Throwable t){
		super(t);
	}
	
	public SecurityException(String message){
		super(message);
	}
	
	public SecurityException(String message, Throwable t){
		super(message, t);
	}
	
	
}
