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
//�¼���Ӧ��

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

	//���巢���¼��ĺ���
	public void sendEvent(ReactContext reactContext, String eventName, WritableMap params)
	{
		reactContext
				.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
				.emit(eventName,params);
	}
	//ɨ����Ӧ
	public void On_LeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
	{

		@SuppressWarnings("unused")
		long lThreadID = tool_thread.GetCurrentThreadID();
		//�����¼�,�¼���ΪEventName
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
		
//		m_oMainActivity.ShowStringInUI("ɨ�赽�豸:" + strDevName + " " + strDevMac);
		if(strDevName.equals("HX-BLE"))
		{
			m_oDevice = device;
			m_ohxgcBLECentral.StopScan();
			m_bIsFindDevice = true;
//			m_oMainActivity.ShowStringInUI("ɨ�����");
			m_Status = "1";
			params.putString("ble", "1");
			sendEvent(MainActivity.myContext,"EventReminder",params);

		}
		else if(strDevName.equals("ID-BLE"))
		{
			m_oDevice = device;
			m_ohxgcBLECentral.StopScan();
			m_bIsFindDevice = true;
//			m_oMainActivity.ShowStringInUI("ɨ�����");
			m_Status = "1";
			params.putString("ble", "2");
			sendEvent(MainActivity.myContext,"EventReminder",params);
		}
	}
	
	//����״̬
	public void On_ConnectionStateChange(BluetoothGatt gatt, int status, int newState)
    {
		Log.d("TAG", "On_ConnectionStateChange: ���ӳɹ�");
        if (status == BluetoothGatt.GATT_SUCCESS) 
        {
                if (newState == BluetoothGatt.STATE_CONNECTED) 
                {
                     // ���ӳɹ�
//                	m_oMainActivity.ShowStringInUI("���ӳɹ�.\n\n");

					//�����¼�,�¼���ΪEventName
					WritableMap params1 = Arguments.createMap();
					params1.putString("BluetoothGatt", "0");
					sendEvent(MainActivity.myContext,"EventGatt",params1);
                	
                } 
                else if(newState == BluetoothGatt.STATE_CONNECTING)
                {
                	//m_oMainActivity.ShowStringInUI("��������....\n");
                }
                else if (newState == BluetoothGatt.STATE_DISCONNECTED) 
                {
                      // �Ͽ�����
                	//m_oMainActivity.ShowStringInUI("���ӶϿ�.\n");
                }
        }
    }
	
	//�ܱ��ϱ�����
	public void On_CharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
		//long lThrdID = Thread.currentThread().getId();
		
		byte[] bysResp = characteristic.getValue();
		m_bIsGetIDCompleteResp = GetIDCardCompleteResp(bysResp);
	}
	
	//д����
	public void On_CharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		if (status == BluetoothGatt.GATT_SUCCESS) 
        {
			
        }
		else
		{
			m_oMainActivity.ShowStringInUI("Writeʧ��!!!\n");
		}
	}
	
	//Ѱ�ҷ���ص�
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
@SuppressWarnings("deprecation") //API 18�Ժ󣬳�����ActionBarActivity
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
	//���������Ķ���
	public static ReactContext myContext;
    /**
     * ����
     * */
    public void searchService(){
        @SuppressWarnings("unused")
        long lThreadID = tool_thread.GetCurrentThreadID();

        OnButton1();
    }
    /**
     * ����
     * */
    public void concatService(){
        OnButton2();
    }
    /**
     * ����
     * */
    public void readService(){
        //d���Σ���ζ�����
        boolean bIsMutiRead = false; //true:��ζ�;false:���ζ�

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
                        //Ҫ�������飬�����ٴε��ô�Runnable������ʵ��ÿ����ʵ��һ�εĶ�ʱ������
                        OnButton3();

                        //�ٴε��ô�Runnable����
                        m_handler.postDelayed(this, 1);
                    }
                };

                //ʹ��PostDelayed������1�������ô�Runnable����
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
		
		//��׿6.0��Ҫ��λȨ�޲���ʹ��BLE.
		//if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) //Build.VERSION_CODES.M = 0x17
		//{
		//	 int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
		//	 if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED)
		//	 {
		//		 ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
		//	 }
		//}
		
		////////////////////////////////////////////////////
		//�������
		
		//�����¼�
		m_oMyBLECentralEvent = new MyBLECentralEvent();
		m_oMyBLECentralEvent.m_oMainActivity = this;
		
		//BLE ���ĳ�ʼ������
		m_oInitParam = new hxgcBLECentralInit();
		m_oInitParam.m_oActivity = activity;
		m_oInitParam.m_oBLECentralEvent = m_oMyBLECentralEvent;
		
		//����BLE���Ķ���
		m_ohxgcBLECentral = new hxgcBLECentral();
		m_oMyBLECentralEvent.m_ohxgcBLECentral = m_ohxgcBLECentral;
		
		m_oMyBLECentralEvent.m_iRecvOffset = 0;
				
		///////////////////////////////////////////////////
		//����Ԫ��
