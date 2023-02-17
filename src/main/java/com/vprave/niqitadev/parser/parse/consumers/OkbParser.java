package com.vprave.niqitadev.parser.parse.consumers;

import com.vprave.niqitadev.parser.parse.IllegalDocumentException;
import com.vprave.niqitadev.parser.parse.Patterns;
import com.vprave.niqitadev.parser.parse.Utils;
import com.vprave.niqitadev.parser.table.Result;
import com.vprave.niqitadev.parser.table.Row;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.INVALID;

@Service
public class OkbParser {
    private int i;
    private final int[] sumI = new int[1], startHeader = new int[2];
    private String[] split;

    private final Patterns patterns;

    public OkbParser(Patterns patterns) {
        this.patterns = patterns;
    }

    public Result get(String useful, String fullText) throws IllegalDocumentException {
        Matcher m = patterns.okb.closed.matcher(useful);
        if (!m.find()) throw new IllegalDocumentException(INVALID, null);
        Result result = new Result();

        String name = StringUtils.substringBetween(fullText, "ИСТОРИИ", "Дата");
        Matcher matcher = patterns.okb.fullName.matcher(name.substring(name.indexOf(':') + 1));
        if (matcher.find()) result.fullName = matcher.group(0);
        if ((matcher = patterns.date.matcher(fullText.substring(0, fullText.indexOf("Паспорт")))).find())
            result.dateOfBirth = matcher.group(0);


        String[] activeSplit = patterns.okb.mainSplit.split(useful.substring(0, m.start())), closedSplit = patterns.okb.mainSplit.split(useful.substring(m.end()));
        String header = activeSplit[0].substring(activeSplit[0].substring(0, activeSplit[0].indexOf('№')).lastIndexOf('\n') + 1);
        if (!patterns.okb.closedIsEmpty.matcher(closedSplit[0]).find()) {
            String temp;
            header += (temp = closedSplit[0].substring(closedSplit[0].indexOf('№'))).substring(temp.substring(0, header.indexOf('1')).lastIndexOf('\n'));
        }

        i = 0;
        startHeader[0] = header.indexOf("Источник");
        startHeader[1] = header.indexOf("Размер");
        int indexOf = header.indexOf("Вид");
        if (indexOf != -1 && (startHeader[0] == -1 || indexOf < startHeader[0])) startHeader[0] = indexOf;
        sumI[0] = header.indexOf("Задолженность");
        assert startHeader[0] != -1 && startHeader[1] != -1 && sumI[0] != -1;
        split = patterns.okb.headerSplit.split(header);

        Stream.concat(Arrays.stream(activeSplit).skip(1), Arrays.stream(closedSplit).skip(1)).forEach(s -> result.resultArrayList.add(accept(s)));
        return result;
    }

    public Row accept(String page) {
        Matcher m = patterns.okb.end.matcher(page);
        if (!m.find()) return null;
        if (!(m = patterns.okb.type.matcher(page = Utils.removeAll(patterns.okb.bs, page.substring(0, m.end(0))))).find())
            return null;
        String[] lines = page.lines().filter(s -> !s.isBlank()).dropWhile(a -> !patterns.okb.start.matcher(a).find()).toArray(String[]::new);
        Row row = new Row();

        Matcher matcher;
        String string;
        if (!(matcher = patterns.moneyPattern.matcher(string = split[++i].substring(sumI[0]))).find()) return row;
        row.currency = string.substring(matcher.end(0), string.indexOf(' ', matcher.end(0) + 2));
        row.debt = string = StringUtils.deleteWhitespace(matcher.group(0)).replace(',', '.');
        BigDecimal debt = BigDecimal.valueOf(Double.parseDouble(string));
        if (!(matcher = patterns.moneyPattern.matcher(split[i].substring(startHeader[1]))).find()) return row;
        row.size = string = StringUtils.deleteWhitespace(matcher.group(0)).replace(',', '.');
        double doubleDebt = BigDecimal.valueOf(Double.parseDouble(string)).subtract(debt).doubleValue();
        row.paid = doubleDebt == (long) doubleDebt ? String.format("%d", (long) doubleDebt) : String.format("%s", doubleDebt);
        if (!(matcher = patterns.okb.creditor.matcher(string = split[i].lines().filter(f -> !f.isBlank())
                .map(d -> d.substring(startHeader[0], startHeader[1])).collect(Collectors.joining()))).find())
            return row;

        if (matcher.start(0) != 0) row.creditCompany = string.substring(0, matcher.start(0));
        else row.creditCompany = string.substring(string.indexOf(',') + 1);

        String s = page.substring(0, m.start(0));
        if ((m = patterns.okb.typeEnd.matcher(s)).find())
            row.type = patterns.spacePattern.matcher(s.substring(m.end(0))).replaceAll(" ");
        if ((m = patterns.uuid.matcher(StringUtils.deleteWhitespace(page))).find()) row.uuid = m.group(0);
        int tempI0 = (page = String.join("\n", lines)).indexOf("ИНН");
        if (tempI0 != -1 && (m = patterns.innPattern.matcher(page.substring(tempI0 + 5))).find())
            row.inn = m.group(0);

        for (int j = 0; j < lines.length; j++) {
            if (row.closed != null) break;
            if (patterns.okb.newClosed.matcher(lines[j]).find() && (m = patterns.date.matcher(lines[j += 2])).find())
                row.closed = m.group(0);
            if (patterns.okb.newOpened.matcher(lines[j]).find() && (m = patterns.date.matcher(lines[j += 2])).find())
                row.opened = m.group(0);
            else if (patterns.okb.oldDateHeader.matcher(lines[j]).find() && (m = patterns.date.matcher(lines[j += 2])).find()) {
                row.opened = m.group(0);
                if (row.closed != null) break;
                s = (String) Arrays.stream(lines[j].split(" ", 0)).filter(f -> !f.isEmpty()).toArray()[2];
                if (!s.equals("-")) row.closed = s;
            }
        }
        return row;
    }
}
