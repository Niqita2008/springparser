package com.vprave.niqitadev.parser.parse.consumers;

import com.vprave.niqitadev.parser.parse.IllegalDocumentException;
import com.vprave.niqitadev.parser.parse.Patterns;
import com.vprave.niqitadev.parser.parse.Utils;
import com.vprave.niqitadev.parser.table.Result;
import com.vprave.niqitadev.parser.table.Row;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.INCORRECT_FORMAT;

@Service
public final class NbkiParser implements BiConsumer<String, Row> {
    private final Patterns patterns;
    private final Logger logger;

    public NbkiParser(Patterns patterns) {
        logger = LogManager.getLogger("main");
        this.patterns = patterns;
    }

    public Result get(String fullText) throws IllegalDocumentException {
        final int[] indexes = {-1, -1}; // {start, end};
        fullText.lines().forEach(line -> {
            if (indexes[0] == -1 && patterns.nbki.start.matcher(line).find()) indexes[0] = fullText.indexOf(line);
            else if (indexes[1] == -1 && patterns.nbki.end.matcher(line).find()) indexes[1] = fullText.indexOf(line);
        });
        if (indexes[0] == -1 || indexes[1] == -1) {
            logger.error("NBKI didnt get the borders [start: " + indexes[0] + ", end: " + indexes[1] + "]\nfull text:\n" + fullText);
            throw new IllegalDocumentException(INCORRECT_FORMAT, null);
        }
        String[] split = patterns.nbki.normalisedSplit.split(Utils.removeAll(patterns.nbki.bs, fullText.substring(indexes[0], indexes[1])), 0);
        Result result = new Result();
        IntStream.range(1, split.length).forEach(i -> {
            Row row = new Row();
            accept(split[i], row);
            result.resultArrayList.add(row);
        });

        String about = StringUtils.substringBetween(fullText, "Субъект", "Тел:");
        Matcher matcher = patterns.date.matcher(about);
        if (!matcher.find()) return result;
        result.dateOfBirth = matcher.group(0);
        if ((matcher = patterns.nbki.fullName.matcher(about.substring(0, matcher.start(0)))).find())
            result.fullName = matcher.group(0);
        return result;
    }

    @Override
    public void accept(String page, Row row) {
        String[] lines = (page = Utils.removeAll(patterns.nbki.pageBs, page)).lines().filter(s -> !s.isBlank()).toArray(String[]::new);
        boolean closed = StringUtils.substringBetween(page, "Статус: ", "\n").contains("закрыт");
        Matcher matcher = patterns.uuid.matcher(page);
        if (matcher.find()) row.uuid = matcher.group(0);

        int tempIndex = page.indexOf("Вид: ");
        if (tempIndex != -1) {
            Matcher typeMatcher = patterns.nbki.typePattern.matcher(page.substring(tempIndex + 5));
            if (typeMatcher.find()) row.type = typeMatcher.group(0);
        }

        tempIndex = page.indexOf("Открыт: ");
        if (tempIndex != -1) {
            Matcher dateMatcher = patterns.date.matcher(page.substring(tempIndex + 8));
            if (dateMatcher.find()) row.opened = dateMatcher.group(0);
        }

        tempIndex = page.indexOf("Всего выплачено: ");
        if (tempIndex != -1) {
            Matcher paidMatcher = patterns.straitMoneyPattern.matcher(page.substring(tempIndex + 17));
            if (paidMatcher.find()) row.paid = paidMatcher.group(0);
        }

        tempIndex = page.indexOf("Задолж-сть: ");
        if (tempIndex != -1) {
            Matcher debtMatcher = patterns.straitMoneyPattern.matcher(page.substring(tempIndex + 12));
            if (debtMatcher.find()) row.debt = debtMatcher.group(0);
        }

        if (closed) {
            tempIndex = page.indexOf("статуса: ");
            if (tempIndex != -1) {
                Matcher dateMatcher = patterns.date.matcher(page.substring(tempIndex + 15));
                if (dateMatcher.find()) row.closed = dateMatcher.group(0);
            }
        }

        tempIndex = page.indexOf("ИНН: ");
        if (tempIndex != -1) {
            Matcher innMatcher = patterns.innPattern.matcher(page.substring(tempIndex + 5));
            if (innMatcher.find()) row.inn = innMatcher.group(0);
        }

        tempIndex = page.indexOf("Размер/лимит: ");
        if (tempIndex != -1) {
            Matcher sizeMatcher = patterns.straitMoneyPattern.matcher(page.substring(tempIndex += 14)), currencyMatcher = patterns.nbki.currencyPattern.matcher(page.substring(tempIndex));
            if (sizeMatcher.find()) row.size = sizeMatcher.group(0);
            if (currencyMatcher.find()) row.currency = currencyMatcher.group(0);
        }

        int companyEnd = lines[0].indexOf("Договор"), j = 0;
        for (; lines[0].charAt(j) == ' '; j++) companyEnd--;
        for (j = 0; j < lines.length; j++) {
            if (row.creditCompany != null) break;
            tempIndex = lines[j].indexOf("Кредитор: ");
            if (tempIndex != -1) {
                StringBuilder temp = new StringBuilder().append(lines[j], tempIndex + 10, companyEnd + tempIndex);
                for (int g = j + 1; g < lines.length; g++) {
                    String s = Utils.removeAll(patterns.spacePattern, lines[g]);
                    if (s.startsWith("Стр.")) continue;
                    if (s.startsWith("Счет:") || s.startsWith("УИД договора(сделки):")) break;
                    temp.append(lines[g], tempIndex, companyEnd + tempIndex);
                }
                row.creditCompany = patterns.spacePattern.matcher(temp).replaceAll(" ");
            }
        }
    }
}