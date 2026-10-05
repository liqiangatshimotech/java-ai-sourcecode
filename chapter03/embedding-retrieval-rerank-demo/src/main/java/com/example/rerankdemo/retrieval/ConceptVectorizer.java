package com.example.rerankdemo.retrieval;

import java.util.List;

/**
 * 一个故意不完美的一阶段向量化器。
 *
 * <p>真实线上系统也经常遇到类似问题：一阶段 Embedding 模型负责快速召回，
 * 但它不一定能把所有业务同义词都压到正确方向。这里让“报销凭证”更像企业资料，
 * 是为了模拟候选召回里常见的业务干扰项。
 */
public final class ConceptVectorizer {

    private static final List<Concept> CONCEPTS = List.of(
        new Concept(List.of("报销凭证", "公司名称", "企业名称", "企业资料", "营业执照", "主体名称", "资料", "提交")),
        new Concept(List.of("发票", "开票", "票据", "抬头", "重开")),
        new Concept(List.of("写错", "填错", "不对", "错误", "更正", "重新")),
        new Concept(List.of("退款", "退费", "原路退回", "到账")),
        new Concept(List.of("物流", "快递", "收货", "地址", "出库", "改派")),
        new Concept(List.of("登录", "密码", "验证码", "账号"))
    );

    public double[] vectorize(String text) {
        double[] vector = new double[CONCEPTS.size()];
        for (int i = 0; i < CONCEPTS.size(); i++) {
            vector[i] = TextSignals.countPhraseMatches(text, CONCEPTS.get(i).phrases());
        }
        normalize(vector);
        return vector;
    }

    public double cosineSimilarity(double[] left, double[] right) {
        if (left.length != right.length) {
            throw new IllegalArgumentException("向量维度不一致");
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

    private void normalize(double[] vector) {
        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        if (norm == 0.0) {
            return;
        }

        double scale = 1.0 / Math.sqrt(norm);
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= scale;
        }
    }

    private record Concept(List<String> phrases) {
    }
}
