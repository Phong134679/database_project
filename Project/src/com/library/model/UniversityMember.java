package com.library.model;

/**
 * Subclass of Person representing internal university members (STUDENT or FACULTY).
 * member_id is an independent primary key; person_id is a UNIQUE FK to PERSON.
 */
public class UniversityMember extends Person {
    private String memberId;
    private String memberType;
    private String major;
    private String academicRank;

    public UniversityMember() {
        super();
    }

    public UniversityMember(int personId, String name, String email, String phone,
                            String memberId, String memberType,
                            String major, String academicRank) {
        super(personId, name, email, phone);
        this.memberId = memberId;
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
                ", name='" + getName() + '\'' +
                ", memberId='" + memberId + '\'' +
                ", memberType='" + memberType + '\'' +
                ", major='" + major + '\'' +
                ", academicRank='" + academicRank + '\'' +
                '}';
    }
}
