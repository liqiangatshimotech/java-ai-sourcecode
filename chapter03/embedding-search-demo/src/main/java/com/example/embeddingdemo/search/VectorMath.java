package com.example.embeddingdemo.search;

/**
 * 向量计算工具。
 *
 * <p>Embedding 检索最常见的是余弦相似度：两个向量夹角越小，语义越接近。
 */
public final class VectorMath {

    private VectorMath() {
    }

    public static double cosineSimilarity(float[] left, float[] right) {
        if (left.length != right.length) {
            throw new IllegalArgumentException("向量维度不一致，无法计算相似度："
                + left.length + " vs " + right.length);
        }

        double dotProduct = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;

        for (int i = 0; i < left.length; i++) {
            dotProduct += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
