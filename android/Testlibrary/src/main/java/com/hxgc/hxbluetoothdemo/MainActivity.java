package com.hxgc.hxbluetoothdemo;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Handler;


import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.react.bridge.ReactApplicationContext;
import com.hxgc.tool.tool_thread;

import java.io.UnsupportedEncodingException;

import cn.ywho.api.decode.DecodeWlt;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ReactContext;
///////////////////////////////////////////////////////////////////////////////
//事件响应类

@SuppressWarnings("deprecation")
class MyBLECentralEvent extends hxgcBLECentralEvent
{
	public MainActivity m_oMainActivity = null;
	public hxgcBLECentral m_ohxgcBLECentral = null;
	public BluetoothDevice m_oDevice = null;
	public boolean m_bIsFindDevice = false;
	
	public boolean m_bIsGetIDCompleteResp = false;
	public boolean m_bFindCradOK = false;
	public boolean m_bSelectCradOK = false;
	public boolean m_bReadCradOK = false;
	
	protected int m_iRecvBufSize = 1024*3;
	protected byte m_bysRecvBuffer[] = new byte[m_iRecvBufSize];
	protected int m_iRecvOffset = 0;
	protected int m_iLen1Len2 = 0;
	
	public String m_strRecvNum = "";
	private String m_Status = "0";

	//定义发送事件的函数
	public void sendEvent(ReactContext reactContext, String eventName, WritableMap params)
	{
		reactContext
				.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
				.emit(eventName,params);
	}
	//扫描响应
	public void On_LeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
	{

		@SuppressWarnings("unused")
		long lThreadID = tool_thread.GetCurrentThreadID();
		//发送事件,事件名为EventName
		WritableMap params = Arguments.createMap();

		if(null == device)
		{
			if(hxgcBLECentral.STATE_SCAN_END==rssi)
			{
				if(m_Status=="0"){
					params.putString("ble", "4");
					sendEvent(MainActivity.myContext,"EventReminder",params);
				}else{
					m_Status = "0";
				}
			}
			return;
		}


		
		String strDevName = device.getName();
		String strDevMac = device.getAddress();
		
		if(null == strDevName || strDevName.length() <=0)
		{
			return;
		}
		
//		m_oMainActivity.ShowStringInUI("扫描到设备:" + strDevName + " " + strDevMac);
		if(strDevName.equals("HX-BLE"))
		{
			m_oDevice = device;
			m_ohxgcBLECentral.StopScan();
			m_bIsFindDevice = true;
//			m_oMainActivity.ShowStringInUI("扫描结束");
			m_Status = "1";
			params.putString("ble", "1");
			sendEvent(MainActivity.myContext,"EventReminder",params);

		}
		else if(strDevName.equals("ID-BLE"))
		{
			m_oDevice = device;
			m_ohxgcBLECentral.StopScan();
			m_bIsFindDevice = true;
//			m_oMainActivity.ShowStringInUI("扫描结束");
			m_Status = "1";
			params.putString("ble", "2");
			sendEvent(MainActivity.myContext,"EventReminder",params);
		}
	}
	
	//连接状态
	public void On_ConnectionStateChange(BluetoothGatt gatt, int status, int newState)
    {
		Log.d("TAG", "On_ConnectionStateChange: 连接成功");
        if (status == BluetoothGatt.GATT_SUCCESS) 
        {
                if (newState == BluetoothGatt.STATE_CONNECTED) 
                {
                     // 连接成功
//                	m_oMainActivity.ShowStringInUI("连接成功.\n\n");

					//发送事件,事件名为EventName
					WritableMap params1 = Arguments.createMap();
					params1.putString("BluetoothGatt", "0");
					sendEvent(MainActivity.myContext,"EventGatt",params1);
                	
                } 
                else if(newState == BluetoothGatt.STATE_CONNECTING)
                {
                	//m_oMainActivity.ShowStringInUI("正在连接....\n");
                }
                else if (newState == BluetoothGatt.STATE_DISCONNECTED) 
                {
                      // 断开连接
                	//m_oMainActivity.ShowStringInUI("连接断开.\n");
                }
        }
    }
	
	//周边上报数据
	public void On_CharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
		//long lThrdID = Thread.currentThread().getId();
		
		byte[] bysResp = characteristic.getValue();
		m_bIsGetIDCompleteResp = GetIDCardCompleteResp(bysResp);
	}
	
	//写数据
	public void On_CharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		if (status == BluetoothGatt.GATT_SUCCESS) 
        {
			
        }
		else
		{
			m_oMainActivity.ShowStringInUI("Write失败!!!\n");
		}
	}
	
	//寻找服务回调
	public void On_ServicesDiscovered(BluetoothGatt gatt, int status)
	{
		
	}
	
	////////////////////////////////////////////////////////////////////////////
	protected boolean GetIDCardCompleteResp(byte[] _i_bys_resp)
	{
		int i = 0, j = 0;
		int iRespLen = _i_bys_resp.length;
		int iCurRecvLen = m_iRecvOffset + iRespLen;  
		
		j = 0;
		for(i = m_iRecvOffset; i< iCurRecvLen; i++, j++)
		{
			m_bysRecvBuffer[i] = _i_bys_resp[j];
		}
		
		m_iRecvOffset += iRespLen;
		
		if((0 == m_iLen1Len2) && (m_iRecvOffset >= 7))
		{
			m_iLen1Len2 = m_bysRecvBuffer[5] << 8;
			m_iLen1Len2 += m_bysRecvBuffer[6];
		}
		
		//////////////////////////////////////////////////////////////////
		if(0 != m_iLen1Len2)
		{
			   m_strRecvNum = "m_iRecvOffset = "  + m_iRecvOffset + " : " + "m_iLen1Len2 = " + m_iLen1Len2 + "\n";
			   //m_oMainActivity.ShowStringInUI2(m_strRecvNum);
		}
		//////////////////////////////////////////////////////////////////
		
		if((0 != m_iLen1Len2) && (m_iRecvOffset >= (m_iLen1Len2+7)))
		{
			return true;
		}
		
		return false;
	}
}

///////////////////////////////////////////////////////////////////////////////
//

@SuppressLint("DefaultLocale")
@SuppressWarnings("deprecation") //API 18以后，出现了ActionBarActivity
public class MainActivity
{
	//protected TaskThread m_oTaskThread = null;

