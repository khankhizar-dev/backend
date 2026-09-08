CREATE TABLE checklists
(
    id          UUID PRIMARY KEY,
    trip_id     UUID         NOT NULL,
    created_by  UUID         NOT NULL,

    name        VARCHAR(200) NOT NULL,
    description VARCHAR(1000),

    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    CONSTRAINT fk_checklists_trip
        FOREIGN KEY (trip_id)
            REFERENCES trips (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_checklists_created_by
        FOREIGN KEY (created_by)
            REFERENCES users (id),

    CONSTRAINT chk_checklists_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_checklists_trip_id
    ON checklists(trip_id);

CREATE INDEX idx_checklists_created_by
    ON checklists(created_by);

CREATE INDEX idx_checklists_trip_status
    ON checklists(trip_id, status);


CREATE TABLE checklist_sections
(
    id           UUID PRIMARY KEY,
    checklist_id UUID         NOT NULL,

    name         VARCHAR(200) NOT NULL,
    position     INTEGER      NOT NULL,

    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,

    CONSTRAINT fk_checklist_sections_checklist
        FOREIGN KEY (checklist_id)
            REFERENCES checklists (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_checklist_sections_position
        CHECK (position >= 0)
);

CREATE INDEX idx_checklist_sections_checklist_id
    ON checklist_sections(checklist_id);

CREATE INDEX idx_checklist_sections_checklist_position
    ON checklist_sections(checklist_id, position);


CREATE TABLE checklist_items
(
    id         UUID PRIMARY KEY,
    section_id UUID         NOT NULL,
    created_by UUID         NOT NULL,

    name       VARCHAR(300) NOT NULL,
    category   VARCHAR(30)  NOT NULL,

    essential  BOOLEAN      NOT NULL DEFAULT FALSE,
    completed  BOOLEAN      NOT NULL DEFAULT FALSE,

    due_date   TIMESTAMP,

    position   INTEGER      NOT NULL,

    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,

    CONSTRAINT fk_checklist_items_section
        FOREIGN KEY (section_id)
            REFERENCES checklist_sections (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_checklist_items_created_by
        FOREIGN KEY (created_by)
            REFERENCES users (id),

    CONSTRAINT chk_checklist_items_category
        CHECK (
            category IN (
                         'CLOTHING',
                         'DOCUMENTS',
                         'ELECTRONICS',
                         'HEALTH',
                         'TOILETRIES',
                         'OTHER'
                )
            ),

    CONSTRAINT chk_checklist_items_position
        CHECK (position >= 0)
);

CREATE INDEX idx_checklist_items_section_id
    ON checklist_items(section_id);

CREATE INDEX idx_checklist_items_section_position
    ON checklist_items(section_id, position);

CREATE INDEX idx_checklist_items_section_completed
    ON checklist_items(section_id, completed);

CREATE INDEX idx_checklist_items_section_category
    ON checklist_items(section_id, category);

CREATE INDEX idx_checklist_items_due_date
    ON checklist_items(due_date);