//		m_textView01 = (TextView) findViewById(R.id.textView01);
		
		//��ť1
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
		
		//��ť2
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
		
		//��ť3
//		final Button button3 = (Button)findViewById(R.id.button3);
//		button3.setOnClickListener
//		(
//			new View.OnClickListener()
//            {
//			    public void onClick(View v)
//				{
//			    	//d���Σ���ζ�����
//			    	boolean bIsMutiRead = false; //true:��ζ�;false:���ζ�
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
//								    //Ҫ�������飬�����ٴε��ô�Runnable������ʵ��ÿ����ʵ��һ�εĶ�ʱ������
//									OnButton3();
//
//									//�ٴε��ô�Runnable����
//								    m_handler.postDelayed(this, 1);
//							   }
//					    	};
//
//					    	//ʹ��PostDelayed������1�������ô�Runnable����
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
		
		//��ʼ��
		int iResult = m_ohxgcBLECentral.Init(m_oInitParam);
		if(0 == iResult)
		{
//			ShowStringInUI("��ʼ���ɹ�.\n\n");
//			Toast.makeText(mActivity, "��ʼ���ɹ�", Toast.LENGTH_SHORT).show();
			callback.invoke(iResult);
		}
		else
		{
//			ShowStringInUI("��ʼ��ʧ��.\n");
			Toast.makeText(mActivity, "��ʼ��ʧ��", Toast.LENGTH_SHORT).show();
		}
		
		///////////////////////////////////////////////////
		//���������߳�
		//m_oTaskThread = new TaskThread();
		//m_oTaskThread.m_oActivity = this;
		//m_oTaskThread.m_oTextView = m_textView01;
		//m_oTaskThread.start();
	}


	public void onDestroy()
	{
		MyExit();
		//���ظ��෽��
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
//		//����˼: ����������߳�ID��1, �����������ص���ͬһ���߳�.
//		//long lThreadID = Thread.currentThread().getId();
//
//		int id = item.getItemId();
//
//		if (id == R.id.action_findDev) //��ʼ���������豸
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
//		else if (id == R.id.action_settings)  //�˳�
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
	
	//��ʾͼƬ
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
		byte bysCardSign[] = new byte[2];		//֤�����ͱ�ʶ
		
        //������ֶ�
        byte bysNameEN[] = new byte[120];		//Ӣ������
        byte bysNationCodeEx[] = new byte[6];	//����
        byte bysCardVer[] = new byte[4];		//֤���汾
        byte bysIssAuthCode[] = new byte[8];	//��֤���ش���   
        
		//�۰�̨�ֶ�
		byte bysPassNumber[] = new byte[18];	// ͨ��֤����
		byte bysIssueCount[] = new byte[4];		// ǩ������
		
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
		//��ȡ����
		iOffset = 16;
		
		//��ȡ֤�����ͱ�ʶ
        try 
        {
        	bysCardSign[0] = _i_bys_resp[iOffset + 248];
            bysCardSign[1] = _i_bys_resp[iOffset + 249];
			strCardSign = new String(bysCardSign, "UTF-16LE");
		} 
        catch (UnsupportedEncodingException e2) 
        {
			// TODO �Զ����ɵ� catch ��
			e2.printStackTrace();
		}
          
        //����֤�����ͽ�������
        if(strCardSign.equals("I"))//�����֤
        {
        	//��������֤��ȡ�ֶ�
            try
            {
	            //��ȡӢ������
	            j = 0;
	            for(i = iOffset; i < (iOffset + 120); i++)
	            {
	                bysNameEN[j] = _i_bys_resp[i];
	                j++;
	            }
            	strNameEN = new String(bysNameEN, "UTF-16LE");
            	iOffset += 120;

	            //��ȡ�Ա�
	            j = 0;
	            for(i = iOffset; i < (iOffset + 2); i++)
	            {
	                bysSexCode[j] = _i_bys_resp[i];
	                j++;
	            }
    			String strSexCode =new String(bysSexCode, "UTF-16LE");
    			strSex = getSexFromCode(strSexCode);
    			iOffset += 2;

	            //��ȡ���þ���֤��
	            j = 0;
	            for(i = iOffset; i < (iOffset + 30); i++)
	            {
	                bysIdCode[j] = _i_bys_resp[i];
	                j++;
	            }
            	strIdCode = new String(bysIdCode);
            	iOffset += 30;

	            //��ȡ��������
	            j = 0;
	            for(i = iOffset; i < (iOffset + 6); i++)
	            {
	                bysNationCodeEx[j] = _i_bys_resp[i];
	                j++;
	            }
	            strNationCodeEx = new String(bysNationCodeEx);
	            iOffset += 6;
	
	            //��ȡ��������
	            j = 0;
	            for(i = iOffset; i < (iOffset + 30); i++)
	            {
	                bysName[j] = _i_bys_resp[i];
	                j++;
	            }
	            strName = new String(bysName, "UTF-16LE");
	            iOffset += 30;
	
	            //��ȡ��Ч�ڿ�ʼ����
	            j = 0;
	            for(i = iOffset; i < (iOffset + 16); i++)
	            {
	                bysBeginDate[j] = _i_bys_resp[i];
	                j++;
	            }
	            strBeginDate = new String(bysBeginDate);
	            iOffset += 16;
	
	            //��ȡ��Ч�ڽ�������
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
	
	            //��ȡ����
	            j = 0;
	            for(i = iOffset; i < (iOffset + 16); i++)
	            {
	                bysBirth[j] = _i_bys_resp[i];
	                j++;
	            }
	            strBirth = new String(bysBirth, "UTF-16LE");
    			iOffset += 16;
	
	            //��ȡ֤���汾
	            j = 0;
	            for(i = iOffset; i < (iOffset + 4); i++)
	            {
	                bysCardVer[j] = _i_bys_resp[i];
	                j++;
	            }
	            strCardVer = new String(bysCardVer);
	            iOffset += 4;
	
	            //��ȡǩ�����ش���
	            j = 0;
	            for(i = iOffset; i < (iOffset + 8); i++)
	            {
	                bysIssAuthCode[j] = _i_bys_resp[i];
	                j++;
	            }
	            strIssAuthCode = new String(bysIssAuthCode);
	            iOffset += 8;
	
	          //֤�����ͱ�־���Ѵ������
	            iOffset += 2;
	
	            //Ԥ����
	            iOffset += 6;
            } 
			catch (UnsupportedEncodingException e2) 
			{
				e2.printStackTrace();
			}
	            
	        //��ʾ������Ϣ
//    		ShowStringInUI("Ӣ������:" + strNameEN + "\n");
//    		ShowStringInUI("�Ա�:" + strSex + "\n");
//    		ShowStringInUI("���þ���֤��:" + strIdCode + "\n");
//    		ShowStringInUI("��������:" + strNationCodeEx + "\n");
//    		ShowStringInUI("��������:" + strName + "\n");
//    		ShowStringInUI("��Ч����:" + strBeginDate + "-" + strEndDate + "\n");
//    		ShowStringInUI("����:" + strBirth + "\n");
//    		ShowStringInUI("֤���汾:" + strCardVer + "\n");
//    		ShowStringInUI("ǩ�����ش���:" + strIssAuthCode + "\n");
//    		ShowStringInUI("֤�����ͱ�ʶ:" + strCardSign + "\n");
//    		ShowStringInUI("\n");

//			successCallback.invoke(strNameEN,strIdCode);
			//�����¼�,�¼���ΪEventName
			WritableMap params = Arguments.createMap();
			params.putString("strName", strNameEN);
			params.putString("strIdCode", strIdCode);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"IdCardInfoEvent",params);
        }
        else if(strCardSign.equals("J"))//�۰�̨֤
        {
            try
            {
	        	//��ȡ����
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+30); i++)
	    		{
	    			bysName[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strName = new String(bysName, "UTF-16LE");
    			strName = strName.replace(" ", "");
    			iOffset += 30;
    		
	    		//��ȡ�Ա�
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+2); i++)
	    		{
	    			bysSexCode[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			String strSexCode =new String(bysSexCode, "UTF-16LE");
    			strSex = getSexFromCode(strSexCode);
    			iOffset += 2;
    		
	    		//Ԥ�� 4 ���ֽ�
	    		iOffset += 4;
    		
	    		//��ȡ����
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+16); i++)
	    		{
	    			bysBirth[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strBirth = new String(bysBirth, "UTF-16LE");
    			iOffset += 16;
    		
	    		//��ȡ��ַ
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+70); i++)
	    		{
	    			bysAddr[j] = _i_bys_resp[i];
	    			j++;
	    		}
	    		strAddr = new String(bysAddr, "UTF-16LE");
	    		iOffset += 70;
    		
	    		//��ȡ���֤��
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+36); i++)
	    		{
	    			bysIdCode[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strIdCode = new String(bysIdCode, "UTF-16LE");
    			iOffset += 36;
    		
	    		//��ȡǩ������
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+30); i++)
	    		{
	    			bysIssue[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strIssue = new String(bysIssue, "UTF-16LE");
    			iOffset += 30;
    		
	    		//��ȡ��Ч�ڿ�ʼ����
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+16); i++)
	    		{
	    			bysBeginDate[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strBeginDate = new String(bysBeginDate, "UTF-16LE");
    			iOffset += 16;
    		
	    		//��ȡ��Ч�ڽ�������
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
    		
	    		//��ȡͨ��֤����
	            j = 0;
	            for(i = iOffset; i < (iOffset + 18); i++)
	            {
	                bysPassNumber[j] = _i_bys_resp[i];
	                j++;
	            }
                strPassNumber = new String(bysPassNumber, "UTF-16LE");
                iOffset += 18;
            
	            //��ȡǩ������
	            j = 0;
	            for(i = iOffset; i < (iOffset + 4); i++)
	            {
	                bysIssueCount[j] = _i_bys_resp[i];
	                j++;
	            }
                strIssueCount = new String(bysIssueCount, "UTF-16LE");
	            iOffset += 4;
	            
	            //Ԥ�� 6 �ֽ�
	            iOffset += 6;
	           
	            //֤�����ͱ�־���Ѵ������
	            iOffset += 2;
	
	            //Ԥ����
	            iOffset += 6;
			} 
			catch (UnsupportedEncodingException e2) 
			{
				e2.printStackTrace();
			}
	    		
    		//��ʾ������Ϣ
//    		ShowStringInUI("����:" + strName + "\n");
//    		ShowStringInUI("�Ա�:" + strSex + "\n");
//    		ShowStringInUI("����:" + strBirth + "\n");
//    		ShowStringInUI("סַ:" + strAddr + "\n");
//    		ShowStringInUI("���֤��:" + strIdCode + "\n");
//    		ShowStringInUI("ǩ������:" + strIssue + "\n");
//    		ShowStringInUI("��Ч����:" + strBeginDate + "-" + strEndDate + "\n");
//    		ShowStringInUI("ͨ��֤��:" + strPassNumber + "\n");
//    		ShowStringInUI("ǩ������:" + strIssueCount + "\n");
//    		ShowStringInUI("֤�����ͱ�ʶ:" + strCardSign + "\n");
//    		ShowStringInUI("\n");
//			successCallback.invoke(strName,strIdCode);
			WritableMap params = Arguments.createMap();
			params.putString("strName", strName);
			params.putString("strIdCode", strIdCode);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"IdCardInfoEvent",params);
        }
        else//��ͨ�������֤
        {
        	try 
    		{
        		//��ȡ����
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+30); i++)
	    		{
	    			bysName[j] = _i_bys_resp[i];
	    			j++;
	    		}
    		
    			strName = new String(bysName, "UTF-16LE");
    			strName = strName.replace(" ", "");
    			iOffset += 30;
    		
	    		//��ȡ�Ա�
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
    		
	    		//��ȡ����
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
    		
	    		//��ȡ����
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+16); i++)
	    		{
	    			bysBirth[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strBirth = new String(bysBirth, "UTF-16LE");
    			iOffset += 16;
    		
	    		//��ȡ��ַ
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+70); i++)
	    		{
	    			bysAddr[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strAddr = new String(bysAddr, "UTF-16LE");
    			iOffset += 70;
    		
	    		//��ȡ���֤��
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+36); i++)
	    		{
	    			bysIdCode[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strIdCode = new String(bysIdCode, "UTF-16LE");
    			iOffset += 36;
    		
	    		//��ȡǩ������
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+30); i++)
	    		{
	    			bysIssue[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strIssue = new String(bysIssue, "UTF-16LE");
    			iOffset += 30;
    		
	    		//��ȡ��Ч�ڿ�ʼ����
	    		j = 0;
	    		for(i=iOffset; i<(iOffset+16); i++)
	    		{
	    			bysBeginDate[j] = _i_bys_resp[i];
	    			j++;
	    		}
    			strBeginDate = new String(bysBeginDate, "UTF-16LE");
    			iOffset += 16;
    		
    			//��ȡ��Ч�ڽ�������
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
    		
    		//��ʾ������Ϣ
//    		ShowStringInUI("����:" + strName + "\n");
//    		ShowStringInUI("�Ա�:" + strSex + "\n");
//    		ShowStringInUI("����:" + strNation + "\n");
//    		ShowStringInUI("����:" + strBirth + "\n");
//    		ShowStringInUI("סַ:" + strAddr + "\n");
//    		ShowStringInUI("���֤��:" + strIdCode + "\n");
//    		ShowStringInUI("ǩ������:" + strIssue + "\n");
//    		ShowStringInUI("��Ч����:" + strBeginDate + "-" + strEndDate + "\n");
//    		ShowStringInUI("\n");
//			successCallback.invoke(strName,strIdCode);
			WritableMap params = Arguments.createMap();
			params.putString("strName", strName);
			params.putString("strIdCode", strIdCode);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"IdCardInfoEvent",params);
        }
        
		//��Ƭ
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
		//��ʾ����
		
		//��Ƭ
