package com.hiprogrammer.util;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: VerificationUtil
 * @Description: 验证码图片生成工具类
 * @Version: 1.0
 * @author: XPF
 * @date: 2018/10/29 16:08
 */
public class VerificationUtil {

    /**
     * 生成正方性拼图边长（像素）
     */
    public static final int SQUARE_SIDE_LENGTH = 50;
    /**
     * 拼图距离图像的边距（像素）
     */
    public static final int BOARD = 5;
    /**
     * 原始图片长度（像素）
     */
    private static final int TARGET_LENGTH = 300;
    /**
     * 原始图片宽度（像素）
     */
    private static final int TARGET_WIDTH = 150;
    /**
     * 拼图块圆形区域半径（像素）
     */
    private static final int CIRCLE_R = 10;

    /**
     * 高斯图片模糊处理并加工
     *
     * @param radius
     * @param horizontal
     * @return
     */
    private static ConvolveOp getGaussianBlurFilter( int radius, boolean horizontal ) {
        if (radius < 1) {
            throw new IllegalArgumentException("Radius must be >= 1");
        }

        int size = radius * 2 + 1;
        float[] data = new float[size];

        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;

        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += data[index];
        }

        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }

        Kernel kernel;
        if (horizontal) {
            kernel = new Kernel(size, 1, data);
        } else {
            kernel = new Kernel(1, size, data);
        }
        return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
    }

    /**
     * 图像模糊处理
     *
     * @param source
     * @param dest
     */
    private static void simpleBlur( BufferedImage source, BufferedImage dest ) {
        BufferedImageOp op = VerificationUtil.getGaussianBlurFilter(1, false);
        op.filter(source, dest);
    }

    /**
     * 图像处理测试方法
     *
     * @param args
     */
    public static void main( String[] args ) {
        URL backgroundUrl = VerificationUtil.class.getClassLoader().getResource("images/verification/background1.png");
        URL pathUrl = VerificationUtil.class.getClassLoader().getResource("");
        try {
            BufferedImage originalImage = ImageIO.read(new File(backgroundUrl.toURI()));
            BufferedImage targetImage = new BufferedImage(
                    (VerificationUtil.SQUARE_SIDE_LENGTH + VerificationUtil.CIRCLE_R * 2),
                    (VerificationUtil.TARGET_WIDTH),
                    BufferedImage.TYPE_4BYTE_ABGR);
            BufferedImage originalImageBlur = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            BufferedImage targetImageBlur = new BufferedImage(
                    (VerificationUtil.SQUARE_SIDE_LENGTH + VerificationUtil.CIRCLE_R * 2),
                    (VerificationUtil.TARGET_WIDTH),
                    BufferedImage.TYPE_4BYTE_ABGR);
            //正方形中心可以浮动范围
            int xRange = VerificationUtil.TARGET_LENGTH - (VerificationUtil.BOARD + VerificationUtil.CIRCLE_R) * 2 - VerificationUtil.SQUARE_SIDE_LENGTH;
            int yRange = VerificationUtil.TARGET_WIDTH - VerificationUtil.BOARD * 2 - VerificationUtil.SQUARE_SIDE_LENGTH;
            //正方形中心位置
            int pointX = VerificationUtil.BOARD + VerificationUtil.SQUARE_SIDE_LENGTH / 2 + new Double(xRange * Math.random()).intValue();
            int pointY = VerificationUtil.BOARD + VerificationUtil.SQUARE_SIDE_LENGTH / 2 + new Double(yRange * Math.random()).intValue();
            int[][] templateImage = VerificationUtil.getGraphValue(pointX, pointY);
            VerificationUtil.addOriginalShadow(originalImage, targetImage, templateImage, pointX, pointY);
            VerificationUtil.simpleBlur(originalImage, originalImageBlur);
            VerificationUtil.simpleBlur(targetImage, targetImageBlur);
            //拼图使用png格式，保证透明度
            File temp = File.createTempFile("temp", ".png");
            ImageIO.write(targetImageBlur, "png", temp);
            FileInputStream input = new FileInputStream(temp);
            OutputStream os = new FileOutputStream(new File(pathUrl.getPath() + "images/verification/targetImage.png"));
            byte[] b = new byte[1024];
            while (input.read(b) != -1) {
                os.write(b);
            }
            os.flush();
            os.close();
            //背景改用jpg格式，缩小图片体积
            temp = File.createTempFile("temp", ".jpg");
            ImageIO.write(originalImageBlur, "jpg", temp);
            input = new FileInputStream(temp);
            OutputStream os2 = new FileOutputStream(new File(pathUrl.getPath() + "images/verification/originalImage.png"));
            byte[] b2 = new byte[1024];
            while (input.read(b2) != -1) {
                os2.write(b2);
            }
            os2.flush();
            os2.close();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据拼图的横纵坐标，计算需要挖取的拼图区域
     *
     * @param x 拼图中间横坐标
     * @param y 拼图中心纵坐标
     * @return 需要挖去的图形信息
     */
    private static int[][] getGraphValue( int x, int y ) {
        int[][] data = new int[VerificationUtil.TARGET_LENGTH][VerificationUtil.TARGET_WIDTH];
        //正方形四个边中心点
        int left_x = x - VerificationUtil.SQUARE_SIDE_LENGTH / 2;
        int left_y = y;
        int right_x = x + VerificationUtil.SQUARE_SIDE_LENGTH / 2;
        int right_y = y;
        int top_x = x;
        int top_y = y + VerificationUtil.SQUARE_SIDE_LENGTH / 2;
        int bottom_x = x;
        int bottom_y = y - VerificationUtil.SQUARE_SIDE_LENGTH / 2;
        int i = 0;
        double circleArea = Math.pow(VerificationUtil.CIRCLE_R, 2);
        while (i < VerificationUtil.TARGET_LENGTH) {
            for (int j = 0; j < VerificationUtil.TARGET_WIDTH; j++) {
                if ((Math.pow(i - top_x, 2) + Math.pow(j - top_y, 2) < circleArea)
                        || (Math.pow(i - bottom_x, 2) + Math.pow(j - bottom_y, 2) < circleArea)) {
                    //正方形上下的圆形不挖去
                    data[i][j] = 0;
                } else if (i > left_x && i < right_x && j > bottom_y && j < top_y) {
                    //正方形需要挖去
                    data[i][j] = 1;
                } else if ((Math.pow(i - left_x, 2) + Math.pow(j - left_y, 2) < circleArea)
                        || (Math.pow(i - right_x, 2) + Math.pow(j - right_y, 2)) < circleArea) {
                    //正方形左右的半圆挖去
                    data[i][j] = 1;
                } else {
                    //剩下的不挖去
                    data[i][j] = 0;
                }
            }
            i++;
        }
        return data;
    }

    /**
     * 在原始图片上添加阴影层，创建新的拼图块
     *
     * @param originalImage 原始图片
     * @param targetImage   目标图片
     * @param templateImage 临时图片
     * @param pointX        坐标x
     * @param pointY        坐标 y
     */
    private static void addOriginalShadow( BufferedImage originalImage, BufferedImage targetImage, int[][] templateImage, int pointX, int pointY ) {
        int x = pointX - (VerificationUtil.SQUARE_SIDE_LENGTH / 2 + VerificationUtil.CIRCLE_R);
        int y = pointY - (VerificationUtil.SQUARE_SIDE_LENGTH / 2);
        int i = 0;
        while (i < VerificationUtil.TARGET_LENGTH) {
            int j = 0;
            while (j < VerificationUtil.TARGET_WIDTH) {
                int valRGB = templateImage[i][j];
                try {
                    if (valRGB == 1) {
                        // 原图中对应位置变色处理
                        int originalPictureRGB = originalImage.getRGB(i, j);
                        //抠图上复制对应颜色值
                        targetImage.setRGB(i - x, j, originalPictureRGB);
                        int r = (33 & originalPictureRGB >> 0);
                        int g = (33 & (originalPictureRGB >> 8));
                        int b = (33 & (originalPictureRGB >> 16));
                        originalPictureRGB = (r << 0) + (g << 8) + (b << 16) + (200 << 25);
                        //原图对应位置颜色变化
                        originalImage.setRGB(i, j, originalPictureRGB);
                    }
                } catch (Exception e) {
                    System.out.println(i + " " + j);
                }
                j++;
            }
            i++;
        }
    }

    /**
     * 创建验证码图片
     *
     * @return Map:包含背景和拼图块，拼图挖取的横纵坐标位移
     */
    public static Map<String, String> createVerificationImage() {
        Map<String, String> map = new HashMap<>();
        int pictureIndex = (int) (Math.random() * 6) + 1;
        URL background = VerificationUtil.class.getClassLoader().getResource("images/verification/background" + pictureIndex + ".png");
        try {
            BufferedImage originalImage = ImageIO.read(new File(background.toURI()));
            BufferedImage targetImage = new BufferedImage(
                    (VerificationUtil.SQUARE_SIDE_LENGTH + VerificationUtil.CIRCLE_R * 2),
                    (VerificationUtil.TARGET_WIDTH),
                    BufferedImage.TYPE_4BYTE_ABGR);
            BufferedImage originalImageBlur = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            BufferedImage targetImageBlur = new BufferedImage(
                    (VerificationUtil.SQUARE_SIDE_LENGTH + VerificationUtil.CIRCLE_R * 2),
                    (VerificationUtil.TARGET_WIDTH),
                    BufferedImage.TYPE_4BYTE_ABGR);
            //正方形中心可以浮动范围
            int xRange = VerificationUtil.TARGET_LENGTH - (VerificationUtil.BOARD + VerificationUtil.CIRCLE_R) * 2 - VerificationUtil.SQUARE_SIDE_LENGTH;
            int yRange = VerificationUtil.TARGET_WIDTH - VerificationUtil.BOARD * 2 - VerificationUtil.SQUARE_SIDE_LENGTH;
            //正方形中心位置
            int pointX = VerificationUtil.BOARD + VerificationUtil.CIRCLE_R + VerificationUtil.SQUARE_SIDE_LENGTH / 2 + new Double(xRange * Math.random()).intValue();
            int pointY = VerificationUtil.BOARD + VerificationUtil.SQUARE_SIDE_LENGTH / 2 + new Double(yRange * Math.random()).intValue();
            int[][] templateImage = VerificationUtil.getGraphValue(pointX, pointY);
            VerificationUtil.addOriginalShadow(originalImage, targetImage, templateImage, pointX, pointY);
            VerificationUtil.simpleBlur(originalImage, originalImageBlur);
            VerificationUtil.simpleBlur(targetImage, targetImageBlur);
            //背景改用jpg格式，缩小图片体积
            map.put("original", "data:image/jpg;base64," + VerificationUtil.imageToBase64(originalImageBlur, "jpg"));
            //拼图使用jpg格式，保留透明度
            map.put("target", "data:image/png;base64," + VerificationUtil.imageToBase64(targetImageBlur, "png"));
            //将拼图左下角位置记录返回，以便与前端比对
            pointX -= (VerificationUtil.CIRCLE_R + VerificationUtil.SQUARE_SIDE_LENGTH / 2);
            pointY -= (VerificationUtil.SQUARE_SIDE_LENGTH / 2);
            map.put("pointX", String.valueOf(pointX));
            map.put("pointY", String.valueOf(pointY));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 图像转Base64
     *
     * @param image
     * @return
     * @throws Exception
     */
    private static String imageToBase64( BufferedImage image, String imageType ) throws Exception {
        byte[] imagedata = null;
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        ImageIO.write(image, imageType == null ? "png" : imageType, bao);
        imagedata = bao.toByteArray();
        BASE64Encoder encoder = new BASE64Encoder();
        String BASE64IMAGE = encoder.encodeBuffer(imagedata).trim();
        BASE64IMAGE = BASE64IMAGE.replaceAll("\n", "").replaceAll("\r", "");//删除 \r\n
        return BASE64IMAGE;
    }

    /**
     * Base64转图片
     *
     * @param base64String
     * @return
     */
    private BufferedImage base64StringToImage( String base64String ) {
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] bytes1 = decoder.decodeBuffer(base64String);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes1);
            return ImageIO.read(bais);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
