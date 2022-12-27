package com.yildiztarik.stegoapplicationn;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    //    final String MY_SECRET_SIGNATURE ="@3.14";
    Bitmap photo_bitmap;
    ImageView select_image;
    EditText key, message;
    Button encode_button;
    Uri image_uri;
    String encoded_message;
    Switch controller_switch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tanimla();
        islevver();
    }


    void tanimla() {
        controller_switch = findViewById(R.id.switch_item);
        select_image = findViewById(R.id.select_image);
        key = findViewById(R.id.key_edittext);
        message = findViewById(R.id.message_edittext);
        encode_button = findViewById(R.id.encode_image_button);
    }

    void islevver() {
        controller_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    controller_switch.setText(R.string.switch_decode);
                    encode_button.setText(R.string.decode_button_text);
                    message.setEnabled(false);
                    message.setText(R.string.disabled_text);

                } else {
                    controller_switch.setText(R.string.switch_encode);
                    encode_button.setText(R.string.encode_button_text);
                    message.setEnabled(true);
                    message.setText(R.string.encoded_text);

                }
            }
        });

        select_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // izin alınmadıysa, izin isteği yapılır
                    String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
                }
                Toast.makeText(MainActivity.this, "izinverildi", Toast.LENGTH_SHORT).show();

                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    //izin verilmediyse if bloğu çalışır.
                    //kullanıcıdan izin almak için
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            1);
                } else {
                    Intent image = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(image, 2);
                }
            }
        });

        encode_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (controller_switch.isChecked()) {
                    //decode modunda
                    if (key.getText().toString().length() < 8) {
                        Toast.makeText(MainActivity.this, "Lütfen en az 8 haneli bir anahtar giriniz.", Toast.LENGTH_SHORT).show();
                    }else if(image_uri==null){
                        Toast.makeText(MainActivity.this, "Lütfen fotoğraf seçin.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        try {
                            photo_bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), image_uri);
                            //   getEncodedText(photo_bitmap,key.getText().toString());
                            String extracted_text = extractTextFromImage(image_uri);
                            String user_key=key.getText().toString();
                            String mesaj = AES256.decrypt(extracted_text, user_key);
                            if (mesaj!=null){
                                Toast.makeText(MainActivity.this, "Gizli mesaj: " + mesaj, Toast.LENGTH_LONG).show();
                                temizle();
                            }else{
                                Toast.makeText(MainActivity.this, "Şifre yok ya da anahtar hatalı.", Toast.LENGTH_SHORT).show();
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    //encode modunda
                    if (image_uri == null) {
                        Toast.makeText(MainActivity.this, "Bir fotoğraf seçmeniz gerekiyor!", Toast.LENGTH_SHORT).show();
                    } else if (message.getText().length() == 0) {
                        Toast.makeText(MainActivity.this, "Şifrelemek istediğiniz mesajı henüz girmediniz!", Toast.LENGTH_SHORT).show();
                    } else if (key.getText().length() < 8) {
                        Toast.makeText(MainActivity.this, "Lütfen en az 8 haneli anahtar belirleyin!", Toast.LENGTH_SHORT).show();
                    } else {
                        encoded_message = AES256.encrypt(message.getText().toString(), key.getText().toString());
                        //       Toast.makeText(EncodeActivity.this, "Mesaj başarıyla şifrendi", Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this, encoded_message, Toast.LENGTH_SHORT).show();
                        try {
                            photo_bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), image_uri);

                            addTextToImage(photo_bitmap, encoded_message);
                            temizle();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            image_uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), image_uri);
                select_image.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void addTextToImage(Bitmap image, String text) {
        // Fotoğrafın byte dizisine dönüştür
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        // Fotoğrafın son byte'larına metni ekle
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] newImageBytes = new byte[imageBytes.length + textBytes.length];
        System.arraycopy(imageBytes, 0, newImageBytes, 0, imageBytes.length);
        System.arraycopy(textBytes, 0, newImageBytes, imageBytes.length, textBytes.length);

        try {
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES); //or getExternalFilesDir(null); for external storage
            String fileName = Long.toString(System.currentTimeMillis()).replaceAll(":", ".") + "_" + imageBytes.length + ".jpg";
            File file = new File(directory, fileName);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(newImageBytes);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(this, fileName + " Dosyası olusturuldu...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void temizle() {
        image_uri = null;
        key.setText("");
        message.setText("");
        select_image.setImageBitmap(null);
    }

    public String extractTextFromImage(Uri imageUri) throws IOException {
        String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(image_uri, projection, null, null, null);
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
        cursor.moveToFirst();
        String imageName = cursor.getString(columnIndex);
        int location1 = imageName.indexOf('_');
        int location2 = imageName.indexOf('.');
        int bytes_of_original_photo = Integer.parseInt(imageName.substring(location1 + 1, location2));
        // Fotoğraf dosyasının yolunu elde et
        String filePath = getFilePathFromUri(imageUri);
        // Fotoğraf dosyasını oku
        FileInputStream fileInputStream = new FileInputStream(filePath);
        // Fotoğraf dosyasının boyutunu al
        long fileSize = new File(filePath).length();
        // Dosyanın son byte'larından itibaren metni ayrıştır
        byte[] textInBytes = new byte[(int) (fileSize - bytes_of_original_photo)];

        fileInputStream.skip(bytes_of_original_photo);
        fileInputStream.read(textInBytes);
        // Dosya okuma işlemini gerçekleştir
        fileInputStream.read(textInBytes);
        // Metni UTF-8 formatta bir "String" değişkene ata
        String extractedText = new String(textInBytes, StandardCharsets.UTF_8);
        // Dosya okuma işlemini kapat
        fileInputStream.close();
        return extractedText;
    }

    public String getFilePathFromUri(Uri uri) {
// Uri formundaki adresi "ContentResolver" sınıfı kullanarak fotoğraf dosyasına dönüştür
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
// Fotoğraf dosyasının yolunu elde et
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}