erDiagram
    %% Entities and Attributes
    PERSON {
        int person_id PK
        varchar(12) ssn
        varchar(100) name
        varchar(200) address
        varchar(100) email
        varchar(15) phone
        enum person_type
    }

    EXTERNAL_AUTHOR {
        int person_id PK, FK
        varchar(150) affiliation
        text biography
    }

    UNIVERSITY_MEMBER {
        int person_id PK, FK
        varchar(20) member_id
        varchar(100) department
        enum member_type
        varchar(100) major "Note: Chỉ dùng cho Sinh viên"
        varchar(50) academic_rank "Note: Chỉ dùng cho Giảng viên"
    }

    GRANTED_LICENSE {
        int grant_id PK
        int person_id FK
        int license_id FK
        date valid_from
        date valid_to
    }

    LICENSE_PACKAGE {
        int license_id PK
        varchar(150) license_name
        varchar(100) provider
        text description
    }

    RESOURCE_LICENSE {
        int resource_id PK, FK
        int license_id PK, FK
    }

    AUTHOR_OF {
        int person_id PK, FK
        int resource_id PK, FK
    }

    RESOURCE {
        int resource_id PK
        varchar(250) title
        date publish_date
        varchar(100) topic
        enum resource_type
    }

    PHYSICAL_BOOK {
        int resource_id PK, FK
        varchar(20) isbn
        varchar(50) shelf_location
    }

    DIGITAL_JOURNAL {
        int resource_id PK, FK
        varchar(50) doi
        varchar(250) url
        int file_size
        varchar(10) format
    }

    BOOK_COPY {
        int copy_id PK
        int resource_id FK
        varchar(50) barcode
        varchar(50) condition_status
        varchar(20) availability_status
    }

    BORROW {
        int borrow_id PK
        int member_person_id FK
        int copy_id FK
        date borrow_date
        date due_date
        date return_date
    }

    FINE {
        int fine_id PK
        int borrow_id FK
        decimal total_fine_amount
        enum payment_status
        date due_date
    }

    PAYMENT_TRANSACTION {
        int transaction_id PK
        int fine_id FK
        decimal amount_paid
        timestamp payment_date
        varchar(50) payment_method
    }

    ACCESS_LOG {
        bigint log_id PK
        int person_id FK
        int journal_resource_id FK
        timestamp access_timestamp
        varchar(45) ip_address
        boolean download_success
    }

    %% Relationships

    %% Subtypes (Inheritance)
    PERSON ||--|| EXTERNAL_AUTHOR : "is a"
    PERSON ||--|| UNIVERSITY_MEMBER : "is a"
    RESOURCE ||--|| PHYSICAL_BOOK : "is a"
    RESOURCE ||--|| DIGITAL_JOURNAL : "is a"

    %% Other Relationships
    PERSON ||--o{ GRANTED_LICENSE : "receives"
    LICENSE_PACKAGE ||--o{ GRANTED_LICENSE : "issues"

    PERSON ||--o{ AUTHOR_OF : "writes"
    RESOURCE ||--o{ AUTHOR_OF : "written by"

    LICENSE_PACKAGE ||--o{ RESOURCE_LICENSE : "includes"
    RESOURCE ||--o{ RESOURCE_LICENSE : "part of"

    PHYSICAL_BOOK ||--o{ BOOK_COPY : "has copies"
    
    UNIVERSITY_MEMBER ||--o{ BORROW : "makes"
    BOOK_COPY ||--o{ BORROW : "is part of"

    BORROW ||--o{ FINE : "incurs"
    FINE ||--o{ PAYMENT_TRANSACTION : "paid via"

    DIGITAL_JOURNAL ||--o{ ACCESS_LOG : "has"
    PERSON ||--o{ ACCESS_LOG : "creates"
