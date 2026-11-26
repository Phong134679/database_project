package com.library.service;

import com.library.exception.ResourceNotFoundException;
import com.library.model.Person;
import com.library.model.UniversityMember;
import com.library.repository.BorrowRepository;
import com.library.repository.FineRepository;
import com.library.repository.PersonRepository;
import com.library.repository.UniversityMemberRepository;
import com.library.validation.DataValidator;

import java.util.List;
import java.util.Map;

/**
 * Service class coordinating user identity and university member queries.
 * Eligibility, quota, and loan duration rules are enforced in MySQL
 * (VIEW_MEMBERS_DETAILED and sp_borrow_book).
 */
public class MemberService {

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
