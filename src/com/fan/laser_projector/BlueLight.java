package com.fan.laser_projector;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

public class BlueLight {

	private Switch blue_light_power_switch = null;
	
	private Button blue_light_dac_button = null;
	
	private TextView blue_light_dac_value = null;
	
	public TextView getBlue_light_dac_value() {
		return blue_light_dac_value;
	}

	public void setBlue_light_dac_value(TextView blue_light_dac_value) {
		this.blue_light_dac_value = blue_light_dac_value;
	}

	// image view 
	private ImageView blue_light_channel_1_state_iv = null;
	private ImageView blue_light_channel_2_state_iv = null;

	private EditText blue_light_channel_1 = null;
	private EditText blue_light_channel_2 = null;
	
	
	
	
	
	public EditText getBlue_light_channel_1() {
		return blue_light_channel_1;
	}

	public void setBlue_light_channel_1(EditText blue_light_channel_1) {
		this.blue_light_channel_1 = blue_light_channel_1;
	}

	public EditText getBlue_light_channel_2() {
		return blue_light_channel_2;
	}

	public void setBlue_light_channel_2(EditText blue_light_channel_2) {
		this.blue_light_channel_2 = blue_light_channel_2;
	}

	public Button getBlue_light_dac_button() {
		return blue_light_dac_button;
	}

	public void setBlue_light_dac_button(Button blue_light_dac_button) {
		this.blue_light_dac_button = blue_light_dac_button;
	}

	public Switch getBlue_light_power_switch() {
		return blue_light_power_switch;
	}

	public void setBlue_light_power_switch(Switch blue_light_power_switch) {
		this.blue_light_power_switch = blue_light_power_switch;
	}

	public ImageView getBlue_light_channel_1_state_iv() {
		return blue_light_channel_1_state_iv;
	}

	public void setBlue_light_channel_1_state_iv(
			ImageView blue_light_channel_1_state_iv) {
		this.blue_light_channel_1_state_iv = blue_light_channel_1_state_iv;
	}

	public ImageView getBlue_light_channel_2_state_iv() {
		return blue_light_channel_2_state_iv;
	}

	public void setBlue_light_channel_2_state_iv(
			ImageView blue_light_channel_2_state_iv) {
		this.blue_light_channel_2_state_iv = blue_light_channel_2_state_iv;
	}
	
	
	
}