//		if(null != bitmapIdPhoto)
//		{
//			showBitmap(bitmapIdPhoto);
//			ShowStringInUI("\n");
//		}
//
//		//��д���ݳ���
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
		
		//�ָ���'-'
		strCode += "-";
		
		//20101129
		byte bysCode02[] = new byte[4];
		bysCode02[0] = _i_bys_resp[17];
		bysCode02[1] = _i_bys_resp[16];
		bysCode02[2] = _i_bys_resp[15];
		bysCode02[3] = _i_bys_resp[14];
		lTemp = unsigned4BytesToInt(bysCode02, 0);
		strCode += Long.toString(lTemp);
		
		//�ָ���'-'
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
		
		//�ָ���'-'
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
		
		//��ʾ��ȫģ���
//		ShowStringInUI("��ȫģ���:\n" + strCode + "\n");
		
		m_strSamID = strCode;
        
		return true;
	}
	
	protected void OnButton1()
	{
		int iResult = 0;
		
		//ShowStringInUI2("");
		
		m_oMyBLECentralEvent.m_bIsFindDevice = false;
		
		//ɨ��
		iResult = m_ohxgcBLECentral.StartScan();
		if(0 == iResult)
		{
//			ShowStringInUI("��ʼɨ��......\n");
		}
		else
		{
			ShowStringInUI("ɨ������ʧ��!!!\n");
			if(1 == iResult)
			{
				ShowStringInUI("ERROR: û�л�ȡ���ش���\n");
			}
			else if(2 == iResult)
			{
				ShowStringInUI("ERROR: û�ж���ɨ��ص�\n");
			}
			else if(3 == iResult)
			{
				ShowStringInUI("ERROR: ��ʼ��ɨ��ʱ��ʧ��\n");
			}
			else if(4 == iResult)
			{
				ShowStringInUI("ERROR: ����ɨ�跽��ʧ��\n");
			}
			else if(5 == iResult)
			{
				ShowStringInUI("ERROR: ����ɨ��ʱ��ʧ��\n");
			}
		}
	}
	
	protected void OnButton2()
	{
		if(!m_oMyBLECentralEvent.m_bIsFindDevice)
		{
			ShowStringInUI("��δɨ�赽�豸.\n");
			return;
		}
		
		m_oMyBLECentralEvent.m_bIsFindDevice = false;
		
		//����
//		ShowStringInUI("��ʼ����......\n");
		m_oGatt =  m_ohxgcBLECentral.ConnectDevice(m_oMyBLECentralEvent.m_oDevice);
		if(null != m_oGatt)
		{
			//ShowStringInUI("���ӷ���ɹ�.\n");
		}
		else
		{
			ShowStringInUI("���ӷ���ʧ��!!!\n");
		}
		
		//��ȡ��ȫģ���
		m_oMyBLECentralEvent.m_iRecvOffset = 0;
		m_oMyBLECentralEvent.m_iLen1Len2 = 0;
		m_oMyBLECentralEvent.m_bIsGetIDCompleteResp = false;
		byte bysCmdReadSamID[] = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0x96, 0x69, 0x00, 0x03, 0x12, (byte)0xFF, (byte)0xEE};
		int iResult = m_ohxgcBLECentral.Send(m_oGatt, bysCmdReadSamID);
		if(0 == iResult)
		{
			//ShowStringInUI("����ȫģ��ŷ���ɹ�.\n\n");
		}
		else
		{
			ShowStringInUI("����ȫģ��ŷ���ʧ��!!!\n\n");
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
			ShowStringInUI("��ȡ��ȫģ���ʧ��!!!\n\n");
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
		
		//��ʾ��ȫģ���
//		ShowStringInUI("--------------------------------------------------------------------------------" + "\n");
//		ShowStringInUI("SAMID: " + m_strSamID + "\n");
//		ShowStringInUI("--------------------------------------------------------------------------------" + "\n");
		
		//��ʾ��ȡ����
//		String strReadNum = "�� " + m_iReadNum + " �ζ���\n\n";
//		ShowStringInUI(strReadNum);
		
		///////////////////////////////////////////////////
		//Ѱ��
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
			//ShowStringInUI("Ѱ������ɹ�.\n");
		}
		else
		{
//			ShowStringInUI("Ѱ������ʧ��!!!\n");
			status = 1;
			//�����¼�,�¼���ΪEventName
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
//			ShowStringInUI("Ѱ��ʧ��!!!\n");
			status = 1;
			paramss.putInt("searchCard", status);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"isSearchCard",paramss);
			return;
		}
		else
		{
			//ShowStringInUI("Ѱ���ɹ�.\n");
		}
		
		lTimeFindCardEnd = System.currentTimeMillis();
		lTimeFindCard = lTimeFindCardEnd - lTimeFindCardStart;
		//String strFindCardTime = String.format("Ѱ��ʱ�� %d ����.\n", lTimeFindCard);
		//ShowStringInUI(strFindCardTime);
		
		///////////////////////////////////////////////////
		//ѡ��
		
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
			//ShowStringInUI("ѡ������ɹ�.\n");
		}
		else
		{
			status = 2;
			paramss.putInt("searchCard", status);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"isSearchCard",paramss);
