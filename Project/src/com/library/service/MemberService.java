package com.library.service;

import com.library.model.Person;
import com.library.model.UniversityMember;
import com.library.repository.BorrowRepository;
import com.library.repository.FineRepository;
import com.library.repository.PersonRepository;
import com.library.repository.UniversityMemberRepository;

import java.util.List;

/**
 * Service class managing user identities, membership verification,
 * borrowing eligibility checks (Test 01, BR01, BR05, BR09).
 */
public class MemberService {

    public static final int STUDENT_MAX_QUOTA = 3;
    public static final int FACULTY_MAX_QUOTA = 10;
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
     * Rejects:
     * 1. External guests or authors (BR01 / Test 01).
     * 2. Members with overdue unreturned books.
     * 3. Members with outstanding unpaid fines.
     * 4. Members who have reached their borrow quota (BR05).
     */
    public boolean isEligibleToBorrow(int personId) {
        Person person = personRepository.findById(personId);
        if (person == null) {
            System.err.println("[MemberService] Person ID " + personId + " does not exist in the system.");
            return false;
        }

        // Test 01 & BR01: Chặn khách ngoài và tác giả ngoài mượn sách về nhà
        String pType = person.getPersonType();
        if (!"STUDENT".equalsIgnoreCase(pType) && !"FACULTY".equalsIgnoreCase(pType)) {
            System.err.println("[MemberService] ELIGIBILITY REJECTED (Test 01 / BR01): Person [" + person.getName() +
                    "] with role '" + pType + "' is an external user and only permitted to read on-site.");
            return false;
        }

        // Kiểm tra xem có sách quá hạn chưa trả hay không
        if (borrowRepository.hasOverdueBorrows(personId)) {
            System.err.println("[MemberService] ELIGIBILITY REJECTED: Member [" + person.getName() +
                    "] currently holds overdue books that must be returned first.");
            return false;
        }

        // Kiểm tra xem có nợ tiền phạt chưa thanh toán hay không
        if (fineRepository.hasUnpaidFinesByMember(personId)) {
            System.err.println("[MemberService] ELIGIBILITY REJECTED: Member [" + person.getName() +
                    "] has outstanding unpaid fines that must be settled.");
            return false;
        }

        // BR05: Kiểm tra hạn mức mượn tối đa theo loại thành viên
        boolean isFaculty = "FACULTY".equalsIgnoreCase(pType);
        int maxQuota = isFaculty ? FACULTY_MAX_QUOTA : STUDENT_MAX_QUOTA;
        int activeBorrows = borrowRepository.countActiveBorrowsByMember(personId);

        if (activeBorrows >= maxQuota) {
            System.err.println("[MemberService] ELIGIBILITY REJECTED (BR05): Member [" + person.getName() +
                    "] has reached their maximum borrowing quota (" + activeBorrows + "/" + maxQuota + " books).");
            return false;
        }

        return true;
    }

    /**
     * Determines loan duration in days according to user classification (BR05).
     * Faculty: 30 days, Students: 14 days.
     */
    public int getLoanDurationDays(int personId) {
        Person person = personRepository.findById(personId);
        if (person != null && "FACULTY".equalsIgnoreCase(person.getPersonType())) {
            return FACULTY_LOAN_DAYS;
        }
        return STUDENT_LOAN_DAYS;
    }

    /**
     * Retrieves member profile.
     */
    public UniversityMember getMemberProfile(int personId) {
        return memberRepository.findByPersonId(personId);
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