	protected TextView m_textView01 = null;
	
	protected hxgcBLECentralInit m_oInitParam = null;
	protected hxgcBLECentral m_ohxgcBLECentral = null;
	protected MyBLECentralEvent m_oMyBLECentralEvent = null;
	
	protected BluetoothGatt m_oGatt = null;
	
	protected int m_iReadNum = 0;
	protected String m_strSamID = "";
	
	protected Handler m_handler = null;
	protected Runnable m_runnable = null;
    public Activity mActivity;
	//定义上下文对象
	public static ReactContext myContext;
    /**
     * 搜索
     * */
    public void searchService(){
        @SuppressWarnings("unused")
        long lThreadID = tool_thread.GetCurrentThreadID();

        OnButton1();
    }
    /**
     * 链接
     * */
    public void concatService(){
        OnButton2();
    }
    /**
     * 读卡
     * */
    public void readService(){
        //d单次，多次读控制
        boolean bIsMutiRead = false; //true:多次读;false:单次读

        if(bIsMutiRead)
        {
            if(null == m_handler)
            {
                m_handler = new Handler();
            }

            if(null == m_runnable)
            {
                m_runnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        //要做的事情，这里再次调用此Runnable对象，以实现每两秒实现一次的定时器操作
                        OnButton3();

                        //再次调用此Runnable对象
                        m_handler.postDelayed(this, 1);
                    }
                };

                //使用PostDelayed方法，1毫秒后调用此Runnable对象
                m_handler.postDelayed(m_runnable, 1);
            }
        }
        else
        {
            OnButton3();
        }
    }
	public void loadCreate(Activity activity, Context context,Callback callback)
	{

		@SuppressWarnings("unused")
		long lThreadID = tool_thread.GetCurrentThreadID();
        mActivity = activity;
		///////////////////////////////////
		//String strlibPath = System.getProperty("java.library.path"); //("java.library.path:);
		///////////////////////////////////
		
		//安卓6.0需要定位权限才能使用BLE.
		//if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) //Build.VERSION_CODES.M = 0x17
		//{
		//	 int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
		//	 if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED)
		//	 {
		//		 ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
		//	 }
		//}
		
		////////////////////////////////////////////////////
		//蓝牙相关
		
		//重载事件
		m_oMyBLECentralEvent = new MyBLECentralEvent();
		m_oMyBLECentralEvent.m_oMainActivity = this;
		
		//BLE 中心初始化参数
		m_oInitParam = new hxgcBLECentralInit();
		m_oInitParam.m_oActivity = activity;
		m_oInitParam.m_oBLECentralEvent = m_oMyBLECentralEvent;
		
		//创建BLE中心对象
		m_ohxgcBLECentral = new hxgcBLECentral();
		m_oMyBLECentralEvent.m_ohxgcBLECentral = m_ohxgcBLECentral;
		
		m_oMyBLECentralEvent.m_iRecvOffset = 0;
				
		///////////////////////////////////////////////////
		//界面元素
//		m_textView01 = (TextView) findViewById(R.id.textView01);
		
		//按钮1
//		final Button button1 = (Button)findViewById(R.id.button1);
//		button1.setOnClickListener
//		(
//			new View.OnClickListener()
//            {
//			    public void onClick(View v)
//				{
//			    	@SuppressWarnings("unused")
//					long lThreadID = tool_thread.GetCurrentThreadID();
//
//			    	OnButton1();
//			    }
//		    }
//        );
		
		//按钮2
//		final Button button2 = (Button)findViewById(R.id.button2);
//		button2.setOnClickListener
//		(
//			new View.OnClickListener()
//            {
//			    public void onClick(View v)
//				{
//			    	OnButton2();
//			    }
//		    }
//        );
		
		//按钮3
//		final Button button3 = (Button)findViewById(R.id.button3);
//		button3.setOnClickListener
//		(
//			new View.OnClickListener()
//            {
//			    public void onClick(View v)
//				{
//			    	//d单次，多次读控制
//			    	boolean bIsMutiRead = false; //true:多次读;false:单次读
//
//			    	if(bIsMutiRead)
//			    	{
//				    	if(null == m_handler)
//				    	{
//				    		m_handler = new Handler();
//				    	}
//
//				    	if(null == m_runnable)
//				    	{
//					    	m_runnable = new Runnable()
//					    	{
//							   @Override
//							   public void run()
//							   {
//								    //要做的事情，这里再次调用此Runnable对象，以实现每两秒实现一次的定时器操作
//									OnButton3();
//
//									//再次调用此Runnable对象
//								    m_handler.postDelayed(this, 1);
//							   }
//					    	};
//
//					    	//使用PostDelayed方法，1毫秒后调用此Runnable对象
//					    	m_handler.postDelayed(m_runnable, 1);
//				    	}
//			    	}
//			    	else
//			    	{
//			    		OnButton3();
//			    	}
//
//			    } //onClick(View v)
//		    }
//        );
		
		//初始化
		int iResult = m_ohxgcBLECentral.Init(m_oInitParam);
		if(0 == iResult)
		{
//			ShowStringInUI("初始化成功.\n\n");
//			Toast.makeText(mActivity, "初始化成功", Toast.LENGTH_SHORT).show();
			callback.invoke(iResult);
		}
		else
		{
//			ShowStringInUI("初始化失败.\n");
			Toast.makeText(mActivity, "初始化失败", Toast.LENGTH_SHORT).show();
		}
		
		///////////////////////////////////////////////////
		//启动任务线程
		//m_oTaskThread = new TaskThread();
		//m_oTaskThread.m_oActivity = this;
		//m_oTaskThread.m_oTextView = m_textView01;
		//m_oTaskThread.start();
	}


	public void onDestroy()
	{
		MyExit();
		//重载父类方法
    	System.exit(0);
    	
	}
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu)
//	{
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}

