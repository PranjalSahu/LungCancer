/*      This example of the use of both TriCubicInterpolation and TriCubicSpline
        demonstrates interpolation within a calculated data set
            y = x1*x2 + 2.x3.x2^2 - 3.x1.x2.x3
        allowing a comparison of the use of gradients calculated analytically
        and by numerical differentiation

        Michael Thomas Flanagan
        Created:    13 January 2011
*/

package mariannelinhares.mnistandroid;

import flanagan.interpolation.*;
import flanagan.io.*;

public class TriCubicExampleOne{

    public static void main(String arg[]){

        // Array of x1
        double[] x1 = {0.0,	1.0, 2.0, 3.0, 4.0, 5.0};
        // Array of x2
        double[] x2 = {1.0, 5.0, 9.0, 13.0, 17.0, 21.0, 25.0, 29.0, 33.0, 37.0};
        // Array of x3
        double[] x3 = {6.0,	7.0, 8.0, 9.0, 10.0, 11.0, 12.0};

        double[][][] y = new double[6][10][7];          // Array of y
        double[][][] dy1 = new double[6][10][7];        // Array of gradients, dy/dx1
        double[][][] dy2 = new double[6][10][7];        // Array of gradients, dy/dx2
        double[][][] dy3 = new double[6][10][7];        // Array of gradients, dy/dx3
        double[][][] dy12 = new double[6][10][7];       // Array of gradients, d2y/dx1dx2
        double[][][] dy13 = new double[6][10][7];       // Array of gradients, d2y/dx1dx3
        double[][][] dy23 = new double[6][10][7];       // Array of gradients, d2y/dx2dx3
        double[][][] dy123 = new double[6][10][7];      // Array of gradients, d3y/dx1dx2dx3
        double xx1 = 0.0;                               // x1 value at interpolation point
        double xx2 = 0.0;                               // x2 value at interpolation point
        double xx3 = 0.0;                               // x3 value at interpolation point

        double y1 = 0.0;                            // interpolated y value using BiCubicInterpolation with user supplied
        //   analytically calculated gradients
        double y2 = 0.0;                            // interpolated y value using BiCubicInterpolation with gradients obtained
        //   as numerical differences using a bicubic spline interpolation
        double y3 = 0.0;                            // interpolated y value using BiCubicInterpolation with gradients obtained
        //   as numerical differences using only the supplied data points
        double y4 = 0.0;                            // interpolated y value using BiCubicSpline
        double yt = 0.0;                            // true y value

        // Calculate the data
        for(int i=0; i<6; i++){
            for(int j=0; j<10; j++){
                for(int k=0; k<7; k++){
                    y[i][j][k] = x1[i]*x2[j] + 2.0*x2[j]*x2[j]*x3[k] - 3.0*x1[i]*x2[j]*x3[k];
                    dy1[i][j][k] = x2[j] - 3.0*x2[i]*x3[k];
                    dy2[i][j][k] = x1[i] + 4.0*x2[j]*x3[k] - 3.0*x1[i]*x3[i];
                    dy3[i][j][k] = 2.0*x2[j]*x2[j] - 3.0*x1[i]*x2[i];
                    dy12[i][j][k] = 1.0 - 3.0*x3[k];
                    dy13[i][j][k] = - 3.0*x2[j];
                    dy23[i][j][k] = 4.0*x2[j] - 3.0*x1[i];
                    dy123[i][j][k] = -3.0;
                }
            }
        }

        // Create an instance of TriCubicInterpolation with supplied analytically determined derivatives
        TriCubicInterpolation tci1 = new TriCubicInterpolation(x1, x2, x3, y, dy1, dy2, dy3, dy12, dy13, dy23, dy123);

        // Create an instance of TriCubicInterpolation with calculated derivatives using interpolation
        TriCubicInterpolation tci2 = new TriCubicInterpolation(x1, x2, x3, y, 1);

        // Create an instance of TriCubicInterpolation with calculated derivatives using only the supplied data points
        TriCubicInterpolation tci3 = new TriCubicInterpolation(x1, x2, x3, y, 0);

        // Create an instance of TriCubicSpline
        TriCubicSpline tcs = new TriCubicSpline(x1, x2, x3, y);

        // First interpolation at a x1 = 2.5, x2 = 13.3
        xx1 = 1.5;
        xx2 = 5.3;
        xx3 = 7.5;
        yt = xx1*xx2 + 2.0*xx2*xx2*xx3 - 3.0*xx1*xx2*xx3;

        System.out.println("First interpolation at x1 = " + xx1 + " x2 = " + xx2 + " and x3 = " + xx3);
        System.out.println("The true y value is         " + yt);

        y1 = tci1.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicInterpolation with supplied analytical gradients:");
        System.out.println(" The interpolated y value is " + y1);

        y2 = tci2.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicInterpolation with numerical differencing and interpolation:");
        System.out.println(" The interpolated y value is " + y2);

        y3 = tci3.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicInterpolation with numerical differencing of the supplied data points:");
        System.out.println(" The interpolated y value is " + y3);

        y4 = tcs.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicSpline interpolation:");
        System.out.println(" The interpolated y value is " + y4);
        System.out.println(" ");

        // Second interpolation at a x1 = 3.3, x2 = 8.7
        xx1 = 3.3;
        xx2 = 8.7;
        yt = xx1*xx2 + 2.0*xx2*xx2*xx3 - 3.0*xx1*xx2*xx3;

        System.out.println("Second interpolation at x1 = " + xx1 + " x2 = " + xx2 + " and x3 = " + xx3);
        System.out.println("The true y value is         " + yt);

        y1 = tci1.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicInterpolation with supplied analytical gradients:");
        System.out.println(" The interpolated y value is " + y1);

        y2 = tci2.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicInterpolation with numerical differencing and interpolation:");
        System.out.println(" The interpolated y value is " + y2);

        y3 = tci3.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicInterpolation with numerical differencing of the supplied data points:");
        System.out.println(" The interpolated y value is " + y3);

        y4 = tcs.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicSpline interpolation:");
        System.out.println(" The interpolated y value is " + y4);
        System.out.println(" ");

        // Third interpolation at a x1 = 4.2, x2 = 36.1
        xx1 = 4.9;
        xx2 = 36.9;
        yt = xx1*xx2 + 2.0*xx2*xx2*xx3 - 3.0*xx1*xx2*xx3;

        System.out.println("Third interpolation at x1 = " + xx1 + " x2 = " + xx2 + " and x3 = " + xx3);
        System.out.println("The true y value is         " + yt);

        y1 = tci1.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicInterpolation with supplied analytical gradients:");
        System.out.println(" The interpolated y value is " + y1);

        y2 = tci2.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicInterpolation with numerical differencing and interpolation:");
        System.out.println(" The interpolated y value is " + y2);

        y3 = tci3.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicInterpolation with numerical differencing of the supplied data points:");
        System.out.println(" The interpolated y value is " + y3);

        y4 = tcs.interpolate(xx1, xx2, xx3);
        System.out.println(" TriCubicSpline interpolation:");
        System.out.println(" The interpolated y value is " + y4);
        System.out.println(" ");

    }
}
