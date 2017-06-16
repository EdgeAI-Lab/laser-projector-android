package com.fan.laser_projector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DecimalFormat;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ShowToast")
public class MainActivity extends Activity implements OnClickListener, OnCheckedChangeListener{

	private static String TAG = MainActivity.class.getSimpleName();

	private Toast mToast;
	
	// 语音合成对象
	private SpeechSynthesizer mTts;
	
	//BT connect flag
	private static boolean isConnectBT = false;
	private boolean noConnectBT = true;
	private boolean read_thread_on = true;
	
	/*************** UI component *******************/
	
	// use for debug
	//private EditText etShowEditText = null;
	
	// module
	private RedLight red_light     = new RedLight();
	private GreenLight green_light = new GreenLight();
	private BlueLight blue_light   = new BlueLight();
	private Green532Light green532_light = new Green532Light();
	private Temperature temperature = new Temperature();
	
	//add for BT
	private BluetoothAdapter _bluetooth;    //获取本地蓝牙适配器，即蓝牙设备
	BluetoothDevice _device = null;         //蓝牙设备
    static BluetoothSocket _socket = null;  //蓝牙通信socket
	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号

	// alert dialog
	private AlertDialog.Builder builder;  
	private AlertDialog dialog; 
	
	// BT input stream
	private InputStream is = null;
	
	// loop queue
//	private LoopQueue queue = null;
	
	// queue
	LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<Integer>(1024);
	
	// data handle
	private int[] data_handle_buf = new int[255];
	private int[] uart_rx_buf= new int[255];
	private int rx_index = 0;
	private int offset = 0;
	private int rx_frame_len = 0;
	//private int frame_data_len = 0;
	
	private final int  HEAD_FIRST      = 0;
	private final int  HEAD_SECOND     = 1;
	private final int  FRAME_LENGTH    = 2;
	private final int  CMD             = 3;
	private final int  DEVICE_ENDPOINT = 4 ;
	private final int  DATA            = 5;

	private final int  READ            = 0x00;
	private final int  READ_ACK        = 0x01;
	private final int  WRITE	       = 0x02;
	private final int  WRITE_ACK       = 0x03;
	private final int  DATA_UPLOAD     = 0x05;


	private final  int PROTOCOL_FIXED_LENGTH = 6;
	
	private Switch all_power_switch = null;
	
	private int[] adc_buf = new int[255];
	private int[] led_state_buf = new int[2];
	private int[] temperature_buf = new int[2];
	
	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.control_ui);
		
		// loop queue init
