import React, { Component } from 'react';
import { View, Text, ScrollView,NativeEventEmitter,TouchableOpacity, RefreshControl, Dimensions, SafeAreaView, StatusBar, StyleSheet, ActivityIndicator, Image, Alert, TouchableHighlight, TouchableWithoutFeedback, TextInput, ToastAndroid, BackHandler, NativeModules } from 'react-native';
import { Actions } from 'react-native-router-flux';
// import requestCameraAndAudioPermission from '../components/Permission'
import RadioButtonRN from 'radio-buttons-react-native';
import { init, Geolocation, setLocatingWithReGeocode } from "react-native-amap-geolocation";
import DeviceInfo from 'react-native-device-info';
import { httpRequest } from '../utils/httpRequest'
const dimensions = {
    width: Dimensions.get('window').width,
    height: Dimensions.get('window').height - 100,
}
const { Test } = NativeModules
// import Toast from 'react-native-easy-toast'
// import { TipModal } from 'react-native-ms';
// declare module 'react-native-ms'
const baseUrls = 'http://39.100.60.237:18186/'
interface State {
    isShow: Boolean,
    type: Number,
    server: Boolean,
    toastlVisible: Boolean,
    toastType: Number,
    serverList: any,
    hasFree: number,
    hasBusy: number,
    userId: any,
    userName: String,
    idCardPhone: String,
    tip: string,
    recordId: any,
    customerCard: any,
    customerName: any,
    startTime: any,
    endTime: any,
    level: number,
    isRefreshing: boolean,
    joinSuccess: boolean,
    isToast: boolean,
    evenatt:any;
    isTimer:any;
    last:any,
    location:string
}
interface Props {

}
const w33 = (100 / 3) + '%'
const data = [
    {
        label: '非常满意',
        value: 1
    },
    {
        label: '满意',
        value: 2
    },
    {
        label: '不满意',
        value: 3
    }
];
export default class Index extends Component<Props, State> {
    backHandler: any;
    eventListener:any;
    eventListener1:any;
    eventListener2:any;
    eventListener3:any;
    constructor(props: any) {
        super(props)
        this.state = {
            isShow: false,
            type: 0, //默认0 都不选择
            server: false,
            toastlVisible: false,
            toastType: 0,
            serverList: [],
            hasFree: 0,
            hasBusy: 0,
            userId: '',
            userName: '',
            idCardPhone: '',
            tip: '',
            recordId: '',
            customerCard: '',
            customerName: '',
            startTime: '',
            endTime: '',
            level: 1,
            isRefreshing: false,
            joinSuccess: false,
            isToast: false,
            evenatt:null,
            isTimer:null,
            last:0,
            location:''
        }

        /**初始化读卡器 */
        Test.initData((res: any) => {
            // ToastAndroid.showWithGravity('初始化成功', ToastAndroid.SHORT, ToastAndroid.CENTER)
            Test.concatService()
        });

        this._getUserName = this._getUserName.bind(this);
        this._getUserIdCard = this._getUserIdCard.bind(this);
    }
    async componentDidMount() {
        
        this.backHandler = BackHandler.addEventListener(
            "hardwareBackPress",
            this.backAction
        );
        await init({
            ios: "ab52dc48f297c45cf17a61ac29e82235",
            android: "59d33158d8ee7990c2780b0e8710ee03"
        });
        this._getServerList()
        const eventEmitter = new NativeEventEmitter(NativeModules.Test);
        this.eventListener = eventEmitter.addListener('EventReminder', (event) => {
          console.log(event) // "someValue"
          if(event.ble != 4){
            ToastAndroid.showWithGravity('扫描到设备', ToastAndroid.SHORT, ToastAndroid.CENTER)
            Test.closeService()
          }else if(event.ble ==4){
            this.setState({evenatt:1,isTimer:null})
            ToastAndroid.showWithGravity('未扫描到设备，检查设备是否开启?', ToastAndroid.LONG, ToastAndroid.CENTER)
          }
        })
        this.eventListener1 = eventEmitter.addListener('EventGatt', (event) => {
            console.log(event)
            if(event.BluetoothGatt == 0){
                this.setState({evenatt:0,isTimer:null})
                ToastAndroid.showWithGravity('设备连接成功', ToastAndroid.SHORT, ToastAndroid.CENTER)
            }else{
                this.setState({evenatt:1,isTimer:null})
                ToastAndroid.showWithGravity('连接设备失败，重新连接', ToastAndroid.SHORT, ToastAndroid.CENTER)
            }
        })
        this.eventListener2 = eventEmitter.addListener('IdCardInfoEvent', (event) => {
            console.log(event)
            this.setState({
                toastlVisible: true,
                toastType: 1,
                isTimer:null
            })
           const timer = setTimeout(()=>{
                Actions.videoTalk({ 'idCardPhone': event.strIdCode, 'userName': event.strName })
                this.setState({
                    isShow: false,
                    type: 0, //默认0 都不选择
                    server: false,
                    toastlVisible: false
                })
                clearTimeout(timer);
            },1500)
        })
        this.eventListener3 = eventEmitter.addListener('isSearchCard',(event)=>{
          if(event.searchCard==1){
            ToastAndroid.showWithGravity('请确认设备是否开启或身份证是否放在设备上？', ToastAndroid.SHORT, ToastAndroid.CENTER)
          }else if(event.searchCard==2 || event.searchCard==3){
            ToastAndroid.showWithGravity('操作失误，请重新读卡', ToastAndroid.SHORT, ToastAndroid.CENTER)
          }
          this.setState({
            isShow: false,
            type: 0, //默认0 都不选择
            server: false,
            toastlVisible: false,
            evenatt:null
          })
        })
    }
    // 退出app
    backAction = () => {
        Alert.alert("提示", "确定退出当前应用吗？", [
            {
                text: "取消",
                onPress: () => null,
                style: "cancel"
            },
            { text: "确定", onPress: () => BackHandler.exitApp() }
        ]);
        return true;
    };
    componentWillUnmount() {
        this.backHandler.remove();
        this.eventListener.remove(); // 组件卸载时记得移除监听事件
        this.eventListener1.remove();
        this.eventListener2.remove();
        this.eventListener3.remove();
        Test.exitOutCard();
        console.log('componentWillUnmount')
    }
    /**防抖 */
    _debounce = (fn:any,delay:number) =>{
      const _self = this;
      return function(...args:any){
          if(_self.state.isTimer) clearTimeout(_self.state.isTimer);
          let callNow = _self.state.isTimer;
          if(!callNow){
            fn.apply(_self,args)
            _self.setState({
                isTimer:1
            })
          }else{
            _self.setState({
                isTimer:setTimeout(()=>{
                    fn.apply(_self,args);
                },delay)
              })
          }
      }
    }
    /**节流 */
    _throttle = (fn:any,delay:number)=>{
      const _self = this;
      return function(...args:any){
          let now = Date.now();
          if(now - _self.state.last>delay){
              fn.apply(_self,args);
              _self.setState({
                last: now
              })
          }
      }
    }
    /*** 客服列表接口*/
    _getServerList = () => {
        const baseUrl = `${baseUrls}/system/video/getServerList`
        let hasFree1 = 0;
        let hasBusy1 = 0
        fetch(baseUrl, {
            method: 'get',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json'
            },
        }).then((response) => response.json()).then((result) => {
            // console.log(result)
            // this.setState({isRefreshing: true});
            if (result.code == 0) {
                for (const res of result.data) {
                    if (res.serverStatus == 1) {
                        hasFree1++
                    } else if (res.serverStatus == 2) {
                        hasBusy1++
                    }
                }
                this.setState({
                    serverList: result.data,
                    hasFree: hasFree1,
                    hasBusy: hasBusy1
                })
            }
        }).catch(err => {
            console.log(err)
        })
    }

    /**选择认证方式 */
    _onPressButton = (ev: Number) => {
        this.setState({
            type: ev
        })
        if (ev == 1) {
            if(this.state.evenatt == 1 || this.state.evenatt == null){
                ToastAndroid.showWithGravity('设备还未连接，请先连接设备～', ToastAndroid.SHORT, ToastAndroid.CENTER)
            }else{
                this._debounce(this._fn4,1000)()
            }
            
            // timer = setTimeout(() => {
            //     Actions.videoTalk({ 'idCardPhone': '362330202001126082', 'userName': 'amyli' })
            //     this.setState({
            //         isShow: false,
            //         type: 0, //默认0 都不选择
            //         server: false,
            //         toastlVisible: false
            //     })
            //     clearTimeout(timer)
            // }, 2000)
        }
    }
    _fn4 = ()=>{
        if(this.state.evenatt==0){
            this.setState({
                toastlVisible: true,
                toastType: 0
            })
            const timer = setTimeout(()=>{
                Test.readIDCard()
                clearTimeout(timer)
            },800)
        }else{
            this.setState({isTimer:null})
            ToastAndroid.showWithGravity('连接设备失败，重新连接', ToastAndroid.SHORT, ToastAndroid.CENTER)
        }
    }
    /*** 展示认证方式*/
    concatServer = () => {
        if (this.state.hasFree == 0) {
            ToastAndroid.showWithGravity('目前没有空闲客服，请稍后～', ToastAndroid.SHORT, ToastAndroid.CENTER)
        } else {
            this.setState({
                isShow: true
            })
        }
    }
    /**输入用户姓名 */
    _getUserName(inputData: any) {
        this.setState({
            userName: inputData
        })
    }
    /**正则表达式 身份号校验 */
    _IsCard = (str: any) => {
        var reg = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/;
        return reg.test(str);
    }
    /**获取用户身份证 */
    _getUserIdCard(num: any) {
        this.setState({
            idCardPhone: num
        })
    }
    // 提交视频认证
    submitSure = () => {
        this._debounce(this._fn2,1000)();
    }
    _fn2 = ()=>{
        if (this.state.userName == '') {
            ToastAndroid.showWithGravity('请输入您的姓名', ToastAndroid.SHORT, ToastAndroid.CENTER)
        } else if (!this._IsCard(this.state.idCardPhone) || this.state.idCardPhone == '') {
            ToastAndroid.showWithGravity('请输入正确的身份证号', ToastAndroid.SHORT, ToastAndroid.CENTER)
        } else {
            this.setState({
                isShow: false,
                type: 0, //默认0 都不选择
                server: false
            })
            Actions.videoTalk({ 'idCardPhone': this.state.idCardPhone, 'userName': this.state.userName })
        }
    }
    // 关闭弹窗
    _handleClose = () => {
        this.setState({
            isShow: false,
            type: 0, //默认0 都不选择
            server: false,
            toastlVisible: false,
        })
       
        if (this.state.joinSuccess) {
            this.setState({ isToast: true })
            this._handleServerComment()
        }
    }
    // 返回上一页
    _handleBack = () => {
        this.setState({
            isShow: true,
            server: false,
            type: 0,
            toastlVisible: false
        })
       
    }
    componentWillReceiveProps(nextProps: any) {
        this._getServerList();
        console.log(nextProps);
        this.setState({
            server: nextProps.joinSuccess != false ? true : false,
            recordId: nextProps.recordId,
            customerCard: nextProps.customerCard,
            customerName: nextProps.customerName,
            startTime: nextProps.startTime,
            endTime: nextProps.endTime,
            joinSuccess: nextProps.joinSuccess,
            idCardPhone: '',
            userName: '',
            isTimer:null
        })
         /**获取地理位置 */
        Geolocation.getCurrentPosition((result: any) => {
            const location = result.location.address;
            this.setState({
                location:location
            })
        }, (error) => {
            console.log(error)
        }, { enableHighAccuracy: true, timeout: 20000 });
    }
    /*服务评价提交 */
    _handleServerComment = async () => {
        /**设备ID */
        const androidId = await DeviceInfo.getAndroidId()
        /**获取地理位置 */
        this._handleFunction(this.state.location, androidId)
        // this._handleFunction('浙江', androidId)
    }
    /**下拉刷新 */
    _onRefresh = () => {
        // this.setState({isRefreshing: true});
        this._getServerList()
    }
    /**封装请求方法 */
    _handleFunction = (address: string, androidId: any) => {
        const baseUrl = `${baseUrls}/system/video/updateTalkRecord`
        fetch(baseUrl, {
            method: 'post',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: `customerCard=${this.state.customerCard}&customerName=${this.state.customerName}&startTime=${this.state.startTime}&endTime=${this.state.endTime}&address=${address}&id=${this.state.recordId}&level=${this.state.level}&machineNo=${androidId}`
        }).then((response) => response.json()).then((result) => {
            console.log(result)
            if (result.code == 0) {
                if (this.state.joinSuccess && !this.state.isToast) {
                    ToastAndroid.showWithGravity('谢谢您的评价', ToastAndroid.SHORT, ToastAndroid.CENTER)
                }
              const timer = setTimeout(() => {
                    this.setState({ server: false, joinSuccess: false, isToast: false })
                    clearTimeout(timer);
                }, 1500)

            } else {
                ToastAndroid.showWithGravity(result.msg, ToastAndroid.SHORT, ToastAndroid.CENTER)
            }
        }).catch(err => {
            console.log(err)
        })
    }
    _concatService = () => {
       this._debounce(this._fn,1000)()
    }
    /**连接设备 */
    _fn = ()=>{
        Test.concatService()
    }
    render() {
        const { hasFree, hasBusy, isRefreshing } = this.state
        return (
            <>
                <StatusBar hidden={true}></StatusBar>
                <SafeAreaView style={styles.container}>
                    <ScrollView style={styles.scrollView}
                        refreshControl={
                            <RefreshControl
                                // tintColor="#ff0000"
                                title="加载中..."
                                // titleColor="#00ff00"
                                size={0}
                                progressViewOffset={30}
                                // colors={['#0000ff', '#ff0000', '#00ff00',]}
                                // progressBackgroundColor="#ffff00"
                                refreshing={false}
                                onRefresh={this._onRefresh}
                            />
                        }>
                        {/* 标题 */}
                        <View style={styles.padding}>
                            <View style={styles.thead}>
                                <Text style={styles.theadTh}>照片</Text>
                                <Text style={styles.theadTh}>姓名</Text>
                                <Text style={styles.theadTh}>状态</Text>
                            </View>
                            {/* 主体 */}
                            {this._serverList()}
                            <View style={styles.tbody}>
                                <View style={styles.tip}>
                                    <Image style={styles.iconTip} source={require('../images/icon-tip.png')} />
                                    <Text style={styles.textStyle}>当前有共有{hasFree + hasBusy}名客服在线,其中有{hasBusy}名忙碌,{hasFree}名空闲,您可直接连线视频客服！</Text>
                                </View>
                            </View>
                        </View>
                        <View style={styles.buttonSubmit}>
                            <View style={styles.w30}>
                                <TouchableOpacity style={styles.idcard} onPress={this._concatService}>
                                    <Image style={styles.idcard} source={require('../images/concatService.png')}/>
                                </TouchableOpacity>
                            </View>
                            <TouchableHighlight style={styles.button} onPress={this.concatServer}>
                                <Text style={styles.buttonText}>连线视频客服</Text>
                            </TouchableHighlight>
                            <View style={styles.w30}></View>
                        </View>
                    </ScrollView>
                </SafeAreaView>
                {this._shadowBox()}
                {this._codeVerifica()}
                {this._serverEvalua()}
                {this._shadowTosat()}
            </>
        )
    }
    // 客服列表展示
    _serverList = () => {
        const { serverList } = this.state
        return (
            <View style={styles.tbody}>
                {
                    serverList.map((item: any, index: number) => {
                        return (
                            <View style={serverList.length == index + 1 ? [styles.tbodyLine] : [styles.tbodyLine, styles.borderLine]} key={index}>
                                <View style={styles.tbodys}>
                                    <Image style={styles.avatar} source={{ uri: item.avatar }} />
                                </View>
                                {item.serverStatus == 0 ? (
                                    <Text style={[styles.tbodyTh, styles.busy]}>{item.userName}</Text>
                                ) : <Text style={[styles.tbodyTh]}>{item.userName}</Text>}

                                {item.serverStatus == 0 ? (
                                    <Text style={[styles.tbodyTh, styles.busy]}>
                                        <View style={[styles.dot, styles.backgroundb2]}></View>
                                      离线
                                    </Text>
                                ) : null}
                                {item.serverStatus == 1 ? (

                                    <Text style={styles.tbodyTh} >
                                        <View style={[styles.dot, styles.background16]}></View>
                                        空闲
                                    </Text>

                                ) : null}
                                {item.serverStatus == 2 ? (
                                    <Text style={styles.tbodyTh}>
                                        <View style={[styles.dot, styles.background1d]}></View>
                                        忙碌
                                    </Text>
                                ) : null}
                            </View>
                        )
                    })
                }
            </View>
        )
    }
    /**提示框 */
    _shadowTosat = () => {
        const { toastlVisible, toastType, tip } = this.state;
        return toastlVisible ? (
            <View style={toast.shadowBox}>
                {
                    toastType == 0 ? (
                        <View style={toast.toastBox}>
                            <ActivityIndicator style={toast.icon} size="large" color="#ffffff" />
                            <Text style={toast.toastText}>身份识别中</Text>
                        </View>
                    ) : null
                }
                {
                    toastType == 1 ? (
                        <View style={toast.toastBox}>
                            <Image style={toast.icon} source={require('../images/icon-success.png')} />
                            <Text style={toast.toastText}>识别成功</Text>
                        </View>
                    ) : null
                }
                {
                    toastType == 2 ? (
                        <View style={toast.toastBox}>
                            <Image style={toast.icon} source={require('../images/icon-fail.png')} />
                            <Text style={toast.toastText}>识别失败,请重新放入</Text>
                        </View>
                    ) : null
                }
                {
                    toastType == 3 ? (
                        <View style={toast.toastBox}>
                            <ActivityIndicator style={toast.icon} size="large" color="#ffffff" />
                            <Text style={toast.toastText}>{tip}</Text>
                        </View>
                    ) : null
                }
                {
                    toastType == 4 ? (
                        <View style={toast.toastBox}>
                            {/* <ActivityIndicator style={toast.icon} size="large" color="#ffffff" /> */}
                            <Text style={toast.toastText}>{tip}</Text>
                        </View>
                    ) : null
                }
            </View>
        ) : null
    }
    /**选择身份认证方式 */
    _shadowBox = () => {
        const { isShow } = this.state;
        return isShow ? (
            <View style={shadow.shadowBox}>
                <View style={shadow.container}>
                    {/* 标题 */}
                    <View style={shadow.title}>
                        <View style={{width:21.33,height:21.33}}></View>
                        <Text style={shadow.titleText}>身份认证</Text>
                        <TouchableWithoutFeedback onPress={this._handleClose}>
                            <Image style={shadow.close} source={require('../images/icon-close.png')} />
                        </TouchableWithoutFeedback>
                    </View>
                    <View style={shadow.selectWay}>
                        <TouchableWithoutFeedback onPress={this._onPressButton.bind(this, 1)}>
                            <Image style={shadow.idcard} source={require('../images/idcard.png')} />
                        </TouchableWithoutFeedback>
                        <TouchableWithoutFeedback onPress={this._onPressButton.bind(this, 2)}>
                            <Image style={shadow.idNum} source={require('../images/idNum.png')} />
                        </TouchableWithoutFeedback>
                    </View>
                    <View style={shadow.tip}>
                        <Image style={shadow.tipImg} source={require('../images/icon-tip1.png')} />
                        <Text style={shadow.tipText}>您未进行身份认证，请先进行简单的身份认证即可连线视频客服</Text>
                    </View>
                </View>
            </View>
        ) : null
    }
    /**提交身份认证 */
    _codeVerifica = () => {
        const { type } = this.state;
        return type != 0 ? (
            <View style={shadow.shadowBox}>
                <View style={shadow.container}>
                    {/* 标题 */}
                    <View style={shadow.title}>
                        <TouchableWithoutFeedback style={shadow.w} onPress={this._handleBack}>
                            <Image style={shadow.back} source={require('../images/back.png')} />
                        </TouchableWithoutFeedback>
                        <Text style={shadow.titleText}>身份认证</Text>
                        <TouchableWithoutFeedback onPress={this._handleClose}>
                            <Image style={shadow.close} source={require('../images/icon-close.png')} />
                        </TouchableWithoutFeedback>
                    </View>
                    {
                        type === 1 ? (
                            <View>
                                <View style={shadow.cardCenter}>
                                    <TouchableWithoutFeedback>
                                        <Image style={shadow.card} source={require('../images/card1.png')} />
                                    </TouchableWithoutFeedback>
                                </View>
                                <View style={shadow.tip}>
                                    <Image style={shadow.tipImg} source={require('../images/icon-tip1.png')} />
                                    <Text style={shadow.tipText}>您未进行身份认证，请先进行简单的身份认证即可连线视频客服</Text>
                                </View>
                            </View>
                        ) : (
                                <View>
                                    <View style={shadow.cardCenter}>
                                        <View style={shadow.card1}>
                                            <TextInput style={shadow.textInput} onChangeText={this._getUserName}
                                                placeholder='请输入您的姓名' maxLength={5} placeholderTextColor='#BCBFC5'></TextInput>
                                            <TextInput style={[shadow.textInput, shadow.marginTop5]} onChangeText={this._getUserIdCard} placeholder='请输入身份证号' placeholderTextColor='#BCBFC5'></TextInput>
                                        </View>
                                    </View>
                                    <View style={shadow.alitemCenter}>
                                        <TouchableWithoutFeedback onPress={this.submitSure}>
                                            <Text style={shadow.submit}>确定</Text>
                                        </TouchableWithoutFeedback>
                                    </View>
                                </View>)
                    }
                </View>
            </View>
        ) : null
    }
    /**服务评价 */
    _serverEvalua = () => {
        const { server } = this.state;
        return server ? (
            <View style={shadow.shadowBox}>
                <View style={shadow.container}>
                    {/* 标题 */}
                    <View style={shadow.title}>
                        <View style={{width:21.33,height:21.33}}></View>
                        {/* <Image style={shadow.back} source={require('../images/back.png')} /> */}
                        <Text style={shadow.titleText}>服务评价</Text>
                        <TouchableWithoutFeedback onPress={this._handleClose}>
                            <Image style={shadow.close} source={require('../images/icon-close.png')} />
                        </TouchableWithoutFeedback>
                    </View>
                    <View style={shadow.cardCenter}>
                        <View style={shadow.server}>
                            <Text style={shadow.serverTip}>请对此次视频服务做出评价</Text>
                            <View>
                                <RadioButtonRN
                                    data={data}
                                    selectedBtn={(e: any) => {
                                        this.setState({
                                            level: e.value
                                        })
                                    }}
                                    initial={1}
                                    circleSize={10}
                                    activeColor='#2878FF'
                                    textStyle={shadow.radio}
                                    boxStyle={shadow.boxStyle}
                                    box={false}
                                />
                            </View>
                        </View>
                    </View>
                    <View style={[shadow.alitemCenter, shadow.serverTop]}>
                        <TouchableHighlight style={{ borderRadius: 16.33, }} onPress={this._handleServerComment}>
                            <Text style={shadow.submit}>确定</Text>
                        </TouchableHighlight>
                    </View>
                </View>
            </View>
        ) : null
    }
}
const toast = StyleSheet.create({
    shadowBox: {
        position: 'absolute',
        top: 0,
        left: 0,
        height: '100%',
        width: '100%',
        justifyContent: 'center',
        alignItems: 'center',
        flexDirection: 'row',
    },
    toastBox: {
        // width: 93.33,
        // height: 93.33,
        borderRadius: 8,
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        paddingLeft: 17.33,
        paddingRight: 17.33,
        paddingTop: 6.33,
        paddingBottom: 10.33,
        alignItems: 'center'
    },
    toastText: {
        color: '#ffffff',
        fontSize: 11,
        fontWeight: '500',
        textAlign: 'center',
        marginTop: 1
    },
    icon: {
        width: 47.33,
        height: 47.33
    }
})

