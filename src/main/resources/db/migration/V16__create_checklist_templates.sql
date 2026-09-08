CREATE TABLE checklist_templates
(
    id          UUID PRIMARY KEY,
    created_by  UUID,
    name        VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    type        VARCHAR(20)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    CONSTRAINT fk_checklist_templates_created_by
        FOREIGN KEY (created_by)
            REFERENCES users (id),

    CONSTRAINT chk_checklist_templates_type
        CHECK (type IN ('SYSTEM', 'USER', 'TRIP')),

    CONSTRAINT chk_checklist_templates_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_checklist_templates_created_by
    ON checklist_templates(created_by);

CREATE INDEX idx_checklist_templates_type
    ON checklist_templates(type);

CREATE INDEX idx_checklist_templates_status
    ON checklist_templates(status);

CREATE INDEX idx_checklist_templates_type_status
    ON checklist_templates(type, status);


CREATE TABLE checklist_template_sections
(
    id          UUID PRIMARY KEY,
    template_id UUID         NOT NULL,
    name        VARCHAR(200) NOT NULL,
    position    INTEGER      NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    CONSTRAINT fk_checklist_template_sections_template
        FOREIGN KEY (template_id)
            REFERENCES checklist_templates (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_checklist_template_sections_position
        CHECK (position >= 0)
);

CREATE INDEX idx_checklist_template_sections_template_id
    ON checklist_template_sections(template_id);

CREATE INDEX idx_checklist_template_sections_template_position
    ON checklist_template_sections(template_id, position);


CREATE TABLE checklist_template_items
(
    id         UUID PRIMARY KEY,
    section_id UUID         NOT NULL,
    name       VARCHAR(300) NOT NULL,
    category   VARCHAR(30)  NOT NULL,
    essential  BOOLEAN      NOT NULL DEFAULT FALSE,
    position   INTEGER      NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,

    CONSTRAINT fk_checklist_template_items_section
        FOREIGN KEY (section_id)
            REFERENCES checklist_template_sections (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_checklist_template_items_category
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

    CONSTRAINT chk_checklist_template_items_position
        CHECK (position >= 0)
);

CREATE INDEX idx_checklist_template_items_section_id
    ON checklist_template_items(section_id);

CREATE INDEX idx_checklist_template_items_section_position
    ON checklist_template_items(section_id, position);

CREATE INDEX idx_checklist_template_items_section_category
    ON checklist_template_items(section_id, category);