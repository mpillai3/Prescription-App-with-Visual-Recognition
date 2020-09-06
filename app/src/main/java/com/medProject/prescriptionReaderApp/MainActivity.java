

package com.medProject.prescriptionReaderApp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageView imageView;
    private ImageButton captureButton, detectButton, addMedButton;
    private TextView recognizedText;
    private Bitmap imageBitmap;
    int medicineNameBlock;
    String medicineName2 = "";
    String medicineName1 = "";
    int directionsOfUseBlock = 100000;
    String unitOfMeasure = null;
    String numberOfUnits = null;
    String dayFrequency = null;
    String frequency = null;
    List<FirebaseVisionDocumentText.Block> blocks;
    private String currentPhotoPath;
    TextToSpeech speechTool;
    FirebaseDatabase rootNode;
    DatabaseReference userRef, reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);

        addMedButton = findViewById(R.id.addMedBtn);
        captureButton = findViewById(R.id.takePicture);
        detectButton = findViewById(R.id.detect);
        detectButton.setEnabled(false);

        recognizedText = findViewById(R.id.recognizedText);

//        Picasso.get().load("https://cdn.discordapp.com/attachments/750832888857231371/751991880266547220/free-image-resizer-cropper_35.png").into(imageView);

        rootNode = FirebaseDatabase.getInstance();
        userRef = rootNode.getReference("Users");
        reference = userRef.child("John Doe");

        speechTool = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    speechTool.setLanguage(Locale.US);
                }
            }
        });

        /*
        Capture a picture when the user clicks on the button
         */
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
                recognizedText.setText("");
                detectButton.setEnabled(true);
            }
        });

        detectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectTextFromImage();
                detectButton.setEnabled(false);
            }
        });

        addMedButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                MedicineObject medObject = new MedicineObject(numberOfUnits, unitOfMeasure, frequency, dayFrequency);
                reference.child(medicineName1 + " " + medicineName2).setValue(medObject);
                Toast.makeText(MainActivity.this, "MEDICATION ADDED", Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * Detect the text inside the picture
     */
    private void detectTextFromImage() {
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionDocumentTextRecognizer firebaseVisionTextRecognizer = FirebaseVision.getInstance().getCloudDocumentTextRecognizer();
        //FirebaseVisionTextRecognizer firebaseVisionTextRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        Task<FirebaseVisionDocumentText> firebaseVisionTextTask = firebaseVisionTextRecognizer.processImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
            @Override
            public void onSuccess(FirebaseVisionDocumentText firebaseVisionDocumentText) {
                categorizeBlocks(firebaseVisionDocumentText);
                displayText();
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
     */
    private void displayText() {
        String finalText = "Medicine Name: " + medicineName1 + " " + medicineName2 + "\n" + "Take " + numberOfUnits + " " + unitOfMeasure + " " + frequency + " times " + dayFrequency;
        speechTool.speak(finalText, TextToSpeech.QUEUE_FLUSH, null);
        recognizedText.setText(finalText);
    }


    /*
    this is code to categorize the blocks into the ones that are useful to us - the textblock with the name of the medicine and
    the text block with the directions of use
    */
    private void categorizeBlocks(FirebaseVisionDocumentText firebaseVisionText) {

        blocks = firebaseVisionText.getBlocks();
        if (blocks.isEmpty())
            Toast.makeText(this, "No text in image", Toast.LENGTH_SHORT).show();
        else
            for (int i = 0; i < blocks.size(); i++) {
                // System.out.println(blocks.get(i).getText().toString().toLowerCase());
                List<FirebaseVisionDocumentText.Paragraph> paras = blocks.get(i).getParagraphs();

                for (int j = 0; j < paras.size(); j++) {
                    List<FirebaseVisionDocumentText.Word> elements = paras.get(j).getWords();
                    //System.out.println(elements.get(2).getText());
                    for (int k = 0; k < elements.size(); k++) {
                        String word = elements.get(k).getText().toString().toLowerCase();

                        if ((word.equals("take") || (word.equals("taken"))) && (directionsOfUseBlock == 100000)) {
                            System.out.println("FOUND DIRECTIONS");
                            directionsOfUseBlock = i;
                            extractDetails(blocks);
                        }
                        String mySubstring = null;
                        if (word.length() > 2) {
                            mySubstring = word.substring(word.length() - 2, word.length());
                        }

                        //System.out.println(mySubstring);
                        if (word.length() > 2 && ((mySubstring.compareTo("mg")) == 0 || (mySubstring.compareTo("ml")) == 0)) {
                            System.out.println("you should only see this once");
                            //System.out.println(word);
                            medicineNameBlock = i;
                            if (k - 1 > -1) {

                                //System.out.println(elements.get(k - 1));
                                medicineName2 = elements.get(k - 1).getText();

                                if (k - 2 > -1) {
                                    // System.out.println("med1");
                                    medicineName1 = elements.get(k - 2).getText();
                                }
                            }


                        } //else if (word.length() > 1 && (word.substring(word.length() - 1, word.length() - 1).equals("g") || (word.substring(word.length() - 1, word.length() - 1).equals("l")))) {
                        //medicineNameBlock = i;
                    }//System.out.println("Next statement");
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

    private void extractDetails(List<FirebaseVisionDocumentText.Block> blocks) {
        List<FirebaseVisionDocumentText.Paragraph> paras = blocks.get(directionsOfUseBlock).getParagraphs();
        for (int j = 0; j < paras.size(); j++) {
            List<FirebaseVisionDocumentText.Word> words = paras.get(j).getWords();

            for (int k = 0; k < words.size(); k++) {
                String word = words.get(k).getText().toLowerCase().trim();
                //System.out.println(word);
                String prevWord = null;
                if (k != 0) {
                    prevWord = words.get(k - 1).getText().toString().toLowerCase().trim();
                }
                switch (word) {
                    case "daily":
                    case "weekly":
                    case "monthly":
                        dayFrequency = word;
                        break;
                    case "day":
                        if (prevWord.equals("a")) {
                            dayFrequency = "daily";
                        }
                        break;
                    case "week":
                        if (prevWord.equals("a")) {
                            dayFrequency = "weekly";
                        }
                        break;
                    case "month":
                        if (prevWord.equals("a")) {
                            dayFrequency = "monthly";
                        }
                        break;
                    case "once":
                    case "time":
                        frequency = "1";
                        break;
                    case "twice":
                        frequency = "2";
                        break;
                    case "thrice":
                        frequency = "3";
                        break;
                    case "times":
                        if (checkIfWordNumber(prevWord)) { //checks if its a number spelt out as a word
                            frequency = convertWordNumber(prevWord); //converts the word into a number but in string format
                        } else if (checkIfNumber(prevWord)) {
                            frequency = prevWord;
                        }
                        break;
                    case "tablet":
                    case "capsule":
                        numberOfUnits = "1";
                        unitOfMeasure = word;
                        break;
                    case "tablets":
                        unitOfMeasure = "tablet";
                        if (checkIfWordNumber(prevWord)) { //checks if its a number spelt out as a word
                            numberOfUnits = convertWordNumber(prevWord); //converts the word into a number but in string format
                        } else if (checkIfNumber(prevWord)) {
                            numberOfUnits = prevWord;
                        }
                        break;
                    case "capsules":
                        unitOfMeasure = "capsule";
                        if (checkIfWordNumber(prevWord)) { //checks if its a number spelt out as a word
                            numberOfUnits = convertWordNumber(prevWord); //converts the word into a number but in string format
                        } else if (checkIfNumber(prevWord)) {
                            numberOfUnits = prevWord;
                        }
                        break;


                }


            }
        }


    }


    private boolean checkIfWordNumber(String prevWord) {
        switch (prevWord) {
            case "one":
            case "two":
            case "three":
            case "four":
            case "five":
            case "six":
            case "seven":
            case "eight":
            case "nine":
            case "ten":
                return true;
            default:
                return false;
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
        switch (prevWord) {
            case "1":
            case "10":
            case "9":
            case "8":
            case "7":
            case "6":
            case "5":
            case "4":
            case "3":
            case "2":
                return true;
            default:
                return false;
        }

    }
}
