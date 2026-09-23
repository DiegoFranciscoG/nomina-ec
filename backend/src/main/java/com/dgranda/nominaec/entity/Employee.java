package com.dgranda.nominaec.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "first_names")
    private String firstNames;

    @Column(name = "last_names")
    private String lastNames;

    private String email;

    @Column(name = "family_dependents")
    private int familyDependents;

    @Column(name = "catastrophic_condition")
    private boolean catastrophicCondition;

    private boolean active;

    public Long getId() {
        return id;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public void setFirstNames(String firstNames) {
        this.firstNames = firstNames;
    }

    public String getLastNames() {
        return lastNames;
    }

    public void setLastNames(String lastNames) {
        this.lastNames = lastNames;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getFamilyDependents() {
        return familyDependents;
    }

    public void setFamilyDependents(int familyDependents) {
        this.familyDependents = familyDependents;
    }

    public boolean isCatastrophicCondition() {
        return catastrophicCondition;
    }

    public void setCatastrophicCondition(boolean catastrophicCondition) {
        this.catastrophicCondition = catastrophicCondition;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
