package com.dgranda.nominaec.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "personal_expense_caps")
public class PersonalExpenseCap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year")
    private int fiscalYear;

    @Column(name = "family_dependents")
    private int familyDependents;

    @Column(name = "basket_multiplier")
    private BigDecimal basketMultiplier;

    @Column(name = "source_url")
    private String sourceUrl;

    public Long getId() {
        return id;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(int fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public int getFamilyDependents() {
        return familyDependents;
    }

    public void setFamilyDependents(int familyDependents) {
        this.familyDependents = familyDependents;
    }

    public BigDecimal getBasketMultiplier() {
        return basketMultiplier;
    }

    public void setBasketMultiplier(BigDecimal basketMultiplier) {
        this.basketMultiplier = basketMultiplier;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
