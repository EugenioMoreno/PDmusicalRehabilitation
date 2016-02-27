package com.androidhive.musicplayer;

import java.util.Calendar;
import java.util.Date;

public class MedidaAcc {
	private float x, y, z =0;
	//private float y=0;
	//private float z=0;
	private float modulo=(float) Math.sqrt(x*x+y*y+z*z);
	private Calendar calendario = Calendar.getInstance();
	private int hora =calendario.get(Calendar.HOUR_OF_DAY);
	private int minutos = calendario.get(Calendar.MINUTE);
	private int segundos = calendario.get(Calendar.MILLISECOND);
	private String horaMedida;
	
	public MedidaAcc(float x, float y, float z,float modulo, String horaMedida){
		this.x=x;
		this.y=y;
		this.z=z;
		this.modulo=modulo;
		this.horaMedida=horaMedida;
	}

	//Getters
	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}
	public float getModulo() {
		return modulo;
	}

	public String getHoraMedida() {
		return horaMedida;
	}
	
	//Setters
	public void setX(float x) {
		this.x = x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public void setZ(float z) {
		this.z = z;
	}
	public void setModulo(float modulo) {
		this.modulo = modulo;
	}

	public void setHoraMedida(String horaMedida) {
		this.horaMedida = horaMedida;
	}
	
	

}
