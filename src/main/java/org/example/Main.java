package org.example;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {

        double kay = 3.55;
        int jay = 30;
        int[] arr = new int[6];
        for (int i =0 ;i<arr.length;i++)
        {
            arr[i]=1+i;
        }
        int[] arr2 ={2,4,6};
        for(int i = 0;i<arr2.length;i++)
        {
            System.out.println("the "+i+1+"th of Array = "+arr2[i]);
        }
        for(int i:arr)
        {
            if (i % 2 != 0)
            {
                System.out.println(i);

            }
        }
        ArrayList a = new ArrayList();
        a.add("Ahmed");
        a.add(3);
        a.add(true);
        a.add(3.5);
        a.remove(2);
       for(int i =0;i<a.size();i++)
       {
           System.out.println(a.get(i));
       }
        System.out.println(a.contains("Ahmed"));


    }

}