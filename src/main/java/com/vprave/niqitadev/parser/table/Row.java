package com.vprave.niqitadev.parser.table;

import org.apache.commons.lang3.StringUtils;

public final class Row {

    public boolean isTypeSet() {
        return null == type;
    }

    public boolean isSizeSet() {
        return null == size;
    }

    public boolean isPaidSet() {
        return null == paid;
    }

    public boolean isCreditCompanySet() {
        return null == creditCompany;
    }

    public boolean isCurrencySet() {
        return null == currency;
    }

    public boolean isInnSet() {
        return null == inn;
    }

    public boolean isClosedSet() {
        return null == closed;
    }

    public boolean isOpenedSet() {
        return null == opened;
    }

    public boolean isDebtSet() {
        return null == debt;
    }

    public boolean isUuidSet() {
        return null == uuid;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setPaid(String paid) {
        this.paid = paid;
    }

    public void setCreditCompany(String creditCompany) {
        this.creditCompany = creditCompany;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public void setClosed(String closed) {
        this.closed = closed;
    }

    public void setOpened(String opened) {
        this.opened = opened;
    }

    public void setDebt(String debt) {
        this.debt = debt;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    private String type, size, paid, creditCompany, currency, inn, closed, opened, debt, uuid;

    void combine(StringBuilder result, int j) {
        result
                .append("<tr><td>")
                .append(j)
                .append("</td><td>")
                .append(StringUtils.defaultString(creditCompany))
                .append("</td><td>")
                .append(StringUtils.defaultString(inn))
                .append("</td><td>")
                .append(StringUtils.defaultString(type))
                .append("</td><td>")
                .append(StringUtils.defaultString(uuid).toLowerCase())
                .append("</td><td>")
                .append(StringUtils.defaultString(opened))
                .append("</td><td>")
                .append(StringUtils.defaultString(closed))
                .append("</td><td>")
                .append(StringUtils.defaultString(currency))
                .append("</td><td>")
                .append(StringUtils.defaultString(size))
                .append("</td><td>")
                .append(StringUtils.defaultString(paid))
                .append("</td><td>")
                .append(StringUtils.defaultString(debt))
                .append("</td></tr>");
    }
}
