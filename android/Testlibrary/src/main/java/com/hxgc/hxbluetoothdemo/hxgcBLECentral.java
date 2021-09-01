/******************************************************************************
 * 
 * ----------------------------------------------------------------------------
 *                                 ����֪ʶ
 * ----------------------------------------------------------------------------
 * <һ> ���ʽ���
 * ATT                : Attribute Protocol (����Э��) - GATT�ǻ���ATT Protocol�ġ�ATT���BLE�豸����ר�ŵ��Ż�����������ڴ��������ʹ�þ����ٵ����ݡ�ÿ�����Զ���һ��Ψһ��UUID�����Խ���characteristics and services����ʽ���䡣
 *                        ATTʵ�������Կͻ��˺ͷ�����֮��ĵ�Ե�Э��. ATT�ͻ��˸�ATT����������������. ATT��������ATT�ͻ��˷��ͻظ���֪ͨ.
 * BLE                : Bluetooth Low Energy (�����͹���) - BLEʹ�������豸��ͨ��һ��Ŧ�۵�ع�����ά������������֮�á������ԣ�BLEʹ�������豸���ӱ�Զ�̿��ơ�ҽ�Ʊ������˶���Ӧ�����г����м�������Ӧ�ó�����
 *                        BLE��Android 4.3��ʼ֧��. BLE���������淶4.0.
 * Characteristic  : �������Ϊһ���������ͣ�������һ��value��0������Դ�value������(Descriptor).  
 * Descriptor      : ��Characteristic������, ���緶Χ��������λ��.
 * GATT             : Generic Attribute Profile - ��ʾ���������ԺͿͻ�������, ���������Է�������ʹ�õķ�����, �ص������. BLE�豸ʹ������Ϊ�����͹���Ӧ�ù淶�ķ�����. 
 * Service           : Characteristic�ļ���. ����һ��service���� "Heart Rate Monitor", �����ܰ������Characteristics, ���п��ܰ���һ��������heart rate measurement" ��Characteristic.
 * SMP              :  Security manager Protocol(��ȫ����Э��) - �������ɶԵ�Э��ļ�����Կ�������Կ. SMP���������Կ�������Կ�Ĵ洢, ��ͨ�����ɺͽ����豸�ĵ�ַ��ʶ�������豸.
 * 
 * ----------------------------------------------------------------------------
 *                                 ��ɫ��ְ��
 * ----------------------------------------------------------------------------
 *  Android�豸��BLE�豸�����������ɫ:
 *  [1] Central vs. peripheral (�����豸����Χ�豸).
 *       �����豸����Χ�豸�ĸ�����Ե���BLE���ӱ���. Central��ɫ����scan advertisement. ��peripheral��ɫ����make advertisement.
 *  [2] GATT server vs. GATT client.
 *       �����ֽ�ɫȡ����BLE���ӳɹ���, �����豸��ͨ�ŵķ�ʽ.
 * 
 * ����˵��:
 * ����һ���׷�ٵ�BLE�豸��һ��֧��BLE��Android�豸��Android�豸֧��Central��ɫ����BLE�豸֧��peripheral�� ɫ������һ��BLE
 * ������Ҫ��������ɫ�����ڣ�����֧��Central��ɫ���߶���֧��peripheral��ɫ���޷��������ӡ�
 * �����ӽ���������֮�����Ҫ����GATT���ݡ�˭��server��˭��client����ȡ���ھ������ݴ������������磬����׷�ٵ�BLE�豸��Ҫ
 * �� Android�豸����sensor���ݣ���׷������Ȼ��Ϊ��server�ˣ�������׷������Ҫ��Android�豸��ȡ������Ϣ���� Android�豸��
 * Ϊserver�˿��ܸ����ʡ�
 * 
 * Android BLE SDK���ĸ��ؼ��ࣺ
 * BluetoothGattServer��Ϊ�ܱ����ṩ���ݣ�BluetoothGattServerCallback�����ܱߵ�״̬��
 * BluetoothGatt��Ϊ������ʹ�úʹ������ݣ�BluetoothGattCallback���������״̬���ܱ��ṩ�����ݡ� 
 * 
 * ----------------------------------------------------------------------------
 *                                Ȩ�޼�feature
 * ----------------------------------------------------------------------------
 * �;�������һ����Ӧ��ʹ����������Ҫ����BLUETOOTHȨ�ޣ������Ҫɨ���豸���߲����������ã�����ҪBLUETOOTH_ADMINȨ�ޣ�
 * <uses-permission android:name="android.permission.BLUETOOTH"/>
 * <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
 *
 * ��������Ȩ���⣬�����ҪBLE feature����Ҫ����uses-feature��
 * <uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
 * requiredΪtrueʱ����Ӧ��ֻ����֧��BLE��Android�豸�ϰ�װ���У�requiredΪfalseʱ��Android�豸����������װ���У���Ҫ�ڴ�������ʱ�ж��豸�Ƿ�֧��BLE feature��
 * // Use this check to determine whether BLE is supported on the device. Then
 * // you can selectively disable BLE-related features.
*  if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) 
*  {
*    Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
*    finish();
*  }
* 
* 
******************************************************************************/

