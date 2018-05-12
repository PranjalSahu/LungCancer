package mariannelinhares.mnistandroid;

/*
   Copyright 2016 Narrative Nights Inc. All Rights Reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   From: https://raw.githubusercontent
   .com/miyosuda/TensorFlowAndroidMNIST/master/app/src/main/java/jp/narr/tensorflowmnist
   /DrawModel.java
*/

//An activity is a single, focused thing that the user can do. Almost all activities interact with the user,
//so the Activity class takes care of creating a window for you in which you can place your UI with setContentView(View)
import android.Manifest;
import android.app.Activity;
//PointF holds two float coordinates
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
//A mapping from String keys to various Parcelable values (interface for data container values, parcels)
import android.media.Image;
import android.os.Bundle;
//Object used to report movement (mouse, pen, finger, trackball) events.
// //Motion events may hold either absolute or relative movements and other data, depending on the type of device.
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
//This class represents the basic building block for user interface components.
// A View occupies a rectangular area on the screen and is responsible for drawing
import android.view.View;
//A user interface element the user can tap or click to perform an action.
import android.widget.Button;
//A user interface element that displays text to the user. To provide user-editable text, see EditText.
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
//Resizable-array implementation of the List interface. Implements all optional list operations, and permits all elements,
// including null. In addition to implementing the List interface, this class provides methods to
// //manipulate the size of the array that is used internally to store the list.
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
// basic list
import java.util.List;
//encapsulates a classified image
//public interface to the classification class, exposing a name and the recognize function
import mariannelinhares.mnistandroid.models.Classifier;
import mariannelinhares.mnistandroid.models.Classification;
//contains logic for reading labels, creating classifier, and classifying
import mariannelinhares.mnistandroid.models.TensorFlowClassifier;
//class for drawing MNIST digits by finger
import mariannelinhares.mnistandroid.views.DrawModel;
//class for drawing the entire app
import mariannelinhares.mnistandroid.views.DrawView;