//	@Override
//	public boolean onOptionsItemSelected(MenuItem item)
//	{
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//
//		//有意思: 这个方法的线程ID是1, 和蓝牙搜索回调是同一个线程.
//		//long lThreadID = Thread.currentThread().getId();
//
//		int id = item.getItemId();
//
//		if (id == R.id.action_findDev) //开始搜索蓝牙设备
//		{
//			if(null != m_handler)
//			{
//				if(null != m_runnable)
//				{
//					m_handler.removeCallbacks(m_runnable);
//					m_runnable = null;
//				}
//				m_handler = null;
//			}
//			return true;
//		}
//		else if (id == R.id.action_settings)  //退出
//		{
//			//MyExit();
//			finish();
//			return true;
//		}
//
//		return super.onOptionsItemSelected(item);
//	}
	
	public void ShowStringInUI(final String _i_str)
	{
//		Log.d("ShowStringInUI", "ShowStringInUI: "+_i_str);
//		runOnUiThread
//		(
//            new Runnable()
//            {
//            	public void run()
//            	{
//            		if (m_textView01 != null)
//            		{
//            			m_textView01.append(_i_str);
//            		}
//            	}
//            }
//        );
	Toast.makeText(mActivity, _i_str, Toast.LENGTH_SHORT).show();
	}
	
	public void ShowStringInUI2(final String _i_str)
	{
//		runOnUiThread
//		(
//            new Runnable()
//            {
//            	public void run()
//            	{
//            		if (m_textView01 != null)
//            		{
//            			m_textView01.setText(_i_str);
//            		}
//            	}
//            }
//        );
//		Log.d("ShowStringInUI2", "ShowStringInUI: "+_i_str);
//		Toast.makeText(mActivity, _i_str, Toast.LENGTH_SHORT).show();
	}
	
	//显示图片
	public void showBitmap(final Bitmap bitmap)
	{
//		ImageSpan imgSpan =new ImageSpan(this, bitmap);
//		final SpannableString spanString = new SpannableString("icon");
//		spanString.setSpan(imgSpan, 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		runOnUiThread
//		(
//            new Runnable()
//            {
//            	public void run()
//            	{
//            		if (m_textView01 != null)
//            		{
//            			m_textView01.append(spanString);
//            		}
//            	}
//            }
//        );
	}
		
	protected void MyExit()
	{
		if(null != m_handler)
		{
			if(null != m_runnable)
			{
				m_handler.removeCallbacks(m_runnable);
				m_runnable = null;
			}
			m_handler = null;
		}
		
		m_ohxgcBLECentral.UnInit();
		//m_oTaskThread.Exit();
	}
	
	protected boolean IsFindIdOK(byte[] _i_bys_resp)
	{
		byte SW1 = _i_bys_resp[7];
		byte SW2 = _i_bys_resp[8];
		byte SW3 = _i_bys_resp[9];
		
		if( (0x0 != SW1)  ||  (0x0 != SW2) ||  ( ((byte)0x9F) != SW3) )
		{
			return false;
		}
		
		return true;	
	}
	
	protected boolean IsSelectIdOK(byte[] _i_bys_resp)
	{
		byte SW1 = _i_bys_resp[7];
		byte SW2 = _i_bys_resp[8];
		byte SW3 = _i_bys_resp[9];
		
		if( (0x0 != SW1)  ||  (0x0 != SW2) ||  ( ((byte)0x90) != SW3) )
		{
			return false;
		}
		
		return true;
	}
	
	protected boolean onReadIDData(byte[] _i_bys_resp)
	{
		byte SW1 = _i_bys_resp[7];
		byte SW2 = _i_bys_resp[8];
		byte SW3 = _i_bys_resp[9];
		
		if( (0x0 != SW1)  ||  (0x0 != SW2) ||  ( ((byte)0x90) != SW3) )
		{
			return false;
		}
		
		if(_i_bys_resp.length < 1024)
		{
			return false;
		}
		
		///////////////////////////////////////////////////
		//
		int i = 0;
		int j = 0;
		int iOffset = 0;
		
		byte bysName[] = new byte[30];
		byte bysSexCode[] = new byte[2];
		byte bysNationCode[] = new byte[4];
		byte bysBirth[] = new byte[16];
		byte bysAddr[] = new byte[70];
		byte bysIdCode[] = new byte[36];
		byte bysIssue[] = new byte[30];
		byte bysBeginDate[] = new byte[16];
		byte bysEndDate[] = new byte[16];
		byte bysCardSign[] = new byte[2];		//证件类型标识
		
        //外国人字段
        byte bysNameEN[] = new byte[120];		//英文姓名
        byte bysNationCodeEx[] = new byte[6];	//国籍
        byte bysCardVer[] = new byte[4];		//证件版本
        byte bysIssAuthCode[] = new byte[8];	//发证机关代码   
        
		//港澳台字段
		byte bysPassNumber[] = new byte[18];	// 通行证号码
		byte bysIssueCount[] = new byte[4];		// 签发次数
		
		String strName = null;
		String strSex = null;
		String strNation = null;
		String strBirth = null;
		String strAddr = null;
		String strIdCode = null;
		String strIssue = null;
		String strBeginDate = null;
		String strEndDate = null;
		String strCardSign = null;
		
		String strNameEN = null;
		String strNationCodeEx = null;
		String strCardVer = null;
		String strIssAuthCode = null;
		
		String strPassNumber = null;
		String strIssueCount = null;
			
		int iTextSize = 0;
		int iPhotoSize = 0;
		@SuppressWarnings("unused")
		int iFingerSize = 0;
		
		iTextSize = _i_bys_resp[10] << 8 + _i_bys_resp[11];
		iPhotoSize = _i_bys_resp[12] << 8 + _i_bys_resp[13];
		iFingerSize = _i_bys_resp[14] << 8 + _i_bys_resp[15];
		
		///////////////////////////////////////////////////
		//截取数据
		iOffset = 16;
		
		//获取证件类型标识
        try 
        {
        	bysCardSign[0] = _i_bys_resp[iOffset + 248];
            bysCardSign[1] = _i_bys_resp[iOffset + 249];
			strCardSign = new String(bysCardSign, "UTF-16LE");
		} 
        catch (UnsupportedEncodingException e2) 
        {
			// TODO 自动生成的 catch 块
			e2.printStackTrace();
		}
          
        //根据证件类型解析数据
        if(strCardSign.equals("I"))//外国人证
        {
        	//外国人身份证截取字段
            try
            {
	            //截取英文姓名
	            j = 0;
	            for(i = iOffset; i < (iOffset + 120); i++)
	            {
	                bysNameEN[j] = _i_bys_resp[i];
	                j++;
	            }
            	strNameEN = new String(bysNameEN, "UTF-16LE");
            	iOffset += 120;

	            //截取性别
	            j = 0;
	            for(i = iOffset; i < (iOffset + 2); i++)
	            {
	                bysSexCode[j] = _i_bys_resp[i];
	                j++;
	            }
    			String strSexCode =new String(bysSexCode, "UTF-16LE");
    			strSex = getSexFromCode(strSexCode);
    			iOffset += 2;

	            //截取永久居留证号
	            j = 0;
	            for(i = iOffset; i < (iOffset + 30); i++)
	            {
	                bysIdCode[j] = _i_bys_resp[i];
	                j++;
	            }
            	strIdCode = new String(bysIdCode);
            	iOffset += 30;

	            //截取国籍代码
	            j = 0;
	            for(i = iOffset; i < (iOffset + 6); i++)
	            {
	                bysNationCodeEx[j] = _i_bys_resp[i];
	                j++;
	            }
	            strNationCodeEx = new String(bysNationCodeEx);
	            iOffset += 6;
	
	            //截取中文姓名
	            j = 0;
	            for(i = iOffset; i < (iOffset + 30); i++)
	            {
	                bysName[j] = _i_bys_resp[i];
	                j++;
	            }
	            strName = new String(bysName, "UTF-16LE");
	            iOffset += 30;
	
	            //截取有效期开始日期
	            j = 0;
	            for(i = iOffset; i < (iOffset + 16); i++)
	            {
	                bysBeginDate[j] = _i_bys_resp[i];
	                j++;
	            }
	            strBeginDate = new String(bysBeginDate);
	            iOffset += 16;
	
	            //截取有效期结束日期
	            j = 0;
	            for(i = iOffset; i < (iOffset + 16); i++)
	            {
	                bysEndDate[j] = _i_bys_resp[i];
	                j++;
	            }
	            if(bysEndDate[0] >= '0' && bysEndDate[0] <= '9')
    			{
    				strEndDate = new String(bysEndDate, "UTF-16LE");
    			}
    			else
    			{
    				strEndDate = new String(bysEndDate, "UTF-16LE");
    			}
    			iOffset += 16;
	
	            //截取生日
	            j = 0;
	            for(i = iOffset; i < (iOffset + 16); i++)
	            {
	                bysBirth[j] = _i_bys_resp[i];
	                j++;
	            }
	            strBirth = new String(bysBirth, "UTF-16LE");
    			iOffset += 16;
	
	            //截取证件版本
	            j = 0;
	            for(i = iOffset; i < (iOffset + 4); i++)
	            {
	                bysCardVer[j] = _i_bys_resp[i];
	                j++;
	            }
	            strCardVer = new String(bysCardVer);
	            iOffset += 4;
	
	            //截取签发机关代码
	            j = 0;
	            for(i = iOffset; i < (iOffset + 8); i++)
	            {
	                bysIssAuthCode[j] = _i_bys_resp[i];
	                j++;
	            }
	            strIssAuthCode = new String(bysIssAuthCode);
	            iOffset += 8;
	
	          //证件类型标志（已处理过）
	            iOffset += 2;
	
	            //预留项
	            iOffset += 6;
            } 
			catch (UnsupportedEncodingException e2) 
			{
				e2.printStackTrace();
			}
	            
	        //显示文字信息
//    		ShowStringInUI("英文姓名:" + strNameEN + "\n");
//    		ShowStringInUI("性别:" + strSex + "\n");
//    		ShowStringInUI("永久居留证号:" + strIdCode + "\n");
//    		ShowStringInUI("国籍代码:" + strNationCodeEx + "\n");
//    		ShowStringInUI("中文姓名:" + strName + "\n");
//    		ShowStringInUI("有效期限:" + strBeginDate + "-" + strEndDate + "\n");
//    		ShowStringInUI("生日:" + strBirth + "\n");
//    		ShowStringInUI("证件版本:" + strCardVer + "\n");
//    		ShowStringInUI("签发机关代码:" + strIssAuthCode + "\n");
//    		ShowStringInUI("证件类型标识:" + strCardSign + "\n");
//    		ShowStringInUI("\n");

//			successCallback.invoke(strNameEN,strIdCode);
			//发送事件,事件名为EventName
			WritableMap params = Arguments.createMap();
			params.putString("strName", strNameEN);
			params.putString("strIdCode", strIdCode);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"IdCardInfoEvent",params);
        }
        else if(strCardSign.equals("J"))//港澳台证
        {
            try
            {
	        	//截取姓名
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+30); i++)
	    		{
	    			bysName[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strName = new String(bysName, "UTF-16LE");
    			strName = strName.replace(" ", "");
    			iOffset += 30;
    		
	    		//截取性别
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+2); i++)
	    		{
	    			bysSexCode[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			String strSexCode =new String(bysSexCode, "UTF-16LE");
    			strSex = getSexFromCode(strSexCode);
    			iOffset += 2;
    		
	    		//预留 4 个字节
	    		iOffset += 4;
    		
	    		//截取生日
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+16); i++)
	    		{
	    			bysBirth[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strBirth = new String(bysBirth, "UTF-16LE");
    			iOffset += 16;
    		
	    		//截取地址
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+70); i++)
	    		{
	    			bysAddr[j] = _i_bys_resp[i];
	    			j++;
	    		}
	    		strAddr = new String(bysAddr, "UTF-16LE");
	    		iOffset += 70;
    		
	    		//截取身份证号
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+36); i++)
	    		{
	    			bysIdCode[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strIdCode = new String(bysIdCode, "UTF-16LE");
    			iOffset += 36;
    		
	    		//截取签发机关
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+30); i++)
	    		{
	    			bysIssue[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strIssue = new String(bysIssue, "UTF-16LE");
    			iOffset += 30;
    		
	    		//截取有效期开始日期
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+16); i++)
	    		{
	    			bysBeginDate[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strBeginDate = new String(bysBeginDate, "UTF-16LE");
    			iOffset += 16;
    		
	    		//截取有效期结束日期
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+16); i++)
	    		{
	    			bysEndDate[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			if(bysEndDate[0] >= '0' && bysEndDate[0] <= '9')
    			{
    				strEndDate = new String(bysEndDate, "UTF-16LE");
    			}
    			else
    			{
    				strEndDate = new String(bysEndDate, "UTF-16LE");
    			}
    			iOffset += 16;
    		
	    		//截取通行证号码
	            j = 0;
	            for(i = iOffset; i < (iOffset + 18); i++)
	            {
	                bysPassNumber[j] = _i_bys_resp[i];
	                j++;
	            }
                strPassNumber = new String(bysPassNumber, "UTF-16LE");
                iOffset += 18;
            
	            //截取签发次数
	            j = 0;
	            for(i = iOffset; i < (iOffset + 4); i++)
	            {
	                bysIssueCount[j] = _i_bys_resp[i];
	                j++;
	            }
                strIssueCount = new String(bysIssueCount, "UTF-16LE");
	            iOffset += 4;
	            
	            //预留 6 字节
	            iOffset += 6;
	           
	            //证件类型标志（已处理过）
	            iOffset += 2;
	
	            //预留项
	            iOffset += 6;
			} 
			catch (UnsupportedEncodingException e2) 
			{
				e2.printStackTrace();
			}
	    		
    		//显示文字信息
//    		ShowStringInUI("姓名:" + strName + "\n");
//    		ShowStringInUI("性别:" + strSex + "\n");
//    		ShowStringInUI("出生:" + strBirth + "\n");
//    		ShowStringInUI("住址:" + strAddr + "\n");
//    		ShowStringInUI("身份证号:" + strIdCode + "\n");
//    		ShowStringInUI("签发机关:" + strIssue + "\n");
//    		ShowStringInUI("有效期限:" + strBeginDate + "-" + strEndDate + "\n");
//    		ShowStringInUI("通行证号:" + strPassNumber + "\n");
//    		ShowStringInUI("签发次数:" + strIssueCount + "\n");
//    		ShowStringInUI("证件类型标识:" + strCardSign + "\n");
//    		ShowStringInUI("\n");
//			successCallback.invoke(strName,strIdCode);
			WritableMap params = Arguments.createMap();
			params.putString("strName", strName);
			params.putString("strIdCode", strIdCode);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"IdCardInfoEvent",params);
        }
        else//普通居民身份证
        {
        	try 
    		{
        		//截取姓名
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+30); i++)
	    		{
	    			bysName[j] = _i_bys_resp[i];
	    			j++;
	    		}
    		
    			strName = new String(bysName, "UTF-16LE");
    			strName = strName.replace(" ", "");
    			iOffset += 30;
    		
	    		//截取性别
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+2); i++)
	    		{
	    			bysSexCode[j] = _i_bys_resp[i];
	    			j++;
	    		}
	    		String strSexCode=null;
    			strSexCode =new String(bysSexCode, "UTF-16LE");
    			strSex = getSexFromCode(strSexCode);
    			iOffset += 2;
    		
	    		//截取民族
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+4); i++)
	    		{
	    			bysNationCode[j] = _i_bys_resp[i];
	    			j++;
	    		}
	    		String strNationCode = null;
    			strNationCode = new String(bysNationCode, "UTF-16LE");
    			strNation = getNationFromCode(strNationCode);
    			iOffset += 4;
    		
	    		//截取生日
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+16); i++)
	    		{
	    			bysBirth[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strBirth = new String(bysBirth, "UTF-16LE");
    			iOffset += 16;
    		
	    		//截取地址
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+70); i++)
	    		{
	    			bysAddr[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strAddr = new String(bysAddr, "UTF-16LE");
    			iOffset += 70;
    		
	    		//截取身份证号
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+36); i++)
	    		{
	    			bysIdCode[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strIdCode = new String(bysIdCode, "UTF-16LE");
    			iOffset += 36;
    		
	    		//截取签发机关
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+30); i++)
	    		{
	    			bysIssue[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strIssue = new String(bysIssue, "UTF-16LE");
    			iOffset += 30;
    		
	    		//截取有效期开始日期
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+16); i++)
	    		{
	    			bysBeginDate[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strBeginDate = new String(bysBeginDate, "UTF-16LE");
    			iOffset += 16;
    		
    			//截取有效期结束日期
    			j = 0;
	    		for(i=iOffset; i<(iOffset+16); i++)
	    		{
	    			bysEndDate[j] = _i_bys_resp[i];
	    			j++;
	    		}

    			if(bysEndDate[0] >= '0' && bysEndDate[0] <= '9')
    			{
    				strEndDate = new String(bysEndDate, "UTF-16LE");
    			}
    			else
    			{
    				strEndDate = new String(bysEndDate, "UTF-16LE");
    			}

    			iOffset += 16;
    		} 
    		catch (UnsupportedEncodingException e2) 
    		{
    			e2.printStackTrace();
    		}
    		
    		//显示文字信息
//    		ShowStringInUI("姓名:" + strName + "\n");
//    		ShowStringInUI("性别:" + strSex + "\n");
//    		ShowStringInUI("民族:" + strNation + "\n");
//    		ShowStringInUI("出生:" + strBirth + "\n");
//    		ShowStringInUI("住址:" + strAddr + "\n");
//    		ShowStringInUI("身份证号:" + strIdCode + "\n");
//    		ShowStringInUI("签发机关:" + strIssue + "\n");
//    		ShowStringInUI("有效期限:" + strBeginDate + "-" + strEndDate + "\n");
//    		ShowStringInUI("\n");
//			successCallback.invoke(strName,strIdCode);
			WritableMap params = Arguments.createMap();
			params.putString("strName", strName);
			params.putString("strIdCode", strIdCode);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"IdCardInfoEvent",params);
        }
        
		//照片