package com.hxgc.hxbluetoothdemo;


import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.widget.Toast;


public class hxgcBLECentral 
{
	public static final int STATE_SCAN_END = 10000;
	
	protected Activity m_init_oActivity = null;
	protected hxgcBLECentralEvent m_init_oBLECentralEvent = null;
	
	protected boolean bIsInited = false; //
	protected boolean m_bBluetoothInitialstatus = false; //������ʼ״̬. true ��APP����ǰ, �����豸�ǿ��ŵ�; false ��APP����ǰ, �����豸�ǹ��ŵ�.
	
	protected BluetoothManager m_bluetoothManager = null;
	protected BluetoothAdapter m_bluetoothAdapter = null;
	
	protected BluetoothAdapter.LeScanCallback m_LeScanCallback = null;
	protected BluetoothGattCallback m_BluetoothGattCallback = null;
	
	protected boolean m_bIsGetSrvOK = false;
	
	protected BluetoothGatt m_Gatt = null;
	protected BluetoothGattService m_sendService = null;
	protected BluetoothGattCharacteristic  m_sendCharacteristic = null;
	
	protected static final long m_lSCAN_PERIOD = 10*1000;
	protected Handler m_handler_scan = null;
	protected Runnable m_runnable_scan = null;
	
	
	
	///////////////////////////////////////////////////////////////////////////
	//��  ��:  Init
	//��  ��:  ��ʼ��. 
	//           1. ��ȡ��������������. 
	//           2. ����ɨ��ص�.
	//           3. ����ͨ�����Իص�.
	//           4. �Զ���������.
	//��  ��:  _i_o_activity Activity һ��Active����, ���ڻ�ȡ��������������, �Լ�������������.
	//��  ��:  ����: int
	//           ����: 1 BLE������.
	//                   2 ��ȡ����������ʧ��.
	//                   3 ��ȡ��������������ʧ��.
	//                   4 ������ʧ��.
	//˵  ��: ����Init�ɹ������ʹ�ñ�����������.
	///////////////////////////////////////////////////////////////////////////
	//public int Init(Activity _i_o_activity)
	public int Init(hxgcBLECentralInit _i_o_init)
	{
		boolean bResult = true;
		
		m_init_oActivity = _i_o_init.m_oActivity;
		m_init_oBLECentralEvent = _i_o_init.m_oBLECentralEvent;
		
		//���BLE������
		bResult = m_init_oActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
		if(!bResult)
		{
			Toast.makeText(m_init_oActivity, "BLE������!!!", Toast.LENGTH_SHORT).show();
			return 1; //BLE������
		}
		
		//��ȡ����������
		m_bluetoothManager =(BluetoothManager) ( m_init_oActivity.getSystemService(Context.BLUETOOTH_SERVICE) );
		if(null == m_bluetoothManager)
		{
			Toast.makeText(m_init_oActivity, "��ȡ����������ʧ��", Toast.LENGTH_SHORT).show();
			return 2; //��ȡ����������ʧ��
		}
		
		//��ȡ��������������
		m_bluetoothAdapter = m_bluetoothManager.getAdapter();
		if(null == m_bluetoothAdapter)
		{
			Toast.makeText(m_init_oActivity, "��ȡ��������������ʧ��", Toast.LENGTH_SHORT).show();
			return 3; //��ȡ��������������ʧ��
		}

		//�豸ɨ��ص�����
		m_LeScanCallback = new BluetoothAdapter.LeScanCallback()
		{
			@Override
			public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) 
			{
				if(null != m_init_oBLECentralEvent)
				{
					m_init_oBLECentralEvent.On_LeScan(device, rssi, scanRecord);
				}
			}
		};
		
