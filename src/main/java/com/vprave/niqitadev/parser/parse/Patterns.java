package com.vprave.niqitadev.parser.parse;


import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public final class Patterns {
    public final Pattern innPattern, moneyPattern, spacePattern, date, uuid;
    public final Okb okb;
    public final Nbki nbki;
    public Patterns() {
        innPattern = Pattern.compile("\\d{10}", 0);
        moneyPattern = Pattern.compile("\\d{1,3}( +\\d{3})*(,\\d{1,2})?|\\d+", 0);
        spacePattern = Pattern.compile(" {2,}", 0);
        date = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}|\\d{1,2} +[а-я]+ +\\d{4}|\\d{2}-\\d{2}-\\d{4}", 0);
        uuid = Pattern.compile("[a-f\\-\\dA-F]{8}-([a-f\\-\\dA-F]{4}-){3}[a-f\\-\\dA-F]{12}(-[a-f\\-\\dA-F])?", 0);
        okb = new Okb();
        nbki = new Nbki();
    }

    public static final class Nbki {
        public final Pattern currencyPattern, fullName, typePattern, start, end, pageBs, normalisedSplit, bs;

        private Nbki() {
            currencyPattern = Pattern.compile("[A-Z]{3}", 0);
            fullName = Pattern.compile("[А-я\\-]{2,} +[А-я\\-]{2,} +[А-я\\-]{2,}", Pattern.MULTILINE);
            typePattern = Pattern.compile("([А-я.]+ {1,2})+", 0);
            end = Pattern.compile("Информацио|Случаи +привлечения +к +ответственности", Pattern.MULTILINE);
            start = Pattern.compile("Обязательства +и +их +исполнение|Счета", Pattern.MULTILINE);
            bs = Pattern.compile("^ +Кредитный +отчет *(для +субъекта *)?\n +ID +запроса +Пользователь +Предоставлен *\n.+\n|Примечание. +Значение +\"Н/Д\" +указывает +на +то, +что +данные +в +НБКИ +отсутствуют.( *\n)+ *При +наличии +вопросов +обращайтесь +в +НБКИ: +[,\\d -]+", Pattern.MULTILINE);
            normalisedSplit = Pattern.compile("(?=Счет +Договор +Состояние +Баланс)", Pattern.MULTILINE);
            pageBs = Pattern.compile("(?s)\n *Залоги.*?Данные +о +кредиторе *\n", Pattern.MULTILINE);
        }
    }

    public static final class Okb {
        public final Pattern closed, fullName, closedIsEmpty, mainSplit, newOpened, newClosed, oldDateHeader, start, end, bs, typeEnd, creditor, headerSplit, type;

        private Okb() {
            closed = Pattern.compile("ЗАКРЫТЫЕ +КРЕДИТНЫЕ *\n* +ДОГОВОРЫ", Pattern.MULTILINE);
            fullName = Pattern.compile("[А-я\\-]{2,} +[А-я\\-]{2,} +[А-я\\-]{2,}", Pattern.MULTILINE);
            closedIsEmpty = Pattern.compile("ИНФОРМАЦИЯ +ОТСУТСТВУЕТ", 0);
            mainSplit = Pattern.compile("(?=\\d\\..+ +Договор +займа +\\(кредита\\).+)", 0);
            newOpened = Pattern.compile("Дата +совершения +сделки", 0);
            bs = Pattern.compile("^ +Отчет.+\n|\\d+/\\d+", Pattern.MULTILINE);
            end = Pattern.compile("Сведения +об +условиях +платежей|Платежная +дисциплина +по +договору", 0);
            start = Pattern.compile("Информация +о +кредиторе +\\(источнике\\)|Сведения +об +источнике", 0);
            oldDateHeader = Pattern.compile("Дата +открытия +Дата +последнего +платежа", 0);
            newClosed = Pattern.compile("Дата +фактического +прекращения", 0);
            typeEnd = Pattern.compile("\\(кредита\\) +-", 0);
            creditor = Pattern.compile("Договор +займа +\\(кредита\\)", 0);
            headerSplit = Pattern.compile("\n(?= +\\d+)", Pattern.MULTILINE);
            type = Pattern.compile("Информация +о +субъекте|Сведения +об +источнике", 0);
        }
    }
}
