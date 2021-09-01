package com.example.testlibrary;

import android.content.Context;
import android.widget.Toast;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.hxgc.hxbluetoothdemo.MainActivity;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;


public class TestModule extends ReactContextBaseJavaModule{
    public static final String TAG = "MainActivity";
    public MainActivity mMainActivity;
    private static ReactApplicationContext reactContext;

    public TestModule(ReactApplicationContext context){
        super(context);
        reactContext = context;
        //给上下文对象赋值
        MainActivity.myContext=reactContext;
    }


    /**
     * 实现getName方法，返回值即js中调用的方法吗
     * */
    @Override
    public String getName() {
        return "Test";
    }

    /**
     * 定义一个方法，获取应用包名
     * */
    @ReactMethod
    public void getPackageName(){
        String name = getReactApplicationContext().getPackageName();
        Toast.makeText(getReactApplicationContext(),name,Toast.LENGTH_LONG).show();
    }

    /**
     * 搜索
     * */
    @ReactMethod
    public void concatService(){
        mMainActivity.searchService();

    }
    /**
     * 初始化数据
     * */
    @ReactMethod
    public void initData(Callback callback){
        mMainActivity = new MainActivity();
        mMainActivity.loadCreate(getCurrentActivity(),reactContext,callback);
    }

    /**
     * 链接
     * */
    @ReactMethod
    public void closeService(){
        mMainActivity.concatService();
    }

    /**
     * 读卡
     * */
    @ReactMethod
    public void readIDCard() {
        mMainActivity.readService();
    }

    /**
     * 退出读卡
     * */
    @ReactMethod
    public void exitOutCard(){
        mMainActivity.onDestroy();
    }
}
