-- Scored questionnaire templates, per-purchase responses, and demo starter questions (issue #17).

create table questionnaire_templates (
    id binary(16) not null,
    code varchar(50) not null,
    active bit not null,
    primary key (id),
    unique key uk_questionnaire_templates_code (code)
) engine=InnoDB;

create table questionnaire_questions (
    id binary(16) not null,
    template_id binary(16) not null,
    sort_order integer not null,
    message_key varchar(120) not null,
    max_score integer not null,
    weight integer not null,
    active bit not null,
    primary key (id),
    constraint fk_questionnaire_questions_template foreign key (template_id) references questionnaire_templates (id)
) engine=InnoDB;

create index idx_questionnaire_questions_template on questionnaire_questions (template_id, sort_order);

create table test_purchase_questionnaire_responses (
    id binary(16) not null,
    test_purchase_id binary(16) not null,
    template_id binary(16) not null,
    overall_score decimal(5, 2) not null,
    submitted_at datetime(6) not null,
    submitted_by_id binary(16),
    updated_on datetime(6),
    primary key (id),
    unique key uk_tp_questionnaire_response (test_purchase_id),
    constraint fk_tp_q_response_purchase foreign key (test_purchase_id) references test_purchases (id),
    constraint fk_tp_q_response_template foreign key (template_id) references questionnaire_templates (id),
    constraint fk_tp_q_response_submitted_by foreign key (submitted_by_id) references users (id)
) engine=InnoDB;

create index idx_tp_q_response_submitted_at on test_purchase_questionnaire_responses (submitted_at);

create table test_purchase_questionnaire_answers (
    id binary(16) not null,
    response_id binary(16) not null,
    question_id binary(16) not null,
    score integer not null,
    primary key (id),
    unique key uk_tp_q_answer (response_id, question_id),
    constraint fk_tp_q_answer_response foreign key (response_id) references test_purchase_questionnaire_responses (id),
    constraint fk_tp_q_answer_question foreign key (question_id) references questionnaire_questions (id)
) engine=InnoDB;

-- Default starter template (demo / first slice).
insert into questionnaire_templates (id, code, active) values
    (unhex('00000000000140008000000000000001'), 'DEFAULT', 1);

insert into questionnaire_questions (id, template_id, sort_order, message_key, max_score, weight, active) values
    (unhex('00000000000140008000000000000011'), unhex('00000000000140008000000000000001'), 1, 'questionnaire.q.product_availability', 5, 2, 1),
    (unhex('00000000000140008000000000000012'), unhex('00000000000140008000000000000001'), 2, 'questionnaire.q.staff_knowledge', 5, 2, 1),
    (unhex('00000000000140008000000000000013'), unhex('00000000000140008000000000000001'), 3, 'questionnaire.q.checkout_experience', 5, 1, 1),
    (unhex('00000000000140008000000000000014'), unhex('00000000000140008000000000000001'), 4, 'questionnaire.q.store_presentation', 5, 1, 1),
    (unhex('00000000000140008000000000000015'), unhex('00000000000140008000000000000001'), 5, 'questionnaire.q.overall_satisfaction', 5, 2, 1);
