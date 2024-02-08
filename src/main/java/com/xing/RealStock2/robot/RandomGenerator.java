package com.xing.RealStock2.robot;

import java.util.Random;

public class RandomGenerator {
    public static int generateRandomPrice(int currentPrice) {
        Random random = new Random();
        double probability = random.nextDouble(); // 生成一个0到1之间的随机数

        int newPrice;
        if (probability < 0.8) {
            // 80%的概率在当前价格的95%-105%之间
            double range = random.nextDouble() * 0.1 + 0.95; // 生成一个0.95到1.05之间的随机数
            newPrice = (int) (currentPrice * range);
        } else {
            // 20%的概率在当前价格的90%-110%之间
            double range = random.nextDouble() * 0.2 + 0.9; // 生成一个0.9到1.1之间的随机数
            newPrice = (int) (currentPrice * range);
        }

        return newPrice;
    }

    public static long generateRandomVolume(int amount) {
        Random random = new Random();
        // 生成符合正态分布的随机成交量，最大概率在1000手左右
        long volume = (long) (random.nextGaussian() * 100 + amount);
        return Math.max(0, volume); // 确保成交量为非负数
    }

    public static void main(String[] args) {
        for(int i=0;i<100;i++){
            System.out.println(generateRandomVolume(1000));
        }
    }
}
