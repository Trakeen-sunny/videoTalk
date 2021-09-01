/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */
import { Router, Stack, Scene } from 'react-native-router-flux';
import 'react-native-gesture-handler'
import React, { Component } from 'react';
import requestCameraAndAudioPermission from './components/Permission'
import { Platform } from 'react-native';

interface Props {

}
interface State { }
import Index from './pages/Index'
import VideoTalk from './pages/VideoTalk'
class App extends Component<Props, State> {
  constructor(props: any) {
    super(props)
    if (Platform.OS === 'android') {
      requestCameraAndAudioPermission().then(() => {
        console.log('requested!')
      })
    }
  }
  render() {
    return (
      <Router>
        <Stack key="root">
          <Scene key="Index" component={Index} title="视频客服" initial={true} />
          <Scene key="videoTalk" component={VideoTalk} title="视频通话" hideNavBar={true}/>
        </Stack>
      </Router>
    );
  }
};

export default App;
