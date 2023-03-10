/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.util.Arrays;

/**
 *
 * @author thomas
 */
public class TestMultiArray {

    public static void main(String... args) {
        // int test[][] = new int[10][];
        // test[0] = new int[3];
        //  test[1] = new int[3];  

// int l = 0;
// int arr[][][] = new int[3][2][4];
// System.out.println(arr.length);
// for (int i=0; i<arr.length; i++){
//     System.out.println(arr[i].length);
//     for (int j=0; j< arr[i].length; j++){
//         System.out.println(arr[i][j].length);
//         for (int k = 0; k < arr[i][j].length; k++){
//             arr[i][j][k] = ++l;
//             System.out.println(arr[i][j][k]);
//         }
//     }
// }
        int[] array = new int[]{1, 5, 6, 0, 8, 1, 6, 2, 3};
        Arrays.sort(array);
        System.out.println(Arrays.toString(array));
        System.out.println(int[].class.isAssignableFrom(int[].class));
        System.out.println(array instanceof int[]);

//    int arre[][] = new int[4][5];
//    arre[0] = new int[3];
//    arre[1] = new int[2];
//    int count = arre.length;
//    int[] subArray = arre[1];
//    subArray[1] = 1;
//    System.out.println(arre[1][1]);
//    System.out.println("Contents of 2D Jagged Array");
//      for (int i=0; i<arre.length; i++)
//      {
//         for (int j=0; j<arre[i].length; j++)
//             System.out.print(arre[i][j] + " ");
//     }
//   TestObjectSecond[][][] listObject = new TestObjectSecond[2][2][4];
//    System.out.println(listObject.length);
//    for (int i=0; i< listObject.length; i++){
//     System.out.println(listObject[i].length);
//     for (int j=0; j< listObject[i].length; j++){
//         System.out.println(listObject[i][j].length);
//         for (int k = 0; k < listObject[i][j].length; k++){
//             listObject[i][j][k] = new TestObjectSecond();
//             System.out.println(listObject[i][j][k].testFn());
//         }
//     }
        //}
    }
}