//			ShowStringInUI("ѡ������ʧ��!!!\n");
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
//			ShowStringInUI("ѡ��ʧ��!!!\n");
			status = 2;
			paramss.putInt("searchCard", status);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"isSearchCard",paramss);
			return;
		}
		else
		{
			//ShowStringInUI("ѡ���ɹ�!!!\n");
		}
		
		lTimeSelectCardEnd = System.currentTimeMillis();
		lTimeSelectCard = lTimeSelectCardEnd - lTimeSelectCardStart;
		//String strSelectCardTime = String.format("ѡ��ʱ�� %d ����.\n", lTimeSelectCard);
		//ShowStringInUI(strSelectCardTime);
		
		///////////////////////////////////////////////////
		//����
		
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
			//ShowStringInUI("��������ɹ�.\n");
		}
		else
		{
			status = 3;
			paramss.putInt("searchCard", status);
			m_oMyBLECentralEvent.sendEvent(MainActivity.myContext,"isSearchCard",paramss);
//			ShowStringInUI("��������ʧ��.\n");
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
//			ShowStringInUI("����ʧ��!!!\n");
		}
		
//		String strFindCardTime = String.format("Ѱ��ʱ�� %d ����.\n", lTimeFindCard);
//		ShowStringInUI(strFindCardTime);
//
//		String strSelectCardTime = String.format("ѡ��ʱ�� %d ����.\n", lTimeSelectCard);
//		ShowStringInUI(strSelectCardTime);
//
//		String strReadCardTime = String.format("����ʱ�� %d ����.\n", lTimeReadCard);
//		ShowStringInUI(strReadCardTime);
		
		
	} //OnButton3()
	
	protected String getSexFromCode(final String strSexCode)
	{
		if('0' == strSexCode.charAt(0))
		{
			return "δ֪";
		}
	    else if('1' == strSexCode.charAt(0))
		{
			return "��";
		}
		else if('2' == strSexCode.charAt(0))
		{
			return "Ů";
		}
		else if('9' == strSexCode.charAt(0))
		{
			return "δ˵��";
		}
		
		return "δ����";
		
	}
	
	protected String getNationFromCode(final String strNationCode)
	{
		if(strNationCode.equals("01"))
		{
			return "��";
		}
		else if(strNationCode.equals("02"))
		{
			return "�ɹ�";
		}
		else if(strNationCode.equals("03"))
		{
			return "��";
		}
		else if(strNationCode.equals("04"))
		{
			return "��";
		}
		else if(strNationCode.equals("05"))
		{
			return "ά���";
		}
		else if(strNationCode.equals("06"))
		{
			return "��";
		}
		else if(strNationCode.equals("07"))
		{
			return "��";
		}
		else if(strNationCode.equals("08"))
		{
			return "׳";
		}
		else if(strNationCode.equals("09"))
		{
			return "����";
		}
		else if(strNationCode.equals("10"))
		{
			return "����";
		}
		else if(strNationCode.equals("11"))
		{
			return "��";
		}
		else if(strNationCode.equals("12"))
		{
			return "��";
		}
		else if(strNationCode.equals("13"))
		{
			return "��";
		}
		else if(strNationCode.equals("14"))
		{
			return "��";
		}
		else if(strNationCode.equals("15"))
		{
			return "����";
		}
		else if(strNationCode.equals("16"))
		{
			return "����";
		}
		else if(strNationCode.equals("17"))
		{
			return "������";
		}
		else if(strNationCode.equals("18"))
		{
			return "��";
		}
		else if(strNationCode.equals("19"))
		{
			return "��";
		}
		else if(strNationCode.equals("20"))
		{
			return "����";
		}
		else if(strNationCode.equals("21"))
		{
			return "��";
		}
		else if(strNationCode.equals("22"))
		{
			return "�";
		}
		else if(strNationCode.equals("23"))
		{
			return "��ɽ";
		}
		else if(strNationCode.equals("24"))
		{
			return "����";
		}
		else if(strNationCode.equals("25"))
		{
			return "ˮ";
		}
		else if(strNationCode.equals("26"))
		{
			return "����";
		}
		else if(strNationCode.equals("27"))
		{
			return "����";
		}
		else if(strNationCode.equals("28"))
		{
			return "����";
		}
		else if(strNationCode.equals("29"))
		{
			return "�¶�����";
		}
		else if(strNationCode.equals("30"))
		{
			return "��";
		}
		else if(strNationCode.equals("31"))
		{
			return "���Ӷ�";
		}
		else if(strNationCode.equals("32"))
		{
			return "����";
		}
		else if(strNationCode.equals("33"))
		{
			return "Ǽ";
		}
		else if(strNationCode.equals("34"))
		{
			return "����";
		}
		else if(strNationCode.equals("35"))
		{
			return "����";
		}
		else if(strNationCode.equals("36"))
		{
			return "ë��";
		}
		else if(strNationCode.equals("37"))
		{
			return "����";
		}
		else if(strNationCode.equals("38"))
		{
			return "����";
		}
		else if(strNationCode.equals("39"))
		{
			return "����";
		}
		else if(strNationCode.equals("40"))
		{
			return "����";
		}
		else if(strNationCode.equals("41"))
		{
			return "������";
		}
		else if(strNationCode.equals("42"))
		{
			return "ŭ";
		}
		else if(strNationCode.equals("43"))
		{
			return "���α��";
		}
		else if(strNationCode.equals("44"))
		{
			return "����˹";
		}
		else if(strNationCode.equals("45"))
		{
			return "���¿�";
		}
		else if(strNationCode.equals("46"))
		{
			return "�°�";
		}
		else if(strNationCode.equals("47"))
		{
			return "����";
		}
		else if(strNationCode.equals("48"))
		{
			return "ԣ��";
		}
		else if(strNationCode.equals("49"))
		{
			return "��";
		}
		else if(strNationCode.equals("50"))
		{
			return "������";
		}
		else if(strNationCode.equals("51"))
		{
			return "����";
		}
		else if(strNationCode.equals("52"))
		{
			return "���״�";
		}
		else if(strNationCode.equals("53"))
		{
			return "����";
		}
		else if(strNationCode.equals("54"))
		{
			return "�Ű�";
		}
		else if(strNationCode.equals("55"))
		{
			return "���";
		}
		else if(strNationCode.equals("56"))
		{
			return "��ŵ";
		}
		else if(strNationCode.equals("97"))
		{
			return "����";
		}
		else if(strNationCode.equals("98"))
		{
			return "���Ѫͳ�й�����ʿ";
		}
		
		return "δ֪";
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
