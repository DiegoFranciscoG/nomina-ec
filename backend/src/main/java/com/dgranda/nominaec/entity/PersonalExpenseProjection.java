package com.dgranda.nominaec.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "personal_expense_projections")
public class PersonalExpenseProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "fiscal_year")
    private int fiscalYear;

    @Column(name = "projected_amount")
    private BigDecimal projectedAmount;

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(int fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public BigDecimal getProjectedAmount() {
        return projectedAmount;
    }

    public void setProjectedAmount(BigDecimal projectedAmount) {
        this.projectedAmount = projectedAmount;
    }
}
