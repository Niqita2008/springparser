package com.vprave.niqitadev.parser.parse;

import com.vprave.niqitadev.parser.parse.consumers.NbkiParser;
import com.vprave.niqitadev.parser.parse.consumers.OkbParser;
import com.vprave.niqitadev.parser.table.Result;
import io.github.jonathanlink.PDFLayoutTextStripper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.INCORRECT_FORMAT;

@Service
public class Parser {
    private final Logger logger;
    private final Pattern okbBS;
    private final NbkiParser nbkiParser;
    private final OkbParser okbParser;
    private final Pattern okbEnd;

    public Parser(NbkiParser nbkiParser, OkbParser okbParser) {
        okbBS = Pattern.compile("Сформирован.+\n +\\d{1,2}.+\n", Pattern.MULTILINE);
        logger = LogManager.getLogger("main");
        this.nbkiParser = nbkiParser;
        this.okbParser = okbParser;
        okbEnd = Pattern.compile("КТО\\s+ИНТЕРЕСОВАЛСЯ");
    }

    public Result get(PDDocument doc) throws Exception, IllegalDocumentException {
        PDFLayoutTextStripper textStripper = new PDFLayoutTextStripper();
        String fullText = textStripper.getText(doc).replaceAll("\t", "    "),
                detect = fullText.chars().limit(25000).skip(5000).mapToObj(n -> (char) n).filter(Character::isLetter)
                        .limit(5000).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();

        if (detect.contains("ОбъединенноеКредитноеБюро")) { // OKB
            logger.info("OKB DETECTED!");
            textStripper.setStartPage(4);
            String text = textStripper.getText(doc);
            Matcher matcher = okbEnd.matcher(text);
            if (!matcher.find()) throw new IllegalDocumentException(INCORRECT_FORMAT, null);
            return okbParser.get(Utils.removeAll(okbBS, text.substring(0, matcher.start(0))), fullText);
        }
        doc.close();
        if (detect.contains("Приналичиивопросов")) { // NBKI
            logger.info("NBKI DETECTED!");
            return nbkiParser.get(fullText);
        }
        throw new IllegalDocumentException(INCORRECT_FORMAT, null);
    }
}