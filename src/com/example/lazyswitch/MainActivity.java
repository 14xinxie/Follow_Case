package com.example.lazyswitch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	
	private GestureDetector gestureDetector;
	
	private final static int REQUEST_CONNECT_DEVICE = 1;    //�궨���ѯ�豸���
	
	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP����UUID��
	
	private InputStream is;    //������������������������
	//private TextView text0;    //��ʾ������
    private EditText edit0;    //��������������
    private String smsg = "";    //��ʾ�����ݻ���
    
    public String filename=""; //��������洢���ļ���
    BluetoothDevice _device = null;     //�����豸
    BluetoothSocket _socket = null;      //����ͨ��socket
    boolean _discoveryFinished = false;    
    boolean bRun = true;
    boolean bThread = false;
    
    private ActionBar actionBar;
    private SharedPreferences sp;
    private Editor editor;
    private String ifSwitch ;
    private Button bu_turnon;
    
    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();    //��ȡ�����������������������豸
	private SensorManager sensorManager;
	
	private float rotate;// ��ֹ����ƶ����Ƕȳ������⣬��ÿ���ƶ�ʱ�����ƶ�������

	private MySensorEventListener listener;

	private TextView tv_nowAngle;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);   //���û���Ϊ������ main.xml
        
        
        actionBar = getActionBar();
        sp = getSharedPreferences("config", 0);
        editor = sp.edit();
        
        bu_turnon = (Button)findViewById(R.id.bu_turnon);
        tv_nowAngle=(TextView)findViewById(R.id.tv_nowAngle);
        
     // 1����ȡһ��SensorManager
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        // 2����ȡһ��ָ��type�Ĵ�����
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);// ���򴫸�����ʹ��deprecated�ģ����õļ��ݵͰ汾��
        // 3��ע��һ��������
        listener = new MySensorEventListener();
        sensorManager.registerListener(listener , sensor, SensorManager.SENSOR_DELAY_FASTEST);

    
        ifSwitch = sp.getString("switch", null);
        if(TextUtils.isEmpty(ifSwitch)){
        	editor.putString("switch", "on");
        	editor.commit();
        }
        
        
        gestureDetector = new GestureDetector(getApplicationContext(), new OnGestureListener() {
			
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}
			
			@Override
			public void onShowPress(MotionEvent e) {
			}
			
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
					float distanceY) {
				return false;
			}
			
			@Override
			public void onLongPress(MotionEvent e) {
			}
			
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY) {
				if(Math.abs(velocityY)<200){
					return false;
				}
				//���»���
				if(Math.abs(e2.getRawX() - e1.getRawX())<400&&(e2.getRawY() - e1.getRawY())>40){
					actionBar.show();
					
				}
				//���ϻ���
				if(Math.abs(e2.getRawX() - e1.getRawX())<400&&(e1.getRawY() - e2.getRawY())>40){
					actionBar.hide();
					
				}
				return true;
			}
			
			@Override
			public boolean onDown(MotionEvent e) {
				// TODO Auto-generated method stub
				return false;
			}
		}, handler);
        
       //����򿪱��������豸���ɹ�����ʾ��Ϣ����������
        if (_bluetooth == null){
        	Toast.makeText(this, "�޷����ֻ���������ȷ���ֻ��Ƿ����������ܣ�", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // �����豸���Ա�����  
       new Thread(){
    	   public void run(){
    		   if(_bluetooth.isEnabled()==false){
        		_bluetooth.enable();
    		   }
    	   }   	   
       }.start();      
    }
    
    public boolean onTouchEvent(MotionEvent event) {
    	gestureDetector.onTouchEvent(event);
    	return super.onTouchEvent(event);
    }

    //���Ͱ�����Ӧ
    public void onSendButtonClicked(View v){
    	int i=0;
    	int n=0;
    	try{
    		OutputStream os = _socket.getOutputStream();   //�������������
    		byte[] bos = edit0.getText().toString().getBytes();
    		for(i=0;i<bos.length;i++){
    			if(bos[i]==0x0a)n++;
    		}
    		byte[] bos_new = new byte[bos.length+n];
    		n=0;
    		for(i=0;i<bos.length;i++){ //�ֻ��л���Ϊ0a,�����Ϊ0d 0a���ٷ���
    			if(bos[i]==0x0a){
    				bos_new[n]=0x0d;
    				n++;
    				bos_new[n]=0x0a;
    			}else{
    				bos_new[n]=bos[i];
    			}
    			n++;
    		}
    		
    		os.write(bos_new);	
    	}catch(IOException e){  		
    	}  	
    }
    
    
    
    
    
    Thread m=new Thread(new MyThread());
   
    public void onTurnOnClicked(View view){
    	if(sp.getBoolean("isConnected", false)==true){
    		if(sp.getString("switch", null).equals("on")){
        		m.start();
        		editor.putString("switch", "off");
        		editor.commit();
        		bu_turnon.setBackgroundResource(R.drawable.switch_off);
        		bu_turnon.setText("��������");
        	}else{
        		m.stop();
        		editor.putString("switch", "on");
        		editor.commit();
        		bu_turnon.setText("��ʼ����");
        		bu_turnon.setBackgroundResource(R.drawable.switch_on);
        	}
    	}else{

    		Toast.makeText(getApplicationContext(), "�������ܸ��ٻ���������", 0).show();
    	}
    	
    }
    
    
    
    public boolean onCreateOptionsMenu(Menu menu) {
        
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch (item.getItemId()) {
    	
		case R.id.quit:
			editor.putBoolean("isConnected", false);
	    	editor.commit();
			finish();
			break;
		case R.id.pin_stay:
			item.setIcon(R.drawable.pin_goaway);
			actionBar.hide();
			break;
		case android.R.id.home:
			editor.putBoolean("isConnected", false);
	    	editor.commit();
			finish();
			break;
		case R.id.about:
			Intent intent = new Intent(MainActivity.this,AboutActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    public void sendCommand(String command){
    	int i = 0;
    	int n = 0;
    	if(_socket!=null)
    	{
    	try {
			OutputStream os = _socket.getOutputStream();
			byte[] bos = command.getBytes();
			for(i=0;i<bos.length;i++){
    			if(bos[i]==0x0a)n++;
    		}
    		byte[] bos_new = new byte[bos.length+n];
    		n=0;
    		for(i=0;i<bos.length;i++){ //�ֻ��л���Ϊ0a,�����Ϊ0d 0a���ٷ���
    			if(bos[i]==0x0a){
    				bos_new[n]=0x0d;
    				n++;
    				bos_new[n]=0x0a;
    			}else{
    				bos_new[n]=bos[i];
    			}
    			n++;
    		}
    		os.write(bos_new);	
    		Toast.makeText(getApplicationContext(), "�������ݳɹ���", 0).show();
		} catch (IOException e) {
			
			Toast.makeText(getApplicationContext(), "��������ʧ�ܣ�", 0).show();
			e.printStackTrace();
		} 
    }
    	else
    	{
    		Toast.makeText(getApplicationContext(), "�������ܸ��ٻ���������", 0).show();
    	}
    }
    
    //���ջ�������ӦstartActivityForResult()
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode){
    	case REQUEST_CONNECT_DEVICE:     //���ӽ������DeviceListActivity���÷���
    		// ��Ӧ���ؽ��
            if (resultCode == Activity.RESULT_OK) {   //���ӳɹ�����DeviceListActivity���÷���
                // MAC��ַ����DeviceListActivity���÷���
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // �õ������豸���      
                _device = _bluetooth.getRemoteDevice(address);
 
                // �÷���ŵõ�socket
                try{
                	_socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                }catch(IOException e){
                	Toast.makeText(this, "����ʧ�ܣ�", Toast.LENGTH_SHORT).show();
                }
                //����socket
            	Button btn = (Button) findViewById(R.id.Button03);
                try{
                	_socket.connect();
                	Toast.makeText(this, "����"+_device.getName()+"�ɹ���", Toast.LENGTH_SHORT).show();
                	editor.putBoolean("isConnected", true);
                	editor.commit();
                	btn.setText("�Ͽ�");
                }catch(IOException e){
                	try{
                		Toast.makeText(this, "����ʧ�ܣ�", Toast.LENGTH_SHORT).show();
                		_socket.close();
                		_socket = null;
                	}catch(IOException ee){
                		Toast.makeText(this, "����ʧ�ܣ�", Toast.LENGTH_SHORT).show();
                	}
                	return;
                }
                
                //�򿪽����߳�
                try{
            		is = _socket.getInputStream();   //�õ���������������
            		}catch(IOException e){
            			Toast.makeText(this, "��������ʧ�ܣ�", Toast.LENGTH_SHORT).show();
            			return;
            		}
            		if(bThread==false){
            			ReadThread.start();
            			bThread=true;
            		}else{
            			bRun = true;
            		}
            }
    		break;
    	default:break;
    	}
    }
    
    //���������߳�
    Thread ReadThread=new Thread(){
    	
    	public void run(){
    		int num = 0;
    		byte[] buffer = new byte[1024];
    		byte[] buffer_new = new byte[1024];
    		int i = 0;
    		int n = 0;
    		bRun = true;
    		//�����߳�
    		while(true){
    			try{
    				while(is.available()==0){
    					while(bRun == false){}
    				}
    				while(true){
    					num = is.read(buffer);         //��������
    					n=0;
    					
    					String s0 = new String(buffer,0,num);
    					for(i=0;i<num;i++){
    						if((buffer[i] == 0x0d)&&(buffer[i+1]==0x0a)){
    							buffer_new[n] = 0x0a;
    							i++;
    						}else{
    							buffer_new[n] = buffer[i];
    						}
    						n++;
    					}
    					String s = new String(buffer_new,0,n);
    					smsg+=s;   //д����ջ���
    					if(is.available()==0)break;  //��ʱ��û�����ݲ�����������ʾ
    				}    	    		
    	    		}catch(IOException e){
    	    	}
    		}
    	}
    };
    
    //��Ϣ�������
    Handler handler= new Handler(){
    	public void handleMessage(Message msg){
    		super.handleMessage(msg);
    		tv_nowAngle.setText(smsg);
    		if(smsg.length()==2){
    			smsg = "";
    		}
    	}
    };
    
    //�رճ�����ô�����
    public void onDestroy(){
    	super.onDestroy();
    	if(_socket!=null)  //�ر�����socket
    	try{
    		_socket.close();
    	}catch(IOException e){}
    	_bluetooth.disable();  //�ر���������
    	
    	m.destroy();
    	sensorManager.unregisterListener(listener);
        listener = null;
    }
    
    
    
    //���Ӱ�����Ӧ����
    public void onConnectButtonClicked(View v){ 
    	if(_bluetooth.isEnabled()==false){  //����������񲻿�������ʾ
    		Toast.makeText(this, " ��������...", Toast.LENGTH_LONG).show();
    		return;
    	}
    	
    	
        //��δ�����豸���DeviceListActivity�����豸����
    	Button btn = (Button) findViewById(R.id.Button03);
    	if(_socket==null){
    		Intent serverIntent = new Intent(this, DeviceListActivity.class); //��ת��������
    		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //���÷��غ궨��
    	}
    	else{
    		bu_turnon.setBackgroundResource(R.drawable.switch_off);
    		 //�ر�����socket
    	    try{
    	    	
    	    	is.close();
    	    	_socket.close();
    	    	_socket = null;
    	    	bRun = false;
    	    	btn.setText("����");
    	    	
    	    }catch(IOException e){} 
    	    editor.putBoolean("isConnected", false);
	    	editor.commit();
    	}
    	return;
    }
     
    
    
    public class MyThread implements Runnable{
    	@Override
    	public void run(){
    		
    		//System.out.print("run������ִ�С�����������������������");
    		
    		
    		while(true)
    		{
    			try{
    				
    				Message message=new Message();
    				message.what=1;
    				handler1.sendMessage(message);
    				Thread.sleep(5000);
    				}
    			catch(InterruptedException e)
    			{
    				e.printStackTrace();
    			}
    		}
    	}
    }
    
    Handler handler1=new Handler(){
    	public void handleMessage(Message msg){
    		
    		
    		
    		sendCommand(String.valueOf(getrotate()));
    	}
    };
    
    /**
     * ������ڲ��࣬�����������ڲ��࣬��Ϊȡ��ע���ʱ����Ҫ�õ�
     */
    private class MySensorEventListener implements SensorEventListener {
 
        @Override
        public void onSensorChanged(SensorEvent event) {
            // �������򣬵�һ���ƶ�30�㣬�ڶ����ƶ�30�㣬
            float[] values = event.values;
            // 0=North, 90=East, 180=South, 270=West
            // float light = values[0];// ���ڹ��ߴ�������˵��values[0]:������ߵ�ǿ��
            //float jiaodu = values[0];// ���ڷ��򴫸�����˵��values[0]:�����������������ĽǶȣ�����Ϊ0���鿴api
            //System.out.println("�������ļнǣ�" + jiaodu);
            rotate = values[0];
            tv_nowAngle.setText(String.valueOf((int)rotate));
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
    public int getrotate()
    {
    	return (int)rotate;
    }
	

}
