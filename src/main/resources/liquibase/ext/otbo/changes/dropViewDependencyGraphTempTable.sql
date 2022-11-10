DECLARE
c int;

BEGIN
  SELECT COUNT(*)
      INTO c
      FROM user_tables
      WHERE table_name = UPPER('view_temp');
      IF c             = 1 THEN
        EXECUTE immediate 'TRUNCATE TABLE view_temp';
        EXECUTE immediate 'DROP TABLE view_temp';
      END IF;
 END;