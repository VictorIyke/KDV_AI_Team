import { StatusBar } from 'expo-status-bar';
import React, {useState} from 'react';
import { Alert, Button, StyleSheet, Text, View, NativeModules, Image, Platform, ImageProps } from 'react-native';
import * as ImagePicker from 'expo-image-picker';

let ctx: CanvasRenderingContext2D | null

export default function App() {
    const [selectedImage, setSelectedImage] = useState<any>(null);

    async function draw() {
        const canvas = document.getElementById('canvas') as HTMLCanvasElement;
        
        if (canvas.getContext) {
            ctx = canvas.getContext('2d')
            if (ctx != null && selectedImage != null){

                const imag = <img id='selectedImage' src={selectedImage.localUri} width="150" height="150" alt=''/>

                const imagId = document.getElementById("selectedImage") as HTMLImageElement
                ctx.drawImage(imagId, 0, 0)
            }
        }
    }

    async function drawTwo() {
      const myImageData = ctx!!.getImageData(0, 0, 150, 150)
      console.log(Float32Array.from(myImageData.data))
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
            <canvas id='canvas' width="150" height="150">
                {selectedImage != null &&
                <img id='selectedImage' src={selectedImage.localUri} width="150" height="150" alt='' />
                }
            </canvas>
            <Text>Hello There</Text>
            <Button title='Upload Picture' onPress={openImagePickerAsync}></Button>
            {selectedImage != null &&
            <Button title='Draw' onPress={draw}></Button>}
            <Button title='Draw 2' onPress={drawTwo}></Button>
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
