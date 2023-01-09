package com.vprave.niqitadev.parser.parse.consumers;

import com.vprave.niqitadev.parser.parse.Utils;
import com.vprave.niqitadev.parser.table.Row;
import org.apache.commons.lang3.StringUtils;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NbkiConsumer implements BiConsumer<String, Row> {

    private final Pattern
            innPattern = Pattern.compile("\\d{10}", 0),
            spacePattern = Pattern.compile(" {2,}", 0),
            moneyPattern = Pattern.compile("\\d+", 0),
            currencyPattern = Pattern.compile("[A-Z]{3}", 0),
            typePattern = Pattern.compile("([А-я.]+ {1,2})+", 0),
            bs = Pattern.compile("(?s)\n *Залоги.*?Данные +о +кредиторе *\n", Pattern.MULTILINE);

    @Override
    public void accept(String page, Row row) {
        String[] lines = (page = Utils.removeAll(bs, page)).lines().filter(s -> !s.isBlank()).toArray(String[]::new);
        int companyEnd = lines[0].indexOf("Договор"), j = 0;
        for (; lines[0].charAt(j) == ' '; j++) companyEnd--;
        boolean closed = StringUtils.substringBetween(page, "Статус: ", "\n").contains("закрыт");
        Matcher matcher = Utils.uuid.matcher(page);
        if (matcher.find()) row.setUuid(matcher.group(0));
        int tempInd;

        if ((tempInd = page.indexOf("Вид: ")) != -1) {
            Matcher typeMatcher = typePattern.matcher(page.substring(tempInd + 5));
            if (typeMatcher.find()) row.setType(typeMatcher.group(0));
        }
        if ((tempInd = page.indexOf("Открыт: ")) != -1) {
            Matcher dateMatcher = Utils.date.matcher(page.substring(tempInd + 8));
            if (dateMatcher.find()) row.setOpened(dateMatcher.group(0));
        }
        if ((tempInd = page.indexOf("Всего выплачено: ")) != -1) {
            Matcher paidMatcher = moneyPattern.matcher(page.substring(tempInd + 17));
            if (paidMatcher.find()) row.setPaid(paidMatcher.group(0));
        }
        if ((tempInd = page.indexOf("Задолж-сть: ")) != -1) {
            Matcher debtMatcher = moneyPattern.matcher(page.substring(tempInd + 12));
            if (debtMatcher.find()) row.setDebt(debtMatcher.group(0));
        }
        if (closed && (tempInd = page.indexOf("статуса: ")) != -1) {
            Matcher dateMatcher = Utils.date.matcher(page.substring(tempInd + 15));
            if (dateMatcher.find()) row.setClosed(dateMatcher.group(0));
        }
        if ((tempInd = page.indexOf("ИНН: ")) != -1) {
            Matcher innMatcher = innPattern.matcher(page.substring(tempInd + 5));
            if (innMatcher.find()) row.setInn(innMatcher.group(0));
        }

        if ((tempInd = page.indexOf("Размер/лимит: ")) != -1) {
            Matcher sizeMatcher = moneyPattern.matcher(page.substring(tempInd += 14)),
                    currencyMatcher = currencyPattern.matcher(page.substring(tempInd));
            if (sizeMatcher.find()) row.setSize(sizeMatcher.group(0));
            if (currencyMatcher.find()) row.setCurrency(currencyMatcher.group(0));
        }

        for (j = 0; j < lines.length; j++) {
            if (!row.isCreditCompanySet()) break;
            if ((tempInd = lines[j].indexOf("Кредитор: ")) != -1) {
                StringBuilder temp = new StringBuilder().append(lines[j], tempInd + 10, companyEnd + tempInd);
                for (int g = j + 1; g < lines.length; g++) {
                    String s = Utils.removeAll(spacePattern, lines[g]);
                    if (s.startsWith("Стр.")) continue;
                    if (s.startsWith("Счет:") || s.startsWith("УИД договора(сделки):")) break;
                    temp.append(lines[g], tempInd, companyEnd + tempInd);
                }
                row.setCreditCompany(spacePattern.matcher(temp).replaceAll(" "));
            }
        }
    }
}