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

import static android.content.ContentValues.TAG;
//import flanagan.interpolation.*;
//import flanagan.io.*;


//An activity is a single, focused thing that the user can do. Almost all activities interact with the user,
//so the Activity class takes care of creating a window for you in which you can place your UI with setContentView(View)
import android.Manifest;
import android.app.Activity;
//PointF holds two float coordinates
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
//A mapping from String keys to various Parcelable values (interface for data container values, parcels)
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
//Object used to report movement (mouse, pen, finger, trackball) events.
// //Motion events may hold either absolute or relative movements and other data, depending on the type of device.
import android.os.Environment;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
// basic list
import java.util.Arrays;
import java.util.List;
import java.util.Random;
//encapsulates a classified image
//public interface to the classification class, exposing a name and the recognize function
import flanagan.interpolation.*;
import mariannelinhares.mnistandroid.models.Classifier;
import mariannelinhares.mnistandroid.models.Classification;
//contains logic for reading labels, creating classifier, and classifying
import mariannelinhares.mnistandroid.models.TensorFlowClassifier;
//class for drawing MNIST digits by finger
import mariannelinhares.mnistandroid.views.DrawModel;
//class for drawing the entire app
import mariannelinhares.mnistandroid.views.DrawView;

import android.graphics.BitmapFactory;



public class MainActivity extends Activity implements View.OnClickListener, View.OnTouchListener {

    private static final int PIXEL_WIDTH = 28;
    private final int FLAG_TO_READ = 1;

    // numbers are calculated to obtain a volume of size 50x50x50
    // values for interpolation
    double[][][] interp_values  = new double[87][87][18];

    // for storing the touch coordinates
    float screenX;
    float screenY;

    // x and y coordinate calculate by touch
    int xCoord;
    int yCoord;

    // ui elements
    private Button clearBtn, classBtn;
    private TextView probtext;
    private TextView resText;

    private List<Classifier> mClassifiers = new ArrayList<>();
    ArrayList<Bitmap> ctarray = new ArrayList<Bitmap>();

    private ImageView mainimageview;
    private ImageView axial;
    private ImageView coronal;
    private ImageView sagittal;
    private ImageView selectedSection;

    int currentSection = 0;

    int divisions = 3;
    double div    = Math.PI/divisions;

    double thickness_x_y = 0.58203125;
    double thickness_z   = 3.0;

    // views
    private DrawModel drawModel;
    private PointF    mTmpPiont = new PointF();

    private float mLastX;
    private float mLastY;

    private int[]   intValues;
    private float[] floatValues;

    Random randomObject = new Random();

    // the interpolation object
    //TriCubicInterpolation tci3;

    Bitmap get_section(int section_name){

        Log.d("MYAPP", "Inside the method get_section Bitmap, "+Integer.toString(section_name));

        int pixelsIndex = 0;
        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Bitmap bmp = Bitmap.createBitmap(50, 50,  conf);

        if (section_name == 0){
            // Axial section
            for (int i = 0; i < 50; i++) {
                for (int j = 0; j < 50; j++) {
                    int t = (int) (interpolate(i, j, 25)*255);
                    //if(t < 0)
                    //    t= 0;
                    //System.out.println("Color value is "+Integer.toString(t));
                    int c = Color.argb(255, t, t, t);
                    bmp.setPixel(i, j, c);
                }
            }
        }
        else if(section_name == 1){
            // Sagattial section
            for (int j = 0; j < 50; j++) {
                for (int k = 0; k < 50; k++) {
                    int t = (int) (interpolate(25, j, k)*255);
                    if(t <0)
                        t= 0;
                    int c = Color.argb(255, t, t, t);
                    //System.out.println("Color value is "+Integer.toString(t));
                    bmp.setPixel(j, k, c);
                    //bmp.setPixel(k, j, c);
                }
            }
        }
        else{
            // Coronnal section
            for (int i = 0; i < 50; i++) {
                for (int k = 0; k < 50; k++) {
                    int t = (int) (interpolate(i, 25, k)*255);
                    if(t <0)
                        t= 0;
                    int c = Color.argb(255, t, t, t);
                    //System.out.println("Color value is "+Integer.toString(t));
                    bmp.setPixel(i, k, c);
                }
            }
        }

        return bmp;
    }

