package com.dgranda.nominaec.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "payslips")
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id")
    private PayrollPeriod period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(name = "worked_days")
    private BigDecimal workedDays;

    @Column(name = "base_salary")
    private BigDecimal baseSalary;

    @Column(name = "iess_base")
    private BigDecimal iessBase;

    @Column(name = "income_tax_base")
    private BigDecimal incomeTaxBase;

    @Column(name = "projected_annual_tax_base")
    private BigDecimal projectedAnnualTaxBase;

    @Column(name = "total_income")
    private BigDecimal totalIncome;

    @Column(name = "total_deductions")
    private BigDecimal totalDeductions;

    @Column(name = "net_pay")
    private BigDecimal netPay;

    @Column(name = "employer_cost")
    private BigDecimal employerCost;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters_snapshot")
    private Map<String, Object> parametersSnapshot;

    @Column(name = "calculated_at")
    private OffsetDateTime calculatedAt;

    @OneToMany(mappedBy = "payslip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PayslipLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "payslip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Provision> provisions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public PayrollPeriod getPeriod() {
        return period;
    }

    public void setPeriod(PayrollPeriod period) {
        this.period = period;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public BigDecimal getWorkedDays() {
        return workedDays;
    }

    public void setWorkedDays(BigDecimal workedDays) {
        this.workedDays = workedDays;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getIessBase() {
        return iessBase;
    }

    public void setIessBase(BigDecimal iessBase) {
        this.iessBase = iessBase;
    }

    public BigDecimal getIncomeTaxBase() {
        return incomeTaxBase;
    }

    public void setIncomeTaxBase(BigDecimal incomeTaxBase) {
        this.incomeTaxBase = incomeTaxBase;
    }

    public BigDecimal getProjectedAnnualTaxBase() {
        return projectedAnnualTaxBase;
    }

    public void setProjectedAnnualTaxBase(BigDecimal projectedAnnualTaxBase) {
        this.projectedAnnualTaxBase = projectedAnnualTaxBase;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public void setTotalDeductions(BigDecimal totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public BigDecimal getNetPay() {
        return netPay;
    }

    public void setNetPay(BigDecimal netPay) {
        this.netPay = netPay;
    }

    public BigDecimal getEmployerCost() {
        return employerCost;
    }

    public void setEmployerCost(BigDecimal employerCost) {
        this.employerCost = employerCost;
    }

    public Map<String, Object> getParametersSnapshot() {
        return parametersSnapshot;
    }

    public void setParametersSnapshot(Map<String, Object> parametersSnapshot) {
        this.parametersSnapshot = parametersSnapshot;
    }

    public OffsetDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(OffsetDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public List<PayslipLine> getLines() {
        return lines;
    }

    public List<Provision> getProvisions() {
        return provisions;
    }
}