		//BluetoothGatt�ص����� //���ģ� http://www.eoeandroid.com/thread-563868-1-1.html?_dsign=843d16d6
		m_BluetoothGattCallback = new BluetoothGattCallback()
		{
			@Override //1 �յ��豸notifyֵ(�豸�ϱ�ֵ)  //Characteristic�������Ϊһ���������ͣ�������һ��value��0��n���Դ�value������(Descriptor)
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
            {
				//�ڴ˻ص��н��ն�������Ӧ��
				super.onCharacteristicChanged(gatt, characteristic);
				if(null != m_init_oBLECentralEvent)
				{
					m_init_oBLECentralEvent.On_CharacteristicChanged(gatt, characteristic);
				}
            }
			
			@Override //2 ������
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) 
			{
				    super.onCharacteristicRead(gatt, characteristic, status);
            }
			
			@Override //3 д����
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
				super.onCharacteristicWrite(gatt, characteristic, status);
				
                //if (status == BluetoothGatt.GATT_SUCCESS) 
                //{
                //}
                //else
                //{
                //}
                
                if(null != m_init_oBLECentralEvent)
                {
                	m_init_oBLECentralEvent.On_CharacteristicWrite(gatt, characteristic, status);
                }
            }
			
			@Override //4 ����״̬�仯
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
				super.onConnectionStateChange(gatt, status, newState);
				
                if (status == BluetoothGatt.GATT_SUCCESS) 
                {
                        if (newState == BluetoothGatt.STATE_CONNECTED) //���� 
                        {
                        	//Ѱ�ҷ���
                        	//���ӳɹ�֮������Ӧ������ȥѰ�ҷ���(��BluetoothGattService), ֻ��Ѱ�ҵ�����֮��, �ſ��Ժ��豸����ͨ��.
                        	gatt.discoverServices(); //�˺�����ص�onServicesDiscovered.
                        } 
                        else if (newState == BluetoothGatt.STATE_DISCONNECTED) //�Ͽ����� 
                        {
                        }
                }
                
                if(null != m_init_oBLECentralEvent)
                {
                	m_init_oBLECentralEvent.On_ConnectionStateChange(gatt, status, newState);
                }
            }
			
			@Override //5
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
            {
				super.onDescriptorRead(gatt, descriptor, status);
            }
			
			@Override //6
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
            {
				super.onDescriptorWrite(gatt, descriptor, status);
            }
			
			@Override //7 ��ȡ�����ź�ǿ��
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) 
			{
                    super.onReadRemoteRssi(gatt, rssi, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) 
                    {
                              //��ȡ��RSSI��  RSSI ��������� �� һ�� ��ֵ���� -33 �� ���ֵ�ľ���ֵԽС�������豸���ֻ�Խ��
                             //ͨ��BluetoothGatt.readRemoteRssi();����ȡ
                    }
            }
			
			@Override //8
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) 
			{
                    super.onReliableWriteCompleted(gatt, status);
            }
			
			@Override //9 ���ַ���
            public void onServicesDiscovered(BluetoothGatt gatt, int status) 
			{
                    super.onServicesDiscovered(gatt, status);

                    if (status == BluetoothGatt.GATT_SUCCESS) 
                    {
                    	 //Ѱ�ҵ�����
                    	m_bIsGetSrvOK = true;
                    }
                    
                    if(null != m_init_oBLECentralEvent)
                    {
                    	m_init_oBLECentralEvent.On_ServicesDiscovered(gatt, status);
                    }
            }
		};
		
		//��������
		if(m_bluetoothAdapter.isEnabled())
		{
			m_bBluetoothInitialstatus = true; //������ʼ״̬�Ǵ򿪵�
		}
		else
		{
			m_bBluetoothInitialstatus = false; //������ʼ״̬�ǹرյ�
			
			//������
			if(m_bluetoothAdapter.enable())
			{
				while(!m_bluetoothAdapter.isEnabled())
				{
					try 
					{
						Thread.sleep(200);
					} 
					catch (InterruptedException e) 
					{
						e.printStackTrace();
						return 4;
					}
				}
			}
			else
			{
				return 4; //������ʧ��
			}
		}
		
		//������־
		bIsInited = true;
		
		return 0;
	}
	
	//����ʼ��
	public void UnInit()
	{
		//ɾ��ɨ���ӳ������Դ
		if(null != m_handler_scan)
		{
			if(null != m_runnable_scan)
			{
				m_handler_scan.removeCallbacks(m_runnable_scan);
				m_runnable_scan = null;
			}
			m_handler_scan = null;
		}
				
		if(null != m_Gatt)
		{
			m_Gatt.close();
			m_Gatt = null;
		}
		
		//���豸�����Ŀ���״̬�ظ���APP����ǰ
		if(!m_bBluetoothInitialstatus)
		{
			if(m_bluetoothAdapter.isEnabled())
			{
				m_bluetoothAdapter.disable();
			}
		}
		else
		{
			if(!m_bluetoothAdapter.isEnabled())
			{
				m_bluetoothAdapter.enable();
			}
		}
		
		m_init_oActivity = null;
		m_init_oBLECentralEvent = null;
		
		bIsInited = false;
	}
	
	//��ʼɨ��(������ͬʱɨ��BLE�豸�ʹ�ͳ�����豸)
	@SuppressWarnings("deprecation")
	public int StartScan()
	{
		if(null == m_bluetoothAdapter)
		{
			Toast.makeText(m_init_oActivity, "û�л�ȡ���ش���", Toast.LENGTH_SHORT).show();
			return 1; //û�л�ȡ���ش���
		}
		
		if(null == m_LeScanCallback)
		{
			Toast.makeText(m_init_oActivity, "û�ж���ɨ��ص�", Toast.LENGTH_SHORT).show();
			return 2; //û�ж���ɨ��ص�
		}
		
		//����Ҫ�ر�Gatt, ��Ȼ�ٴ�ɨ������
		if(null != m_Gatt)
		{
			m_Gatt.disconnect();
			m_Gatt.close();
			m_Gatt = null;
		}
		
		///////////////////////////////////////////////////
		//��������ʱ��
		if(null == m_handler_scan)
    	{
    		m_handler_scan = new Handler();
    		if(null == m_handler_scan)
    		{
    			Toast.makeText(m_init_oActivity, "��ʼ��ɨ��ʱ��ʧ��", Toast.LENGTH_SHORT).show();
    			return 3; //��ʼ��ɨ��ʱ��ʧ��
    		}
    	}
		if(null == m_runnable_scan)
		{
			m_runnable_scan = new Runnable()
			{  
			   @Override  
			   public void run() 
			   {  
				   if(null != m_LeScanCallback)
				   {
					   m_bluetoothAdapter.stopLeScan(m_LeScanCallback);
					   m_handler_scan.removeCallbacks(m_runnable_scan);
					   m_init_oBLECentralEvent.On_LeScan(null, STATE_SCAN_END, null);
//					   Toast.makeText(m_init_oActivity, "ɨ��ʱ�����, �����¿�ʼɨ��.", Toast.LENGTH_SHORT).show();
					   //m_handler_scan = null;
					   //m_runnable_scan = null;
				   }
			   }   
		    };
		    if(null == m_runnable_scan)
		    {
		    	Toast.makeText(m_init_oActivity, "��ʼ��ɨ��ʱ��ʧ��", Toast.LENGTH_SHORT).show();
		    	return 3; //��ʼ��ɨ��ʱ��ʧ��
		    }
		}
		
		///////////////////////////////////////////////////
		//��ʼ����
		boolean bResult = m_bluetoothAdapter.startLeScan(m_LeScanCallback);
		if(!bResult)
		{
			Toast.makeText(m_init_oActivity, "����ɨ��ʧ��!!!", Toast.LENGTH_SHORT).show();
			return 4; //����ɨ��ʧ�� 
		}
		
		//��ʱ����ɨ��
		bResult = m_handler_scan.postDelayed(m_runnable_scan, m_lSCAN_PERIOD);
		if(!bResult)
		{
			Toast.makeText(m_init_oActivity, "����ɨ��ʱ��ʧ��!!!", Toast.LENGTH_SHORT).show();
			return 5; //����ɨ��ʱ��ʧ��
		}

		return 0;
	}
	
	//ֹͣɨ��
	@SuppressWarnings("deprecation")
	public int StopScan()
	{
		if(null == m_bluetoothAdapter)
		{
			return 1;
		}
		
		if(null == m_LeScanCallback)
		{
			return 2;
		}
		
		m_bluetoothAdapter.stopLeScan(m_LeScanCallback);
		
		return 0;
	}

	//�����豸
	public BluetoothGatt ConnectDevice(BluetoothDevice _i_o_device)
	{
		if(null != m_Gatt)
		{
			m_Gatt.disconnect();
			m_Gatt.close();
			m_Gatt = null;
		}
		
		m_Gatt = _i_o_device.connectGatt(m_init_oActivity,                   //Context context 
				                                         false,                                 //boolean autoConnect
				                                         m_BluetoothGattCallback);  //BluetoothGattCallback callback
		
		if(null == m_Gatt)
		{
			return null;
		}
		
		//m_Gatt = oBluetoothGatt;
		
		//- autoConnect: Ϊfalse���̷���һ������, Ϊtrue�Զ�����, ֻҪ�����豸��ÿ���.
		//ʵ�ⷢ��, ��false���ӱȽϺ�, �ȽϿ�, true��ȸ�ʮ�������������ӲŻ�������. ����������һ�㶼����false, ɨ�赽bluetoothdevice֮��, ֱ����false���Ӽ���.
		//- callback: �ǳ���Ҫ�Ļص�����, �ֻ��������豸��һ��ͨ�Ž��������������.
		
		while(!m_bIsGetSrvOK)
		{
			try 
			{
				Thread.sleep(300);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
		
		m_bIsGetSrvOK = false;
		
		m_sendService = m_Gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
		if(null == m_sendService)
		{
			//oBluetoothGatt.disconnect();
			//oBluetoothGatt.close();
			m_sendService = null;
			m_sendCharacteristic = null;
			return m_Gatt;
		}
			
		m_sendCharacteristic = m_sendService.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
		if(null == m_sendCharacteristic)
		{
			//oBluetoothGatt.disconnect();
			//oBluetoothGatt.close();
			m_sendService = null;
			m_sendCharacteristic = null;
			return m_Gatt;
		}
		
		return m_Gatt;
	}
	
	//�Ͽ��豸
	public void DisconnectDevice(BluetoothGatt _i_o_BluetoothGatt)
	{
		_i_o_BluetoothGatt.disconnect();
		_i_o_BluetoothGatt.close();
		_i_o_BluetoothGatt = null;
	}
	
	//д
	public int  Send(BluetoothGatt _i_o_BluetoothGatt, byte[] _i_bys_write)
	{
		boolean bResult = false;
		
		//BluetoothGattService sendService = null;
		//BluetoothGattCharacteristic  sendCharacteristic = null;
		
		if(null == m_sendService)
		{
			m_sendService = _i_o_BluetoothGatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
			if(null == m_sendService)
			{
				return 1;
			}
		}
			
		if(null == m_sendCharacteristic)
		{
			m_sendCharacteristic = m_sendService.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
			if(null == m_sendCharacteristic)
			{
				return 2;
			}
		}
		
		if(!_i_o_BluetoothGatt.setCharacteristicNotification(m_sendCharacteristic, true))
		{
			return 3;
		}
		
		bResult = m_sendCharacteristic.setValue(_i_bys_write);
		if(!bResult)
		{
			return 4;
		}
		
		bResult = _i_o_BluetoothGatt.writeCharacteristic(m_sendCharacteristic); //д����豸
		if(!bResult)
		{
			return 5;
		}
		
		return 0;
	}

}

///////////////////////////////////////////////////////////////////////////////
//��ʼ������
class hxgcBLECentralInit
{
	public Activity m_oActivity = null;
	public hxgcBLECentralEvent m_oBLECentralEvent = null;
}

///////////////////////////////////////////////////////////////////////////////
//�¼���Ӧ
class hxgcBLECentralEvent
{
	//ɨ����Ӧ
	public void On_LeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
	{
		
	}
	
	//����״̬
	public void On_ConnectionStateChange(BluetoothGatt gatt, int status, int newState)
	{
		
	}
	
	//�ܱ��ϱ�����
	public void On_CharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
		
	}
	
	//д����
	public void On_CharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		
	}
	
	//Ѱ�ҷ���ص�
	public void On_ServicesDiscovered(BluetoothGatt gatt, int status)
	{
		
	}
	
}