import android.graphics.BitmapFactory;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements View.OnClickListener, View.OnTouchListener {

    private static final int PIXEL_WIDTH = 28;
    private final int FLAG_TO_READ = 1;

    // ui elements
    private Button clearBtn, classBtn;
    private TextView resText;
    private List<Classifier> mClassifiers = new ArrayList<>();
    private ImageView mainimageview;

    // views
    private DrawModel drawModel;
    private PointF    mTmpPiont = new PointF();

    private float mLastX;
    private float mLastY;

    private int[] intValues;
    private float[] floatValues;

    void read_the_input_image(){
        // reading the image file to get the time taken to get the inference
        File imgFile = new  File("/sdcard/Pictures/1.jpg");
        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

        // array to read all the pixel values which will feed into the tensorflow model
        intValues = new int[myBitmap.getHeight() * myBitmap.getWidth()];
        floatValues = new float[myBitmap.getHeight() * myBitmap.getWidth() * 3];
        myBitmap.getPixels(intValues, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
            floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
        }

        Log.d("TIME_TO_FEED", "Height of the Image is: " + Integer.toString(myBitmap.getHeight())+" : Widht is : "+Integer.toString(myBitmap.getWidth()));

        return;
    }
    @Override
    // In the onCreate() method, you perform basic application startup logic that should happen
    //only once for the entire life of the activity.
    protected void onCreate(Bundle savedInstanceState) {
        //initialization
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //class button
        //when tapped, this performs classification on the drawn image
        classBtn = (Button) findViewById(R.id.btn_class);
        classBtn.setOnClickListener(this);

        // res text
        //this is the text that shows the output of the classification
        resText = (TextView) findViewById(R.id.tfRes);

        mainimageview = (ImageView) findViewById(R.id.imageView1);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 225);

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            try (FileInputStream in = new FileInputStream(new File("/storage/self/primary/Pictures/convertedpng/000003.dcm.png"))) {
                mainimageview.setImageBitmap(BitmapFactory.decodeStream(in));
                System.out.println("Image is set in the ImageView");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            System.out.println("Error file read permission has not been granted");
        }


        // read the input image to be passed to the tensorflow classifier model
        read_the_input_image();

        SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar);
        final TextView seekBarValue = (TextView)findViewById(R.id.simpleTextView4);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

           @Override
           public void onProgressChanged(SeekBar seekBar, int progress,
           boolean fromUser) {
               // TODO Auto-generated method stub
               seekBarValue.setText("Z position "+String.valueOf(progress));
           }

           @Override
           public void onStartTrackingTouch(SeekBar seekBar) {
               // TODO Auto-generated method stub
           }

           @Override
           public void onStopTrackingTouch(SeekBar seekBar) {
               // TODO Auto-generated method stub
           }
       });

        // tensorflow
        //load up our saved model to perform inference from local storage
        loadModel();
    }

    //the activity lifecycle

    @Override
    //OnResume() is called when the user resumes his Activity which he left a while ago,
    // //say he presses home button and then comes back to app, onResume() is called.
    protected void onResume() {
        super.onResume();
    }

    @Override
    //OnPause() is called when the user receives an event like a call or a text message,
    // //when onPause() is called the Activity may be partially or completely hidden.
    protected void onPause() {
        super.onPause();
    }
    //creates a model object in memory using the saved tensorflow protobuf model file
    //which contains all the learned weights
    private void loadModel() {
        //The Runnable interface is another way in which you can implement multi-threading other than extending the
        // //Thread class due to the fact that Java allows you to extend only one class. Runnable is just an interface,
        // //which provides the method run.
        // //Threads are implementations and use Runnable to call the method run().
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //add 2 classifiers to our classifier arraylist
                    //the tensorflow classifier and the keras classifier
                    mClassifiers.add(
                            TensorFlowClassifier.create(getAssets(), "TensorFlow",
                                    "model.h5.pb", "labels.txt", PIXEL_WIDTH,
                                    "input_1", "output_node0", true));
                    mClassifiers.add(
                            TensorFlowClassifier.create(getAssets(), "Keras",
                                    "opt_mnist_convnet-keras.pb", "labels.txt", PIXEL_WIDTH,
                                    "conv2d_1_input", "dense_2/Softmax", false));
                } catch (final Exception e) {
                    //if they aren't found, throw an error!
                    throw new RuntimeException("Error initializing classifiers!", e);
                }
            }
        }).start();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_class) {
            Classifier classifier = mClassifiers.get(0);
            long startTime = SystemClock.uptimeMillis();
            final Classification res = classifier.recognize(floatValues);
            long endTime = SystemClock.uptimeMillis();
            Log.d("TIME_TO_FEED", "Timecost to model inference: " + Long.toString(endTime - startTime));


//
//            //init an empty string to fill with the classification output
//            String text = "";
//            //for each classifier in our array
//            for (Classifier classifier : mClassifiers) {
//                //perform classification on the image
//                final Classification res = classifier.recognize(pixels);
//                //if it can't classify, output a question mark
//                if (res.getLabel() == null) {
//                    text += classifier.name() + ": ?\n";
//                } else {
//                    //else output its name
//                    text += String.format("%s: %s, %f\n", classifier.name(), res.getLabel(),
//                            res.getConf());
//                }
//            }
//            resText.setText(text);
        }
    }

    @Override
    //this method detects which direction a user is moving
    //their finger and draws a line accordingly in that
    //direction
    public boolean onTouch(View v, MotionEvent event) {
        //get the action and store it as an int
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        //actions have predefined ints, lets match
        //to detect, if the user has touched, which direction the users finger is
        //moving, and if they've stopped moving

        //if touched
        if (action == MotionEvent.ACTION_DOWN) {
            //begin drawing line

            return true;
            //draw line in every direction the user moves
        } else if (action == MotionEvent.ACTION_MOVE) {

            return true;
            //if finger is lifted, stop drawing
        } else if (action == MotionEvent.ACTION_UP) {

            return true;
        }
        return false;
    }

}