const shadow = StyleSheet.create({
    shadowBox: {
        position: 'absolute',
        top: 0,
        left: 0,
        backgroundColor: 'rgba(0,0,0,0.4)',
        height: '100%',
        width: '100%',
        justifyContent: 'center',
        alignItems: 'center',
        flexDirection: 'row',
    },
    container: {
        // width: 360,
        // height: 227,
        width: 400,
        height: 240,
        backgroundColor: '#ffffff',
        borderRadius: 5.33
    },
    title: {
        justifyContent:'space-between',
        flexDirection:'row',
        position: 'relative',
        width: '100%'
    },
    titleText: {
        color: '#333333',
        fontWeight: '500',
        fontSize: 10.67,
        textAlign: 'center',
        marginTop: 13.67
    },
    close: {
        width: 21.33,
        height: 21.33,
        marginRight: 7.33,
        marginTop: 8.33,

    },
    back: {
        width: 21.33,
        height: 21.33,
        marginLeft: 9.33,
        marginTop: 8.33
    },
    w:{
        width:21.33,
        height:21.33,
    },
    idcard: {
        width: 170.33,
        height: 170.33
    },
    idNum: {
        width: 170.33,
        height: 170.33
    },
    selectWay: {
        marginTop: 12.33,
        flexDirection: 'row',
        marginLeft: 21.33,
        marginRight: 21.33,
        justifyContent: 'space-between'
        // justifyContent: 'center'
    },
    tip: {
        flexDirection: 'row',
        alignItems: 'center',
        marginTop: 10,
        marginLeft: 21.33,
    },
    tipText: {
        color: '#FA9305',
        fontSize: 10,
        fontWeight: '400'
    },
    tipImg: {
        width: 16,
        height: 16,
        marginRight: 2
    },
    card: {
        width: 320.33,
        height: 165.33
    },
    card1: {
        width: 317.33,
        height: 130.33,
        alignItems: 'center',
        justifyContent: 'center',
    },
    marginTop5: {
        marginTop: 5
    },
    cardCenter: {
        marginTop: 12,
        alignSelf: 'center'
    },
    textInput: {
        width: 311.33,
        height: 40,
        backgroundColor: '#F6F6F6',
        color: '#666',
        fontSize: 12
    },
    alitemCenter: {
        alignItems: 'center'
    },
    submit: {
        width: 106.67,
        height: 32.67,
        backgroundColor: '#03948E',
        borderRadius: 16.33,
        textAlign: 'center',
        lineHeight: 32.67,
        color: '#fff',
        fontSize: 12,
        fontWeight: '500'
    },
    server: {
        backgroundColor: '#F6F6F6',
        width: 301.33,
        height: 125,
        borderRadius: 5.33,
        padding: 11.33
    },
    serverTop: {
        marginTop: 16,
        borderRadius: 16.33,
    },
    serverTip: {
        color: '#6D6D6D',
        fontSize: 10.67,
        fontWeight: '400'
    },
    serveList: {
        color: '#202331',
        fontSize: 9.33,
        fontWeight: '500'
    },
    radio: {
        color: '#202331',
        fontSize: 13,
        fontWeight: '500'
    },
    boxStyle: {
        borderWidth: 0,
        backgroundColor: '#F6F6F6',
    }
});