//		queue = new LoopQueue(4096);
		
		// 语音控件初始化
		initVoice();
		
		// 连接蓝牙提示框
		initDialog();

		// UI控件初始化
		initUI();
		
		dataHandleThread.start();
		
		// 
		_bluetooth = BluetoothAdapter.getDefaultAdapter();
		
		//如果打开本地蓝牙设备不成功，提示信息，结束程序
        if (_bluetooth == null){
        	Toast.makeText(this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // 设置设备可以被搜索  
       new Thread(){
    	   public void run(){
    		   if(_bluetooth.isEnabled()==false){
        		_bluetooth.enable();
    		   }
    	   }   	   
       }.start();
       
       
	   
	   // 连接蓝牙线程
	   new Thread(new Runnable() {
		
		@Override
		public void run() {

			while (noConnectBT) {
				
				if(_bluetooth.isEnabled()){
				//假如未连接蓝牙，则重新连接蓝牙
					connectBT();
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				
			}
			
		}
	}).start();
	   

     //监听蓝牙是否断开
       IntentFilter disConnectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
       registerReceiver(stateChangeReceiver, disConnectedFilter);
       
	}
	
	
	
	private Thread dataHandleThread = new Thread(){

		public void run() {
			
			while (true) {
				
				while ( (rx_index < 255) && (!queue.isEmpty()) ) {
					
					uart_rx_buf[rx_index++] =  queue.poll();
					
					//System.out.println("recv: 0x"+Integer.toHexString(ch));
				}
				
				if (rx_index <= 6) {
					
					continue;
				}			
				
				while((rx_index - offset) >  6)
				{
				        //System.out.println("prase");		
						if(uart_rx_buf[offset + HEAD_FIRST] != 0x5A)
						{
						  offset ++;
						  //System.out.println("no 0x5A");
						  continue;
						}
						
						if(uart_rx_buf[offset + HEAD_SECOND] != 0xA5)
						{
						  offset ++;
						  continue;
						}      

						rx_frame_len = uart_rx_buf[offset + FRAME_LENGTH];
						
						//
						if( (rx_frame_len > 249) && (rx_frame_len == 0) )
						{
							offset += 2;
							continue;
						}

						if(( rx_index < (rx_frame_len + offset) ) || ( (rx_frame_len + offset) < 1) )
						{
							break;
						}
						
						int recvSum = uart_rx_buf[offset + rx_frame_len - 1];
//						System.out.println("offset: "+Integer.toHexString(offset));
//						System.out.println("rx_frame_len: "+Integer.toHexString(rx_frame_len));
//						System.out.println("recvSum: "+Integer.toHexString(recvSum));

						
						int sum = get_check_sum(uart_rx_buf,offset,(rx_frame_len - 1));
//						System.out.println("sum: "+Integer.toHexString(sum));
						
						
						
						//
						if(sum != recvSum)
						{
							System.out.println("check sum fail");
						  //
						  offset += 2;
						  continue;
						}
						
						System.arraycopy(uart_rx_buf,offset,data_handle_buf,0,rx_frame_len);
						
						cmd_handle();
						
//						System.out.println("one frame parse over");

						offset += rx_frame_len;
						
				}//end while
					
					rx_index -= offset;
					  
					
					if(rx_index > 0)
					{
						System.arraycopy(uart_rx_buf,offset,uart_rx_buf,0,rx_index);
					}

					
					offset = 0;
					rx_frame_len = 0;
				
				
			}
			
		}
		
		
		private void cmd_handle() {
					
			switch (data_handle_buf[CMD]) {
				
			case READ_ACK:
				
				break;	

			case WRITE_ACK:

				break;

			case DATA_UPLOAD:
				
					// upload data handle
					upload_data_handle();
				
				break;

			default:
				break;
			}
			
		}


		private void upload_data_handle() {
			
			switch (data_handle_buf[DEVICE_ENDPOINT]) {
			
			// all adc
			case 0x15:
				
				System.arraycopy(data_handle_buf, 0, adc_buf, 0, rx_frame_len);
				
				runOnUiThread(new Runnable() {
					
					public void run() {						
						
						int i = 0;

						//Float.toString((float) (adc_buf[DATA+i++] * 2.0 * 3.0 / 500.0));

						red_light.getRed_light_channel_1().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 / 500.0))+"A");
						red_light.getRed_light_channel_2().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 / 500.0))+"A");
						red_light.getRed_light_channel_3().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 / 500.0))+"A");
						red_light.getRed_light_channel_4().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 / 500.0))+"A");
						red_light.getRed_light_channel_5().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 / 500.0))+"A");
						red_light.getRed_light_channel_6().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 / 500.0))+"A");
						
						green_light.getGreen_light_channel_1().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 *2.0 / 500.0))+"A");
						green_light.getGreen_light_channel_2().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 *2.0 / 500.0))+"A");
						green_light.getGreen_light_channel_3().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 *2.0 / 500.0))+"A");
						green_light.getGreen_light_channel_4().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 *2.0 / 500.0))+"A");
						green_light.getGreen_light_channel_5().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 *2.0 / 500.0))+"A");
						green_light.getGreen_light_channel_6().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 *2.0 / 500.0))+"A");
						green_light.getGreen_light_channel_7().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 *2.0 / 500.0))+"A");
						
						blue_light.getBlue_light_channel_1().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 * 3.0 / 500.0))+"A");
						blue_light.getBlue_light_channel_2().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 * 3.0 / 500.0))+"A");
						
						green532_light.getGreen532_light_channel_1().setText(Float.toString((float) (adc_buf[DATA+i++] * 2.0 * 6.0 / 500.0))+"A");
						
						
						
						i = 0;
					}	
				});
				
				
				break;
			
			// all state, DATA[0]: channel 9~16 DATA[1]: channel 1~8
			case 0x16:

				turn_off_all_light();
				
				led_state_buf[0] = data_handle_buf[DATA];
				led_state_buf[1] = data_handle_buf[DATA+1];
				
				for (int i=0;i<8;i++) {

					
					
					switch (led_state_buf[1] | (0x7F >> i)) {
					case 0x7F:
						

						runOnUiThread(new Runnable() {
							public void run() {
								
								red_light.getRed_light_channel_1_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						
						break;
						
					case 0xBF:
						
						runOnUiThread(new Runnable() {
							public void run() {
								
								red_light.getRed_light_channel_2_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						
						break;
						
					case 0xDF:
						runOnUiThread(new Runnable() {
							public void run() {
								
								red_light.getRed_light_channel_3_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						break;
						
					case 0xEF:
						runOnUiThread(new Runnable() {
							public void run() {
								
								red_light.getRed_light_channel_4_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						break;
						
					case 0xF7:
						runOnUiThread(new Runnable() {
							public void run() {
								red_light.getRed_light_channel_5_state_iv().setImageResource(R.drawable.light_on);
								
							}	
						});
						
						break;
						
					case 0xFB:
						runOnUiThread(new Runnable() {
							public void run() {
								
								red_light.getRed_light_channel_6_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						break;
						
					case 0xFD:
						runOnUiThread(new Runnable() {
							public void run() {
								
								green_light.getGreen_light_channel_1_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						break;
						
					case 0xFE:
						runOnUiThread(new Runnable() {
							public void run() {
								
								green_light.getGreen_light_channel_2_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						break;

					default:
						break;
					}
					
				}
				
				for (int i=0;i<8;i++) {

					switch (led_state_buf[0] | (0x7F >> i)) {
					
					case 0x7F:
						
						runOnUiThread(new Runnable() {
							public void run() {
								
								green_light.getGreen_light_channel_3_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						
						break;
						
					case 0xBF:
						
						runOnUiThread(new Runnable() {
							public void run() {
								
								green_light.getGreen_light_channel_4_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						
						break;
						
					case 0xDF:
						runOnUiThread(new Runnable() {
							public void run() {
								
								green_light.getGreen_light_channel_5_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						break;
						
					case 0xEF:
						runOnUiThread(new Runnable() {
							public void run() {
								
								green_light.getGreen_light_channel_6_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						break;
						
					case 0xF7:
						runOnUiThread(new Runnable() {
							public void run() {
								green_light.getGreen_light_channel_7_state_iv().setImageResource(R.drawable.light_on);
								
							}	
						});
						
						break;
						
					case 0xFB:
						runOnUiThread(new Runnable() {
							public void run() {
								
								blue_light.getBlue_light_channel_1_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						break;
						
					case 0xFD:
						runOnUiThread(new Runnable() {
							public void run() {
								
								blue_light.getBlue_light_channel_2_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						break;
						
					case 0xFE:
						runOnUiThread(new Runnable() {
							public void run() {
								
								green532_light.getGreen532_light_channel_1_state_iv().setImageResource(R.drawable.light_on);
							}	
						});
						
						break;

					default:
						break;
					}
					
				}
				
					
					//System.out.println("DATA[0]: "+Integer.toHexString(data_handle_buf[DATA]));
					//System.out.println("DATA[1]: "+Integer.toHexString(data_handle_buf[DATA+1]));
				
				break;
				
			case 0x17:
				
				runOnUiThread(new Runnable() {
					
					
					
					@Override
					public void run() {
						
						temperature_buf[0] = data_handle_buf[DATA];
						temperature_buf[1] = data_handle_buf[DATA+1];
						
						float Rt = (float) ((temperature_buf[0]*100.0) / ( (500 - temperature_buf[0])/100.0) );
						
						float T1= (float) ( 1.0 / ( Math.log(Rt/10000.0) /3950.0 + 1/(25+273.15) ) - 273.15);
						
						Rt = (float) ((temperature_buf[1]*100.0) / ( (500 - temperature_buf[1])/100.0) );
						float T2= (float) ( 1.0 / ( Math.log(Rt/10000.0) /3950.0 + 1/(25+273.15) ) - 273.15);
						
						temperature.getTemp_channel_1().setText(Float.toString(T1));
						temperature.getTemp_channel_2().setText(Float.toString(T2));
						
					}
				});
				
				break;
				
				case 0x19: // all dac
				
//				System.arraycopy(data_handle_buf, 0, dac_buf, 0, rx_frame_len);
				
				runOnUiThread(new Runnable() {
					
					private int i = 0;
					@Override
					public void run() {

						DecimalFormat df = new DecimalFormat("#.##");
						DecimalFormat df_a = new DecimalFormat("#.#");
						
						int high = data_handle_buf[DATA+i++]<<8;
						int low = data_handle_buf[DATA+i++];
						int value = high | low;
//						System.out.println("high: "+ Integer.toHexString(high)+ "   " 
//								          + "low: "+Integer.toHexString(low)+ "   "
//								          + "value: "+Integer.toHexString(value));
						String str = df.format(value * 5.0 / 4096.0);
						String str_a = df_a.format(value * 5.0 / 4096.0 *0.93 / 4.5);
						
						red_light.getRed_light_dac_value().setText(str+"V, "+str_a+"A");

						
						high = data_handle_buf[DATA+i++]<<8;
						low = data_handle_buf[DATA+i++];
						value = high | low;
//						System.out.println("high: "+ Integer.toHexString(high) + "   "
//						          + "low: "+Integer.toHexString(low)+ "   "
//						          + "value: "+Integer.toHexString(value));
						str = df.format(value * 5.0 / 4096.0 );
						str_a = df_a.format(value * 5.0 / 4096.0 *2.0 / 4.5);
						green_light.getGreen_light_dac_value().setText(str+"V, "+str_a+"A");
						
						
						high = data_handle_buf[DATA+i++]<<8;
						low = data_handle_buf[DATA+i++];
						value = high | low;
//						System.out.println("high: "+ Integer.toHexString(high) + "   "
//						          + "low: "+Integer.toHexString(low)+ "   "
//						          + "value: "+Integer.toHexString(value));
						str = df.format(value * 5.0 / 4096.0 );
						str_a = df_a.format(value * 5.0 / 4096.0 *3.6 / 4.5);
						blue_light.getBlue_light_dac_value().setText(str+"V, "+str_a+"A");
						
						high = data_handle_buf[DATA+i++]<<8;
						low = data_handle_buf[DATA+i++];
						value = high | low;
//						System.out.println("high: "+ Integer.toHexString(high) + "   "
//						          + "low: "+Integer.toHexString(low)+ "   "
//						          + "value: "+Integer.toHexString(value));
						str = df.format(value * 5.0 / 4096.0 );
						str_a = df_a.format(value * 5.0 / 4096.0 *6.0 / 4.5);
						green532_light.getGreen532_light_dac_value().setText(str+"V, "+str_a+"A");
						
					};
				});
				
				break;

			default:
				break;
			}
			
		}


		private void turn_off_all_light() {
			
			runOnUiThread(new Runnable() {
				public void run() {
					
					red_light.getRed_light_channel_1_state_iv().setImageResource(R.drawable.light_off);
					red_light.getRed_light_channel_2_state_iv().setImageResource(R.drawable.light_off);
					red_light.getRed_light_channel_3_state_iv().setImageResource(R.drawable.light_off);
					red_light.getRed_light_channel_4_state_iv().setImageResource(R.drawable.light_off);
					red_light.getRed_light_channel_5_state_iv().setImageResource(R.drawable.light_off);
					red_light.getRed_light_channel_6_state_iv().setImageResource(R.drawable.light_off);
					
					green_light.getGreen_light_channel_1_state_iv().setImageResource(R.drawable.light_off);
					green_light.getGreen_light_channel_2_state_iv().setImageResource(R.drawable.light_off);
					green_light.getGreen_light_channel_3_state_iv().setImageResource(R.drawable.light_off);
					green_light.getGreen_light_channel_4_state_iv().setImageResource(R.drawable.light_off);
					green_light.getGreen_light_channel_5_state_iv().setImageResource(R.drawable.light_off);
					green_light.getGreen_light_channel_6_state_iv().setImageResource(R.drawable.light_off);
					green_light.getGreen_light_channel_7_state_iv().setImageResource(R.drawable.light_off);
					
					blue_light.getBlue_light_channel_1_state_iv().setImageResource(R.drawable.light_off);
					blue_light.getBlue_light_channel_2_state_iv().setImageResource(R.drawable.light_off);
					
					green532_light.getGreen532_light_channel_1_state_iv().setImageResource(R.drawable.light_off);
				}	
			});
		};
		
	};
	
	/*
	 * 计算校验和
	 */
	int get_check_sum(int[] pack, int pos, int pack_len)
	{
	  int i;
	  int check_sum = 0;
	  
	  for(i = 0; i < pack_len; i ++)
	  {
//		System.out.println("checksum: 0x"+Integer.toHexString(pack[pos+i]));
	    check_sum += pack[pos+i];
	  }
	  
	
	  
	  return check_sum&0xFF;
	}
	
	
	private void initUI() {

		/***************************Red Light *****************************************/
		
		// power switch
		red_light.setRed_light_power_switch((Switch) findViewById(R.id.red_light_power_switch));
		red_light.getRed_light_power_switch().setOnCheckedChangeListener(this);
		
		// dac button
		red_light.setRed_light_dac_button((Button) findViewById(R.id.red_light_dac_button));
		red_light.getRed_light_dac_button().setOnClickListener(this);
		
		//dac value
		red_light.setRed_light_dac_value((TextView) findViewById(R.id.red_light_dac_value));
		
		// image view
		red_light.setRed_light_channel_1_state_iv((ImageView) findViewById(R.id.red_light_channel_1_state_iv));
		red_light.setRed_light_channel_2_state_iv((ImageView) findViewById(R.id.red_light_channel_2_state_iv));
		red_light.setRed_light_channel_3_state_iv((ImageView) findViewById(R.id.red_light_channel_3_state_iv));
		red_light.setRed_light_channel_4_state_iv((ImageView) findViewById(R.id.red_light_channel_4_state_iv));
		red_light.setRed_light_channel_5_state_iv((ImageView) findViewById(R.id.red_light_channel_5_state_iv));
		red_light.setRed_light_channel_6_state_iv((ImageView) findViewById(R.id.red_light_channel_6_state_iv));
		
		// edit text for red light adc value
		red_light.setRed_light_channel_1((EditText) findViewById(R.id.red_light_channel_1));
		red_light.setRed_light_channel_2((EditText) findViewById(R.id.red_light_channel_2));
		red_light.setRed_light_channel_3((EditText) findViewById(R.id.red_light_channel_3));
		red_light.setRed_light_channel_4((EditText) findViewById(R.id.red_light_channel_4));
		red_light.setRed_light_channel_5((EditText) findViewById(R.id.red_light_channel_5));
		red_light.setRed_light_channel_6((EditText) findViewById(R.id.red_light_channel_6));
		
		/***************************Green Light *****************************************/
		
		// power switch
		green_light.setGreen_light_power_switch((Switch) findViewById(R.id.green_light_power_switch));
		green_light.getGreen_light_power_switch().setOnCheckedChangeListener(this);
		
		// dac button
		green_light.setGreen_light_dac_button((Button) findViewById(R.id.green_light_dac_button));
		green_light.getGreen_light_dac_button().setOnClickListener(this);
		
		//dac vlaue
		green_light.setGreen_light_dac_value((TextView) findViewById(R.id.green_light_dac_value));
		
		// image view
		green_light.setGreen_light_channel_1_state_iv((ImageView) findViewById(R.id.green_light_channel_1_state_iv));
		green_light.setGreen_light_channel_2_state_iv((ImageView) findViewById(R.id.green_light_channel_2_state_iv));
		green_light.setGreen_light_channel_3_state_iv((ImageView) findViewById(R.id.green_light_channel_3_state_iv));
		green_light.setGreen_light_channel_4_state_iv((ImageView) findViewById(R.id.green_light_channel_4_state_iv));
		green_light.setGreen_light_channel_5_state_iv((ImageView) findViewById(R.id.green_light_channel_5_state_iv));
		green_light.setGreen_light_channel_6_state_iv((ImageView) findViewById(R.id.green_light_channel_6_state_iv));
		green_light.setGreen_light_channel_7_state_iv((ImageView) findViewById(R.id.green_light_channel_7_state_iv));
		
		// edit text for green light adc value
		green_light.setGreen_light_channel_1((EditText) findViewById(R.id.green_light_channel_1));
		green_light.setGreen_light_channel_2((EditText) findViewById(R.id.green_light_channel_2));
		green_light.setGreen_light_channel_3((EditText) findViewById(R.id.green_light_channel_3));
		green_light.setGreen_light_channel_4((EditText) findViewById(R.id.green_light_channel_4));
		green_light.setGreen_light_channel_5((EditText) findViewById(R.id.green_light_channel_5));
		green_light.setGreen_light_channel_6((EditText) findViewById(R.id.green_light_channel_6));
		green_light.setGreen_light_channel_7((EditText) findViewById(R.id.green_light_channel_7));
		
		/***************************Blue Light *****************************************/
		
		// power switch
		blue_light.setBlue_light_power_switch((Switch) findViewById(R.id.blue_light_power_switch));
		blue_light.getBlue_light_power_switch().setOnCheckedChangeListener(this);
		
		// dac button
		blue_light.setBlue_light_dac_button((Button) findViewById(R.id.blue_light_dac_button));
		blue_light.getBlue_light_dac_button().setOnClickListener(this);
		
		//dac vlaue
		blue_light.setBlue_light_dac_value((TextView) findViewById(R.id.blue_light_dac_value));
		
		// image view
		blue_light.setBlue_light_channel_1_state_iv((ImageView) findViewById(R.id.blue_light_channel_1_state_iv));
		blue_light.setBlue_light_channel_2_state_iv((ImageView) findViewById(R.id.blue_light_channel_2_state_iv));
		
		// edit text for blue light adc value
		blue_light.setBlue_light_channel_1((EditText) findViewById(R.id.blue_light_channel_1));
		blue_light.setBlue_light_channel_2((EditText) findViewById(R.id.blue_light_channel_2));

		/***************************Green532 Light *****************************************/
		
		// power switch
		green532_light.setGreen532_light_power_switch((Switch) findViewById(R.id.green532_light_power_switch));
		green532_light.getGreen532_light_power_switch().setOnCheckedChangeListener(this);
		
		// dac button
		green532_light.setGreen532_light_dac_button((Button) findViewById(R.id.green532_light_dac_button));
		green532_light.getGreen532_light_dac_button().setOnClickListener(this);
		
		//dac vlaue
		green532_light.setGreen532_light_dac_value((TextView) findViewById(R.id.green532_light_dac_value));
		
		// image view
		green532_light.setGreen532_light_channel_1_state_iv((ImageView) findViewById(R.id.green532_light_channel_1_state_iv));
				
		// edit text for green532 light adc value
		green532_light.setGreen532_light_channel_1((EditText) findViewById(R.id.green532_light_channel_1));
		
		/*************************** temperature *****************************************/
		temperature.setTemp_channel_1((EditText) findViewById(R.id.temp_1));
		temperature.setTemp_channel_2((EditText) findViewById(R.id.temp_2));
		
		
		all_power_switch = (Switch) findViewById(R.id.all_power_switch);
		all_power_switch.setOnCheckedChangeListener(this);
		
		
		
		
		/****************************** Debug **********************************/
		//etShowEditText = (EditText) findViewById(R.id.etShow);
		// Toast初始化
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
	}



	private void initVoice() {

		SpeechUtility.createUtility(MainActivity.this, "appid=" + getString(R.string.app_id));
		
		// 初始化合成对象
		mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this, mTtsInitListener);

		// 设置语音合成参数
		setTtsParam();
		
	}



	/*
	 * 初始化连接蓝牙对话框
	 * 
	 */
	private void initDialog() {
		
		builder = new AlertDialog.Builder(this);
		builder.setMessage("正在连接蓝牙，请等待..." )
			   .setCancelable(false)
			   .setPositiveButton("取消连接" ,  new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								
								System.exit(0);
								
							}
						} ) ;
		
		dialog = builder.show();
		
	}

	
	private BroadcastReceiver stateChangeReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	       
	        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
//	        	showTip(action);
	        	isConnectBT = false;
	        	read_thread_on = false;
	        	noConnectBT = true;
//	        	handler.postDelayed(runnable, 3000);
	        	new Thread(new Runnable() {
	        		
	        		@Override
	        		public void run() {

	        			
						while (noConnectBT) {
	        				//假如未连接蓝牙，则重新连接蓝牙
	        				connectBT();
	        				
	        				try {
	        					Thread.sleep(3000);
	        				} catch (InterruptedException e) {
	        					e.printStackTrace();
	        				}
	        			}
	        			
	        		}
	        	}).start();
	        	
	        	mTts.startSpeaking("蓝牙已断开，正在尝试重新连接", null);
	        }
	        
	    }
	};

	

	/*
	 * 根据指定的蓝牙MAC地址，连接到蓝牙
	 * 20:16:05:09:53:95(集成板子上的蓝牙MAC地址)
	 */
	private void connectBT(){
		
		System.out.println("正在连接....");
		// 得到蓝牙设备句柄      
        //_device = _bluetooth.getRemoteDevice("20:16:05:09:53:95"); //粮仓机器人控制器
		_device = _bluetooth.getRemoteDevice("20:16:06:12:09:42"); //Debug
        // 用服务号得到socket
        try{
        	_socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
        	System.out.println("Connecting...");
        }catch(IOException e){
//        	mTts.startSpeaking("蓝牙连接失败，请确认设备状态", null);
        	System.out.println("0....");
        }
        //连接socket
    	
        try{
        	_socket.connect();
        	isConnectBT = true;
        	noConnectBT = false;
        	mTts.startSpeaking("蓝牙连接成功", null);
        	
        	// 蓝牙连接成功，关闭对话框
        	dialog.dismiss();
			
			
			int[] temp_arry = new int[2];
        	temp_arry[0] = 0x00; // attr ID
			temp_arry[1] = 0x00; // data length
			
			
			SendCmd(READ, 0x19, temp_arry, 2);
        	
        	//打开接收线程
            try{
        		is = _socket.getInputStream();   //得到蓝牙数据输入流
        		
        		read_thread_on = true;
        		}catch(IOException e){
        			Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
        			return;
        		}
        		
            
       	 		//接收数据线程
            	new Thread(new Runnable() {
					
					@Override
					public void run() {
						//接收线程
			    		while(read_thread_on){
			    			
								try {
									if(is.available() > 0)
									{
										int ch = (char) is.read();
									    
										if(queue.remainingCapacity() > 0) // add element if queue is not full
										{
											queue.add(ch);
										}
										
										
										//System.out.println(Integer.toHexString(ch));
									}
								} catch (IOException e) {
									
									e.printStackTrace();
								}

			    		}
						
					}
				}).start();
            	

        	
        }catch(IOException e){
        	try{
        		isConnectBT = false;
//        		mTts.startSpeaking("蓝牙连接失败，请确认设备状态", null);
        		_socket.close();
        		_socket = null;
        	}catch(IOException ee){
        		isConnectBT = false;
//        		mTts.startSpeaking("蓝牙连接失败，请确认设备状态", null);
        	}
        	
        	return;
        }	

	}
	


	

    
    
	/**
	 * 初始化语音合成监听。
	 */
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	} else {
				// 初始化成功，之后可以调用startSpeaking方法
        		// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
        		// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}		
		}
	};

	
	
	
	
	/**
	 * 语音合成参数设置
	 * @param param
	 * @return 
	 */
	private void setTtsParam(){
		// 清空参数
		mTts.setParameter(SpeechConstant.PARAMS, null);
		// 根据合成引擎设置相应参数
		
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		// 设置在线合成发音人
		mTts.setParameter(SpeechConstant.VOICE_NAME, "jiajia");
		
		//设置合成语速
		mTts.setParameter(SpeechConstant.SPEED, "50");
		//设置合成音调
		mTts.setParameter(SpeechConstant.PITCH, "60");
		//设置合成音量
		mTts.setParameter(SpeechConstant.VOLUME, "3");
		
		//设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
		// 设置播放合成音频打断音乐播放，默认为true
		mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
		
		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
	}
	



	@Override
	protected void onStop() {
		super.onStop();

    	if (mTts != null) {
    		mTts.stopSpeaking();
    	}
	}
	
	@Override
    //关闭程序掉用处理部分
    public void onDestroy(){
    	super.onDestroy();
    	
    	isConnectBT = false;
    	noConnectBT = false;
    	
    	
    	
    	if (mTts != null) {
//    		mTts.stopSpeaking();
    		// 退出时释放连接
    		mTts.destroy();
		}
    	
    	
    	
    	if(_socket!=null)  //关闭连接socket
    	try{
    		_socket.close();
    	}catch(IOException e){}
    	_bluetooth.disable(); //退出程序时时，不想关闭蓝牙，所以注释掉
    	
    
    	unregisterReceiver(stateChangeReceiver);
    	
    	System.exit(0);
		
    }
	

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}


	
	@Override
	public void onClick(View v) {	
		
		switch (v.getId()) {
		
			case R.id.red_light_dac_button:
				
					
				
				    showInputDialog(0x10,0x30,0x01);
					Toast.makeText(this, "red_dac", Toast.LENGTH_SHORT).show();
					
					


				break;
				
			case R.id.green_light_dac_button:
				
				showInputDialog(0x11,0x30,0x02);
				Toast.makeText(this, "green_dac", Toast.LENGTH_SHORT).show();
//				green_light.getGreen_light_dac_value().setText(Double.toString(d_value/100));

				
				break;
				
			case R.id.blue_light_dac_button:
		
				showInputDialog(0x12,0x30,0x03);
				Toast.makeText(this, "blue_dac", Toast.LENGTH_SHORT).show();
//				blue_light.getBlue_light_dac_value().setText(Double.toString(d_value/100));

				break;
		
			case R.id.green532_light_dac_button:
		
				showInputDialog(0x13,0x30,0x04);
				Toast.makeText(this, "green532_dac", Toast.LENGTH_SHORT).show();
//				green532_light.getGreen532_light_dac_value().setText(Double.toString(d_value/100));

				break;
	
			default:
				break;
		}
		
		
	}
	
	
	public boolean isNumeric(String str){ 
		   Pattern pattern = Pattern.compile("[0-9]*"); 
		   Matcher isNum = pattern.matcher(str);
		   if( !isNum.matches() ){
		       return false; 
		   } 
		   return true; 
		}
	
	
	/*
	 * the dialog for seting DAC value
	 * 
	 */
	private void showInputDialog(final int endpoint,final int attrID,final int channel) {
	    /*@setView 装入一个EditView
	     */
	    final EditText editText = new EditText(MainActivity.this);
	    AlertDialog.Builder inputDialog = 
	        new AlertDialog.Builder(MainActivity.this);
	    inputDialog.setMessage("请输入电流值").setView(editText);
	    inputDialog.setPositiveButton("确定", 
	        new DialogInterface.OnClickListener() {
	    	
	    	int value = 0;
			int temp_arry[] = new int[50];
			String current_value = null;
			
	        @Override
	        public void onClick(DialogInterface dialog, int which) {
	         
		         if( (current_value = editText.getText().toString()).isEmpty() ){
		        	 return;
		         }
//		         else {
//		        	 
//		        	 if(!isNumeric(current_value)){
//		        		 return;
//		        	 } 
//				}
		        	 
		         
		        float float_value = Float.parseFloat(current_value);
		        
		       
		        if (float_value < 0) {
		        	float_value = 0;
				}
		        
		        
		        /**
		         * 
		         * Red , Green, Blue , Green532 DAC set
		         * 
		         */
		        switch (channel) {
		        
					case 0x01: // Red
						
						 if ( float_value > 0.93 ) {
					        	
					        	float_value = (float) 0.93;
							}
						 value = (int) (float_value*4.5/0.93*100);
						
						 red_light.getRed_light_dac_value().setText(((float)value/100.0)+"V,"+float_value+"A");	
	
						break;
						
					case 0x02: // Green
						 if ( float_value > 2 ) {
					        	
					        	float_value = 2;
							}
						 value = (int) (float_value*4.5/2*100);
						 green_light.getGreen_light_dac_value().setText(((float)value/100.0)+"V,"+float_value+"A");
						break;
						
					case 0x03: // Blue
						 if ( float_value > 3.6 ) {
					        	
					        	float_value = (float) 3.6;
							}
						 value = (int) (float_value*4.5/3.6*100);
						 
						 blue_light.getBlue_light_dac_value().setText(((float)value/100.0)+"V,"+float_value+"A");
						break;
		
					case 0x04: // Green532
						 if ( float_value > 6 ) {
					        	
					        	float_value = 6;
							}
						 value = (int) (float_value*4.5/6.0*100);
						 green532_light.getGreen532_light_dac_value().setText(((float)value/100.0)+"V,"+float_value+"A");
						break;
	
					default:
						break;
				}	 
	        	
				
				temp_arry[0] = attrID; // attr ID
				temp_arry[1] = 0x02; // data length
				temp_arry[2] = value/100; // data 
				temp_arry[3] = value%100; // data
				
				
				SendCmd(WRITE, endpoint, temp_arry, 4);
	            

	        }
	    })
	    .setNegativeButton("取消", null)
	    .show();
		
		/*EditText editText = new EditText(this);
		new AlertDialog.Builder(this)  
		.setTitle("请输入")  
		.setIcon(android.R.drawable.ic_dialog_info)  
		.setView(editText )  
		.setPositiveButton("确定", null)  
		.setNegativeButton("取消", null)  
		.show();*/
	   
	}


	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

		int[] temp_arry = new int[50];
		
		switch (buttonView.getId()) {
		
			case R.id.red_light_power_switch:
				
					if (isChecked) {
						
						temp_arry[0] = 0x20; // attr ID
						temp_arry[1] = 0x01; // data length
						temp_arry[2] = 0x01; // data
						
						SendCmd(WRITE, 0x10, temp_arry, 3);
						//Toast.makeText(this, "red_switch_on", Toast.LENGTH_SHORT).show();
					}
					else {
						
						temp_arry[0] = 0x20; // attr ID
						temp_arry[1] = 0x01; // data length
						temp_arry[2] = 0x00; // data
						
						SendCmd(WRITE, 0x10, temp_arry, 3);
						
						//Toast.makeText(this, "red_switch_off", Toast.LENGTH_SHORT).show();
					}
				
				break;
				
			case R.id.green_light_power_switch:
				
					if (isChecked) {
						
						temp_arry[0] = 0x20; // attr ID
						temp_arry[1] = 0x01; // data length
						temp_arry[2] = 0x01; // data
						
						SendCmd(WRITE, 0x11, temp_arry, 3);
						
						//Toast.makeText(this, "green_switch_on", Toast.LENGTH_SHORT).show();
					}
					else {
						
						temp_arry[0] = 0x20; // attr ID
						temp_arry[1] = 0x01; // data length
						temp_arry[2] = 0x00; // data
						
						SendCmd(WRITE, 0x11, temp_arry, 3);
						//Toast.makeText(this, "green_switch_off", Toast.LENGTH_SHORT).show();
					}
				
				break;
				
			case R.id.blue_light_power_switch:
				
					if (isChecked) {
						
						temp_arry[0] = 0x20; // attr ID
						temp_arry[1] = 0x01; // data length
						temp_arry[2] = 0x01; // data
						
						SendCmd(WRITE, 0x12, temp_arry, 3);
						//Toast.makeText(this, "blue_switch_on", Toast.LENGTH_SHORT).show();
					}
					else {
						
						temp_arry[0] = 0x20; // attr ID
						temp_arry[1] = 0x01; // data length
						temp_arry[2] = 0x00; // data
						
						SendCmd(WRITE, 0x12, temp_arry, 3);
						//Toast.makeText(this, "blue_switch_off", Toast.LENGTH_SHORT).show();
					}
		
				break;
		
			case R.id.green532_light_power_switch:
		
					if (isChecked) {
						temp_arry[0] = 0x20; // attr ID
						temp_arry[1] = 0x01; // data length
						temp_arry[2] = 0x01; // data
						
						SendCmd(WRITE, 0x13, temp_arry, 3);
						//Toast.makeText(this, "green532_switch_on", Toast.LENGTH_SHORT).show();
					}
					else {
						temp_arry[0] = 0x20; // attr ID
						temp_arry[1] = 0x01; // data length
						temp_arry[2] = 0x00; // data
						
						SendCmd(WRITE, 0x13, temp_arry, 3);
						//Toast.makeText(this, "green532_switch_off", Toast.LENGTH_SHORT).show();
					}
				
				break;
				
			case R.id.all_power_switch:
				
				if (isChecked) {
					
					//temp_arry[0] = 0x01;
					
					//SendCmd(WRITE, 0x18, temp_arry, 1);
					
					red_light.getRed_light_power_switch().setChecked(true);
					green_light.getGreen_light_power_switch().setChecked(true);
					blue_light.getBlue_light_power_switch().setChecked(true);
					green532_light.getGreen532_light_power_switch().setChecked(true);
					
					//Toast.makeText(this, "all_power_switch_on", Toast.LENGTH_SHORT).show();
				}
				else {
					
					//temp_arry[0] = 0x00;
					
					//SendCmd(WRITE, 0x18, temp_arry, 1);
					
					red_light.getRed_light_power_switch().setChecked(false);
					green_light.getGreen_light_power_switch().setChecked(false);
					blue_light.getBlue_light_power_switch().setChecked(false);
					green532_light.getGreen532_light_power_switch().setChecked(false);
					
					
					//Toast.makeText(this, "all_power_switch_off", Toast.LENGTH_SHORT).show();
				}
			
			break;
	
			default:
				break;
		}
	}

	/*
	 * 将命令通过蓝牙发送出去
	 * add by Frank
	 */
	public void SendCmd(int cmd, int endpoint ,int[] data,int data_length)
	{
		OutputStream os = null;
		int[] temp_buf = new int[100];
		try {
			os = MainActivity._socket.getOutputStream();
		} catch (IOException e) {} 
		
		int frame_length = PROTOCOL_FIXED_LENGTH + data_length;
	
		try {
			
			temp_buf[0] = 0x5A;
			os.write(0x5A);
			
			temp_buf[1] = 0xA5;
			os.write(0xA5);
			
			temp_buf[2] = frame_length;
			os.write(frame_length);
			
			temp_buf[3] = cmd;
			os.write(cmd);
			
			temp_buf[4] = endpoint;
			os.write(endpoint);
			for (int i = 0; i<data_length;i++) {
				temp_buf[5+i] = data[i];
				os.write(data[i]);
			}
			
			os.write(get_check_sum(temp_buf,0,frame_length-1));
			
		} catch (IOException e) {} 
			
	}

	
	
}
