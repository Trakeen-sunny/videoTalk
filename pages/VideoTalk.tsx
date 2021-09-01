import React, { Component } from 'react'
import { StatusBar, ScrollView, Text, Alert, TouchableOpacity, View, ToastAndroid, BackHandler } from 'react-native'
// 导入 RtcEngine 类和视图渲染组件。
import RtcEngine, { RtcLocalView, RtcRemoteView, VideoRenderMode } from 'react-native-agora'
// 导入自定义的用户界面样式。
import requestCameraAndAudioPermission from '../components/Permission'
import { Actions } from 'react-native-router-flux';
import styles from '../components/Style'
const baseUrls = 'http://39.100.60.237:18186/'

let startTime = ''
let endTime = ''
let timer: any = null

interface Props {
}

interface State {
    appId: string,
    channelName: string,
    token: string,
    joinSucceed: boolean,
    peerIds: number[],
    boastPlay: Boolean,
    idCardPhone: string,
    userName: string,
    recordId: any,
    videoRemote: boolean,
    joinSuccess: boolean,
}

export default class VideoTalk extends Component<Props, State> {
    _engine?: RtcEngine
    backHandler: any;

    constructor(props: any) {
        super(props)
        this.state = {
            appId: '',
            channelName: '',
            token: '',
            joinSucceed: false,
            peerIds: [],
            boastPlay: true,
            idCardPhone: props.idCardPhone,
            userName: props.userName,
            recordId: undefined,
            videoRemote: false,
            joinSuccess: false
        }
        this._getConcatHelp(props.userName)
        const date = new Date()
        startTime = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${date.getMinutes()}:${date.getSeconds()}`
    }

    /**链接帮助 */
    _getConcatHelp = (username: string) => {
        const baseUrl = `${baseUrls}/system/video/requestServer`
        fetch(baseUrl, {
            method: 'post',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: `customerName=${username}`
        }).then((response) => response.json()).then((result) => {
            console.log(result)
            if (result.code == 0) {
                this.setState({
                    appId: result.data.appId,
                    channelName: result.data.channelName,
                    token: result.data.tocken,
                    recordId: result.data.recordId
                }, () => {
                    this.init()
                })

            } else {
                ToastAndroid.showWithGravity(result.msg, ToastAndroid.SHORT, ToastAndroid.CENTER)
                setTimeout(() => {
                    Actions.popTo('Index')
                    setTimeout(() => {
                        Actions.refresh({ joinSuccess: false })
                    })
                }, 2000)
            }

        })
    }
    // 将组件挂载到 DOM 中。
    componentDidMount() {
        this.backHandler = BackHandler.addEventListener(
            "hardwareBackPress",
            this.backAction
        );
    }
    // 返回上一页
    backAction = () => {
        Alert.alert("提示", "你确定离开当前视频通话页面吗？", [
            {
                text: "取消",
                onPress: () => null,
                style: "cancel"
            },
            { text: "确定", onPress: () => this.endCall() }
        ]);
        return true;
    };
    componentWillUnmount() {
        if (this.state.boastPlay) {
            this.endCall()
        }
        clearInterval(timer)
        this._engine?.destroy();
        this.backHandler.remove();
    }
    // 通过 this.state 传入你的 App ID, 创建并初始化 RtcEngine 实例。
    init = async () => {
        const { appId } = this.state

        this._engine = await RtcEngine.create(appId)
        // 启用视频模块。
        await this._engine.enableVideo()
        // 开启本地视频预览。
        await this._engine.startPreview()
        //打开后置摄像头
        // await this._engine?.switchCamera()
        // 开始通话
        await this._engine?.joinChannel(this.state.token, this.state.channelName, null, 0)

        this._engine.addListener('Warning', (warn) => {
            console.log('Warning', warn)
        })

        this._engine.addListener('Error', (err) => {
            console.log('Error', err)
        })

        // 注册 JoinChannelSuccess 回调。
        // 本地用户成功加入频道时，会触发该回调。
        this._engine.addListener('JoinChannelSuccess', (channel, uid, elapsed) => {
            console.log('JoinChannelSuccess', channel, uid, elapsed)
            this.setState({
                joinSucceed: true
            })
        })

        timer = setInterval(() => {
            this._getTalkRecord(this.state.recordId)
        }, 5000)

        // 注册 UserJoined 回调。
        // 远端用户成功加入频道时，会触发该回调，并返回该用户的 id。
        this._engine.addListener('UserJoined', (uid, elapsed) => {
            console.log('UserJoined', uid, elapsed)
            const { peerIds } = this.state
            this.setState({ joinSuccess: true })
            if (peerIds.indexOf(uid) === -1) {
                this.setState({
                    peerIds: [...peerIds, uid]
                })
            }
            clearInterval(timer)

        })

        // 注册 UserOffline 回调。
        // 远端用户离开频道时，会触发该回调，并返回该用户的 id。
        this._engine.addListener('UserOffline', (uid, reason) => {
            console.log('UserOffline', uid, reason)
            const { peerIds } = this.state
            this.setState({
                // Remove peer ID from state array
                peerIds: peerIds.filter(id => id !== uid)
            })
            if (reason == 0 || reason == 1 || reason == 2) {
                ToastAndroid.showWithGravity('视频通话结束～', ToastAndroid.SHORT, ToastAndroid.CENTER)
                this.endCall()
            }
        })


    }
    /**判断客服状态 */
    _getTalkRecord = (talkRecordId: any) => {
        const baseUrl = `${baseUrls}/system/video/getTalkRecordById?talkRecordId=${talkRecordId}`
        fetch(baseUrl, {
            method: 'get',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            // body: `talkRecordId=${talkRecordId}`
        }).then((response) => response.json()).then((result) => {
            console.log('客服正在忙碌，请稍后～')
            if (result.code == 0) {
                if (result.data.status == 3) {
                    clearInterval(timer)
                    ToastAndroid.showWithGravity('客服正在忙碌，请稍后～', ToastAndroid.SHORT, ToastAndroid.CENTER)
                    setTimeout(() => {
                        Actions.popTo('Index')
                        setTimeout(() => {
                            Actions.refresh({ joinSuccess: false })
                        })
                    }, 2000)
                }
            } else {
                clearInterval(timer)
                ToastAndroid.showWithGravity(result.msg, ToastAndroid.SHORT, ToastAndroid.CENTER)
                setTimeout(() => {
                    Actions.popTo('Index')
                    setTimeout(() => {
                        Actions.refresh({ joinSuccess: false })
                    })
                }, 2000)
            }

        })
    }

    // 通过 this.state.token 和 this.state.channelName 获取传入的 Token 和频道名。
    // 本地用户的 ID。数据类型为整型，且频道内每个用户的 uid 必须是唯一的。若将 uid 设为 0，则 SDK 会自动分配一个 uid，并在 JoinChannelSuccess 回调中报告。
    startCall = async () => {
        this.setState({
            boastPlay: true
        })
        await this._engine?.joinChannel(this.state.token, this.state.channelName, null, 0)
    }

    endCall = async () => {
        this.setState({
            boastPlay: false
        })
        if (!this.state.joinSuccess) {
            this._endConcatCall()
        }
        const date = new Date()
        endTime = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${date.getMinutes()}:${date.getSeconds()}`
        // // 启用视频模块。
        await this._engine?.disableVideo()
        // // 开启本地视频预览。
        await this._engine?.stopPreview()
        await this._engine?.leaveChannel()
        this.setState({ peerIds: [], joinSucceed: false })
        Actions.popTo('Index')
        setTimeout(() => {
            Actions.refresh({ recordId: this.state.recordId, customerCard: this.state.idCardPhone, customerName: this.state.userName, startTime: startTime, endTime: endTime, joinSuccess: this.state.joinSuccess })
        })
    }
    /**客服未接通客户挂断 */
    _endConcatCall = () => {
        const baseUrl = `${baseUrls}/system/video/setTalkRecordStatusOffNotalk/${this.state.recordId}`
        fetch(baseUrl, {
            method: 'post',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: 'talkRecordId=' + this.state.recordId
        })
    }
    _switchCarema = async () => {
        await this._engine?.switchCamera()
    }
    _changeVideoRemote = async () => {
        this.setState({
            videoRemote: !this.state.videoRemote
        })
    }
    render() {
        return (
            <>
                <StatusBar hidden={true}></StatusBar>
                <View style={styles.max}>
                    <View style={styles.max}>
                        <View style={styles.buttonHolder}>
                            {/* <TouchableOpacity
                                onPress={this.startCall}
                                style={styles.button}>
                                <Text style={styles.buttonText}> Start Call </Text>
                            </TouchableOpacity> */}
                            <TouchableOpacity
                                onPress={this.endCall}
                                style={styles.button}>
                                <Text style={styles.buttonText}>结束</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                onPress={this._switchCarema}
                                style={styles.button}>
                                <Text style={styles.buttonText}>切换摄像头</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                onPress={this._changeVideoRemote}
                                style={styles.button}>
                                <Text style={styles.buttonText}>本地与远端视频切换</Text>
                            </TouchableOpacity>
                        </View>
                        {this._renderVideos()}
                    </View>
                </View>
            </>
        )
    }

