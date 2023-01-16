package com.vprave.niqitadev.parser.parse.consumers;

import com.vprave.niqitadev.parser.parse.IllegalDocumentException;
import com.vprave.niqitadev.parser.parse.Patterns;
import com.vprave.niqitadev.parser.parse.Utils;
import com.vprave.niqitadev.parser.table.Result;
import com.vprave.niqitadev.parser.table.Row;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.INCORRECT_FORMAT;

public final class NbkiParser implements BiConsumer<String, Row> {

    private final Logger logger;

    public NbkiParser() {
        logger = LogManager.getRootLogger();
    }

    public Result get(String fullText) throws IllegalDocumentException {
        final int[] indexes = {-1, -1}; // {start, end};
        fullText.lines().forEach(line -> {
            if (indexes[0] == -1 && Patterns.nbki.start.matcher(line).find()) indexes[0] = fullText.indexOf(line);
            else if (indexes[1] == -1 && Patterns.nbki.end.matcher(line).find()) indexes[1] = fullText.indexOf(line);
        });
        if (indexes[0] == -1 || indexes[1] == -1) {
            logger.debug("NBKI didnt get the borders [start: " + indexes[0] + ", end: " + indexes[1] + "]\nfull text:\n" + fullText);
            throw new IllegalDocumentException(INCORRECT_FORMAT, null);
        }
        String[] split = Patterns.nbki.normalisedSplit.split(Utils.removeAll(Patterns.nbki.bs, fullText.substring(indexes[0], indexes[1])), 0);
        Result result = new Result();
        IntStream.range(1, split.length).forEach(i -> {
            Row row = new Row();
            accept(split[i], row);
            result.resultArrayList.add(row);
        });

        String about = StringUtils.substringBetween(fullText, "Субъект", "Тел:");
        Matcher matcher = Patterns.date.matcher(about);
        if (!matcher.find()) return result;
        result.dateOfBirth = matcher.group(0);
        if ((matcher = Patterns.nbki.fullName.matcher(about.substring(0, matcher.start(0)))).find())
            result.fullName = matcher.group(0);
        return result;
    }

    @Override
    public void accept(String page, Row row) {
        String[] lines = (page = Utils.removeAll(Patterns.nbki.pageBs, page)).lines().filter(s -> !s.isBlank()).toArray(String[]::new);
        int companyEnd = lines[0].indexOf("Договор"), j = 0;
        for (; lines[0].charAt(j) == ' '; j++) companyEnd--;
        boolean closed = StringUtils.substringBetween(page, "Статус: ", "\n").contains("закрыт");
        Matcher matcher = Patterns.uuid.matcher(page);
        if (matcher.find()) row.uuid = matcher.group(0);
        int tempInd;

        if ((tempInd = page.indexOf("Вид: ")) != -1) {
            Matcher typeMatcher = Patterns.nbki.typePattern.matcher(page.substring(tempInd + 5));
            if (typeMatcher.find()) row.type = typeMatcher.group(0);
        }
        if ((tempInd = page.indexOf("Открыт: ")) != -1) {
            Matcher dateMatcher = Patterns.date.matcher(page.substring(tempInd + 8));
            if (dateMatcher.find()) row.opened = dateMatcher.group(0);
        }
        if ((tempInd = page.indexOf("Всего выплачено: ")) != -1) {
            Matcher paidMatcher = Patterns.moneyPattern.matcher(page.substring(tempInd + 17));
            if (paidMatcher.find()) row.paid = paidMatcher.group(0);
        }
        if ((tempInd = page.indexOf("Задолж-сть: ")) != -1) {
            Matcher debtMatcher = Patterns.moneyPattern.matcher(page.substring(tempInd + 12));
            if (debtMatcher.find()) row.debt = debtMatcher.group(0);
        }
        if (closed && (tempInd = page.indexOf("статуса: ")) != -1) {
            Matcher dateMatcher = Patterns.date.matcher(page.substring(tempInd + 15));
            if (dateMatcher.find()) row.closed = dateMatcher.group(0);
        }
        if ((tempInd = page.indexOf("ИНН: ")) != -1) {
            Matcher innMatcher = Patterns.innPattern.matcher(page.substring(tempInd + 5));
            if (innMatcher.find()) row.inn = innMatcher.group(0);
        }

        if ((tempInd = page.indexOf("Размер/лимит: ")) != -1) {
            Matcher sizeMatcher = Patterns.moneyPattern.matcher(page.substring(tempInd += 14)), currencyMatcher = Patterns.nbki.currencyPattern.matcher(page.substring(tempInd));
            if (sizeMatcher.find()) row.size = sizeMatcher.group(0);
            if (currencyMatcher.find()) row.currency = currencyMatcher.group(0);
        }

        for (j = 0; j < lines.length; j++) {
            if (row.creditCompany != null) break;
            if ((tempInd = lines[j].indexOf("Кредитор: ")) != -1) {
                StringBuilder temp = new StringBuilder().append(lines[j], tempInd + 10, companyEnd + tempInd);
                for (int g = j + 1; g < lines.length; g++) {
                    String s = Utils.removeAll(Patterns.spacePattern, lines[g]);
                    if (s.startsWith("Стр.")) continue;
                    if (s.startsWith("Счет:") || s.startsWith("УИД договора(сделки):")) break;
                    temp.append(lines[g], tempInd, companyEnd + tempInd);
                }
                row.creditCompany = Patterns.spacePattern.matcher(temp).replaceAll(" ");
            }
        }
    }
}