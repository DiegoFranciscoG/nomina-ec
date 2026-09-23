package com.dgranda.nominaec.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id")
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type")
    private ContractType contractType;

    @Column(name = "weekly_hours")
    private int weeklyHours;

    @Column(name = "monthly_salary")
    private BigDecimal monthlySalary;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(name = "thirteenth_mode")
    private PaymentMode thirteenthMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "fourteenth_mode")
    private PaymentMode fourteenthMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reserve_fund_mode")
    private PaymentMode reserveFundMode;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public void setContractType(ContractType contractType) {
        this.contractType = contractType;
    }

    public int getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(int weeklyHours) {
        this.weeklyHours = weeklyHours;
    }

    public BigDecimal getMonthlySalary() {
        return monthlySalary;
    }

    public void setMonthlySalary(BigDecimal monthlySalary) {
        this.monthlySalary = monthlySalary;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public PaymentMode getThirteenthMode() {
        return thirteenthMode;
    }

    public void setThirteenthMode(PaymentMode thirteenthMode) {
        this.thirteenthMode = thirteenthMode;
    }

    public PaymentMode getFourteenthMode() {
        return fourteenthMode;
    }

    public void setFourteenthMode(PaymentMode fourteenthMode) {
        this.fourteenthMode = fourteenthMode;
    }

    public PaymentMode getReserveFundMode() {
        return reserveFundMode;
    }

    public void setReserveFundMode(PaymentMode reserveFundMode) {
        this.reserveFundMode = reserveFundMode;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }
}
