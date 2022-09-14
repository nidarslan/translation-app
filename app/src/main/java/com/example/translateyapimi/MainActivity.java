package com.example.translateyapimi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    LinearLayout cameraLayout, detectLayout, galleryLayout;
    ImageView imgView;
    TextView textArea;
    // creating variables for our image view,
    // text view and two buttons.
    private EditText edtLanguage;
    private TextView translateLanguageTV;
    private Button translateLanguageBtn;

    // create a variable for our
    // firebase language translator.
    FirebaseTranslator englishTurkishTranslator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        detectLayout = findViewById(R.id.detect);
        cameraLayout = findViewById(R.id.camera);
        galleryLayout = findViewById(R.id.gallery);


        imgView = findViewById(R.id.image);
        textArea = findViewById(R.id.idEdtLanguage);

        detectLayout.setOnClickListener(this);
        cameraLayout.setOnClickListener(this);
        galleryLayout.setOnClickListener(this);
        // firebase ceviri secenegimizi olusturuyoruz.
        FirebaseTranslatorOptions options =
                new FirebaseTranslatorOptions.Builder()
                        // kaynak dili
                        .setSourceLanguage(FirebaseTranslateLanguage.EN)
                        // ceviri dili
                        .setTargetLanguage(FirebaseTranslateLanguage.TR)
                        // sonra secenekleri olusturacagiz.
                        .build();
        // örnek alma satiri
        // firebase natural language icin.
        englishTurkishTranslator = FirebaseNaturalLanguage.getInstance().getTranslator(options);

        // degiskenlerin id'sini buldurma
        edtLanguage = findViewById(R.id.idEdtLanguage);
        translateLanguageTV = findViewById(R.id.idTVTranslatedLanguage);
        translateLanguageBtn = findViewById(R.id.idBtnTranslateLanguage);

        // on click listener buton için
        translateLanguageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // dili indirmek icin cagri yöntemi
                // cevirmemiz gereken modal
                String string = edtLanguage.getText().toString();
                downloadModal(string);
            }
        });
    }


    private void downloadModal(String input) {
        // modu indirme kodu
        // almanca dilinde tercüme etmemiz gerekli
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();

        // modu indirmek için kod
        englishTurkishTranslator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                //modal basariyla indirildiyse;
                Toast.makeText(MainActivity.this, "Lütfen bekleyin dil modeli indiriliyor.", Toast.LENGTH_SHORT).show();

                //girilen metni cevirmek için arama yöntemi.
                translateLanguage(input);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Modal yüklenemedi...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void translateLanguage(String input) {
        englishTurkishTranslator.translate(input).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                translateLanguageTV.setText(s);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Ceviri yapilamadi...", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.detect:
                try {
                    runTextRecognition();
                }catch (Exception e){
                    Toast.makeText(MainActivity.this, "Görsel Seçilmedi", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.camera:
                askCameraPermissions();
                break;
            case R.id.gallery:
                galleryAddPic();
                break;
        }
    }

    //Kamera Fonksiyonları
    static final int REQUEST_IMAGE = 0;
    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE);
    }

    static final int CAMERA_PERM = 2;

    private void askCameraPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
            },CAMERA_PERM);
        }
        else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            dispatchTakePictureIntent();
        }
        else{
            Toast.makeText(this, "Kamera İzni Kabul Edilmedi", Toast.LENGTH_SHORT).show();
        }
    }

    // Galeri Fonksiyonları
    static final int REQUEST_GALLERY = 1;

    private void galleryAddPic(){
        Intent mediaScanIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(mediaScanIntent, REQUEST_GALLERY);
    }

    // On Activity Result
    Bitmap galleryImage;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_IMAGE:
                if (resultCode == RESULT_OK){
                    Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
                    imgView.setImageBitmap(selectedImage);
                }
                break;
            case REQUEST_GALLERY:
                try {
                    Uri imageUri = data.getData();
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    galleryImage = BitmapFactory.decodeStream(imageStream);
                    imgView.setImageBitmap(galleryImage);

                }catch (Exception e){
                    Toast.makeText(this, "Görsel Galeriden Alınamadı",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    // Text Tespit İşlemleri

    //görseldeki yazıyı tespit edecek fonksiyonları calistirir
    private void runTextRecognition(){
        Bitmap bitmap = ((BitmapDrawable) imgView.getDrawable()).getBitmap();
        int rotationDegree = 0;
        InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);
        TextRecognizer recognizer = TextRecognition.getClient();
        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                processTextRecognition(visionText);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "Başarısız İşlem",Toast.LENGTH_SHORT).show();
                                    }
                                });
    }

    private void processTextRecognition(Text visionText) {
        List<Text.TextBlock> blocks = visionText.getTextBlocks();
        if (blocks.size() == 0){
            Toast.makeText(MainActivity.this, "Görselde Metin Tespit Edilemedi",Toast.LENGTH_LONG).show();
        }

        StringBuilder text = new StringBuilder();

        for (int i = 0; i<blocks.size();i++){
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j<lines.size();j++){
                List<Text.Element> elements = lines.get(j).getElements();
                for (int k = 0; k<elements.size();k++){
                    text.append(elements.get(k).getText() + " ");
                }
            }
        }
        textArea.setText(text);
    }

}