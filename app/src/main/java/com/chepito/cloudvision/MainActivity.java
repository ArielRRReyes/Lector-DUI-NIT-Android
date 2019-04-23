package com.chepito.cloudvision;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "CloudVisionExample";
    static final int REQUEST_GALLERY_IMAGE = 100;

    private ProgressDialog mProgressDialog;
    List<String> listita;
    ListView lista;
    ArrayAdapter<String> arrayAdapter;
    private static final String REGEX_DUI = "\\b[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]-[0-9]\\b";
    private static final String REGEX_NIT = "\\b[0-9][0-9][0-9][0-9]-[0-9][0-9][0-9][0-9][0-9][0-9]-[0-9][0-9][0-9]-[0-9]\\b";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toast.makeText(this, "Selecciona una imagen o toma una!", Toast.LENGTH_SHORT).show();

    mProgressDialog = new ProgressDialog(this);
    lista=findViewById(R.id.lista);
    listita = new ArrayList<>();
    arrayAdapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_list_item_1,
                    listita );

    lista.setAdapter(arrayAdapter);
    }

    private void launchImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"),
                REQUEST_GALLERY_IMAGE);
    }

    private void launchCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePicture, 0);//zero can be replaced with any action code
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(data!= null)
        { performCloudVisionRequest(data.getData());

        }


    }

    public void performCloudVisionRequest(Uri uri) {
        if (uri != null) {
            try {
                Bitmap bitmap = resizeBitmap(
                        MediaStore.Images.Media.getBitmap(getContentResolver(), uri));
                callCloudVision(bitmap);

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void callCloudVision(final Bitmap bitmap) throws IOException {
        mProgressDialog = ProgressDialog.show(this, null,"Escaneando imagen con API Vision...", true);

        new AsyncTask<Object, Void, BatchAnnotateImagesResponse>() {
        @Override
        protected BatchAnnotateImagesResponse doInBackground(Object... params) {
            try {
               Vision  vision =  core.getVisionInstance();
                List<Feature> featureList = new ArrayList<>();

                Feature textDetection = new Feature();
                textDetection.setType("TEXT_DETECTION");
                textDetection.setMaxResults(10);
                featureList.add(textDetection);

                List<AnnotateImageRequest> imageList = new ArrayList<>();
                AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
                Image base64EncodedImage = getBase64EncodedJpeg(bitmap);
                annotateImageRequest.setImage(base64EncodedImage);
                annotateImageRequest.setFeatures(featureList);
                imageList.add(annotateImageRequest);

                BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
                batchAnnotateImagesRequest.setRequests(imageList);

                Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
                annotateRequest.setDisableGZipContent(true);
                Log.d(TAG, "Sending request to Google Cloud");

                BatchAnnotateImagesResponse response = annotateRequest.execute();
                return response;

            } catch (GoogleJsonResponseException e) {
                Log.e(TAG, "Request error: " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "Request error: " + e.getMessage());
            }
            return null;
        }

        protected void onPostExecute(BatchAnnotateImagesResponse response) {
            mProgressDialog.dismiss();
            Log.d(TAG,getDetectedTexts(response));

            if(getDetectedTexts(response).trim().length()>0)
            {
                listita.add(getDetectedTexts(response));
                arrayAdapter.notifyDataSetChanged();
            }

        }

    }.execute();
    }


    private String getDetectedTexts(BatchAnnotateImagesResponse response){

        boolean isDui=false;
        int slashCounter=0;
        int namesCounter=0;

        String message= "";

        if(response!=null)
        {
            Pattern pDui = Pattern.compile(REGEX_DUI);
            Pattern pNit = Pattern.compile(REGEX_NIT);

            List<EntityAnnotation> texts = response.getResponses().get(0)
                    .getTextAnnotations();
            if (texts != null) {
                for (EntityAnnotation text : texts) {
                    Matcher m = pDui.matcher(text.getDescription());   // get a matcher object
                    Matcher m1 = pNit.matcher(text.getDescription());   // get a matcher object

                    Log.d(TAG,"Hey: "+text.getDescription().length()+ " "+ text.getDescription());

                    if(namesCounter==2)
                    {
                        Log.d(TAG,"Yeih: "+text.getDescription().length()+ " "+ text.getDescription());
                        namesCounter++;

                    }
                    if(namesCounter==1)
                    {
                        Log.d(TAG,"Yeih: "+text.getDescription().length()+ " "+ text.getDescription());
                        namesCounter++;
                    }
                    if(slashCounter==3)
                    {
                        Log.d(TAG,"Yeih: Apellido "+text.getDescription().length()+ " "+ text.getDescription());
                        slashCounter++;

                    }
                    if(slashCounter==2)
                    {
                        Log.d(TAG,"Yeih: Apellido"+text.getDescription().length()+ " "+ text.getDescription());
                        slashCounter++;

                    }
                    if(slashCounter==1)
                    {
                        slashCounter++;
                    }


                    if(isDui)
                    {
                        if(text.getDescription().trim().equals("Names") && namesCounter ==0)
                        {
                            namesCounter++;
                        }

                        if( (text.getDescription().trim().equals("/")||(text.getDescription().trim().equals("/Surname")) ) && slashCounter ==0)
                        {
                            slashCounter++;
                        }
                       
                    }

                    if(m.find())
                    {
                        isDui = true;
                        message ="DUI: "+ (text.getDescription());
                        message += ("\n");
                    }
                    if(m1.find())
                    {
                        message ="NIT: "+ (text.getDescription());
                        message += ("\n");
                    }


                }
            } else {
                message = ("nothing\n");
            }
        }
        else{
            Toast.makeText(this, "Hubo un error al procesar la imagen, intentelo de nuevo!", Toast.LENGTH_SHORT).show();
        }

        return message.toString();
    }

    public Bitmap resizeBitmap(Bitmap bitmap) {

        int maxDimension = 1024;
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public Image getBase64EncodedJpeg(Bitmap bitmap) {
        Image image = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        image.encodeContent(imageBytes);
        return image;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.imagen:
                launchImagePicker();
                return true;
            case R.id.foto:
                launchCamera();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

}
