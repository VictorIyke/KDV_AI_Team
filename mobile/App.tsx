import { StatusBar } from 'expo-status-bar';
import React, {useState} from 'react';
import { Alert, Button, StyleSheet, Text, View, NativeModules, Image } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import * as ImageManipulator from 'expo-image-manipulator';

import * as ort from 'onnxruntime-react-native';
import { Asset } from 'expo-asset';
import { pixelsRGBToYCbCr, pixelsYCbCrToRGB } from './pixelator';
import { FlipType } from 'expo-image-manipulator';

let myModel: ort.InferenceSession;
let floatPixelsY = new Float32Array()
let floatPixelsCb = new Float32Array()
let floatPixelsCr = new Float32Array()
const [imgHeight, imgWidth] = [224, 224]
const [postImgHeight, postImgWidth] = [imgHeight*3, imgWidth*3]
let bitmapPixel: number[] = Array(imgHeight*imgWidth);
let bitmapScaledPixel: number[] = Array(imgHeight*imgWidth);
const bitmapModule = NativeModules.Bitmap


export default function App() {
  const [selectedImage, setSelectedImage] = useState<any>(null);
  const [outputImage, setOutputImage] = useState<any>(null);


  async function openImagePickerAsync() {
  
    let permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();
  
    if (permissionResult.granted === false) {
      alert("Permission to access Camera Roll is Required!");
      return;
    }

    let pickerResult = await ImagePicker.launchImageLibraryAsync();
    
    if (pickerResult.cancelled === true) {
      return;
    }

    const imageResult = await ImageManipulator.manipulateAsync(
      pickerResult.uri, [
        {resize: {height: imgHeight, width: imgWidth}}
      ]
    )

    const imageScaled = await ImageManipulator.manipulateAsync(
      pickerResult.uri, [
        {resize: {height: postImgHeight, width: postImgWidth}}
      ]
    )

    bitmapPixel = await bitmapModule.getPixels(imageResult.uri).then(
      (image: any) => {
        return Array.from(image.pixels);
      }
    )

    bitmapScaledPixel = await bitmapModule.getPixels(imageScaled.uri).then(
      (image: any) => {
        return Array.from(image.pixels);
      }
    )

    setSelectedImage({ 
      localUri: imageResult.uri,
      localHeight: imageResult.height,
      localWidth: imageResult.width 
    });

    setOutputImage(null)

    return

    
  };


  async function preprocess(): Promise<ort.Tensor> {  

    floatPixelsY = Float32Array.from(bitmapPixel)
    floatPixelsCb = Float32Array.from(bitmapScaledPixel)
    floatPixelsCr = Float32Array.from(bitmapScaledPixel)

    floatPixelsY.forEach((value, index) => {

      floatPixelsY[index] = pixelsRGBToYCbCr(value, "y")
    });

    floatPixelsCb.forEach((value, index) => {
      floatPixelsCb[index] = pixelsRGBToYCbCr(value, "cb")
      floatPixelsCr[index] = pixelsRGBToYCbCr(value, "cr")
    })
    let tensor: ort.Tensor = new ort.Tensor(floatPixelsY, [1, 1, imgHeight, imgWidth])
    return tensor
  };


  async function postprocess(floatArray: Array<number>): Promise<string> {
    

    const intArray = Array(postImgHeight*postImgWidth);

    floatArray.forEach((value, index) => {
      intArray[index] = pixelsYCbCrToRGB(value, floatPixelsCb[index], floatPixelsCr[index])
    })
    let imageUri = await bitmapModule.getImageUri(intArray).then(
      (image:any) => {

        return image.uri
      }
    )
    const imageRotated = await ImageManipulator.manipulateAsync(imageUri, [
      {rotate: 90},
      {flip: FlipType.Horizontal}
    ])
    setOutputImage({ localUri: imageRotated.uri })
    return imageUri
  };
  

  async function loadModel() {
    try {
      const assets = await Asset.loadAsync(require('./assets/super_resnet12.ort'));
      const modelUri = assets[0].localUri;
      if (!modelUri) {
        Alert.alert('failed to get model URI', `${assets[0]}`);
      } else {
        myModel = await ort.InferenceSession.create(modelUri);

        Alert.alert(
          'model loaded successfully',
          `input names: ${myModel.inputNames}, output names: ${myModel.outputNames}`);
      }

    } catch (e) {
      Alert.alert('failed to load model', `${e}`);
      throw e;
    }
  };

  async function runModel() {
    try {
      
      const feeds:Record<string, ort.Tensor> = {};
      if (bitmapPixel.length == imgHeight*imgWidth) {
        feeds[myModel.inputNames[0]] = await preprocess();
      } else{
        Alert.alert("No Image selected", "You need to upload and/or show image")
        return
      }
      const fetches = await myModel.run(feeds);
      const output = fetches[myModel.outputNames[0]];
      if (!output) {
        Alert.alert('failed to get output', `${myModel.outputNames[0]}`);
      }else {
        const out = output.data as Float32Array;
        const array = Array.from(out);
        await postprocess(array);
      }
    } catch (e) {
      Alert.alert('failed to inference model', `${e}`);
      throw e;
    }
  };


  return (
    <View style={styles.container}>
      <Text>using ONNX Runtime for React Native</Text>
      
      <Button title='Upload' onPress={openImagePickerAsync}></Button>
      <Button title='Load model' onPress={loadModel}></Button>
      {
        selectedImage !== null &&
        <Image
          source={{ uri: selectedImage.localUri }}
          style={styles.thumbnail}
        />}
      {
        outputImage !== null &&
        <Image
          source={{ uri: outputImage.localUri }}
          style={styles.thumbnail}
        />
      
      }
      <Button title='Run' onPress={runModel}></Button>

      <StatusBar style="auto" />
    </View>
  );
};


const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
  item: {
    margin: 24,
    fontSize: 18,
    fontWeight: "bold",
    textAlign: "center"
  },
  thumbnail: {
    margin: 8,
    width: 300,
    height: 300,
    resizeMode: "contain"
  },
});