    _renderVideos = () => {
        const { joinSucceed, videoRemote } = this.state
        return joinSucceed ? (
            <View style={styles.fullView}>
                {/* // 将视频渲染模式设为 Hidden, 即优先保证视窗被填满。 */}
                <View
                    style={videoRemote ? styles.remoteContainer : styles.fullView}>
                    <RtcLocalView.SurfaceView
                        style={videoRemote ? styles.remote : styles.max}
                        channelId={this.state.channelName}
                        renderMode={VideoRenderMode.Hidden} />
                </View>
                {this._renderRemoteVideos()}
            </View>
        ) : null
    }

    _renderRemoteVideos = () => {
        const { peerIds, videoRemote } = this.state
        return (
            <ScrollView
                style={videoRemote ? styles.fullView : styles.remoteContainer}
                contentContainerStyle={{ paddingHorizontal: 2.5 }}
                horizontal={true}>
                {peerIds.map((value, index, array) => {
                    return (
                        // 将视频渲染模式设为 Hidden, 即优先保证视窗被填满。
                        <RtcRemoteView.SurfaceView
                            style={videoRemote ? styles.fullView : styles.remote}
                            uid={value}
                            channelId={this.state.channelName}
                            renderMode={VideoRenderMode.Hidden}
                            zOrderMediaOverlay={true} />
                    )
                })}
            </ScrollView>
        )
    }
}
