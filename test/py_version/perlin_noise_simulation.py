import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

# 实现简单的柏林噪声
def perlin_noise(x, y, seed=0):
    np.random.seed(seed)
    
    # 确定网格点
    xi = int(x)
    yi = int(y)
    
    # 计算插值权重
    xf = x - xi
    yf = y - yi
    
    # 平滑曲线插值
    u = fade(xf)
    v = fade(yf)
    
    # 生成随机梯度向量
    p = np.arange(256, dtype=int)
    np.random.shuffle(p)
    p = np.append(p, p)
    
    # 计算噪声值
    n00 = grad(p[p[xi % 256] + (yi % 256)], xf, yf)
    n01 = grad(p[p[xi % 256] + ((yi + 1) % 256)], xf, yf - 1)
    n11 = grad(p[p[(xi + 1) % 256] + ((yi + 1) % 256)], xf - 1, yf - 1)
    n10 = grad(p[p[(xi + 1) % 256] + (yi % 256)], xf - 1, yf)
    
    # 双线性插值
    x1 = lerp(n00, n10, u)
    x2 = lerp(n01, n11, u)
    return lerp(x1, x2, v)

# 平滑函数
def fade(t):
    return t * t * t * (t * (t * 6 - 15) + 10)

# 线性插值
def lerp(a, b, t):
    return a + t * (b - a)

# 梯度函数
def grad(h, x, y):
    vectors = np.array([[0, 1], [0, -1], [1, 0], [-1, 0]])
    g = vectors[h % 4]
    return g[0] * x + g[1] * y

# 模拟大坐标下的精度问题
def simulate_border_issue(size=100, step=10, normal_offset=0, far_offset=10000000):
    # 生成正常坐标范围的噪声
    normal_noise = np.zeros((size, size))
    for i in range(size):
        for j in range(size):
            x = normal_offset + i/step
            y = normal_offset + j/step
            normal_noise[i][j] = perlin_noise(x, y)
    
    # 生成远坐标范围的噪声（可能会有精度问题）
    far_noise = np.zeros((size, size))
    for i in range(size):
        for j in range(size):
            # 添加大偏移量模拟远距离
            x = far_offset + i/step
            y = far_offset + j/step
            far_noise[i][j] = perlin_noise(x, y)
    
    # 可视化
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 6))
    
    # 正常噪声
    im1 = ax1.imshow(normal_noise, cmap='viridis')
    ax1.set_title('正常坐标下的柏林噪声')
    fig.colorbar(im1, ax=ax1)
    
    # 远距离噪声
    im2 = ax2.imshow(far_noise, cmap='viridis')
    ax2.set_title('远距离坐标下的柏林噪声')
    fig.colorbar(im2, ax=ax2)
    
    plt.tight_layout()
    plt.savefig('c:\\Users\\dsascx\\Desktop\\perlin_noise_comparison.png')# 这里缺失字体包
    plt.close()
    
    # 分析噪声的连续性
    # 计算水平方向的差异
    horizontal_diff_normal = np.mean(np.abs(normal_noise[:, 1:] - normal_noise[:, :-1]))
    horizontal_diff_far = np.mean(np.abs(far_noise[:, 1:] - far_noise[:, :-1]))
    
    # 计算垂直方向的差异
    vertical_diff_normal = np.mean(np.abs(normal_noise[1:] - normal_noise[:-1]))
    vertical_diff_far = np.mean(np.abs(far_noise[1:] - far_noise[:-1]))
    
    # 计算噪声值范围
    normal_range = np.max(normal_noise) - np.min(normal_noise)
    far_range = np.max(far_noise) - np.min(far_noise)
    
    # 计算噪声熵(简单版本)
    normal_hist, _ = np.histogram(normal_noise, bins=20, range=(-1, 1), density=True)
    normal_entropy = -np.sum(normal_hist * np.log(normal_hist + 1e-10))
    
    far_hist, _ = np.histogram(far_noise, bins=20, range=(-1, 1), density=True)
    far_entropy = -np.sum(far_hist * np.log(far_hist + 1e-10))
    
    return {
        "horizontal_diff_normal": horizontal_diff_normal,
        "horizontal_diff_far": horizontal_diff_far,
        "vertical_diff_normal": vertical_diff_normal,
        "vertical_diff_far": vertical_diff_far,
        "horizontal_diff_ratio": horizontal_diff_far / horizontal_diff_normal,
        "vertical_diff_ratio": vertical_diff_far / vertical_diff_normal,
        "normal_range": normal_range,
        "far_range": far_range,
        "range_ratio": far_range / normal_range,
        "normal_entropy": normal_entropy,
        "far_entropy": far_entropy,
        "entropy_ratio": far_entropy / normal_entropy
    }

# 运行模拟
print("开始模拟柏林噪声在大坐标下的精度问题...")
results = simulate_border_issue()

print("\n模拟结果:")
print(f"正常水平差异: {results['horizontal_diff_normal']:.6f}")
print(f"远距离水平差异: {results['horizontal_diff_far']:.6f}")
print(f"水平差异比率: {results['horizontal_diff_ratio']:.2f}x")
print(f"正常垂直差异: {results['vertical_diff_normal']:.6f}")
print(f"远距离垂直差异: {results['vertical_diff_far']:.6f}")
print(f"垂直差异比率: {results['vertical_diff_ratio']:.2f}x")
print(f"正常噪声范围: {results['normal_range']:.6f}")
print(f"远距离噪声范围: {results['far_range']:.6f}")
print(f"范围比率: {results['range_ratio']:.2f}x")
print(f"正常噪声熵: {results['normal_entropy']:.6f}")
print(f"远距离噪声熵: {results['far_entropy']:.6f}")
print(f"熵比率: {results['entropy_ratio']:.2f}x")

# 综合判断
if (results['horizontal_diff_ratio'] > 1.3 or results['vertical_diff_ratio'] > 1.3) or \
   (results['range_ratio'] < 0.8 or results['range_ratio'] > 1.2) or \
   (results['entropy_ratio'] < 0.8 or results['entropy_ratio'] > 1.2):
    print("\n边境地形正常出现 是噪声的问题")
else:
    print("\n错误边境没有正常出现\n可能是数值不够大或就是照抄")