//		byte[] wlt = new byte[1024];
//		byte[] bmp = new byte[14 + 40 + 308 * 126];
//		for(i=0; i<iPhotoSize; i++)
//		{
//			wlt[i] = _i_bys_resp[16+iTextSize+i];
//		}
//		//int iResult = JniCall.hxgc_Wlt2Bmp(wlt, bmp, 0);
//		int iResult = DecodeWlt.hxgc_Wlt2Bmp(wlt, bmp, 708);
//		Bitmap bitmapIdPhoto = null;
//		if(iResult != -1)
//		{
//			bitmapIdPhoto = BitmapFactory.decodeByteArray(bmp, 0, bmp.length);
//		}
		
		///////////////////////////////////////////////////
		//显示数据
		
		//照片
//		if(null != bitmapIdPhoto)
//		{
//			showBitmap(bitmapIdPhoto);
//			ShowStringInUI("\n");
//		}
//
//		//读写数据长度
//		ShowStringInUI(m_oMyBLECentralEvent.m_strRecvNum);
//		ShowStringInUI("\n");
		
		return true;
	}
	
	protected boolean onReadSamID(byte[] _i_bys_resp)
	{
		int i = 0;
		int j = 0;
		long lTemp = 0;
		
		byte SW1 = _i_bys_resp[7];
		byte SW2 = _i_bys_resp[8];
		byte SW3 = _i_bys_resp[9];
		
		String strCode = ""; 
		String strTemp = null;
		
		if( (0x0 != SW1)  ||  (0x0 != SW2) ||  ( ((byte)0x90) != SW3) )
		{
			return false;
		}
		
		//05.01
		byte bysCode01A[] = new byte[4];
		bysCode01A[0] = 0;
		bysCode01A[1] = 0;
		bysCode01A[2] = _i_bys_resp[11];
		bysCode01A[3] = _i_bys_resp[10];
		lTemp = unsigned4BytesToInt(bysCode01A, 0);
		strTemp = Long.toString(lTemp);
		j = 2 - strTemp.length();
		for(i=0; i<j; i++)
	    {
		   strCode += "0";
	    }
		strCode += Long.toString(lTemp);
		
		strCode += ".";
		
		byte bysCode01B[] = new byte[4];
		bysCode01B[0] = 0;
		bysCode01B[1] = 0;
		bysCode01B[2] = _i_bys_resp[13];
		bysCode01B[3] = _i_bys_resp[12];
		lTemp = unsigned4BytesToInt(bysCode01B, 0);
		strTemp = Long.toString(lTemp);
		j = 2 - strTemp.length();
		for(i=0; i<j; i++)
	    {
		   strCode += "0";
	    }
		strCode += Long.toString(lTemp);
		
		//分隔符'-'
		strCode += "-";
		
		//20101129
		byte bysCode02[] = new byte[4];
		bysCode02[0] = _i_bys_resp[17];
		bysCode02[1] = _i_bys_resp[16];
		bysCode02[2] = _i_bys_resp[15];
		bysCode02[3] = _i_bys_resp[14];
		lTemp = unsigned4BytesToInt(bysCode02, 0);
		strCode += Long.toString(lTemp);
		
		//分隔符'-'
		strCode += "-";
		
		//1228293
		byte bysCode03[] = new byte[4];
		bysCode03[0] = _i_bys_resp[21];
		bysCode03[1] = _i_bys_resp[20];
		bysCode03[2] = _i_bys_resp[19];
		bysCode03[3] = _i_bys_resp[18];
		lTemp = unsigned4BytesToInt(bysCode03, 0);
		strTemp = Long.toString(lTemp);
		j = 10 - strTemp.length();
		for(i=0; i<j; i++)
	    {
		   strCode += "0";
	    }
		strCode += Long.toString(lTemp);
		
		//分隔符'-'
		strCode += "-";
		
		//296863149
		byte bysCode04[] = new byte[4];
		bysCode04[0] = _i_bys_resp[25];
		bysCode04[1] = _i_bys_resp[24];
		bysCode04[2] = _i_bys_resp[23];
		bysCode04[3] = _i_bys_resp[22];
		lTemp = unsigned4BytesToInt(bysCode04, 0);
		strTemp = Long.toString(lTemp);
		j = 10 - strTemp.length();
		for(i=0; i<j; i++)
	    {
		   strCode += "0";
	    }
		strCode += Long.toString(lTemp);
		
		//显示安全模块号
//		ShowStringInUI("安全模块号:\n" + strCode + "\n");
		
		m_strSamID = strCode;
        
		return true;
	}
	
	protected void OnButton1()
	{
		int iResult = 0;
		
		//ShowStringInUI2("");
		
		m_oMyBLECentralEvent.m_bIsFindDevice = false;
		
		//扫描
		iResult = m_ohxgcBLECentral.StartScan();
		if(0 == iResult)
		{
//			ShowStringInUI("开始扫描......\n");
		}
		else
		{
			ShowStringInUI("扫描启动失败!!!\n");
			if(1 == iResult)
			{
				ShowStringInUI("ERROR: 没有获取本地代理\n");
			}
			else if(2 == iResult)
			{
				ShowStringInUI("ERROR: 没有定义扫描回调\n");
			}
			else if(3 == iResult)
			{
				ShowStringInUI("ERROR: 初始化扫描时间失败\n");
			}
			else if(4 == iResult)
			{
				ShowStringInUI("ERROR: 启动扫描方法失败\n");
			}
			else if(5 == iResult)
			{
				ShowStringInUI("ERROR: 设置扫描时间失败\n");
			}
		}
	}
	
	protected void OnButton2()
	{
		if(!m_oMyBLECentralEvent.m_bIsFindDevice)
		{
			ShowStringInUI("还未扫描到设备.\n");
			return;
		}
		
		m_oMyBLECentralEvent.m_bIsFindDevice = false;
		
		//连接
//		ShowStringInUI("开始连接......\n");
		m_oGatt =  m_ohxgcBLECentral.ConnectDevice(m_oMyBLECentralEvent.m_oDevice);
		if(null != m_oGatt)
		{
			//ShowStringInUI("连接发起成功.\n");
		}
		else
		{
			ShowStringInUI("连接发起失败!!!\n");
		}
		
		//读取安全模块号
		m_oMyBLECentralEvent.m_iRecvOffset = 0;
		m_oMyBLECentralEvent.m_iLen1Len2 = 0;
		m_oMyBLECentralEvent.m_bIsGetIDCompleteResp = false;
		byte bysCmdReadSamID[] = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0x96, 0x69, 0x00, 0x03, 0x12, (byte)0xFF, (byte)0xEE};
		int iResult = m_ohxgcBLECentral.Send(m_oGatt, bysCmdReadSamID);
		if(0 == iResult)
		{
			//ShowStringInUI("读安全模块号发起成功.\n\n");
		}
		else
		{
			ShowStringInUI("读安全模块号发起失败!!!\n\n");
			return;
		}
		
		while(!m_oMyBLECentralEvent.m_bIsGetIDCompleteResp)
		{
			try 
			{
				Thread.sleep(10);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}	
		}
		
		if(!onReadSamID(m_oMyBLECentralEvent.m_bysRecvBuffer))
		{
			ShowStringInUI("读取安全模块号失败!!!\n\n");
		}
	}
	
	protected void OnButton3()
	{
		WritableMap paramss = Arguments.createMap();
		int status = 0;
		//long lThrdID = Thread.currentThread().getId();
		
		int iResult = 0;
		
		m_iReadNum ++;
		
//		ShowStringInUI2("");
		
		//显示安全模块号
//		ShowStringInUI("--------------------------------------------------------------------------------" + "\n");
//		ShowStringInUI("SAMID: " + m_strSamID + "\n");
//		ShowStringInUI("--------------------------------------------------------------------------------" + "\n");
		
		//显示读取次数
//		String strReadNum = "第 " + m_iReadNum + " 次读卡\n\n";
//		ShowStringInUI(strReadNum);
		
		///////////////////////////////////////////////////
		//寻卡
		Long lTimeFindCardStart = System.currentTimeMillis();  
		Long lTimeFindCardEnd = 0L;
		long lTimeFindCard = 0L;
		
		m_oMyBLECentralEvent.m_iRecvOffset = 0;
		m_oMyBLECentralEvent.m_iLen1Len2 = 0;
		m_oMyBLECentralEvent.m_bIsGetIDCompleteResp = false;
		
		byte bysCmdFind[] = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0x96, 0x69, 0x00, 0x03, 0x20, 0x01, 0x22};
		iResult = m_ohxgcBLECentral.Send(m_oGatt, bysCmdFind);
		if(0 == iResult)
		{
			//ShowStringInUI("寻卡发起成功.\n");
		}
		else
		{
//			ShowStringInUI("寻卡发起失败!!!\n");
			status = 1;
			//发送事件,事件名为EventName
			paramss.putInt("searchCard", status);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"isSearchCard",paramss);
			return;
		}
		
