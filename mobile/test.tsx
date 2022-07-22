import { StatusBar } from 'expo-status-bar';
import React, {useEffect, useState} from 'react';
import { Alert, Button, StyleSheet, Text, View, NativeModules, Image, Platform, ImageProps } from 'react-native';
import * as ImagePicker from 'expo-image-picker';

import * as ImageManipulator from 'expo-image-manipulator';

let gl: OffscreenCanvasRenderingContext2D | null
let isLoaded = false
let bitmapPixels: Float32Array
let bitmapScaledPixels: Float32Array
let small = document.getElementById("smallImage") as HTMLImageElement
let big = document.getElementById("bigImage") as HTMLImageElement


export default function App() {
  const [selectedImage, setSelectedImage] = useState<any>(null);
  let [imageSmall, imageBig] = [small, big]

  useEffect(() => {
    if (!isLoaded) {
      small = document.getElementById("smallImage") as HTMLImageElement
      big = document.getElementById("bigImage") as HTMLImageElement
      if (small && big) {
        imageSmall = small
        imageBig = big
        isLoaded = true
            }     }
  })

  async function draw() {
      const offscreen = new OffscreenCanvas(1000, 1000)
      gl = offscreen.getContext('2d')



      if (selectedImage != null && gl) {
          console.log("GL loaded")



          gl.drawImage(imageSmall, 0, 0, 224, 224)
          const myImageData = gl.getImageData(0, 0, 224, 224)
          bitmapPixels = Float32Array.from(myImageData.data)
          gl.clearRect(0, 0, 224, 224)



     
          gl.drawImage(imageBig, 0, 0, 672, 672)
          const myImageScaledData = gl.getImageData(0, 0, 672, 672)
          gl.clearRect(0, 0, 672, 672)
          bitmapScaledPixels = Float32Array.from(myImageScaledData.data)
          }

          console.log(bitmapPixels)
          console.log(bitmapScaledPixels)
      
      }
  


  async function openImagePickerAsync() {
  
    const permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();
  
    if (permissionResult.granted === false) {
      alert("Permission to access Camera Roll is Required!");
      return;
    }
    const options = {base64: true, exif: true}
    const pickerResult = await ImagePicker.launchImageLibraryAsync(options);
    
    if (pickerResult.cancelled === true) {
      return;
    }

    setSelectedImage({ 
      localUri: pickerResult.uri,
      localHeight: pickerResult.height,
      localWidth: pickerResult.width 
    });

    return

    
  };
  


  return(
      <View style={styles.container}>
        <canvas>
          {selectedImage &&
            <View>
              <img id='smallImage' src={selectedImage.localUri} width="150" height="150" alt='' />
              <img id='bigImage' src={selectedImage.localUri} width="150" height="150" alt='' />
            </View>
          }
          </canvas>
          <Text>Hello There</Text>
          <Button title='Upload Picture' onPress={openImagePickerAsync}></Button>
          {selectedImage != null &&
          <Button title='Draw' onPress={draw}></Button>}
          {
        selectedImage !== null &&
        <Image
          source={{ uri: selectedImage.localUri }}
          style={styles.thumbnail}
        />}
      </View>
      
  )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#ffffff',
        alignItems: 'center',
        justifyContent: 'center',
      },
      thumbnail: {
        alignSelf: "center",
        margin: 8,
        width: 450,
        height: 450,
        resizeMode: "contain"
      },
    }
)
