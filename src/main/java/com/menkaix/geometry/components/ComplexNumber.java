package com.menkaix.geometry.components;

import java.io.Serializable;

public class ComplexNumber implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7586023998341014793L;
	private double real ;
	private double imaginary ;
	
	public static final ComplexNumber i =  new ComplexNumber(0,1);
	
	
	public ComplexNumber() {
		real = 0 ;
		imaginary = 0 ;
	}
	
	public ComplexNumber(double x, double y) {
		real = x ;
		imaginary = y ;
	}
	
	public ComplexNumber(SimplePoint origin, SimplePoint end) {
		real = end.getX() - origin.getX();
		imaginary = end.getY() - origin.getY();
	}
	
	
	public ComplexNumber add(ComplexNumber other) {
		return new ComplexNumber(real + other.real, imaginary+other.imaginary) ;
	}
	
	public ComplexNumber multiply(ComplexNumber other) {
		return new ComplexNumber(
				real * other.real - imaginary * other.imaginary, 
				real  * other.imaginary + imaginary * other.real
				) ;
	}
	
	public ComplexNumber conjugate() {
		return new ComplexNumber(real, -imaginary);
	}
	
	public ComplexNumber normalize() {
		return new ComplexNumber(real/getModule(),imaginary/getModule());
	}

	
	public double getModule() {
		return Math.sqrt(real*real + imaginary*imaginary);
	}
	
	public double getArg() {
		return Math.atan2(imaginary, real);
	}
	
	
	
	public double getReal() {
		return real;
	}
	public void setReal(double real) {
		this.real = real;
	}
	public double getImaginary() {
		return imaginary;
	}
	public void setImaginary(double imaginary) {
		this.imaginary = imaginary;
	}
	
	
	

}
