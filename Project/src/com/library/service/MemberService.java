package com.library.service;

import com.library.exception.ResourceNotFoundException;
import com.library.model.Person;
import com.library.model.UniversityMember;
import com.library.repository.BorrowRepository;
import com.library.repository.FineRepository;
import com.library.repository.PersonRepository;
import com.library.repository.UniversityMemberRepository;
import com.library.validation.DataValidator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class coordinating user identity, university member rules,
 * borrow quota/loan duration calculation, and borrow eligibility evaluation.
 */
public class MemberService {

    public static final int FACULTY_QUOTA = 10;
    public static final int STUDENT_QUOTA = 3;
    public static final int FACULTY_LOAN_DAYS = 30;
    public static final int STUDENT_LOAN_DAYS = 14;

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

    public int getBorrowQuota(String memberType) {
        return "FACULTY".equalsIgnoreCase(memberType) ? FACULTY_QUOTA : STUDENT_QUOTA;
    }

    public int getLoanDurationDays(String memberType) {
        return "FACULTY".equalsIgnoreCase(memberType) ? FACULTY_LOAN_DAYS : STUDENT_LOAN_DAYS;
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

    /**
     * Enriches raw person/member records with business-rule fields:
     * loanDurationDays, borrowQuota, hasOverdue, hasUnpaidFines, isEligibleToBorrow, ineligibilityReason.
     */
    public List<Map<String, Object>> getAllPersonsDetailed() {
        List<Map<String, Object>> rawList = personRepository.findAllDetailedRaw();
        List<Map<String, Object>> result = new ArrayList<>(rawList.size());

        for (Map<String, Object> raw : rawList) {
            String name = (String) raw.get("name");
            String memberId = (String) raw.get("memberId");
            String memberType = (String) raw.get("memberType");
            boolean isMember = memberId != null;
            boolean isAuthor = Boolean.TRUE.equals(raw.get("isAuthor"));
            int activeCount = ((Number) raw.get("activeBorrowsCount")).intValue();
            boolean hasOverdue = ((Number) raw.get("overdueBorrowsCount")).intValue() > 0;
            boolean hasUnpaidFines = ((Number) raw.get("unpaidFinesCount")).intValue() > 0;

            int quota = isMember ? getBorrowQuota(memberType) : 0;
            int loanDays = isMember ? getLoanDurationDays(memberType) : 0;
            boolean isEligible = isMember && !hasOverdue && !hasUnpaidFines && activeCount < quota;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("personId", raw.get("personId"));
            map.put("name", name);
            map.put("email", raw.get("email"));
            map.put("phone", raw.get("phone"));
            map.put("memberId", memberId);
            map.put("isMember", isMember);
            map.put("isAuthor", isAuthor);
            map.put("memberType", memberType);
            map.put("major", raw.get("major"));
            map.put("academicRank", raw.get("academicRank"));
            map.put("loanDurationDays", loanDays);
            map.put("borrowQuota", quota);
            map.put("activeBorrowsCount", activeCount);
            map.put("hasOverdue", hasOverdue);
            map.put("hasUnpaidFines", hasUnpaidFines);
            map.put("isEligibleToBorrow", isEligible);
            map.put("ineligibilityReason", buildIneligibilityReason(
                    name, isMember, hasOverdue, hasUnpaidFines, activeCount, quota, isEligible));
            result.add(map);
        }
        return result;
    }

    private String buildIneligibilityReason(String name, boolean isMember, boolean hasOverdue,
                                            boolean hasUnpaidFines, int activeCount, int quota, boolean isEligible) {
        if (isEligible) {
            return null;
        }
        if (!isMember) {
            return "Người dùng [" + name + "] không phải UNIVERSITY_MEMBER; chỉ UNIVERSITY_MEMBER mới được phép mượn sách (BR01).";
        }
        if (hasOverdue) {
            return "Thành viên [" + name + "] đang có sách mượn quá hạn chưa trả (BR05).";
        }
        if (hasUnpaidFines) {
            return "Thành viên [" + name + "] còn phiếu phạt ở trạng thái Unpaid. "
                    + "Yêu cầu thanh toán trước khi tiếp tục mượn mới (BR05).";
        }
        if (activeCount >= quota) {
            return "Thành viên [" + name + "] đã đạt hạn mức mượn tối đa (" + activeCount + "/" + quota + ") theo quy chế BR05.";
        }
        return "Không đủ điều kiện mượn sách.";
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