//		while(!m_oMyBLECentralEvent.m_bIsGetIDCompleteResp)
//		{
//			try
//			{
//				Thread.sleep(10);
//			}
//			catch (InterruptedException e)
//			{
//				e.printStackTrace();
//			}
//		}

		int count = 0 ;

		while(!m_oMyBLECentralEvent.m_bIsGetIDCompleteResp)
		{
			if (count > 400){
				break;
			}
			try
			{
				Thread.sleep(10);
				count ++ ;
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}


		if(!IsFindIdOK(m_oMyBLECentralEvent.m_bysRecvBuffer))
		{
//			ShowStringInUI("寻卡失败!!!\n");
			status = 1;
			paramss.putInt("searchCard", status);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"isSearchCard",paramss);
			return;
		}
		else
		{
			//ShowStringInUI("寻卡成功.\n");
		}
		
		lTimeFindCardEnd = System.currentTimeMillis();
		lTimeFindCard = lTimeFindCardEnd - lTimeFindCardStart;
		//String strFindCardTime = String.format("寻卡时间 %d 毫秒.\n", lTimeFindCard);
		//ShowStringInUI(strFindCardTime);
		
		///////////////////////////////////////////////////
		//选卡
		
		Long lTimeSelectCardStart = System.currentTimeMillis();  
		Long lTimeSelectCardEnd = 0L;
		long lTimeSelectCard = 0L;
		
		m_oMyBLECentralEvent.m_iRecvOffset = 0;
		m_oMyBLECentralEvent.m_iLen1Len2 = 0;
		m_oMyBLECentralEvent.m_bIsGetIDCompleteResp = false;
		
		byte bysCmdSelect[] = {(byte)0xAA, (byte)0xAA,  (byte)0xAA, (byte)0x96, 0x69, 0x00, 0x03, 0x20, 0x02, 0x21};
		iResult = m_ohxgcBLECentral.Send(m_oGatt, bysCmdSelect);
		if(0 == iResult)
		{
			//ShowStringInUI("选卡发起成功.\n");
		}
		else
		{
			status = 2;
			paramss.putInt("searchCard", status);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"isSearchCard",paramss);
