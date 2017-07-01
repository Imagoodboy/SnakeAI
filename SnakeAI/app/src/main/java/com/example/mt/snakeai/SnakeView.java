package com.example.mt.snakeai;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;
import static com.example.mt.snakeai.MainActivity.mHeight;
import static com.example.mt.snakeai.MainActivity.mWidth;

/**
 * Created by mt on 2017/6/26.
 */

public class SnakeView extends View {
    //---------------------------------------

    private final int UP    =  0;
    private final int DOWN  =  1;
    private final int LEFT  =  2;
    private final int RIGHT =  3;

    private final char BACKGROUND = ' '; //map->背景
    private final char BODY = 'o'; //map->蛇身
    private final char FOOD = '@';//map->食物

    private final int INF = 0x3f3f3f3f;//无穷大
    private final int MAXX = 1500; // 地图最大边长
    private final int BOXWIDTH = 40; //食物蛇体方块边长
    private final int STROKEWIDTH = 10;//空心矩形宽度

    private final int[][] dir = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};//方向数组
    private int[] mFood = new int[2];//食物坐标
    private int[] tempFood = new int[2];//虚拟食物坐标
    private Snake mSnake= new Snake();//实例化蛇属性类
    private Snake tempSnake = new Snake();//实例化虚拟蛇属性
    private boolean mIsFoodDone = true; //食物标记
    private int calcStep = -1;//用来记录最大可行走步数

    private Random mRandom = new Random(); //产生随机数

    private Paint mFoodPaint = new Paint(); //初始化食物画笔
    private Paint mSnakePaint = new Paint(); //初始化蛇身画笔
    private Paint mLinePaint = new Paint();//蛇身描边
    private Paint mHeadPaint = new Paint();//蛇头
    private Paint mTailPaint = new Paint();//蛇尾

    private int wCount; //地图宽度
    private int hCount; //地图高度

    public static int mScore = 0;//设置分数
    private char[][] map = new char[MAXX][MAXX];//地图数组
    private char[][] tempMap = new char[MAXX][MAXX];//实例化虚拟地图
    private int[][] dis = new int[MAXX][MAXX];//离食物最短距离数组
    private int[][] tempDis = new int[MAXX][MAXX];//虚拟dis数组
    private boolean[][] visRoad = new boolean[MAXX][MAXX];//用于标记寻最深路

    Canvas canvass = new Canvas();

    private class Snake {
        private int length;//蛇长
        private int dir; //蛇方向
        private int[][] node = new int[1000][2]; //蛇身坐标
    };
    private class Node{
        private int x;
        private int y;
    };
    private class RoadNode{
        private int x;
        private int y;
        private int step;
    };
    //---------------------------------------

    public SnakeView(Context context) {
        super(context);
        InitPaint();
        InitSnake();
    }

    public SnakeView(Context context, AttributeSet attrs){
        super(context,attrs);
        InitPaint();
        InitSnake();
    }

    public SnakeView(Context context,AttributeSet attrs,int defStyle){
        super(context,attrs,defStyle);
        InitPaint();
        InitSnake();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.canvass = canvas;
        InitMap();
        //DrawSnake(canvas,mSnakePaint);
        DrawFood(canvas,mFoodPaint);
        DoSearch();
    }
    private void ReStart(){
        mScore = 0;
        MainActivity.isPaused = false;
        MainActivity.mRefreshThread.start();
        InitSnake();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        /*mWidth = w;
        mHeight = h;
        wCount = mWidth / BOXWIDTH; // 宽
        hCount = mHeight / BOXWIDTH; //高
        Log.e("SIZE","WIDTH = " + wCount + "HEIGHT = " + hCount);
        //InitFood();*/
    }
    //画蛇体
    private void Make_Move(Canvas canvas, Paint paint,int moveDir) {
        int dx,dy;
        //蛇头移动
        dx = mSnake.node[1][0] + dir[moveDir][0];
        dy = mSnake.node[1][1] + dir[moveDir][1];
        if(isFoodEaten(dx,dy)){ //判断是否碰到食物
            mSnake.length++;
            mIsFoodDone = true;
        }
        else{
            map[mSnake.node[mSnake.length][0]][mSnake.node[mSnake.length][1]] = BACKGROUND;
        }
        for(int i=mSnake.length;i>1;i--){ //蛇身移动
            mSnake.node[i][0]=mSnake.node[i-1][0];
            mSnake.node[i][1]=mSnake.node[i-1][1];
        }
        mSnake.node[1][0]=dx; //确定新蛇头位置
        mSnake.node[1][1]=dy;
        map[dy][dx] = BODY;
        for(int i = 1 ; i <= mSnake.length ; i++ ) {
            Point point = new Point();
            point.x = mSnake.node[i][1];
            point.y = mSnake.node[i][0];
            Rect snake = new Rect(point.x * BOXWIDTH , point.y * BOXWIDTH, point.x * BOXWIDTH + BOXWIDTH , point.y * BOXWIDTH + BOXWIDTH);
            if(i==1){
                canvas.drawRect(snake, mHeadPaint);
            }
            else if(i==mSnake.length){
                canvas.drawRect(snake, mTailPaint);
            }
            else {
                canvas.drawRect(snake, paint);
            }
            canvas.drawRect(snake, mLinePaint);
        }
    }
    //画食物
    private void DrawFood(Canvas canvas, Paint paint) {
        //Log.d("SIZEE--","size" + mWidth + " " + mHeight);
        //Log.d("SIZEE--","size" + wCount + " " + hCount + mIsFoodDone);
        while(mIsFoodDone) {
            mFood[0] = mRandom.nextInt(hCount); //获取0到wCount随机数
            mFood[1] = mRandom.nextInt(wCount);
            if(map[mFood[0]][mFood[1]]==BACKGROUND) {
                mIsFoodDone = false;
                mScore++;
                break;
            }

        }
        map[mFood[0]][mFood[1]] = FOOD;
        //Log.d("FOOD--","size"+ mFood[0] + " "  + mFood[1]);
        //OutMap();
        //-----
        Rect food = new Rect(mFood[1] * BOXWIDTH , mFood[0] *BOXWIDTH , mFood[1] * BOXWIDTH + BOXWIDTH , mFood[0] * BOXWIDTH + BOXWIDTH);
        canvas.drawRect(food, paint);
        canvas.drawRect(food, mLinePaint);
    }
    //初始化画笔
    private void InitPaint(){
        mFoodPaint.setColor(Color.RED);
        mFoodPaint.setStyle(Paint.Style.FILL);
        mSnakePaint.setColor(Color.WHITE);
        mSnakePaint.setStyle(Paint.Style.FILL);
        //--DEBUG
        mLinePaint.setColor(Color.BLACK);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(STROKEWIDTH);
        mHeadPaint.setColor(Color.BLUE);
        mHeadPaint.setStyle(Paint.Style.FILL);
        mTailPaint.setColor(Color.YELLOW);
        mTailPaint.setStyle(Paint.Style.FILL);
    }
    //初始化蛇属性
    private void InitSnake(){
        mSnake.node[1][0] = 10;mSnake.node[1][1] = 10;
        mSnake.node[2][0] = 10;mSnake.node[2][1] = 9;
        mSnake.node[3][0] = 10;mSnake.node[3][1] = 8;
        mSnake.length  = 3;
        mSnake.dir = 3;
    }

    //判断食物是否可以被吃
    private boolean isFoodEaten(int dx,int dy){
        if(!mIsFoodDone) {
            if(dx == mFood[0] && dy == mFood[1]) return true;
        }
        return false;
    }

    //初始化地图
    private void InitMap(){
        wCount = mWidth / BOXWIDTH;
        hCount = mHeight / BOXWIDTH;
        //Log.d("SIZEE--",wCount+" " + hCount);
        for(int i = 0;i <= hCount;i++){
            for(int j = 0;j <= wCount;j++){
                map[i][j] = BACKGROUND;
            }
        }
        for(int i = 1;i <= mSnake.length;i++){
            map[mSnake.node[i][0]][mSnake.node[i][1]] = BODY;
            //Log.d("MAP--", "WIDTH = " + mSnake.node[i][0] + "HEIGHT = " + mSnake.node[i][1]);
        }
        if (!mIsFoodDone) {
            map[mFood[0]][mFood[1]] = FOOD;
        }
        //OutMap();
    }
    //输出地图 -->DEBUG
    private void OutMap(){
        //---debug
        for(int i = 0;i <= hCount;i++){
            String s="";
            for(int j = 0;j <= wCount;j++){
                s+=map[i][j];
            }
            Log.d("MAP--", s);
        }
    }

    private void dialog_Choose(Context context) { //弹出对话框，是否重新开始游戏
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("游戏结束,分数:"+ mScore + "重新开始游戏?");
        builder.setTitle("提示");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton("确认",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ReStart();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton("取消",
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    private void InitDis(int[][] dis2){ //初始化dis数组
        //Log.d("SIZEE--",wCount+" " + hCount);
        for(int i = 0;i <= hCount;i++){
                for(int j = 0;j <= wCount;j++){
                    dis2[i][j] = INF;
                }
        }
    }
    private void Bfs_Dis(char[][] map2,int[] food,int[][] dis2){ //bfs遍历求到食物最短路
        dis2[food[0]][food[1]] = 0;
        //Log.d("FOOdD--",mFood[0]+" " + mFood[1]);
        Queue<Node> queue = new LinkedList<Node>();
        Node myFood = new Node();
        //Node diss = new Node();
        myFood.x = food[0]; //行坐标
        myFood.y = food[1]; //列坐标
        //Log.d("FOOdD--",food[0]+" " + food[1]);
        queue.add(myFood);
        while(!queue.isEmpty()){
            Node t = queue.poll();
            for(int i = 0;i < 4;i++){
                Node diss = new Node();
                diss.x = t.x + dir[i][0];
                diss.y = t.y + dir[i][1];
                //Log.d("FOOdD--",dir[i][0] + " " + dir[i][1]);
                //Log.d("FOOdD--","i=" + i + " " + t.x+" " + t.y + " "+diss.x + " " + diss.y);
                //Log.d("FOOdD--",dis[diss.x][diss.y]+" " + dis[t.x][t.y] + map[diss.x][diss.y]);
                if(diss.x<0 || diss.y<0 || diss.x>=hCount || diss.y>=wCount ){ //脱离地图
                    continue;
                }
                if(dis2[diss.x][diss.y] == INF && map2[diss.x][diss.y]==BACKGROUND){
                    dis2[diss.x][diss.y] = dis2[t.x][t.y] + 1;
                    //Log.d("www--",dis[diss.x][diss.y]+" " + dis[t.x][t.y]);
                    queue.add(diss);
                }
            }
        }
        //---debug
        /*
        Log.d("DIS--",hCount + " " + wCount);
        for(int i = 0;i <= hCount;i++){
            String s = "";
            for(int j = 0;j <= wCount;j++){
                s+=String.valueOf(dis2[i][j])+" ";
            }
            Log.d("DIS--",s);
        }*/
    }
    private void DoSearch(){
        int dirFlag;
        InitDis(dis); //初始化dis数组
        Bfs_Dis(map,mFood,dis); //bfs遍历求到食物最短路
        if(IfCanEat(mSnake.node,dis)){//蛇到食物有路径
            if(mSnake.length == hCount*wCount-1){
                dirFlag = Choose_shortest_Safe_Move(mSnake.node,dis);
            }
            else{
            dirFlag = Find_Safe_Way();}
            //Log.e("JUDGE---","have way");
        }
        else{
            dirFlag = Follow_Tail();
        }
        if(dirFlag==-1){
            dirFlag = Any_Possible_move();
        }
        if(dirFlag != -1) {
            Make_Move(canvass, mSnakePaint, dirFlag);
        }
        else{
            MainActivity.isPaused = true;
            dialog_Choose(getContext());
        }

    }
    private int Choose_shortest_Safe_Move(int[][] snake_node,int[][] tempDiss){ //从蛇头选周围4领域最短路
        int min = INF;
        int best_move = -1;
        for(int i =0;i < 4;i++){
            int dx = snake_node[1][0] + dir[i][0];
            int dy = snake_node[1][1] + dir[i][1];
            if(dx<0 || dy<0 || dx>=hCount || dy>=wCount ){ //脱离地图
                continue;
            }
            if(tempDiss[dx][dy] < min){
                min = tempDiss[dx][dy];
                best_move = i;
            }
        }
        return best_move;
    }

    private int Choose_longest_Safe_Move(int[][] snake_node,int[][] tempDiss){ //从蛇头选周围4领域最远路
        int max = -INF;
        int best_move = -1;
        for(int i =0;i < 4;i++){
            int dx = snake_node[1][0] + dir[i][0];
            int dy = snake_node[1][1] + dir[i][1];
            if(dx<0 || dy<0 || dx>=hCount || dy>=wCount){ //脱离地图
                continue;
            }
            if(tempDiss[dx][dy] > max && tempDiss[dx][dy] != INF){//&& tempDiss[dx][dy] != INF
                max = tempDiss[dx][dy];
                best_move = i;
            }
        }
        //Log.e("JUDGE---"," " + best_move + "fx" + max);
        return best_move;
    }

    private void Virtual_Shortest_Move() { //虚拟蛇模拟吃食物-->当蛇与食物之间有路径
        Copy_map();
        Copy_Snake();
        Copy_dis();
        boolean eatFood = false;
        while(!eatFood){
            int snakeDir = Choose_shortest_Safe_Move(tempSnake.node,tempDis);
            int dx = tempSnake.node[1][0] + dir[snakeDir][0];
            int dy = tempSnake.node[1][1] + dir[snakeDir][1];
            if(isFoodEaten(dx,dy)){ //判断是否碰到食物
                tempSnake.length++;
                eatFood = true;
            }
            else{
                tempMap[tempSnake.node[tempSnake.length][0]][tempSnake.node[tempSnake.length][1]] = BACKGROUND;
            }
            for(int i=tempSnake.length;i>1;i--){ //虚拟蛇身移动
                tempSnake.node[i][0]=tempSnake.node[i-1][0];
                tempSnake.node[i][1]=tempSnake.node[i-1][1];
            }
            tempSnake.node[1][0]=dx; //确定新蛇头位置
            tempSnake.node[1][1]=dy;
            tempMap[dx][dy] = BODY;
        }
    }
    private void Copy_map(){ //拷贝地图
        for(int i = 0;i <= hCount;i++){
            for(int j = 0;j <= wCount;j++){
                tempMap[i][j] = map[i][j];
            }
        }
    }
    private void Copy_Snake(){ //拷贝蛇属性
        for(int i = 1;i <= mSnake.length;i++){
            tempSnake.node[i][0] = mSnake.node[i][0];
            tempSnake.node[i][1] = mSnake.node[i][1];
            tempSnake.length = mSnake.length;
            tempSnake.dir = mSnake.dir;
        }
    }
    private void Copy_dis(){//拷贝dis属性
        for(int i = 0;i <= hCount;i++){
            for(int j = 0;j <= wCount;j++){
                tempDis[i][j] = dis[i][j];
            }
        }
    }
    private int Find_Safe_Way(){ //蛇到食物有路径，进行虚拟蛇模拟
        if(IfCanEat(mSnake.node,dis)){//蛇到食物有路径
            Virtual_Shortest_Move(); //派虚拟蛇模拟
            if(Is_Tail_Inside()){//蛇尾存在通路，则真实蛇选最短路运行一步
                return Choose_shortest_Safe_Move(mSnake.node,dis);
            }
            //Log.e("JUDGE---","if can follow Tail");
            return Follow_Tail();//如果跟蛇尾没有通路
        }
        return -1;
    }
    private boolean IfCanEat(int[][] snake_node,int[][] dis2){ //判断蛇到食物是否有路径
        for(int i = 0;i < 4;i++){
            int dx = snake_node[1][0] + dir[i][0];
            int dy = snake_node[1][1] + dir[i][1];
            if(dx<0 || dy<0 || dx>=hCount || dy>=wCount ){ //脱离地图
                continue;
            }
            if(dis2[dx][dy] != INF){
                return true;
            }
        }
        return false;
    }
    private boolean Is_Tail_Inside(){ //虚拟蛇吃完食物是否可以追尾运动
        tempMap[tempSnake.node[tempSnake.length][0]][tempSnake.node[tempSnake.length][1]] = FOOD;//将蛇尾变成食物
        tempFood[0] = tempSnake.node[tempSnake.length][0];//将蛇尾变成食物
        tempFood[1] = tempSnake.node[tempSnake.length][1];
        InitDis(tempDis);//初始化
        Bfs_Dis(tempMap,tempFood,tempDis);//bfs最短路，求其他地方到蛇尾的最短距离
        if(IfCanEat(tempSnake.node,tempDis)){
            return true;
        }
        return false;
    }
    private void Is_Move_Possible(int dir,int movedir){ //判断是否可以向指定方向移动

    }
    private int Follow_Tail(){ //蛇头朝蛇尾运行一步
        Copy_map();
        Copy_Snake();
        Copy_dis();
        tempMap[tempSnake.node[tempSnake.length][0]][tempSnake.node[tempSnake.length][1]] = FOOD;//将蛇尾变成食物
        tempFood[0] = tempSnake.node[tempSnake.length][0];//将蛇尾变成食物
        tempFood[1] = tempSnake.node[tempSnake.length][1];
        InitDis(tempDis);
        Bfs_Dis(tempMap,tempFood,tempDis);
        tempMap[tempSnake.node[tempSnake.length][0]][tempSnake.node[tempSnake.length][1]] = BODY;//还原蛇尾

        return Choose_longest_Safe_Move(tempSnake.node,tempDis);
    }
    private int Any_Possible_move(){ //任意走一步,走能走的最深的路
        Log.e("JUDGE---","use");
        boolean flag = false;
        int dirr = -1;int roadNum = 0;
        int maxStep = -1; int maxDir = -1;
        int[] roadDir = new int[4];
        for(int i=0;i<4;i++){ //先判断是否存在可行走的路
            int dx = mSnake.node[1][0] + dir[i][0];
            int dy = mSnake.node[1][1] + dir[i][1];
            if (dx < 0 || dy < 0 || dx >= hCount || dy >= wCount || map[dx][dy] == BODY)
            {
                continue;
            }
            else{
                flag = true;
                roadDir[roadNum++] = i;
            }
        }
        if(flag)
        {
            for(int i = 0;i < roadNum;i++){ //对每个可行走方向遍历，寻找深度最深的路
                calcStep = 0;
                Bfs_Road(mSnake.node[1][0]+dir[roadDir[i]][0],mSnake.node[1][1]+dir[roadDir[i]][1],1);
                if(calcStep > maxStep){
                    maxStep = calcStep;
                    maxDir = roadDir[i];
                }
            }
        }
        dirr = maxDir;
        return dirr;
    }

    private void Bfs_Road(int x,int y,int step){ //当有多个方向可以任意走时，进行bfs，寻找深度最深的那条路进行走
        InitVis();
        Queue<RoadNode> queue = new LinkedList<RoadNode>();
        RoadNode myRoad = new RoadNode();
        myRoad.x = x;
        myRoad.y = y;
        myRoad.step = step;
        queue.add(myRoad);
        visRoad[myRoad.x][myRoad.y] = true;
        if(myRoad.step > calcStep){
            calcStep = myRoad.step;
        }
        while(!queue.isEmpty()){
            RoadNode t = queue.poll();
            for(int i = 0;i < 4;i++){
                RoadNode diss = new RoadNode();
                diss.x = t.x + dir[i][0];
                diss.y = t.y + dir[i][1];
                diss.step = t.step + 1;
                if (diss.x < 0 || diss.y < 0 || diss.x >= hCount || diss.y >= wCount || map[diss.x][diss.y] != BACKGROUND || visRoad[diss.x][diss.y])
                {
                    continue;
                }
                if(diss.step > calcStep){
                    calcStep = diss.step;
                }
                queue.add(diss);
                visRoad[diss.x][diss.y] = true;
            }
        }
    }
    private void InitVis(){ //初始化vis数组
        for(int i = 0;i <= hCount;i++){
            for(int j = 0;j <= wCount;j++){
                visRoad[i][j] = false;
            }
        }
    }
}
