package com.fan.laser_projector;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

public class RedLight {

	// switch
	private Switch red_light_power_switch = null;
	
	// button
	private Button red_light_dac_button = null;
	
	private TextView red_light_dac_value = null;
	
	public TextView getRed_light_dac_value() {
		return red_light_dac_value;
	}

	public void setRed_light_dac_value(TextView red_light_dac_value) {
		this.red_light_dac_value = red_light_dac_value;
	}

	// image view
	private ImageView red_light_channel_1_state_iv = null;
	private ImageView red_light_channel_2_state_iv = null;
	private ImageView red_light_channel_3_state_iv = null;
	private ImageView red_light_channel_4_state_iv = null;
	private ImageView red_light_channel_5_state_iv = null;
	private ImageView red_light_channel_6_state_iv = null;
	
	// for show adc value
	private EditText red_light_channel_1 = null;
	private EditText red_light_channel_2 = null;
	private EditText red_light_channel_3 = null;
	private EditText red_light_channel_4 = null;
	private EditText red_light_channel_5 = null;
	private EditText red_light_channel_6 = null;
	
	
	// getter and setter
	public EditText getRed_light_channel_1() {
		return red_light_channel_1;
	}

	public void setRed_light_channel_1(EditText red_light_channel_1) {
		this.red_light_channel_1 = red_light_channel_1;
	}

	public EditText getRed_light_channel_2() {
		return red_light_channel_2;
	}

	public void setRed_light_channel_2(EditText red_light_channel_2) {
		this.red_light_channel_2 = red_light_channel_2;
	}

	public EditText getRed_light_channel_3() {
		return red_light_channel_3;
	}

	public void setRed_light_channel_3(EditText red_light_channel_3) {
		this.red_light_channel_3 = red_light_channel_3;
	}

	public EditText getRed_light_channel_4() {
		return red_light_channel_4;
	}

	public void setRed_light_channel_4(EditText red_light_channel_4) {
		this.red_light_channel_4 = red_light_channel_4;
	}

	public EditText getRed_light_channel_5() {
		return red_light_channel_5;
	}

	public void setRed_light_channel_5(EditText red_light_channel_5) {
		this.red_light_channel_5 = red_light_channel_5;
	}

	public EditText getRed_light_channel_6() {
		return red_light_channel_6;
	}

	public void setRed_light_channel_6(EditText red_light_channel_6) {
		this.red_light_channel_6 = red_light_channel_6;
	}

	public Button getRed_light_dac_button() {
		return red_light_dac_button;
	}

	public void setRed_light_dac_button(Button red_light_dac_button) {
		this.red_light_dac_button = red_light_dac_button;
	}

	public Switch getRed_light_power_switch() {
		return red_light_power_switch;
	}

	public void setRed_light_power_switch(Switch red_light_power_switch) {
		this.red_light_power_switch = red_light_power_switch;
	}

	public ImageView getRed_light_channel_1_state_iv() {
		return red_light_channel_1_state_iv;
	}

	public void setRed_light_channel_1_state_iv(
			ImageView red_light_channel_1_state_iv) {
		this.red_light_channel_1_state_iv = red_light_channel_1_state_iv;
	}

	public ImageView getRed_light_channel_2_state_iv() {
		return red_light_channel_2_state_iv;
	}

	public void setRed_light_channel_2_state_iv(
			ImageView red_light_channel_2_state_iv) {
		this.red_light_channel_2_state_iv = red_light_channel_2_state_iv;
	}

	public ImageView getRed_light_channel_3_state_iv() {
		return red_light_channel_3_state_iv;
	}

	public void setRed_light_channel_3_state_iv(
			ImageView red_light_channel_3_state_iv) {
		this.red_light_channel_3_state_iv = red_light_channel_3_state_iv;
	}

	public ImageView getRed_light_channel_4_state_iv() {
		return red_light_channel_4_state_iv;
	}

	public void setRed_light_channel_4_state_iv(
			ImageView red_light_channel_4_state_iv) {
		this.red_light_channel_4_state_iv = red_light_channel_4_state_iv;
	}

	public ImageView getRed_light_channel_5_state_iv() {
		return red_light_channel_5_state_iv;
	}

	public void setRed_light_channel_5_state_iv(
			ImageView red_light_channel_5_state_iv) {
		this.red_light_channel_5_state_iv = red_light_channel_5_state_iv;
	}

	public ImageView getRed_light_channel_6_state_iv() {
		return red_light_channel_6_state_iv;
	}

	public void setRed_light_channel_6_state_iv(
			ImageView red_light_channel_6_state_iv) {
		this.red_light_channel_6_state_iv = red_light_channel_6_state_iv;
	}

	
	
}