//			ShowStringInUI("选卡发起失败!!!\n");
			return;
		}
		
		while(!m_oMyBLECentralEvent.m_bIsGetIDCompleteResp)
		{
			try 
			{
				Thread.sleep(10);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
		
		if(!IsSelectIdOK(m_oMyBLECentralEvent.m_bysRecvBuffer))
		{
//			ShowStringInUI("选卡失败!!!\n");
			status = 2;
			paramss.putInt("searchCard", status);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"isSearchCard",paramss);
			return;
		}
		else
		{
			//ShowStringInUI("选卡成功!!!\n");
		}
		
		lTimeSelectCardEnd = System.currentTimeMillis();
		lTimeSelectCard = lTimeSelectCardEnd - lTimeSelectCardStart;
		//String strSelectCardTime = String.format("选卡时间 %d 毫秒.\n", lTimeSelectCard);
		//ShowStringInUI(strSelectCardTime);
		
		///////////////////////////////////////////////////
		//读卡
		
		Long lTimeReadCardStart = System.currentTimeMillis();
		Long lTimeReadCardEnd = 0L;
		long lTimeReadCard = 0L;
		
		m_oMyBLECentralEvent.m_iRecvOffset = 0;
		m_oMyBLECentralEvent.m_iLen1Len2 = 0;
		m_oMyBLECentralEvent.m_bIsGetIDCompleteResp = false;
		
		byte bysCmdRead[] = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0x96, 0x69, 0x00, 0x03, 0x30, 0x10, 0x23};
		iResult = m_ohxgcBLECentral.Send(m_oGatt, bysCmdRead);
		if(0 == iResult)
		{
			//ShowStringInUI("读卡发起成功.\n");
		}
		else
		{
			status = 3;
			paramss.putInt("searchCard", status);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"isSearchCard",paramss);