const styles = StyleSheet.create({
    container: {
        backgroundColor: '#F7F7F7',
        flex: 1,
    },
    scrollView: {
        width: dimensions.width,
        height: dimensions.height
        // width: 100,
        // height: 100
    },
    padding: {
        marginTop: 10.67,
        marginLeft: 8.67,
        marginRight: 8.67,
        backgroundColor: '#ffffff',
    },
    thead: {
        backgroundColor: '#03948E',
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderTopLeftRadius: 5,
        borderTopRightRadius: 5,
        height: 30,
        paddingLeft: 15.6,
        paddingRight: 15.6
    },
    tbody: {
        backgroundColor: '#fff',
        paddingLeft: 15.6,
        paddingRight: 15.6,
        paddingBottom: 10,
        borderBottomLeftRadius: 5,
        borderBottomRightRadius: 5,
    },
    theadTh: {
        color: '#fff',
        fontWeight: '600',
        fontSize: 12,
        width: w33,
        textAlign: 'center',
        justifyContent: 'center'
    },

    avatar: {
        width: 36,
        height: 36,
        borderRadius: 100
    },
    tbodyLine: {
        height: 71.67,
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',

        borderStyle: 'solid',
        borderColor: '#E0E1E1'
    },
    borderLine: {
        borderBottomWidth: 1,
    },
    dot: {
        width: 10,
        height: 10,
        borderRadius: 100,
        alignSelf: 'center',
        marginRight: 6
    },
    background16: {
        backgroundColor: '#16CE01'
    },
    background1d: {
        backgroundColor: '#FF1D1D'
    },
    backgroundb2: {
        backgroundColor: '#B2B2B2'
    },
    busy: {
        color: '#9A9A9A'
    },
    tip: {
        height: 47.67,
        backgroundColor: '#FFF9F9',
        borderRadius: 2.67,
        flexDirection: 'row',
        alignItems: 'center',
    },
    iconTip: {
        width: 16,
        height: 16,
        marginRight: 2
    },
    textStyle: {
        color: '#FF0000',
        fontWeight: '300',
        fontSize: 10
    },
    tbodyTh: {
        color: '#333333',
        fontSize: 10,
        fontWeight: '500',
        width: w33,
        textAlign: 'center',
        justifyContent: 'center'
    },
    buttonContainer: {
        padding: 10
    },
    buttonSubmit: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems:'center',
        marginTop: 36,
        width: '100%',
    },
    button: {
        width: 170.67,
        height: 40.67,
        borderRadius: 16.33,
        backgroundColor: '#03948E',
        textAlign: 'center',
        justifyContent: 'center',
        alignItems: 'center',
        
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 12,
        fontWeight: '500'
    },
    tbodys: {
        width: w33,
        textAlign: 'center',
        alignItems: 'center'
    },
    idcard:{
        width:90,
        height:97.5
    },
   w30:{
     width:'25%'
   }
});