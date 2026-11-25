# ERD — Library Management System (Updated)

```mermaid
erDiagram
    PERSON ||--o| UNIVERSITY_MEMBER : "has membership"
    UNIVERSITY_MEMBER ||--o{ BORROW : "borrows"
    BOOK_COPY ||--o{ BORROW : "is borrowed in"
    BORROW ||--o{ FINE : "may incur"
    FINE ||--o{ PAYMENT_TRANSACTION : "has payments"

    PERSON ||--o{ AUTHOR_OF : "authors"
    RESOURCE ||--o{ AUTHOR_OF : "has authors"

    RESOURCE ||--o| PHYSICAL_BOOK : "physical subtype"
    RESOURCE ||--o| DIGITAL_JOURNAL : "digital subtype"
    PHYSICAL_BOOK ||--o{ BOOK_COPY : "has copies"

    RESOURCE ||--o{ RESOURCE_LICENSE : "requires"
    LICENSE_PACKAGE ||--o{ RESOURCE_LICENSE : "required by"
    PERSON ||--o{ GRANTED_LICENSE : "receives"
    LICENSE_PACKAGE ||--o{ GRANTED_LICENSE : "granted as"

    PERSON {
        int person_id PK
        varchar name
        varchar email UK
        varchar phone
    }

    UNIVERSITY_MEMBER {
        varchar member_id PK
        int person_id FK, UK
        enum member_type
        varchar major
        varchar academic_rank
    }

    RESOURCE {
        int resource_id PK
        varchar title
        varchar topic
        boolean is_public
    }

    PHYSICAL_BOOK {
        int resource_id PK, FK
        varchar shelf_location
    }

    DIGITAL_JOURNAL {
        int resource_id PK, FK
        varchar url
        int file_size
        varchar format
    }

    BOOK_COPY {
        varchar copy_id PK
        int resource_id FK
        enum availability_status
    }

    AUTHOR_OF {
        int person_id PK, FK
        int resource_id PK, FK
    }

    LICENSE_PACKAGE {
        int license_id PK
        varchar license_name
        text description
    }

    RESOURCE_LICENSE {
        int resource_id PK, FK
        int license_id PK, FK
    }

    GRANTED_LICENSE {
        int grant_id PK
        int person_id FK
        int license_id FK
    }

    BORROW {
        int borrow_id PK
        varchar member_id FK
        varchar copy_id FK
        date borrow_date
        date due_date
        date return_date
    }

    FINE {
        int fine_id PK
        int borrow_id FK
        decimal total_fine_amount
        enum payment_status
    }

    PAYMENT_TRANSACTION {
        int transaction_id PK
        int fine_id FK
        timestamp payment_date
    }

    ACCESS_LOG {
        bigint log_id PK
        int journal_resource_id
        varchar ip_address
    }
```

## Structural rules

- `UNIVERSITY_MEMBER.member_id` is the independent member key. `BORROW.member_id` references it; it is not a `person_id`.
- Every University Member has exactly one `person_id`; `person_id` is unique in `UNIVERSITY_MEMBER` and is used to test `AUTHOR_OF`.
- `STUDENT` requires `major`; `FACULTY` requires `academic_rank`. The other attribute may also be populated.
- Any `PERSON` can be an author through `AUTHOR_OF`. There is no `EXTERNAL_AUTHOR` table or `PERSON.person_type` discriminator.
- Resource kind is represented by the subtype table (`PHYSICAL_BOOK` or `DIGITAL_JOURNAL`), not a `RESOURCE.resource_type` column.
- Multiple `FINE` rows may exist for one `BORROW`: a new late penalty is added to an unpaid fine; if prior fines are settled, a new fine row preserves the settlement history.
- `ACCESS_LOG.journal_resource_id` remains without a foreign-key constraint, matching the previous logging design.
