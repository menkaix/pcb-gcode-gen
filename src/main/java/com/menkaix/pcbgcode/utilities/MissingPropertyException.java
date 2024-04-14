package com.menkaix.pcbgcode.utilities;

public class MissingPropertyException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3208829657811349511L;
	
	private String message = "" ;
	
	public MissingPropertyException() {
		
	}
	
	public MissingPropertyException(String message) {
		setMessage(message);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	

}
