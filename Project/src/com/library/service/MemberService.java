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

/**
 * Service class managing user identities, membership verification,
 * borrowing eligibility checks (Test 01, BR01, BR05, BR09).
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
     * Checks if a person is eligible to borrow physical books home (Validation Case Test 01, BR01, BR05).
     * Rejects by throwing specific BusinessException if invalid, returns true otherwise.
     */
    public boolean isEligibleToBorrow(int personId) {
        DataValidator.validatePositiveId(personId, "Person ID");

        Person person = personRepository.findById(personId);
        MemberValidator.validatePersonExists(person, personId);

        boolean hasOverdue = borrowRepository.hasOverdueBorrows(personId);
        boolean hasFines = fineRepository.hasUnpaidFinesByMember(personId);
        int activeBorrows = borrowRepository.countActiveBorrowsByMember(personId);

        MemberValidator.validateBorrowEligibility(person, hasOverdue, hasFines, activeBorrows);
        return true;
    }

    /**
     * Determines loan duration in days according to user classification (BR05).
     * Faculty: 30 days, Students: 14 days.
     */
    public int getLoanDurationDays(int personId) {
        DataValidator.validatePositiveId(personId, "Person ID");
        Person person = personRepository.findById(personId);
        MemberValidator.validatePersonExists(person, personId);

        if ("FACULTY".equalsIgnoreCase(person.getPersonType())) {
            return FACULTY_LOAN_DAYS;
        }
        return STUDENT_LOAN_DAYS;
    }

    /**
     * Retrieves member profile.
     */
    public UniversityMember getMemberProfile(int personId) {
        DataValidator.validatePositiveId(personId, "Person ID");
        UniversityMember member = memberRepository.findByPersonId(personId);
        if (member == null) {
            throw new ResourceNotFoundException("Thành viên đại học (UniversityMember)", personId);
        }
        return member;
    }

    /**
     * Retrieves all university members.
     */
    public List<UniversityMember> getAllMembers() {
        return memberRepository.findAll();
    }

    /**
     * Retrieves all persons in the database.
     */
    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }

    /**
     * Retrieves all persons with university details and borrow eligibility summary for UI display.
     */
    public List<java.util.Map<String, Object>> getAllPersonsDetailed() {
        List<java.util.Map<String, Object>> persons = personRepository.findAllDetailed();
        for (java.util.Map<String, Object> p : persons) {
            int personId = (int) p.get("personId");
            int activeCount = borrowRepository.countActiveBorrowsByMember(personId);
            boolean hasOverdue = borrowRepository.hasOverdueBorrows(personId);
            boolean hasFines = fineRepository.hasUnpaidFinesByMember(personId);
            p.put("activeBorrowsCount", activeCount);
            p.put("hasOverdue", hasOverdue);
            p.put("hasUnpaidFines", hasFines);

            boolean eligible = false;
            String ineligibilityReason = null;
            try {
                isEligibleToBorrow(personId);
                eligible = true;
            } catch (com.library.exception.LibraryException ex) {
                ineligibilityReason = ex.getMessage();
            }
            p.put("isEligibleToBorrow", eligible);
            p.put("ineligibilityReason", ineligibilityReason);
        }
        return persons;
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
