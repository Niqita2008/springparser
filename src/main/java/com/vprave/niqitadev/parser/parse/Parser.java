package com.vprave.niqitadev.parser.parse;

import com.vprave.niqitadev.parser.parse.consumers.NbkiConsumer;
import com.vprave.niqitadev.parser.parse.consumers.OkbConsumer;
import com.vprave.niqitadev.parser.table.Result;
import com.vprave.niqitadev.parser.table.Row;
import io.github.jonathanlink.PDFLayoutTextStripper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.*;
import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.INCORRECT_FORMAT;

public class Parser {

    private final NbkiConsumer nbkiConsumer = new NbkiConsumer();
    private final OkbConsumer okbConsumer = new OkbConsumer();
    private final Logger logger = LogManager.getRootLogger();

    private final Pattern
            okbBS = Pattern.compile("Сформирован.+\n +\\d{1,2}.+\n", Pattern.MULTILINE),
            nbkiStart = Pattern.compile("Обязательства +и +их +исполнение|Счета", Pattern.MULTILINE),
            nbkiEnd = Pattern.compile("Информацио|Случаи +привлечения +к +ответственности", Pattern.MULTILINE),
            nbkiBS = Pattern.compile("^ +Кредитный +отчет *(для +субъекта *)?\n +ID +запроса +Пользователь +Предоставлен *\n.+\n|Примечание. +Значение +\"Н/Д\" +указывает +на +то, +что +данные +в +НБКИ +отсутствуют.( *\n)+ *При +наличии +вопросов +обращайтесь +в +НБКИ: +[,\\d -]+", Pattern.MULTILINE),
            nbkiNormalisedSplit = Pattern.compile("(?=Счет +Договор +Состояние +Баланс)", Pattern.MULTILINE),
            closed = Pattern.compile("ЗАКРЫТЫЕ +КРЕДИТНЫЕ *\n* +ДОГОВОРЫ", Pattern.MULTILINE),
            fullName = Pattern.compile("[А-я\\-]{2,} +[А-я\\-]{2,} +[А-я\\-]{2,}", Pattern.MULTILINE),
            closedIsEmpty = Pattern.compile("ИНФОРМАЦИЯ +ОТСУТСТВУЕТ", 0),
            okbSplit = Pattern.compile("(?=\\d\\..+ +Договор +займа +\\(кредита\\).+)", 0);

    private Result get(PDDocument doc) throws Exception, IllegalDocumentException {
        PDFLayoutTextStripper textStripper = new PDFLayoutTextStripper();
        String fullText = textStripper.getText(doc).replaceAll("\t", "    "),
                detect = fullText.chars().limit(20000).mapToObj(n -> (char) n).filter(Character::isLetter)
                        .limit(1800).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();

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
            String usefulText = Utils.removeAll(okbBS, textStripper.getText(doc));
            Matcher m = closed.matcher(usefulText);
            if (!m.find()) throw new IllegalDocumentException(INVALID, null);
            Result result = new Result();

            String name = StringUtils.substringBetween(fullText, "ИСТОРИИ", "Дата");
            Matcher matcher = fullName.matcher(name.substring(name.indexOf(':') + 1));
            if (matcher.find()) result.fullName = matcher.group(0);
            if ((matcher = Utils.date.matcher(fullText.substring(0, fullText.indexOf("Паспорт")))).find())
                result.dateOfBirth = matcher.group(0);


            String[] activeSplit = okbSplit.split(usefulText.substring(0, m.start())), closedSplit = okbSplit.split(usefulText.substring(m.end()));
            String str = activeSplit[0].substring(activeSplit[0].substring(0, activeSplit[0].indexOf('№')).lastIndexOf('\n') + 1);
            if (!closedIsEmpty.matcher(closedSplit[0]).find()) {
                String temp;
                str += (temp = closedSplit[0].substring(closedSplit[0].indexOf('№'))).substring(temp.substring(0, str.indexOf('1')).lastIndexOf('\n'));
            }

            Stream.concat(Arrays.stream(activeSplit).skip(1), Arrays.stream(closedSplit).skip(1)).forEach(okbConsumer.reload(result, str));
            doc.close();
            return result;
        }
        doc.close();
        if (detect.contains("Приналичиивопросов")) { // NBKI
            logger.info("NBKI DETECTED!");
            final int[] indexes = {-1, -1}; // {start, end};
            fullText.lines().forEach(line -> {
                if (indexes[0] == -1 && nbkiStart.matcher(line).find()) indexes[0] = fullText.indexOf(line);
                else if (indexes[1] == -1 && nbkiEnd.matcher(line).find()) indexes[1] = fullText.indexOf(line);
            });
            if (indexes[0] == -1 || indexes[1] == -1) {
                IllegalDocumentException e = new IllegalDocumentException(INCORRECT_FORMAT, null);
                logger.info("NBKI gets fucked up [start: " + indexes[0] + ", end: " + indexes[1] + "]\nfull text:\n" + fullText, e);
                throw e;
            }
            String[] split = nbkiNormalisedSplit.split(Utils.removeAll(nbkiBS, fullText.substring(indexes[0], indexes[1])), 0);
            Result result = new Result();
            for (int i = 1; i < split.length; i++) {
                Row row = new Row();
                nbkiConsumer.accept(split[i], row);
                result.resultArrayList.add(row);
            }

            String about = StringUtils.substringBetween(fullText, "Субъект", "Тел:");
            Matcher matcher = Utils.date.matcher(about);
            if (!matcher.find()) return result;
            result.dateOfBirth = matcher.group(0);
            if ((matcher = fullName.matcher(about.substring(0, matcher.start(0)))).find())
                result.fullName = matcher.group(0);
            return result;
        }
        throw new IllegalDocumentException(INCORRECT_FORMAT, null);
    }
}