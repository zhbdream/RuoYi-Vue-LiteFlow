package com.ruoyiliteflow.aikit.platform.knowledge;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;

/** 从 txt/md/pdf/doc/docx 抽取纯文本。 */
public final class KnowledgeTextExtractor
{
    private static final int MAX_CHARS = 500_000;

    private KnowledgeTextExtractor()
    {
    }

    public static String extract(MultipartFile file) throws Exception
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("文件不能为空");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0)
        {
            ext = name.substring(dot + 1).toLowerCase();
        }
        byte[] bytes = file.getBytes();
        String text;
        switch (ext)
        {
            case "pdf":
                text = fromPdf(bytes);
                break;
            case "docx":
                text = fromDocx(bytes);
                break;
            case "txt":
            case "md":
            case "markdown":
            case "text":
            default:
                text = new String(bytes, StandardCharsets.UTF_8);
                if (StringUtils.isNotEmpty(ext) && !"txt".equals(ext) && !"md".equals(ext)
                        && !"markdown".equals(ext) && !"text".equals(ext))
                {
                    throw new ServiceException("不支持的文件类型: " + ext);
                }
                break;
        }
        if (text == null)
        {
            text = "";
        }
        text = text.replace("\u0000", " ").trim();
        if (text.length() > MAX_CHARS)
        {
            text = text.substring(0, MAX_CHARS);
        }
        if (StringUtils.isEmpty(text))
        {
            throw new ServiceException("未能从文件中提取到文本");
        }
        return text;
    }

    private static String fromPdf(byte[] bytes) throws Exception
    {
        try (PDDocument doc = PDDocument.load(bytes))
        {
            return new PDFTextStripper().getText(doc);
        }
    }

    private static String fromDocx(byte[] bytes) throws Exception
    {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes));
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc))
        {
            return extractor.getText();
        }
    }
}