    double interpolate(double x, double y, double z){
        double v000, v100, v010, v001, v101, v011, v110, v111;

        int zindex = (int) (z/thickness_z);
        int xindex = (int) (x/thickness_x_y);
        int yindex = (int) (y/thickness_x_y);

        x = x - xindex*thickness_x_y;
        y = y - yindex*thickness_x_y;
        z = z - zindex*thickness_z;

//        int yindex = (int) (z/thickness_z);
//        int xindex = (int) (x/thickness_x_y);
//        int zindex = (int) (y/thickness_x_y);
//
//        x = x - xindex*thickness_x_y;
//        z = z - zindex*thickness_x_y;
//        y = y - yindex*thickness_z;

        v000 = interp_values[xindex][yindex][zindex];
        v100 = interp_values[xindex+1][yindex][zindex];
        v010 = interp_values[xindex][yindex+1][zindex];
        v110 = interp_values[xindex+1][yindex+1][zindex];

        v001 = interp_values[xindex][yindex][zindex+1];
        v101 = interp_values[xindex+1][yindex][zindex+1];
        v011 = interp_values[xindex][yindex+1][zindex+1];
        v111 = interp_values[xindex+1][yindex+1][zindex+1];

        return  (
                    v000*(thickness_x_y-x)*(thickness_x_y-y)*(thickness_z-z)+
                    v100*x*(thickness_x_y-y)*(thickness_z-z)+
                    v010*(thickness_x_y-x)*y*(thickness_z-z)+
                    v001*(thickness_x_y-x)*(thickness_x_y-y)*z+
                    v101*x*(thickness_x_y-y)*z+
                    v011*(thickness_x_y-x)*y*z+
                    v110*x*y*(thickness_z-z)+
                    v111*x*y*z
                ) /(thickness_x_y*thickness_x_y*thickness_z);
    }

    // fill the array for interpolating the values
    // [x][y][z]
    void fill_interpolate_array(){

//        AsyncTask task = new AsyncTask(){
//            protected void onPostExecute(String result) {
//                //Do your thing
//            }
//
//        };

        //AsyncTask.execute(new Runnable() {
          //  @Override
            //public void run() {
                for(int k=0; k < 18; ++k) {
                    int curr_index = currentSection - 8 + k;
                    Bitmap selectedImage = Bitmap.createBitmap(ctarray.get(curr_index), xCoord - 43,
                            yCoord - 43, 87, 87);
                    for (int i = 0; i < 87; ++i) {
                        for (int j = 0; j < 87; ++j) {
                            // since red, green and blue color are all same in a grayscale image
                            interp_values[i][j][k] = Color.red(selectedImage.getPixel(i, j))/255.0;
                        }
                    }
                }

                //tci3 = null;
                // create a new interpolation object
                //tci3 = new TriCubicInterpolation(interp_x, interp_y, interp_z, interp_values, 0);
                Log.d("MYAPP", "Created a new Interpolation Object");

                //tci3.displayLimits();

                Bitmap axial_image      = get_section(0);
                Bitmap sagattial_image  = get_section(1);
                Bitmap coronal_image    = get_section(2);

                axial.setImageBitmap(axial_image);
                sagittal.setImageBitmap(sagattial_image);
                coronal.setImageBitmap(coronal_image);

            //}
        //});

        ArrayList<Bitmap> allsections  = get_rotated_sections();

        sagittal.setImageBitmap(allsections.get(randomObject.nextInt(allsections.size())));

        int sectioncount = 0;
        for(Bitmap bmp_temp:allsections){
            System.out.println("Saving Interpolated Image "+Integer.toString(sectioncount));

            FileOutputStream outf = null;
            File sd = Environment.getExternalStorageDirectory();
            File dest = new File(sd, "section"+Integer.toString(sectioncount)+".png");
            System.out.println("File created " + dest.getAbsolutePath());

            try {
                //outf = new FileOutputStream("/storage/self/primary/Pictures/section"++".png");
                outf = new FileOutputStream(dest);
                bmp_temp.compress(Bitmap.CompressFormat.PNG, 100, outf);
                outf.flush();
                outf.close();
            } catch (FileNotFoundException e) {
                System.out.println("Saving Interpolated Image with Error");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Saving Interpolated Image with IO Error");
                e.printStackTrace();
            }

            sectioncount = sectioncount+1;

        }

        return;
    }


//    void read_the_input_image(){
//        // reading the image file to get the time taken to get the inference
//        File imgFile = new  File("/sdcard/Pictures/1.jpg");
//        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//
//        // array to read all the pixel values which will feed into the tensorflow model
//        intValues = new int[myBitmap.getHeight() * myBitmap.getWidth()];
//        floatValues = new float[myBitmap.getHeight() * myBitmap.getWidth() * 3*10];
//        myBitmap.getPixels(intValues, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
//        for (int i = 0; i < intValues.length; ++i) {
//            final int val = intValues[i];
//            floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
//            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
//            floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
//        }
//
//        Log.d("TIME_TO_FEED", "Height of the Image is: " + Integer.toString(myBitmap.getHeight())+" : Widht is : "+Integer.toString(myBitmap.getWidth()));
//
//        return;
//    }

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
        resText = (TextView) findViewById(R.id.tfRes);

