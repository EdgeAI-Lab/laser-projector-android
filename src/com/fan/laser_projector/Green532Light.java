package com.fan.laser_projector;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

public class Green532Light {

	private Switch green532_light_power_switch = null;
	
	private Button green532_light_dac_button = null;
	
	private TextView green532_light_dac_value = null;
	
	public TextView getGreen532_light_dac_value() {
		return green532_light_dac_value;
	}

	public void setGreen532_light_dac_value(TextView green532_light_dac_value) {
		this.green532_light_dac_value = green532_light_dac_value;
	}

	// image view 
	private ImageView green532_light_channel_1_state_iv = null;
	
	private EditText green532_light_channel_1 = null;

	
	public EditText getGreen532_light_channel_1() {
		return green532_light_channel_1;
	}

	public void setGreen532_light_channel_1(EditText green532_light_channel_1) {
		this.green532_light_channel_1 = green532_light_channel_1;
	}

	public Button getGreen532_light_dac_button() {
		return green532_light_dac_button;
	}

	public void setGreen532_light_dac_button(Button green532_light_dac_button) {
		this.green532_light_dac_button = green532_light_dac_button;
	}

	public Switch getGreen532_light_power_switch() {
		return green532_light_power_switch;
	}

	public void setGreen532_light_power_switch(Switch green532_light_power_switch) {
		this.green532_light_power_switch = green532_light_power_switch;
	}

	public ImageView getGreen532_light_channel_1_state_iv() {
		return green532_light_channel_1_state_iv;
	}

	public void setGreen532_light_channel_1_state_iv(
			ImageView green532_light_channel_1_state_iv) {
		this.green532_light_channel_1_state_iv = green532_light_channel_1_state_iv;
	}
	
	
	
	
}
