package com.vprave.niqitadev.parser.parse.consumers;

import com.vprave.niqitadev.parser.parse.Utils;
import com.vprave.niqitadev.parser.table.Result;
import com.vprave.niqitadev.parser.table.Row;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OkbConsumer implements Consumer<String> {
    ThreadLocal<Result> TABLE = new ThreadLocal<Result>();

    private final Pattern
            innPattern = Pattern.compile("\\d{10}", 0),
            spacePattern = Pattern.compile(" {2,}", 0),
            newOpened = Pattern.compile("Дата +совершения +сделки", 0),
            newClosed = Pattern.compile("Дата +фактического +прекращения", 0),
            oldDateHeader = Pattern.compile("Дата +открытия +Дата +последнего +платежа", 0),
            start = Pattern.compile("Информация +о +кредиторе +\\(источнике\\)|Сведения +об +источнике", 0),
            end = Pattern.compile("Сведения +об +условиях +платежей|Платежная +дисциплина +по +договору", 0),
            bs = Pattern.compile("^ +Отчет.+\n|\\d+/\\d+", Pattern.MULTILINE),
            typeEnd = Pattern.compile("\\(кредита\\) +-", 0),
            moneyPattern = Pattern.compile("\\d{1,3}( +\\d{3})*(,\\d{1,2})?", 0),
            okbCreditor = Pattern.compile("Договор +займа +\\(кредита\\)", 0),
            headerSplit = Pattern.compile("\n(?= +\\d+)", Pattern.MULTILINE),
            type = Pattern.compile("Информация +о +субъекте|Сведения +об +источнике", 0);
    String string;
    Matcher matcher;
    int i;
    private final int[] sumI = new int[1], startHeader = new int[2];
    private String[] split;

    @Override
    public void accept(String page) {
        Matcher m = end.matcher(page);
        if (!m.find()) return;
        if (!(m = type.matcher(page = Utils.removeAll(bs, page.substring(0, m.end(0))))).find()) return;
        String[] lines = page.lines().filter(s -> !s.isBlank()).dropWhile(a -> !start.matcher(a).find()).toArray(String[]::new);
        Row row = new Row();

        if (!(matcher = moneyPattern.matcher(string = split[++i].substring(sumI[0]))).find()) return;
        row.setCurrency(string.substring(matcher.end(0), string.indexOf(' ', matcher.end(0))));
        row.setDebt(string = StringUtils.deleteWhitespace(matcher.group(0)).replace(',', '.'));
        BigDecimal debt = BigDecimal.valueOf(Double.parseDouble(string));
        if (!(matcher = moneyPattern.matcher(split[i].substring(startHeader[1]))).find()) return;
        row.setSize(string = StringUtils.deleteWhitespace(matcher.group(0)).replace(',', '.'));
        double doubleDebt = BigDecimal.valueOf(Double.parseDouble(string)).subtract(debt).doubleValue();
        row.setPaid(doubleDebt == (long) doubleDebt ? String.format("%d", (long) doubleDebt) : String.format("%s", doubleDebt));
        if (!(matcher = okbCreditor.matcher(string = split[i].lines().filter(f -> !f.isBlank())
                .map(d -> d.substring(startHeader[0], startHeader[1])).collect(Collectors.joining()))).find()) return;

        if (matcher.start(0) != 0) row.setCreditCompany(string.substring(0, matcher.start(0)));
        else row.setCreditCompany(string.substring(string.indexOf(',') + 1));

        String s = page.substring(0, m.start(0));
        if ((m = typeEnd.matcher(s)).find()) row.setType(spacePattern.matcher(s.substring(m.end(0))).replaceAll(" "));
        if ((m = Utils.uuid.matcher(StringUtils.deleteWhitespace(page))).find()) row.setUuid(m.group(0));
        int tempI0 = (page = String.join("\n", lines)).indexOf("ИНН");
        if (tempI0 != -1 && (m = innPattern.matcher(page.substring(tempI0 + 5))).find())
            row.setInn(m.group(0));

        for (int j = 0; j < lines.length; j++) {
            if (!row.isClosedSet()) break;
            if (newClosed.matcher(lines[j]).find() && (m = Utils.date.matcher(lines[j += 2])).find())
                row.setClosed(m.group(0));
            if (newOpened.matcher(lines[j]).find() && (m = Utils.date.matcher(lines[j += 2])).find())
                row.setOpened(m.group(0));
            else if (oldDateHeader.matcher(lines[j]).find() && (m = Utils.date.matcher(lines[j += 2])).find()) {
                row.setOpened(m.group(0));
                if (!row.isClosedSet()) break;
                s = (String) Arrays.stream(lines[j].split(" ", 0)).filter(f -> !f.isEmpty()).toArray()[2];
                if (!s.equals("-")) row.setClosed(s);
            }
        }
        TABLE.get().resultArrayList.add(row);
    }

    public OkbConsumer reload(Result result, String header) {
        i = 0;
        TABLE.set(result);
        startHeader[0] = header.indexOf("Источник");
        startHeader[1] = header.indexOf("Размер");
        int i = header.indexOf("Вид");
        if (i != -1 && (startHeader[0] == -1 || i < startHeader[0])) startHeader[0] = i;
        sumI[0] = header.indexOf("Задолженность");
        assert startHeader[0] != -1 && startHeader[1] != -1 && sumI[0] != -1;
        split = headerSplit.split(header);
        return this;
    }
}
