import { StatusBar } from 'expo-status-bar';
import React from 'react';
import { StyleSheet, Text, SafeAreaView, Image, Button, View } from "react-native";
import * as ImagePicker from 'expo-image-picker';

export default function App() {
  const [selectedImage, setSelectedImage] = React.useState(null);
  let openImagePickerAsync = async () => {
    let permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();

    if (permissionResult.granted === false) {
      alert("Permission to access Camera Roll is Required!");
      return;
    }
    let pickerResult = await ImagePicker.launchImageLibraryAsync();
    if (pickerResult.cancelled === true) {
      return;
    }
    setSelectedImage({ localUri: pickerResult.uri });
  }

  if (selectedImage !== null) {
    return (
      <View style={styles.container}>
        <Text>Hello React Native! Please Click Text Underneath</Text>
        <Button
          title='Upload Image Here'
          color={'#9F0500'}
          onPress={openImagePickerAsync}
        />
        <Text>Here is your uploaded Image</Text>
        <Image
          source={{ uri: selectedImage.localUri }}
          style={styles.thumbnail}
        />
      </View>      
    );

  } 
  
  
  return (
    <View style={styles.container}>
      <Text>Hello React Native! Please Click Text Underneath</Text>
      <Button
        title='Upload Image Here'
        color={'#9F0500'}
        onPress={openImagePickerAsync}
      />
      {/* <TouchableOpacity onPress={openImagePickerAsync}>
        <Text style={styles.item}>HERE!!</Text>
      </TouchableOpacity> */}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#73D8FF',
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
    width: 300,
    height: 300,
    resizeMode: "contain"
  },
});
