import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View, TouchableOpacity, Image, Alert } from 'react-native';
import { Button } from 'react-native-elements';
import React from 'react';
import * as ImagePicker from 'expo-image-picker';
import * as Sharing from 'expo-sharing';
import { Ionicons } from '@expo/vector-icons'; 
import { Platform } from 'expo-modules-core';


export default function App() {
  const [selectedImage, setSelectedImage] = React.useState(null);

  let openImagePickerAsync = async () => {
    let permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();

    if (permissionResult.granted === false) {
      alert("Permission to access camera roll is required!");
    }

    let pickerResult = await ImagePicker.launchImageLibraryAsync();

    if (pickerResult.cancelled === true) {
      return;
    }

    setSelectedImage({ localUri: pickerResult.uri });
  };

  let openShareDialogAsync = async () => {
    if (Platform.OS === 'web') {
      alert("Uh oh, sharing isn't available on your platform");
      return;
    }

    await Sharing.shareAsync(selectedImage.localUri);
  };

  if (selectedImage !== null) {

    return(
      <View style={styles.container}>
        <Text style={styles.instructions}>
        To share a photo, press the button!
        </Text>

      <TouchableOpacity onPress={openShareDialogAsync} style={styles.button}>
        <Button

        title = "Share <3"

        type =''
        containerStyle={{ backgroundColor: 'white', borderColor: 'white' }}
        titleStyle={{ color: 'pink' }}
        onPress={openShareDialogAsync}
        />

      </TouchableOpacity>
        
        <Image
          source={{ uri: selectedImage.localUri }}
          style={styles.thumbnail}  
        />

      <Ionicons name="heart-circle-outline" size={32} color="white" />

      </View>
    )
  }

  return (
    <View style={styles.container}>
      <Text style={styles.instructions}>
        To upload a photo, press the button!
      </Text>

      <TouchableOpacity onPress={openImagePickerAsync} style={styles.button}>
        <Button
        title = "Pick a photo <3"
        type =''
        containerStyle={{ backgroundColor: 'white', borderColor: 'red' }}
        titleStyle={{ color: 'pink' }}
        onPress={openImagePickerAsync}
        />

      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'pink',
    alignItems: 'center',
    justifyContent: 'center',
  },

  thumbnail: {
    width: 250,
    height: 250,
    resizeMode: "contain",
    marginTop: 10,
    borderWidth: 10,
    borderColor: 'white'
  },

  instructions: {
    color: 'white',
    marginBottom: 10
  },
});
