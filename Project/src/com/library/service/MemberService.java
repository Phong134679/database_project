package com.library.service;

import com.library.exception.ResourceNotFoundException;
import com.library.model.Person;
import com.library.model.UniversityMember;
import com.library.repository.BorrowRepository;
import com.library.repository.FineRepository;
import com.library.repository.PersonRepository;
import com.library.repository.UniversityMemberRepository;
import com.library.validation.DataValidator;
import com.library.validation.MemberValidator;

import java.util.List;
import java.util.Map;

/**
 * Service class managing user identities, membership verification,
 * borrowing eligibility checks (BR01, BR05).
 */
public class MemberService {

    public static final int STUDENT_MAX_QUOTA = MemberValidator.STUDENT_MAX_QUOTA;
    public static final int FACULTY_MAX_QUOTA = MemberValidator.FACULTY_MAX_QUOTA;
    public static final int STUDENT_LOAN_DAYS = 14;
    public static final int FACULTY_LOAN_DAYS = 30;

    private PersonRepository personRepository;
    private UniversityMemberRepository memberRepository;
    private BorrowRepository borrowRepository;
    private FineRepository fineRepository;

    public MemberService() {
        this.personRepository = new PersonRepository();
        this.memberRepository = new UniversityMemberRepository();
        this.borrowRepository = new BorrowRepository();
        this.fineRepository = new FineRepository();
    }

    public MemberService(PersonRepository personRepository,
                         UniversityMemberRepository memberRepository,
                         BorrowRepository borrowRepository,
                         FineRepository fineRepository) {
        this.personRepository = personRepository;
        this.memberRepository = memberRepository;
        this.borrowRepository = borrowRepository;
        this.fineRepository = fineRepository;
    }

    /**
     * Checks if a university member is eligible to borrow physical books (BR01, BR05).
     */
    public boolean isEligibleToBorrow(String memberId) {
        DataValidator.validateNonEmptyString(memberId, "Member ID");

        UniversityMember member = memberRepository.findByMemberId(memberId);
        if (member == null) {
            throw new ResourceNotFoundException("Thành viên đại học (UniversityMember) với mã " + memberId, 0);
        }

        boolean hasOverdue = borrowRepository.hasOverdueBorrows(memberId);
        boolean hasFines = fineRepository.hasUnpaidFinesByMember(memberId);
        int activeBorrows = borrowRepository.countActiveBorrowsByMember(memberId);

        MemberValidator.validateBorrowEligibility(member, hasOverdue, hasFines, activeBorrows);
        return true;
    }

    /**
     * Determines loan duration in days according to member_type (BR05).
     * FACULTY: 30 days, STUDENT: 14 days.
     */
    public int getLoanDurationDays(String memberId) {
        DataValidator.validateNonEmptyString(memberId, "Member ID");
        UniversityMember member = memberRepository.findByMemberId(memberId);
        if (member == null) {
            throw new ResourceNotFoundException("Thành viên đại học (UniversityMember) với mã " + memberId, 0);
        }

        if ("FACULTY".equalsIgnoreCase(member.getMemberType())) {
            return FACULTY_LOAN_DAYS;
        }
        return STUDENT_LOAN_DAYS;
    }

    public UniversityMember getMemberProfile(int personId) {
        DataValidator.validatePositiveId(personId, "Person ID");
        UniversityMember member = memberRepository.findByPersonId(personId);
        if (member == null) {
            throw new ResourceNotFoundException("Thành viên đại học (UniversityMember)", personId);
        }
        return member;
    }

    public UniversityMember getMemberByMemberId(String memberId) {
        DataValidator.validateNonEmptyString(memberId, "Member ID");
        UniversityMember member = memberRepository.findByMemberId(memberId);
        if (member == null) {
            throw new ResourceNotFoundException("Thành viên đại học (UniversityMember) với mã " + memberId, 0);
        }
        return member;
    }

    public List<UniversityMember> getAllMembers() {
        return memberRepository.findAll();
    }

    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }

    public List<Map<String, Object>> getAllPersonsDetailed() {
        return personRepository.findAllDetailed();
    }

    public PersonRepository getPersonRepository() {
        return personRepository;
    }

    public void setPersonRepository(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public UniversityMemberRepository getMemberRepository() {
        return memberRepository;
    }

    public void setMemberRepository(UniversityMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public BorrowRepository getBorrowRepository() {
        return borrowRepository;
    }

    public void setBorrowRepository(BorrowRepository borrowRepository) {
        this.borrowRepository = borrowRepository;
    }

    public FineRepository getFineRepository() {
        return fineRepository;
    }

    public void setFineRepository(FineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }
}
