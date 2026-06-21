--liquibase formatted sql

--changeset akarmanov:team-card-add-average-grade-column
ALTER TABLE backend.team_card
    ADD COLUMN IF NOT EXISTS average_grade NUMERIC(3, 2);

--changeset akarmanov:team-card-create-avg-function splitStatements:false
CREATE OR REPLACE FUNCTION update_team_card_average_grade()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$function$
BEGIN
    UPDATE backend.team_card
    SET average_grade = (SELECT AVG(
                                        CASE m.status
                                            WHEN 'OK' THEN 1.0
                                            WHEN 'WITH_ISSUES' THEN 0.5
                                            WHEN 'MANY_ISSUES' THEN 0.25
                                            ELSE 0.0
                                            END)
                         FROM meeting m
                         WHERE m.team_id = COALESCE(NEW.team_id, OLD.team_id))
    WHERE id = COALESCE(NEW.team_id, OLD.team_id);

    RETURN NEW;
END;
$function$;

--changeset akarmanov:team-card-create-insert-trigger splitStatements:false
CREATE OR REPLACE TRIGGER update_average_grade_after_insert
    AFTER INSERT
    ON meeting
    FOR EACH ROW
EXECUTE FUNCTION update_team_card_average_grade();

--changeset akarmanov:team-card-create-update-trigger splitStatements:false
CREATE OR REPLACE TRIGGER update_average_grade_after_update
    AFTER UPDATE OF status
    ON meeting
    FOR EACH ROW
    WHEN (OLD.status IS DISTINCT FROM NEW.status)
EXECUTE FUNCTION update_team_card_average_grade();

--changeset akarmanov:team-card-create-delete-trigger splitStatements:false
CREATE OR REPLACE TRIGGER update_average_grade_after_delete
    AFTER DELETE
    ON meeting
    FOR EACH ROW
EXECUTE FUNCTION update_team_card_average_grade();

--changeset akarmanov:team-card-update-existing-records
UPDATE backend.team_card tc
SET average_grade = (SELECT AVG(
                                    CASE m.status
                                        WHEN 'OK' THEN 1.0
                                        WHEN 'WITH_ISSUES' THEN 0.5
                                        WHEN 'MANY_ISSUES' THEN 0.25
                                        ELSE 0.0
                                        END)
                     FROM meeting m
                     WHERE m.team_id = tc.id);