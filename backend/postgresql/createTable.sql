BEGIN;

CREATE SCHEMA IF NOT EXISTS dbo;

CREATE TABLE IF NOT EXISTS dbo."user"(
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(80) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    CONSTRAINT user_name_length CHECK (char_length(name) >= 3),
    CONSTRAINT user_email_is_valid CHECK (email ~ '^[A-Za-z0-9+_.-]+@(.+)$')
);

CREATE TABLE IF NOT EXISTS dbo.session(
    user_id UUID PRIMARY KEY REFERENCES dbo."user"(id),
    session TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS dbo.physiotherapist(
    id UUID PRIMARY KEY REFERENCES dbo."user"(id)
);

CREATE TABLE IF NOT EXISTS dbo.patient(
    id UUID PRIMARY KEY REFERENCES dbo."user"(id),
    birth_date DATE,
    weight INT,
    height INT,
    physical_condition VARCHAR(50),
    CONSTRAINT patient_age_is_valid CHECK (birth_date IS NULL OR date_part('years', age(CURRENT_DATE, birth_date)) >= 7),
    CONSTRAINT patient_weight_is_valid CHECK (weight IS NULL OR (weight >= 30 AND weight <= 300)),
    CONSTRAINT patient_height_is_valid CHECK (height IS NULL OR (height >= 100 AND height <= 250)),
    CONSTRAINT patient_physical_condition_length CHECK (physical_condition IS NULL OR char_length(physical_condition) >= 5)
);

CREATE TABLE IF NOT EXISTS dbo.admin(
    id UUID PRIMARY KEY REFERENCES dbo."user"(id)
);

CREATE TABLE IF NOT EXISTS dbo.patient_to_physiotherapist(
    physiotherapist_id UUID REFERENCES dbo.physiotherapist(id),
    patient_id UUID UNIQUE REFERENCES dbo.patient(id)
);

CREATE TABLE IF NOT EXISTS dbo.physiotherapist_requests(
    request_id UUID PRIMARY KEY,
    physiotherapist_id UUID REFERENCES dbo.physiotherapist(id),
    patient_id UUID REFERENCES dbo.patient(id),
    request_text TEXT,
    UNIQUE (physiotherapist_id, patient_id),
    CONSTRAINT request_yourself CHECK (patient_id != physiotherapist_id)
);

CREATE TABLE IF NOT EXISTS dbo.physiotherapist_rating(
    physiotherapist_id UUID NOT NULL REFERENCES dbo.physiotherapist(id),
    patient_id UUID NOT NULL REFERENCES dbo.patient(id),
    stars INT NOT NULL,
    PRIMARY KEY (physiotherapist_id, patient_id),
    CONSTRAINT rate_yourself CHECK (patient_id != physiotherapist_id),
    CONSTRAINT stars_are_valid CHECK (stars >= 1 AND stars <= 5)
);

CREATE TABLE IF NOT EXISTS dbo.docs_authenticity(
    physiotherapist_id UUID PRIMARY KEY REFERENCES dbo.physiotherapist(id),
    state VARCHAR(10) NOT NULL,
    dt_submit DATE NOT NULL,
    CONSTRAINT state_check CHECK (state IN ('invalid', 'waiting', 'valid'))
);

CREATE TABLE IF NOT EXISTS dbo.plan(
    id SERIAL PRIMARY KEY,
    template_plan_id INT REFERENCES dbo.plan(id),
    physiotherapist_id UUID NOT NULL REFERENCES dbo.physiotherapist(id),
    title VARCHAR(50) NOT NULL,
    CONSTRAINT title_length CHECK (char_length(title) >= 3)
);

CREATE TABLE IF NOT EXISTS dbo.patient_plan(
    id SERIAL PRIMARY KEY,
    plan_id INT NOT NULL REFERENCES dbo.plan(id),
    patient_id UUID NOT NULL REFERENCES dbo.patient(id),
    dt_start DATE NOT NULL,
    dt_end DATE NOT NULL,
    CONSTRAINT patient_plan_unique UNIQUE (plan_id, patient_id, dt_start, dt_end),
    CONSTRAINT patient_plan_dates_valid CHECK (dt_end >= dt_start)
);

CREATE TABLE IF NOT EXISTS dbo.daily_list(
    id SERIAL PRIMARY KEY,
    day_index INT NOT NULL,
    plan_id INT NOT NULL REFERENCES dbo.plan(id),
    CONSTRAINT daily_list_unique_index UNIQUE (day_index, plan_id),
    CONSTRAINT day_index_is_valid CHECK (day_index >= 0)
);

CREATE TABLE IF NOT EXISTS dbo.exercise_info(
    id UUID PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    exercise_type VARCHAR(20) NOT NULL,
    supports_camera BOOLEAN,
    supports_sensors BOOLEAN,
    camera_joint VARCHAR(10) CHECK (camera_joint IN ('WRIST', 'ELBOW', 'KNEE')),
    camera_movement VARCHAR(10) CHECK (camera_movement IN ('FLEXION', 'EXTENSION')),
    use_roll BOOLEAN,
    movement_direction INTEGER CHECK (movement_direction IN (-1, 1)),
    raise_threshold FLOAT,
    lower_threshold FLOAT,
    cooldown_ms INT,
    min_raise_time_ms INT,
    hold_time_ms INT,
    min_movement_speed FLOAT,
    max_pitch FLOAT,
    min_pitch FLOAT,
    max_roll FLOAT,
    min_roll FLOAT,
    CONSTRAINT exercise_title_length CHECK (char_length(title) >= 3),
    CONSTRAINT exercise_description_length CHECK (char_length(description) >= 10)
);

CREATE TABLE IF NOT EXISTS dbo.daily_exercise(
    id SERIAL PRIMARY KEY,
    exercise_id UUID NOT NULL REFERENCES dbo.exercise_info(id),
    daily_list_id INT NOT NULL REFERENCES dbo.daily_list(id),
    sets INT NOT NULL,
    reps INT NOT NULL,
    sensor_profile JSONB CHECK (sensor_profile IS NULL OR jsonb_typeof(sensor_profile) = 'object'),
    CONSTRAINT sets_is_valid CHECK (sets >= 1 AND sets <= 20),
    CONSTRAINT reps_is_valid CHECK (reps >= 1 AND reps <= 200)
);

CREATE TABLE IF NOT EXISTS dbo.daily_exercise_setting (
    patient_plan_id INT NOT NULL REFERENCES dbo.patient_plan(id) ON DELETE CASCADE,
    daily_exercise_id INT NOT NULL REFERENCES dbo.daily_exercise(id) ON DELETE CASCADE,
    profile JSONB NOT NULL CHECK (jsonb_typeof(profile) = 'object'),
    PRIMARY KEY (patient_plan_id, daily_exercise_id)
);

CREATE TABLE IF NOT EXISTS dbo.plan_exercise_setting(
    id SERIAL PRIMARY KEY,
    patient_plan_id INT NOT NULL REFERENCES dbo.patient_plan(id),
    exercise_id UUID NOT NULL REFERENCES dbo.exercise_info(id),
    use_roll BOOLEAN,
    movement_direction INTEGER CHECK (movement_direction IN (-1, 1)),
    raise_threshold FLOAT,
    lower_threshold FLOAT,
    cooldown_ms INT,
    min_raise_time_ms INT,
    hold_time_ms INT,
    min_movement_speed FLOAT,
    max_pitch FLOAT,
    min_pitch FLOAT,
    max_roll FLOAT,
    min_roll FLOAT,
    CONSTRAINT plan_exercise_setting_unique UNIQUE (patient_plan_id, exercise_id)
);

CREATE TABLE IF NOT EXISTS dbo.exercise_session(
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES dbo.patient(id),
    daily_exercise_id INT NOT NULL REFERENCES dbo.daily_exercise(id),
    dt_submit TIMESTAMP NOT NULL,
    execution_mode VARCHAR(30),
    nr_set INT NOT NULL,
    with_load BOOLEAN,
    load_value FLOAT,
    load_unit VARCHAR(20),
    patient_feedback TEXT,
    physiotherapist_feedback TEXT,
    execution_score VARCHAR(30),
    physiotherapist_feedback_score VARCHAR(30),
    CONSTRAINT exercise_session_set_is_valid CHECK (nr_set > 0),
    CONSTRAINT exercise_session_unique_set UNIQUE (patient_id, daily_exercise_id, nr_set)
);

CREATE TABLE IF NOT EXISTS dbo.exercise_session_file(
    session_id UUID PRIMARY KEY REFERENCES dbo.exercise_session(id),
    file_path TEXT NOT NULL,
    file_format VARCHAR(20),
    source VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS dbo.sensor_result (
    session_id UUID PRIMARY KEY REFERENCES dbo.exercise_session(id) ON DELETE CASCADE,
    repetitions INT NOT NULL CHECK (repetitions > 0),
    duration_ms BIGINT NOT NULL CHECK (duration_ms > 0),
    samples JSONB NOT NULL CHECK (jsonb_typeof(samples) = 'array'),
    profile JSONB CHECK (profile IS NULL OR jsonb_typeof(profile) = 'object')
);

CREATE OR REPLACE FUNCTION validate_exercise_session_nr_set()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM dbo.daily_exercise de
        WHERE de.id = NEW.daily_exercise_id AND NEW.nr_set > de.sets
    ) THEN
        RAISE EXCEPTION 'Invalid nr_set';
    END IF;
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER exercise_session_validate_nr_set
BEFORE INSERT OR UPDATE ON dbo.exercise_session
FOR EACH ROW
EXECUTE FUNCTION validate_exercise_session_nr_set();

COMMIT;
