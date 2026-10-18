package com.library.model;

/**
 * Subclass of Person representing internal university members (Student or Faculty).
 */
public class UniversityMember extends Person {
    private String memberId;
    private String department;
    private String memberType;
    private String major;
    private String academicRank;

    public UniversityMember() {
        super();
    }

    public UniversityMember(int personId, String ssn, String name, String address, String email, String phone,
                            String personType, String memberId, String department, String memberType,
                            String major, String academicRank) {
        super(personId, ssn, name, address, email, phone, personType);
        this.memberId = memberId;
        this.department = department;
        this.memberType = memberType;
        this.major = major;
        this.academicRank = academicRank;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getAcademicRank() {
        return academicRank;
    }

    public void setAcademicRank(String academicRank) {
        this.academicRank = academicRank;
    }

    @Override
    public String toString() {
        return "UniversityMember{" +
                "personId=" + getPersonId() +
                ", ssn='" + getSsn() + '\'' +
                ", name='" + getName() + '\'' +
                ", memberId='" + memberId + '\'' +
                ", department='" + department + '\'' +
                ", memberType='" + memberType + '\'' +
                ", major='" + major + '\'' +
                ", academicRank='" + academicRank + '\'' +
                '}';
    }
}
