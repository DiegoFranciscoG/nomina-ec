package com.dgranda.nominaec.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "provisions")
public class Provision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payslip_id")
    private Payslip payslip;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "period_id")
    private Long periodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provision_type")
    private ProvisionType provisionType;

    private BigDecimal amount;

    @Column(name = "paid_monthly")
    private boolean paidMonthly;

    public Long getId() {
        return id;
    }

    public Payslip getPayslip() {
        return payslip;
    }

    public void setPayslip(Payslip payslip) {
        this.payslip = payslip;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }

    public void setProvisionType(ProvisionType provisionType) {
        this.provisionType = provisionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isPaidMonthly() {
        return paidMonthly;
    }

    public void setPaidMonthly(boolean paidMonthly) {
        this.paidMonthly = paidMonthly;
    }
}
