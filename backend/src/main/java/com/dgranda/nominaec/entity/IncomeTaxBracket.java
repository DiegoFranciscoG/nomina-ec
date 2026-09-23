package com.dgranda.nominaec.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "income_tax_brackets")
public class IncomeTaxBracket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year")
    private int fiscalYear;

    @Column(name = "lower_bound")
    private BigDecimal lowerBound;

    @Column(name = "upper_bound")
    private BigDecimal upperBound;

    @Column(name = "base_tax")
    private BigDecimal baseTax;

    @Column(name = "marginal_rate")
    private BigDecimal marginalRate;

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

    public BigDecimal getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(BigDecimal lowerBound) {
        this.lowerBound = lowerBound;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(BigDecimal upperBound) {
        this.upperBound = upperBound;
    }

    public BigDecimal getBaseTax() {
        return baseTax;
    }

    public void setBaseTax(BigDecimal baseTax) {
        this.baseTax = baseTax;
    }

    public BigDecimal getMarginalRate() {
        return marginalRate;
    }

    public void setMarginalRate(BigDecimal marginalRate) {
        this.marginalRate = marginalRate;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
