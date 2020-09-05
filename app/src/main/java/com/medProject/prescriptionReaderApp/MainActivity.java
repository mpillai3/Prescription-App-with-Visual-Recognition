package com.medProject.prescriptionReaderApp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageView imageView;
    private Button captureButton, detectButton;
    private TextView recognizedText;
    private Bitmap imageBitmap;
    int medicineNameBlock;
    int directionsOfUseBlock;
    String unitOfMeasure;
    String numberOfUnits;
    String dayFrequency;
    String frequency;
    List<FirebaseVisionText.TextBlock> blocks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        captureButton = findViewById(R.id.takePicture);
        detectButton = findViewById(R.id.detectText);
        recognizedText = findViewById(R.id.recognizedText);

        /*
        Capture a picture when the user clicks on the button
         */
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
                recognizedText.setText("");
            }
        });

        /*
        Start the text detection process
         */
        detectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectTextFromImage();
            }
        });
    }

    /**
     * Detect the text inside the picture
     */
    private void detectTextFromImage() {
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextRecognizer firebaseVisionTextRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        Task<FirebaseVisionText> firebaseVisionTextTask = firebaseVisionTextRecognizer.processImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                displayText(firebaseVisionText);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("Error: ", e.getMessage());
            }
        });
    }

    /**
     * Displays the final result of the text recognized, analyzed and categorized to fit into a scheduling app
     * @param firebaseVisionText - The data inside the image
     */
    private void displayText(FirebaseVisionText firebaseVisionText) {
       String finalText = "Medicine Name: " + blocks.get(medicineNameBlock).getText() + "\n" + "frequency: " + frequency +"\n" + "Day Frequency: " + dayFrequency
                + "\n" + "unit of measure: " + unitOfMeasure + "\n" + "number of units: " + numberOfUnits;
        recognizedText.setText(finalText);
    }
    
    
    
    /*
    this is code to categorize the blocks into the ones that are useful to us - the textblock with the name of the medicine and
    the text block with the directions of use
    */
        private void categorizeBlocks(FirebaseVisionText firebaseVisionText) {

        blocks = firebaseVisionText.getTextBlocks();
        if (blocks.isEmpty())
            Toast.makeText(this, "No text in image", Toast.LENGTH_SHORT).show();
        else
            for (int i = 0; i < blocks.size(); i++) {
                List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();

                for (int j = 0; j < lines.size(); j++) {
                    List<FirebaseVisionText.Element> elements = lines.get(j).getElements();

                    for (int k = 0; k < elements.size(); k++) {
                        String word = elements.get(k).getText().toLowerCase();
                        if (word == "take" || word == "taken") {
                            directionsOfUseBlock = i;
                            extractDetails(blocks);
                        }
                        else if (word.substring(word.length() - 2, word.length() - 1) == "mg" || (word.substring(word.length() - 2, word.length() - 1) == "ml")) {
                            medicineNameBlock = i;
                        }
                        else if (word.substring(word.length() - 1, word.length() - 1) == "g" || (word.substring(word.length() - 1, word.length() - 1) == "l")) {
                            medicineNameBlock = i;
                        }

                    }

                }


            }
    }

    }

    /**
     * Brind up the window to capture a picture
     */
    private void dispatchTakePictureIntent() {
        // this means we only store a single picture and override the previous one with the new one
        String fileName = "photo";
        File file = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File imageFile = File.createTempFile(fileName, ".jpg", file);
            currentPhotoPath = imageFile.getAbsolutePath();

            Uri imageUri = FileProvider.getUriForFile(MainActivity.this,
                    "com.medProject.prescriptionReaderApp.fileprovider", imageFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            imageView.setImageBitmap(imageBitmap);
        }
    }
    

    /*
    This code is to analyze each word to check for keywords and store them as relevant information which is later displayed on the app as the final text
    - see display text function
    */

    private void extractDetails(List<FirebaseVisionText.TextBlock> blocks) {
        List<FirebaseVisionText.Line> lines = blocks.get(directionsOfUseBlock).getLines();
        for (int j = 0; j < lines.size(); j++) {
            List<FirebaseVisionText.Element> elements = lines.get(j).getElements();

            for (int k = 0; k < elements.size(); k++) {
                String word = elements.get(k).getText().toLowerCase();
                String prevWord = elements.get(k-1).getText().toLowerCase();
                switch(word){
                    case "daily" : dayFrequency = word;
                    case "weekly": dayFrequency = word;
                    case "monthly": dayFrequency = word;
                    case "day" :

                        if (prevWord == "a"){
                            dayFrequency = "daily";
                        }
                    case "week" :
                        if (prevWord == "a"){
                            dayFrequency = "weekly";
                        }
                    case "month" :
                        if (prevWord == "a"){
                            dayFrequency = "monthly";
                        }
                    case "once": frequency="1";
                    case "twice": frequency="2";
                    case "thrice": frequency="3";
                    case "time" : frequency="1";
                    case "times":
                        if (checkIfWordNumber(prevWord)) { //checks if its a number spelt out as a word
                            frequency = convertWordNumber(prevWord); //converts the word into a number but in string format
                        }
                        else if (checkIfNumber(prevWord)){
                            frequency = prevWord;
                        }
                    case "tablet": numberOfUnits="1"; unitOfMeasure = word;
                    case "capsule": numberOfUnits="1"; unitOfMeasure = word;
                    case "tablets":
                        unitOfMeasure="tablet";
                        if (checkIfWordNumber(prevWord)) { //checks if its a number spelt out as a word
                            numberOfUnits = convertWordNumber(prevWord); //converts the word into a number but in string format
                        }
                        else if (checkIfNumber(prevWord)){
                            numberOfUnits = prevWord;
                        }
                    case "capsules":
                        unitOfMeasure = "capsule";
                        if (checkIfWordNumber(prevWord)) { //checks if its a number spelt out as a word
                            numberOfUnits = convertWordNumber(prevWord); //converts the word into a number but in string format
                        }
                        else if (checkIfNumber(prevWord)){
                            numberOfUnits = prevWord;
                        }




                }





            }
        }


    }



    private boolean checkIfWordNumber(String prevWord) {
        switch(prevWord){
            case "one": return true;
            case "two": return true;
            case "three": return true;
            case "four": return true;
            case "five": return true;
            case "six": return true;
            case "seven": return true;
            case "eight": return true;
            case "nine": return true;
            case "ten": return true;
            default: return false;
        }
    }

    private String convertWordNumber(String prevWord) {
        switch (prevWord) {
            case "one":
                return "1";
            case "two":
                return "2";
            case "three":
                return "3";
            case "four":
                return "4";
            case "five":
                return "5";
            case "six":
                return "6";
            case "seven":
                return "7";
            case "eight":
                return "8";
            case "nine":
                return "9";
            case "ten":
                return "10";
            default:
                return "0"; //check if this is a valid default
        }
    }


        private boolean checkIfNumber(String prevWord) {
            switch(prevWord){
                case "1": return true;
                case "2": return true;
                case "3": return true;
                case "4": return true;
                case "5": return true;
                case "6": return true;
                case "7": return true;
                case "8": return true;
                case "9": return true;
                case "10": return true;
                default: return false;
            }

        }
}
