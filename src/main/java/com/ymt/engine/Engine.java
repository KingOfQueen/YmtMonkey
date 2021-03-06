package com.ymt.engine;

import com.ymt.entity.Constant;
import com.ymt.entity.Step;
import com.ymt.tools.LimitQueue;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

public class Engine {

    private static final Logger logger = LoggerFactory.getLogger(Engine.class);

    public LimitQueue<Step> results;

    //当前年月日时间
    public static String currentTime = new SimpleDateFormat("yyyyMMdd").format(new Date());

    //单次任务保存截图的index
    private Integer screenIndex = 1;

    //单次任务最多保存截图的上限
    private int MAX_SCREENSHOT_COUNT = 10;

    //当前任务的序号
    public static int taskId = 0;

    // 截图文件路径
    public static String SCREENSHOT_PATH = null;

    //默认滑动百分比
    private final int SWIPE_DEFAULT_PERCENT = 5;

    //默认滑动等待时间 100ms
    private final Duration SWIPE_WAIT_TIME=Duration.ofMillis(100);

    private AppiumDriver driver;

    public static int width;

    public static int height;

    public static String deviceName;

    public static boolean isAndroid=true;

    private  TouchAction action;


    public Engine(AppiumDriver driver, LimitQueue<Step> results) {

        //设置存储操作步骤 Queue 长度
        results.setLimit(MAX_SCREENSHOT_COUNT);

        this.results = results;

        this.driver = driver;

        this.width = driver.manage().window().getSize().getWidth();

        this.height = driver.manage().window().getSize().getHeight();


        this.deviceName = driver.getCapabilities().getCapability("deviceName").toString();


        this.action=new TouchAction(this.driver);


        logger.info("当前设备号 deviceName:{}", deviceName);

        taskId++;

        logger.info("当前任务taskId 为:{}", getTaskId());


        //截图加上当前时间，当前的执行taskid
        SCREENSHOT_PATH =
                String.format("%s#%s#screenshots#%s#", Constant.getResultPath().getPath(), currentTime, getTaskId())
                        .replace("#", File.separator);


        try {
            FileUtils.forceMkdir(new File(SCREENSHOT_PATH));
        } catch (IOException e) {
            //e.printStackTrace();
            logger.error("创建截图文件路径:{} 失败", SCREENSHOT_PATH);
        }

        logger.info("保存截图的位置为:{}", SCREENSHOT_PATH);

    }


    public int getTaskId() {

        return taskId;
    }

    private String generateShotName() {

        return String.format("monkey_screenShot%s.png", screenIndex);
    }


    public String takeScreenShot() {

        String screenShotName = generateShotName();

        screenShot(screenShotName);

        screenIndex++;

        if (screenIndex > MAX_SCREENSHOT_COUNT) screenIndex = 1;

        return screenShotName;
    }


    /**
     * 截图 由子类实现
     */
    public void screenShot(String fileName) {


    }

    /**
     * 获取屏幕宽度
     *
     * @return
     */
    public int getScreenWidth() {
        return this.width;
    }

    /**
     * 获取屏幕高度
     *
     * @return
     */
    public int getScreenHeight() {
        return this.height;
    }

    /**
     *
     * @param startx
     * @param starty
     * @param endx
     * @param endy
     */

    private void doSwipe(int startx, int starty, int endx, int endy){

        action.press(startx,starty).waitAction(SWIPE_WAIT_TIME).moveTo(endx,endy).release();

        action.perform();
        
    }

    /**
     * 向上滑动，
     *
     */
    public void swipeToUp(int percent) {

        doSwipe(this.width / 2, this.height * (percent - 1) / percent, this.width / 2, this.height / percent);
    }



    /**
     * 向下滑动，
     * @param percent 位置的百分比，2-10， 例如3就是 从2/3滑到1/3
     */
    public void swipeToDown(int percent) {

        doSwipe(this.width / 2, this.height / percent, this.width / 2, this.height * (percent - 1) / percent);
    }


    /**
     * 向左滑动，
     *
     * @param percent 位置的百分比，2-10， 例如3就是 从2/3滑到1/3
     */
    public void swipeToLeft(int percent) {

        doSwipe(this.width * (percent - 1) / percent, this.height / 2, this.width / percent, this.height / 2);
    }


    /**
     * 向右滑动，
     * @param percent 位置的百分比，2-10， 例如3就是 从1/3滑到2/3
     */
    public void swipeToRight(int percent) {

        doSwipe(this.width / percent, this.height / 2, this.width * (percent - 1) / percent, this.height / 2);
    }

    /**
     * 在某个方向上滑动
     *
     * @param direction 方向，UP DOWN LEFT RIGHT
     */
    public void swipe(String direction) {

        String result = "pass";

        String screenShotName = null;

        logger.info(" Event : {}", direction);

        try {
            //截图
            screenShotName = takeScreenShot();

            logger.info("点击截图:{}", screenShotName);

        } catch (Exception e) {

            logger.error("截图失败:{}", e);

            screenShotName = null;

        }

        Step step = new Step();

        try {

            switch (direction) {

                case Constant.SWIPE_UP:
                    swipeToUp(SWIPE_DEFAULT_PERCENT);
                    break;
                case Constant.SWIPE_DOWN:
                    swipeToDown(SWIPE_DEFAULT_PERCENT);
                    break;
                case Constant.SWIPE_LEFT:
                    swipeToLeft(SWIPE_DEFAULT_PERCENT);
                    break;
                case Constant.SWIPE_RIGHT:
                    swipeToRight(SWIPE_DEFAULT_PERCENT);
                    break;
            }
        } catch (Exception e) {

            logger.error("Event {} error :{}", direction, e);

            result = "fail";

            step.setResult(e.getStackTrace().toString());

        }


        step.setElementName("Page");
        step.setAction(direction);
        step.setScreenShotName(screenShotName);
        step.setResult(result);

        results.offer(step);
    }

    /**
     * 点击屏幕坐标点
     *
     * @param x
     * @param y
     */
    public void clickScreen(int x, int y) {


        String result = "pass";

        //截图
        String screenShotName = takeScreenShot();

        Step step = new Step();

        logger.info("Event 点击屏幕 x:{} ,y:{} ", x, y);

        action.tap(x, y).perform();

        step.setElementName("Page");
        step.setAction(Constant.CLICK_SCREEN);
        step.setX(x);
        step.setY(y);
        step.setScreenShotName(screenShotName);
        step.setResult(result);

        results.offer(step);

    }


    /**
     * home 键
     */
    public void homePress() {


    }

    /**
     * 尝试后退
     */
    public void back() {

    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        System.out.println(SCREENSHOT_PATH);

    }

}
