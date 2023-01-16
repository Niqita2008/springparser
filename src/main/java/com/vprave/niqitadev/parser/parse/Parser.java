package com.vprave.niqitadev.parser.parse;

import com.vprave.niqitadev.parser.parse.consumers.NbkiParser;
import com.vprave.niqitadev.parser.parse.consumers.OkbParser;
import com.vprave.niqitadev.parser.table.Result;
import io.github.jonathanlink.PDFLayoutTextStripper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.util.regex.Pattern;

import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.INCORRECT_FORMAT;
import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.NO_OUTLINE;

public class Parser {

    private final Logger logger;
    private final Pattern okbBS;

    public Parser() {
        okbBS = Pattern.compile("Сформирован.+\n +\\d{1,2}.+\n", Pattern.MULTILINE);
        logger = LogManager.getRootLogger();
    }

    public Result get(PDDocument doc) throws Exception, IllegalDocumentException {
        PDFLayoutTextStripper textStripper = new PDFLayoutTextStripper();
        String fullText = textStripper.getText(doc).replaceAll("\t", "    "),
                detect = fullText.chars().limit(20000).mapToObj(n -> (char) n).filter(Character::isLetter)
                        .limit(1200).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();

        if (detect.contains("ДЕЙСТВУЮЩИЕКРЕДИТНЫЕДОГОВОРЫ")) { // OKB
            logger.info("OKB DETECTED!");
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
            if (outline == null) throw new IllegalDocumentException(NO_OUTLINE, null);
            for (PDOutlineItem item = outline.getFirstChild(); item != null; item = item.getNextSibling()) {
                String title = item.getTitle();
                if (title.equals("ДЕЙСТВУЮЩИЕ КРЕДИТНЫЕ ДОГОВОРЫ")) {
                    textStripper.setStartBookmark(item);
                    continue;
                }
                if (!title.contains("КТО ИНТЕРЕСОВАЛСЯ")) continue;
                textStripper.setEndBookmark(item);
                break;
            }
            return new OkbParser().get(Utils.removeAll(okbBS, textStripper.getText(doc)), fullText);
        }
        doc.close();
        if (detect.contains("Приналичиивопросов")) { // NBKI
            logger.info("NBKI DETECTED!");
            return new NbkiParser().get(fullText);
        }
        throw new IllegalDocumentException(INCORRECT_FORMAT, null);
    }
}