        mainimageview = (ImageView) findViewById(R.id.imageView1);
        axial    = (ImageView) findViewById(R.id.imageView21);
        coronal  = (ImageView) findViewById(R.id.imageView23);
        sagittal = (ImageView) findViewById(R.id.imageView24);
        selectedSection = (ImageView) findViewById(R.id.imageView6);;

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 225);

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            File directory = new File("/storage/self/primary/Pictures/converted/");
            File[] files = directory.listFiles();
            Arrays.sort(files);

            System.out.println("Reading CT files");

            for(int fi=0; fi<files.length;++fi){
                FileInputStream in = null;
                System.out.println("File path is "+files[fi]);
                try {
                    in = new FileInputStream(new File(files[fi].getPath()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                ctarray.add(BitmapFactory.decodeStream(in));
            }

            mainimageview.setImageBitmap(ctarray.get(currentSection));
            System.out.println("Image is set in the ImageView");


            FileInputStream ina = null;
            FileInputStream inb = null;
            FileInputStream inc = null;
            FileInputStream ind = null;
            try {
                ina = new FileInputStream(new File("/storage/self/primary/Pictures/1_a.jpg"));
                inb = new FileInputStream(new File("/storage/self/primary/Pictures/2_a.jpg"));
                inc = new FileInputStream(new File("/storage/self/primary/Pictures/3_a.jpg"));
                ind = new FileInputStream(new File("/storage/self/primary/Pictures/3_a_224.jpg"));

                axial.setImageBitmap(BitmapFactory.decodeStream(ina));
                coronal.setImageBitmap(BitmapFactory.decodeStream(inb));
                sagittal.setImageBitmap(BitmapFactory.decodeStream(inc));
                selectedSection.setImageBitmap(BitmapFactory.decodeStream(ind));

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
        else{
            System.out.println("Error file read permission has not been granted");
        }


        // read the input image to be passed to the tensorflow classifier model
        //read_the_input_image();

        SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar);
        final TextView seekBarValue = (TextView)findViewById(R.id.simpleTextView4);

        probtext = (TextView)findViewById(R.id.simpleTextView);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

           @Override
           public void onProgressChanged(SeekBar seekBar, int progress,
           boolean fromUser) {
               mainimageview.setImageBitmap(ctarray.get(progress));
               seekBarValue.setText("Z position "+String.valueOf(progress));
               currentSection = progress;
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

        mainimageview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    screenX = event.getX();
                    screenY = event.getY();

                    Matrix inverse = new Matrix();
                    mainimageview.getImageMatrix().invert(inverse);
                    float[] touchPoint = new float[] {screenX, screenY};
                    inverse.mapPoints(touchPoint);
                    xCoord = Integer.valueOf((int)touchPoint[0]);
                    yCoord = Integer.valueOf((int)touchPoint[1]);

                    Bitmap sourceBitmap = ctarray.get(currentSection);

                    System.out.println("Crop Image Coordinates are ("+Integer.toString(xCoord)+", "+
                            Integer.toString(yCoord)+"), ("+
                            Integer.toString(xCoord-50)+", "+
                            Integer.toString(yCoord-50)+", "+
                            Integer.toString(xCoord+50)+", "+
                            Integer.toString(yCoord+50)+"), ("+
                            Integer.toString(selectedSection.getHeight())+", "+
                            Integer.toString(selectedSection.getWidth())+"), ("+
                            Integer.toString(ctarray.get(currentSection).getHeight())+ ", "+
                            Integer.toString(ctarray.get(currentSection).getWidth())+")"
                    );


                    Bitmap selectedImage = Bitmap.createBitmap(ctarray.get(currentSection), xCoord-43,
                            yCoord-43, 86, 86);

//                    for(int pk=0;pk<86;pk++){
//                        for(int qk=0;qk<86;++qk){
//                            int pc = selectedImage.getPixel(pk, qk);
//
//                            System.out.println(Integer.toString(Color.alpha(pc))+","+
//                                    Integer.toString(Color.red(pc))+","+
//                                    Integer.toString(Color.green(pc))+","+
//                                    Integer.toString(Color.blue(pc)));
//                        }
//                    }
                    //selectedSection.setImageBitmap(selectedImage);

                    // interpolate the volume
                    fill_interpolate_array();

//                    for(int p=0;p<87;++p){
//                        for(int q=0;q<87;++q){
//                            //selectedImage.se
//                        }
//                    }

                    //System.out.println("Number of colors in the selectedImage is "+Integer.toString(selectedImage.get));
                    //tci3
                    selectedSection.setImageBitmap(selectedImage);

                    return true;
                }

                return true;
            }
        });

        // Dummy code to print the coordinates of the section


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


    double[] rotation_matrix(double[] axis, double theta) {
        double dotvalue = 0;
        for(int i=0;i<3;++i){
            dotvalue = dotvalue+axis[i]*axis[i];
        }
        dotvalue = Math.sqrt(dotvalue);
        for(int i=0;i<3;++i){
            axis[i] = axis[i]/dotvalue;
        }

        double a = Math.cos(theta / 2.0);
        double b, c, d, aa, bb, cc, dd;
        double bc, ad, ac, ab, bd, cd;

        b = -axis[0]* Math.sin(theta / 2.0);
        c = -axis[1]* Math.sin(theta / 2.0);
        d = -axis[2]* Math.sin(theta / 2.0);

        aa = a*a;
        bb = b*b;
        cc = c*c;
        dd = d*d;

        bc = b*c;
        ad = a*d;
        ac = a*c;
        ab = a*b;
        bd = b*d;
        cd = c*d;

        double [] return_ans = {
                                    aa+bb-cc-dd,   2 * (bc + ad),
                                    2 * (bd - ac), 2 * (bc - ad),
                                    aa + cc - bb - dd, 2 * (cd + ab),
                                    2 * (bd + ac), 2 * (cd - ab),
                                    aa + dd - bb - cc
                                };
        return return_ans;
    }

    ArrayList<Bitmap> get_rotated_sections(){
        ArrayList<Bitmap> allsections = new ArrayList<Bitmap>();
        int sectionImagecount = 0;

        for(int divb=0; divb < divisions+1; ++divb){
            for(int diva=0; diva < divisions+1; ++diva){
                double anglea = diva*div;
                double angleb = divb*div;

                double x = Math.cos(anglea)*Math.sin(angleb);
                double y = Math.sin(anglea)*Math.sin(angleb);
                double z = Math.cos(angleb);

                double [] normal = {x, y, z};
                for(int i=0;i<3;++i){
                    if(Math.abs(normal[i]) < 0.000001){
                        normal[i] = 0;
                    }
                }

                double [] yy = new double[2500];
                double [] zz = new double[2500];
                double [] xx = new double[2500];

                int count = 0;
                for(int i=0;i<50;++i){
                    for(int j=0;j<50;++j){
                        yy[count] = j-25;
                        zz[count] = i-25;
                        xx[count] = 0;
                        count = count+1;
                    }
                }

                double [] axis1 = {0, 0, 1};
                double [] rt1 = rotation_matrix(axis1, anglea);

                double [] val1_prime_row1 = new double[2500];
                double [] val1_prime_row2 = new double[2500];
                double [] val1_prime_row3 = new double[2500];

                for(int i=0; i<2500; ++i){
                    val1_prime_row1[i] = rt1[0]*xx[i] + rt1[1]*yy[i] + rt1[2]*zz[i];
                    val1_prime_row2[i] = rt1[3]*xx[i] + rt1[4]*yy[i] + rt1[5]*zz[i];
                    val1_prime_row3[i] = rt1[6]*xx[i] + rt1[7]*yy[i] + rt1[8]*zz[i];
                }

                double [] newaxis = {rt1[3], rt1[4], rt1[5]};
                double newaxissum = 0;
                for(int i=0;i<3;++i){
                    newaxissum = newaxissum+newaxis[i]*newaxis[i];
                }
                newaxissum = Math.sqrt(newaxissum);

                double [] rt2 = rotation_matrix(newaxis, angleb);

                double [] val2_prime_row1 = new double[2500];
                double [] val2_prime_row2 = new double[2500];
                double [] val2_prime_row3 = new double[2500];

                for(int i=0; i<2500; ++i){
                    val2_prime_row1[i] = rt1[0]*val1_prime_row1[i] + rt1[1]*val1_prime_row2[i] + rt1[2]*val1_prime_row3[i];
                    val2_prime_row2[i] = rt1[3]*val1_prime_row1[i] + rt1[4]*val1_prime_row2[i] + rt1[5]*val1_prime_row3[i];
                    val2_prime_row3[i] = rt1[6]*val1_prime_row1[i] + rt1[7]*val1_prime_row2[i] + rt1[8]*val1_prime_row3[i];

                    val2_prime_row1[i] = val2_prime_row1[i]+25;
                    val2_prime_row2[i] = val2_prime_row2[i]+25;
                    val2_prime_row3[i] = val2_prime_row3[i]+25;

                    if(val2_prime_row1[i] > 50)
                        val2_prime_row1[i] = 50;
                    if(val2_prime_row2[i] > 50)
                        val2_prime_row2[i] = 50;
                    if(val2_prime_row3[i] > 50)
                        val2_prime_row3[i] = 50;

                    if(val2_prime_row1[i] < 0)
                        val2_prime_row1[i] = 0;
                    if(val2_prime_row2[i] < 0)
                        val2_prime_row2[i] = 0;
                    if(val2_prime_row3[i] < 0)
                        val2_prime_row3[i] = 0;

                    //System.out.println(Double.toString(val2_prime_row1[i])+" , "+ Double.toString(val2_prime_row2[i])+", "+ Double.toString(val2_prime_row3[i]));
                }

                Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                Bitmap bmp         = Bitmap.createBitmap(50, 50,  conf);

                int countimg = 0;

                for (int i = 0; i < 50; i++) {
                    for (int j = 0; j < 50; j++) {
                        int t = (int) (interpolate(val2_prime_row1[countimg], val2_prime_row2[countimg], val2_prime_row3[countimg])*255);
                        if(sectionImagecount == 0)
                            System.out.println("Coordinate is ("+val2_prime_row1[countimg]+","+val2_prime_row2[countimg]+","+val2_prime_row3[countimg]+")");

                        int c = Color.argb(255, t, t, t);
                        bmp.setPixel(i, j, c);
                        countimg = countimg+1;
                    }
                }

                allsections.add(bmp);
                sectionImagecount = sectionImagecount+1;
            }
        }

        return allsections;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_class) {

            Classifier classifier = mClassifiers.get(0);
            long startTime = SystemClock.uptimeMillis();
            final Classification res = classifier.recognize(floatValues);
            long endTime = SystemClock.uptimeMillis();
            Log.d("TIME_TO_FEED", "Timecost to model inference: " + Long.toString(endTime - startTime));
            probtext.setText("0.86");

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