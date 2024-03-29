package fk.bug.newproj.pojo;

import android.graphics.Path;

public class MyGesture {
    public Path path;
    public int duration;
    public MyGesture(int pointX, int pointY){
        this(pointX,pointY,10);
    }
    public MyGesture(int pointX, int pointY, int duration){
        this(pointX,pointY,pointX,pointY,duration);
    }
    public MyGesture(int fromX, int fromY, int toX, int toY, int duration){
        this.path = new Path();
        this.path.moveTo(fromX,fromY);
        this.path.lineTo(toX,toY);
        this.duration = duration;
    }
    public MyGesture(int[] arr){
        this.path = new Path();
        this.path.moveTo(arr[0],arr[1]);
        this.path.lineTo(arr[2],arr[3]);
        this.duration = arr[4];
    }
}