//			ShowStringInUI("读卡发起失败.\n");
			return;
		}
		
		int iWaitTotal = 10*1000;
		int iWaitStep = 10;
		int iCurWaitTiem = 0;
		while(!m_oMyBLECentralEvent.m_bIsGetIDCompleteResp)
		{
			try 
			{
				Thread.sleep(iWaitStep); //200
				if(iCurWaitTiem > iWaitTotal)
				{
					break;
				}
				iCurWaitTiem += iWaitStep;
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
		
		lTimeReadCardEnd = System.currentTimeMillis();
		lTimeReadCard = lTimeReadCardEnd - lTimeReadCardStart;
		
		if(!onReadIDData(m_oMyBLECentralEvent.m_bysRecvBuffer))
		{
			status = 3;
			paramss.putInt("searchCard", status);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"isSearchCard",paramss);
//			ShowStringInUI("读卡失败!!!\n");
		}
		
//		String strFindCardTime = String.format("寻卡时间 %d 毫秒.\n", lTimeFindCard);
//		ShowStringInUI(strFindCardTime);
//
//		String strSelectCardTime = String.format("选卡时间 %d 毫秒.\n", lTimeSelectCard);
//		ShowStringInUI(strSelectCardTime);
//
//		String strReadCardTime = String.format("读卡时间 %d 毫秒.\n", lTimeReadCard);
//		ShowStringInUI(strReadCardTime);
		
		
	} //OnButton3()
	
	protected String getSexFromCode(final String strSexCode)
	{
		if('0' == strSexCode.charAt(0))
		{
			return "未知";
		}
	    else if('1' == strSexCode.charAt(0))
		{
			return "男";
		}
		else if('2' == strSexCode.charAt(0))
		{
			return "女";
		}
		else if('9' == strSexCode.charAt(0))
		{
			return "未说明";
		}
		
		return "未定义";
		
	}
	
	protected String getNationFromCode(final String strNationCode)
	{
		if(strNationCode.equals("01"))
		{
			return "汉";
		}
		else if(strNationCode.equals("02"))
		{
			return "蒙古";
		}
		else if(strNationCode.equals("03"))
		{
			return "回";
		}
		else if(strNationCode.equals("04"))
		{
			return "藏";
		}
		else if(strNationCode.equals("05"))
		{
			return "维吾尔";
		}
		else if(strNationCode.equals("06"))
		{
			return "苗";
		}
		else if(strNationCode.equals("07"))
		{
			return "彝";
		}
		else if(strNationCode.equals("08"))
		{
			return "壮";
		}
		else if(strNationCode.equals("09"))
		{
			return "布依";
		}
		else if(strNationCode.equals("10"))
		{
			return "朝鲜";
		}
		else if(strNationCode.equals("11"))
		{
			return "满";
		}
		else if(strNationCode.equals("12"))
		{
			return "侗";
		}
		else if(strNationCode.equals("13"))
		{
			return "瑶";
		}
		else if(strNationCode.equals("14"))
		{
			return "白";
		}
		else if(strNationCode.equals("15"))
		{
			return "土家";
		}
		else if(strNationCode.equals("16"))
		{
			return "哈尼";
		}
		else if(strNationCode.equals("17"))
		{
			return "哈萨克";
		}
		else if(strNationCode.equals("18"))
		{
			return "傣";
		}
		else if(strNationCode.equals("19"))
		{
			return "黎";
		}
		else if(strNationCode.equals("20"))
		{
			return "傈僳";
		}
		else if(strNationCode.equals("21"))
		{
			return "佤";
		}
		else if(strNationCode.equals("22"))
		{
			return "畲";
		}
		else if(strNationCode.equals("23"))
		{
			return "高山";
		}
		else if(strNationCode.equals("24"))
		{
			return "拉祜";
		}
		else if(strNationCode.equals("25"))
		{
			return "水";
		}
		else if(strNationCode.equals("26"))
		{
			return "东乡";
		}
		else if(strNationCode.equals("27"))
		{
			return "纳西";
		}
		else if(strNationCode.equals("28"))
		{
			return "景颇";
		}
		else if(strNationCode.equals("29"))
		{
			return "柯尔克孜";
		}
		else if(strNationCode.equals("30"))
		{
			return "土";
		}
		else if(strNationCode.equals("31"))
		{
			return "达斡尔";
		}
		else if(strNationCode.equals("32"))
		{
			return "仫佬";
		}
		else if(strNationCode.equals("33"))
		{
			return "羌";
		}
		else if(strNationCode.equals("34"))
		{
			return "布朗";
		}
		else if(strNationCode.equals("35"))
		{
			return "撒拉";
		}
		else if(strNationCode.equals("36"))
		{
			return "毛南";
		}
		else if(strNationCode.equals("37"))
		{
			return "仡佬";
		}
		else if(strNationCode.equals("38"))
		{
			return "锡伯";
		}
		else if(strNationCode.equals("39"))
		{
			return "阿昌";
		}
		else if(strNationCode.equals("40"))
		{
			return "普米";
		}
		else if(strNationCode.equals("41"))
		{
			return "塔吉克";
		}
		else if(strNationCode.equals("42"))
		{
			return "怒";
		}
		else if(strNationCode.equals("43"))
		{
			return "乌孜别克";
		}
		else if(strNationCode.equals("44"))
		{
			return "俄罗斯";
		}
		else if(strNationCode.equals("45"))
		{
			return "鄂温克";
		}
		else if(strNationCode.equals("46"))
		{
			return "德昂";
		}
		else if(strNationCode.equals("47"))
		{
			return "保安";
		}
		else if(strNationCode.equals("48"))
		{
			return "裕固";
		}
		else if(strNationCode.equals("49"))
		{
			return "京";
		}
		else if(strNationCode.equals("50"))
		{
			return "塔塔尔";
		}
		else if(strNationCode.equals("51"))
		{
			return "独龙";
		}
		else if(strNationCode.equals("52"))
		{
			return "鄂伦春";
		}
		else if(strNationCode.equals("53"))
		{
			return "赫哲";
		}
		else if(strNationCode.equals("54"))
		{
			return "门巴";
		}
		else if(strNationCode.equals("55"))
		{
			return "珞巴";
		}
		else if(strNationCode.equals("56"))
		{
			return "基诺";
		}
		else if(strNationCode.equals("97"))
		{
			return "其他";
		}
		else if(strNationCode.equals("98"))
		{
			return "外国血统中国籍人士";
		}
		
		return "未知";
	} //getNationFromCode(final String strNationCode)
	
	protected  long unsigned4BytesToInt(byte[] buf, int pos) 
	{
        int firstByte = 0;
        int secondByte = 0;
        int thirdByte = 0;
        int fourthByte = 0;
        int index = pos;
        firstByte = (0x000000FF & ((int) buf[index]));
        secondByte = (0x000000FF & ((int) buf[index + 1]));
        thirdByte = (0x000000FF & ((int) buf[index + 2]));
        fourthByte = (0x000000FF & ((int) buf[index + 3]));
        index = index + 4;
        return ((long) (firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte)) & 0xFFFFFFFFL;
    }

} //class MainActivity
