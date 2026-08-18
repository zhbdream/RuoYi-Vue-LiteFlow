package com.ruoyiliteflow.aicore.spi;

/** 知识库检索命中（试测 / 运行时可观测） */
public class KnowledgeHit
{
    private String kbCode;
    private String source;
    private double score;
    private String text;

    public KnowledgeHit()
    {
    }

    public KnowledgeHit(String kbCode, String source, double score, String text)
    {
        this.kbCode = kbCode;
        this.source = source;
        this.score = score;
        this.text = text;
    }

    public String getKbCode()
    {
        return kbCode;
    }

    public void setKbCode(String kbCode)
    {
        this.kbCode = kbCode;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public double getScore()
    {
        return score;
    }

    public void setScore(double score)
    {
        this.score = score;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
}
