package com.dgranda.nominaec.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "concepts")
public class Concept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "concept_type")
    private ConceptType conceptType;

    @Column(name = "iess_taxable")
    private boolean iessTaxable;

    @Column(name = "income_tax_taxable")
    private boolean incomeTaxTaxable;

    @Column(name = "sort_order")
    private int sortOrder;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConceptType getConceptType() {
        return conceptType;
    }

    public void setConceptType(ConceptType conceptType) {
        this.conceptType = conceptType;
    }

    public boolean isIessTaxable() {
        return iessTaxable;
    }

    public void setIessTaxable(boolean iessTaxable) {
        this.iessTaxable = iessTaxable;
    }

    public boolean isIncomeTaxTaxable() {
        return incomeTaxTaxable;
    }

    public void setIncomeTaxTaxable(boolean incomeTaxTaxable) {
        this.incomeTaxTaxable = incomeTaxTaxable;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
