import java.util.Random;
import java.util.Arrays;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.IOException;

public class PerlinNoiseSimulation {
    private static final int PERMUTATION_SIZE = 256;
    private int[] p;
    private Random random;

    public PerlinNoiseSimulation(long seed) {
        random = new Random(seed);
        p = new int[PERMUTATION_SIZE * 2];
        int[] permutation = new int[PERMUTATION_SIZE];

        // 初始化排列数组
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            permutation[i] = i;
        }

        // 打乱排列
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            int j = random.nextInt(PERMUTATION_SIZE);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }

        // 复制一份以避免模运算
        for (int i = 0; i < PERMUTATION_SIZE * 2; i++) {
            p[i] = permutation[i % PERMUTATION_SIZE];
        }
    }

    // 平滑曲线插值
    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    // 线性插值
    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    // 梯度函数
    private double grad(int hash, double x, double y) {
        // 根据hash值选择梯度方向
        int h = hash % 4;
        switch (h) {
            case 0: return x + y;
            case 1: return -x + y;
            case 2: return x - y;
            default: return -x - y;
        }
    }

    // 柏林噪声函数
    public double perlinNoise(double x, double y) {
        // 确定网格点
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;

        // 计算插值权重
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);

        // 平滑曲线插值
        double u = fade(xf);
        double v = fade(yf);

        // 计算噪声值
        double n00 = grad(p[p[xi] + yi], xf, yf);
        double n01 = grad(p[p[xi] + (yi + 1)], xf, yf - 1);
        double n11 = grad(p[p[xi + 1] + (yi + 1)], xf - 1, yf - 1);
        double n10 = grad(p[p[xi + 1] + yi], xf - 1, yf);

        // 双线性插值
        double x1 = lerp(n00, n10, u);
        double x2 = lerp(n01, n11, u);
        return lerp(x1, x2, v);
    }

    // 模拟大坐标下的精度问题
    public void simulateBorderIssue(int size, double step, double normalOffset, double farOffset) {
        System.out.println("使用偏移量: " + farOffset);
        // 生成正常坐标范围的噪声
        double[][] normalNoise = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double x = normalOffset + i / step;
                double y = normalOffset + j / step;
                normalNoise[i][j] = perlinNoise(x, y);
            }
        }

        // 生成远坐标范围的噪声
        double[][] farNoise = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double x = farOffset + i / step;
                double y = farOffset + j / step;
                farNoise[i][j] = perlinNoise(x, y);
            }
        }

        // 保存噪声图像
        saveNoiseImage(normalNoise, "normal_noise.png");
        saveNoiseImage(farNoise, "far_noise.png");

        // 分析噪声的连续性
        double horizontalDiffNormal = calculateHorizontalDiff(normalNoise);
        double horizontalDiffFar = calculateHorizontalDiff(farNoise);
        double verticalDiffNormal = calculateVerticalDiff(normalNoise);
        double verticalDiffFar = calculateVerticalDiff(farNoise);

        // 计算噪声值范围
        double normalRange = calculateRange(normalNoise);
        double farRange = calculateRange(farNoise);

        // 计算噪声熵
        double normalEntropy = calculateEntropy(normalNoise);
        double farEntropy = calculateEntropy(farNoise);

        // 输出分析结果
        System.out.println("模拟结果:");
        System.out.printf("正常水平差异: %.6f\n", horizontalDiffNormal);
        System.out.printf("远距离水平差异: %.6f\n", horizontalDiffFar);
        System.out.printf("水平差异比率: %.2fx\n", horizontalDiffFar / horizontalDiffNormal);
        System.out.printf("正常垂直差异: %.6f\n", verticalDiffNormal);
        System.out.printf("远距离垂直差异: %.6f\n", verticalDiffFar);
        System.out.printf("垂直差异比率: %.2fx\n", verticalDiffFar / verticalDiffNormal);
        System.out.printf("正常噪声范围: %.6f\n", normalRange);
        System.out.printf("远距离噪声范围: %.6f\n", farRange);
        System.out.printf("范围比率: %.2fx\n", farRange / normalRange);
        System.out.printf("正常噪声熵: %.6f\n", normalEntropy);
        System.out.printf("远距离噪声熵: %.6f\n", farEntropy);
        System.out.printf("熵比率: %.2fx\n", farEntropy / normalEntropy);

        // 综合判断
        if ((horizontalDiffFar / horizontalDiffNormal > 1.3 || verticalDiffFar / verticalDiffNormal > 1.3) ||
            (farRange / normalRange < 0.8 || farRange / normalRange > 1.2) ||
            (farEntropy / normalEntropy < 0.8 || farEntropy / normalEntropy > 1.2)) {
            System.out.println("边境出现(懒得打字了)");
        } else {
            System.out.println("边境没有出现");
        }
    }

    // 计算水平方向差异
    private double calculateHorizontalDiff(double[][] noise) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < noise.length; i++) {
            for (int j = 0; j < noise[i].length - 1; j++) {
                sum += Math.abs(noise[i][j + 1] - noise[i][j]);
                count++;
            }
        }
        return sum / count;
    }

    // 计算垂直方向差异
    private double calculateVerticalDiff(double[][] noise) {
        double sum = 0;
        int count = 0;
        for (int j = 0; j < noise[0].length; j++) {
            for (int i = 0; i < noise.length - 1; i++) {
                sum += Math.abs(noise[i + 1][j] - noise[i][j]);
                count++;
            }
        }
        return sum / count;
    }

    // 计算噪声值范围
    private double calculateRange(double[][] noise) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double[] row : noise) {
            for (double value : row) {
                if (value < min) min = value;
                if (value > max) max = value;
            }
        }
        return max - min;
    }

    // 计算噪声熵
    private double calculateEntropy(double[][] noise) {
        int bins = 20;
        double min = -1.0;
        double max = 1.0;
        double binWidth = (max - min) / bins;
        int[] histogram = new int[bins];

        // 构建直方图
        for (double[] row : noise) {
            for (double value : row) {
                // 确保值在范围内
                value = Math.max(min, Math.min(max, value));
                int bin = (int) ((value - min) / binWidth);
                if (bin == bins) bin--; // 处理边界情况
                histogram[bin]++;
            }
        }

        // 计算熵
        double entropy = 0;
        int total = noise.length * noise[0].length;
        for (int count : histogram) {
            if (count > 0) {
                double probability = (double) count / total;
                entropy -= probability * Math.log(probability);
            }
        }
        return entropy;
    }

    // 保存噪声图像
    private void saveNoiseImage(double[][] noise, String filename) {
        int width = noise.length;
        int height = noise[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // 将噪声值映射到0-255范围
                double value = (noise[i][j] + 1) / 2; // 映射到0-1
                int rgb = (int) (value * 255);
                rgb = Math.max(0, Math.min(255, rgb)); // 确保在范围内
                int color = new Color(rgb, rgb, rgb).getRGB();
                image.setRGB(i, j, color);
            }
        }

        try {
            ImageIO.write(image, "png", new File(filename));
            System.out.println("图像已保存: " + filename);
        } catch (IOException e) {
            System.out.println("保存图像失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("开始模拟柏林噪声在大坐标下的精度问题...");
        PerlinNoiseSimulation simulation = new PerlinNoiseSimulation(0);
        // 尝试更大的偏移量
        simulation.simulateBorderIssue(100, 10, 0, 100000000);
    }
}