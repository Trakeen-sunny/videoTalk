/******************************************************************************
 * 
 * ----------------------------------------------------------------------------
 *                                 基础知识
 * ----------------------------------------------------------------------------
 * <一> 名词解释
 * ATT                : Attribute Protocol (属性协议) - GATT是基于ATT Protocol的。ATT针对BLE设备做了专门的优化，具体就是在传输过程中使用尽量少的数据。每个属性都有一个唯一的UUID，属性将以characteristics and services的形式传输。
 *                        ATT实现了属性客户端和服务器之间的点对点协议. ATT客户端给ATT服务器发送请命令. ATT服务器向ATT客户端发送回复和通知.
 * BLE                : Bluetooth Low Energy (蓝牙低功耗) - BLE使得蓝牙设备可通过一粒纽扣电池供电以维持续工作数年之久。很明显，BLE使得蓝牙设备在钟表、远程控制、医疗保健及运动感应器等市场具有极光明的应用场景。
 *                        BLE从Android 4.3开始支持. BLE符合蓝牙规范4.0.
 * Characteristic  : 可以理解为一个数据类型，它包括一个value和0至多个对次value的描述(Descriptor).  
 * Descriptor      : 对Characteristic的描述, 例如范围、计量单位等.
 * GATT             : Generic Attribute Profile - 表示服务器属性和客户端属性, 描述了属性服务器中使用的服务层次, 特点和属性. BLE设备使用它作为蓝牙低功耗应用规范的服务发现. 
 * Service           : Characteristic的集合. 例如一个service叫做 "Heart Rate Monitor", 它可能包含多个Characteristics, 其中可能包含一个叫做“heart rate measurement" 的Characteristic.
 * SMP              :  Security manager Protocol(安全管理协议) - 用于生成对等协议的加密密钥和身份密钥. SMP管理加密密钥和身份密钥的存储, 它通过生成和解析设备的地址来识别蓝牙设备.
 * 
 * ----------------------------------------------------------------------------
 *                                 角色和职责
 * ----------------------------------------------------------------------------
 *  Android设备与BLE设备交互有两组角色:
 *  [1] Central vs. peripheral (中心设备和外围设备).
 *       中心设备和外围设备的概念针对的是BLE连接本身. Central角色负责scan advertisement. 而peripheral角色负责make advertisement.
 *  [2] GATT server vs. GATT client.
 *       这两种角色取决于BLE连接成功后, 两个设备间通信的方式.
 * 
 * 举例说明:
 * 现有一个活动追踪的BLE设备和一个支持BLE的Android设备。Android设备支持Central角色，而BLE设备支持peripheral角 色。创建一个BLE
 * 连接需要这两个角色都存在，都仅支持Central角色或者都仅支持peripheral角色则无法建立连接。
 * 当连接建立后，它们之间就需要传输GATT数据。谁做server，谁做client，则取决于具体数据传输的情况。例如，如果活动追踪的BLE设备需要
 * 向 Android设备传输sensor数据，则活动追踪器自然成为了server端；而如果活动追踪器需要从Android设备获取更新信息，则 Android设备作
 * 为server端可能更合适。
 * 
 * Android BLE SDK的四个关键类：
 * BluetoothGattServer作为周边来提供数据；BluetoothGattServerCallback返回周边的状态。
 * BluetoothGatt作为中央来使用和处理数据；BluetoothGattCallback返回中央的状态和周边提供的数据。 
 * 
 * ----------------------------------------------------------------------------
 *                                权限及feature
 * ----------------------------------------------------------------------------
 * 和经典蓝牙一样，应用使用蓝牙，需要声明BLUETOOTH权限，如果需要扫描设备或者操作蓝牙设置，则还需要BLUETOOTH_ADMIN权限：
 * <uses-permission android:name="android.permission.BLUETOOTH"/>
 * <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
 *
 * 除了蓝牙权限外，如果需要BLE feature则还需要声明uses-feature：
 * <uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
 * required为true时，则应用只能在支持BLE的Android设备上安装运行；required为false时，Android设备均可正常安装运行，需要在代码运行时判断设备是否支持BLE feature：
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
	protected boolean m_bBluetoothInitialstatus = false; //蓝牙初始状态. true 本APP启动前, 蓝牙设备是开着的; false 本APP启动前, 蓝牙设备是关着的.
	
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
	//函  数:  Init
	//功  能:  初始化. 
	//           1. 获取本地蓝牙适配器. 
	//           2. 定义扫描回调.
	//           3. 定义通用属性回调.
	//           4. 自动开启蓝牙.
	//参  数:  _i_o_activity Activity 一个Active对象, 用于获取本地蓝牙适配器, 以及其它蓝牙操作.
	//返  回:  类型: int
	//           意义: 1 BLE不可用.
	//                   2 获取蓝牙管理器失败.
	//                   3 获取本地蓝牙适配器失败.
	//                   4 打开蓝牙失败.
	//说  明: 必须Init成功后才能使用本类其它方法.
	///////////////////////////////////////////////////////////////////////////
	//public int Init(Activity _i_o_activity)
	public int Init(hxgcBLECentralInit _i_o_init)
	{
		boolean bResult = true;
		
		m_init_oActivity = _i_o_init.m_oActivity;
		m_init_oBLECentralEvent = _i_o_init.m_oBLECentralEvent;
		
		//检查BLE可用性
		bResult = m_init_oActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
		if(!bResult)
		{
			Toast.makeText(m_init_oActivity, "BLE不可用!!!", Toast.LENGTH_SHORT).show();
			return 1; //BLE不可用
		}
		
		//获取蓝牙管理器
		m_bluetoothManager =(BluetoothManager) ( m_init_oActivity.getSystemService(Context.BLUETOOTH_SERVICE) );
		if(null == m_bluetoothManager)
		{
			Toast.makeText(m_init_oActivity, "获取蓝牙管理器失败", Toast.LENGTH_SHORT).show();
			return 2; //获取蓝牙管理器失败
		}
		
		//获取本地蓝牙适配器
		m_bluetoothAdapter = m_bluetoothManager.getAdapter();
		if(null == m_bluetoothAdapter)
		{
			Toast.makeText(m_init_oActivity, "获取本地蓝牙适配器失败", Toast.LENGTH_SHORT).show();
			return 3; //获取本地蓝牙适配器失败
		}

		//设备扫描回调定义
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
		
		//BluetoothGatt回调定义 //参阅： http://www.eoeandroid.com/thread-563868-1-1.html?_dsign=843d16d6
		m_BluetoothGattCallback = new BluetoothGattCallback()
		{
			@Override //1 收到设备notify值(设备上报值)  //Characteristic可以理解为一个数据类型，它包括一个value和0至n个对此value的描述(Descriptor)
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
            {
				//在此回调中接收读卡器的应答
				super.onCharacteristicChanged(gatt, characteristic);
				if(null != m_init_oBLECentralEvent)
				{
					m_init_oBLECentralEvent.On_CharacteristicChanged(gatt, characteristic);
				}
            }
			
			@Override //2 读数据
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) 
			{
				    super.onCharacteristicRead(gatt, characteristic, status);
            }
			
			@Override //3 写数据
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
			
			@Override //4 连接状态变化
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
				super.onConnectionStateChange(gatt, status, newState);
				
                if (status == BluetoothGatt.GATT_SUCCESS) 
                {
                        if (newState == BluetoothGatt.STATE_CONNECTED) //连接 
                        {
                        	//寻找服务
                        	//连接成功之后，我们应该立刻去寻找服务(即BluetoothGattService), 只有寻找到服务之后, 才可以和设备进行通信.
                        	gatt.discoverServices(); //此函数会回调onServicesDiscovered.
                        } 
                        else if (newState == BluetoothGatt.STATE_DISCONNECTED) //断开连接 
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
			
			@Override //7 获取蓝牙信号强度
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) 
			{
                    super.onReadRemoteRssi(gatt, rssi, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) 
                    {
                              //获取到RSSI，  RSSI 正常情况下 是 一个 负值，如 -33 ； 这个值的绝对值越小，代表设备离手机越近
                             //通过BluetoothGatt.readRemoteRssi();来获取
                    }
            }
			
			@Override //8
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) 
			{
                    super.onReliableWriteCompleted(gatt, status);
            }
			
			@Override //9 发现服务
            public void onServicesDiscovered(BluetoothGatt gatt, int status) 
			{
                    super.onServicesDiscovered(gatt, status);

                    if (status == BluetoothGatt.GATT_SUCCESS) 
                    {
                    	 //寻找到服务
                    	m_bIsGetSrvOK = true;
                    }
                    
                    if(null != m_init_oBLECentralEvent)
                    {
                    	m_init_oBLECentralEvent.On_ServicesDiscovered(gatt, status);
                    }
            }
		};
		
		//启动蓝牙
		if(m_bluetoothAdapter.isEnabled())
		{
			m_bBluetoothInitialstatus = true; //蓝牙初始状态是打开的
		}
		else
		{
			m_bBluetoothInitialstatus = false; //蓝牙初始状态是关闭的
			
			//打开蓝牙
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
				return 4; //打开蓝牙失败
			}
		}
		
		//创建标志
		bIsInited = true;
		
		return 0;
	}
	
	//反初始化
	public void UnInit()
	{
		//删除扫描延迟相关资源
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
		
		//将设备蓝牙的开关状态回复到APP启动前
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
	
	//开始扫描(不可以同时扫描BLE设备和传统蓝牙设备)
	@SuppressWarnings("deprecation")
	public int StartScan()
	{
		if(null == m_bluetoothAdapter)
		{
			Toast.makeText(m_init_oActivity, "没有获取本地代理", Toast.LENGTH_SHORT).show();
			return 1; //没有获取本地代理
		}
		
		if(null == m_LeScanCallback)
		{
			Toast.makeText(m_init_oActivity, "没有定义扫描回调", Toast.LENGTH_SHORT).show();
			return 2; //没有定义扫描回调
		}
		
		//必须要关闭Gatt, 不然再次扫描会很慢
		if(null != m_Gatt)
		{
			m_Gatt.disconnect();
			m_Gatt.close();
			m_Gatt = null;
		}
		
		///////////////////////////////////////////////////
		//设置搜索时间
		if(null == m_handler_scan)
    	{
    		m_handler_scan = new Handler();
    		if(null == m_handler_scan)
    		{
    			Toast.makeText(m_init_oActivity, "初始化扫描时间失败", Toast.LENGTH_SHORT).show();
    			return 3; //初始化扫描时间失败
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
//					   Toast.makeText(m_init_oActivity, "扫描时间结束, 可重新开始扫描.", Toast.LENGTH_SHORT).show();
					   //m_handler_scan = null;
					   //m_runnable_scan = null;
				   }
			   }   
		    };
		    if(null == m_runnable_scan)
		    {
		    	Toast.makeText(m_init_oActivity, "初始化扫描时间失败", Toast.LENGTH_SHORT).show();
		    	return 3; //初始化扫描时间失败
		    }
		}
		
		///////////////////////////////////////////////////
		//开始搜索
		boolean bResult = m_bluetoothAdapter.startLeScan(m_LeScanCallback);
		if(!bResult)
		{
			Toast.makeText(m_init_oActivity, "启动扫描失败!!!", Toast.LENGTH_SHORT).show();
			return 4; //启动扫描失败 
		}
		
		//延时结束扫描
		bResult = m_handler_scan.postDelayed(m_runnable_scan, m_lSCAN_PERIOD);
		if(!bResult)
		{
			Toast.makeText(m_init_oActivity, "设置扫描时间失败!!!", Toast.LENGTH_SHORT).show();
			return 5; //设置扫描时间失败
		}

		return 0;
	}
	
	//停止扫描
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

	//连接设备
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
		
		//- autoConnect: 为false立刻发起一次连接, 为true自动连接, 只要蓝牙设备变得可用.
		//实测发现, 用false连接比较好, 比较快, true会等个十几秒甚至几分钟才会连接上. 开发过程中一般都是用false, 扫描到bluetoothdevice之后, 直接用false连接即可.
		//- callback: 非常重要的回调函数, 手机与蓝牙设备的一切通信结果都在这里体现.
		
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
	
	//断开设备
	public void DisconnectDevice(BluetoothGatt _i_o_BluetoothGatt)
	{
		_i_o_BluetoothGatt.disconnect();
		_i_o_BluetoothGatt.close();
		_i_o_BluetoothGatt = null;
	}
	
	//写
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
		
		bResult = _i_o_BluetoothGatt.writeCharacteristic(m_sendCharacteristic); //写命令到设备
		if(!bResult)
		{
			return 5;
		}
		
		return 0;
	}

}

///////////////////////////////////////////////////////////////////////////////
//初始化参数
class hxgcBLECentralInit
{
	public Activity m_oActivity = null;
	public hxgcBLECentralEvent m_oBLECentralEvent = null;
}

///////////////////////////////////////////////////////////////////////////////
//事件响应
class hxgcBLECentralEvent
{
	//扫描响应
	public void On_LeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
	{
		
	}
	
	//连接状态
	public void On_ConnectionStateChange(BluetoothGatt gatt, int status, int newState)
	{
		
	}
	
	//周边上报数据
	public void On_CharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
		
	}
	
	//写数据
	public void On_CharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		
	}
	
	//寻找服务回调
	public void On_ServicesDiscovered(BluetoothGatt gatt, int status)
	{
		
	}
	
}


