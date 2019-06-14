DECLARE
c int;
v_query                 VARCHAR2(2000);

BEGIN
  SELECT COUNT(*)
      INTO c
      FROM user_tables
      WHERE table_name = UPPER('view_temp');
      IF c             = 1 THEN
        EXECUTE immediate 'TRUNCATE TABLE view_temp';
      ELSE
        BEGIN
          v_query := 'create global temporary table view_temp(view_name varchar2(1000) , dependant_name varchar2(2000), total_count number , order_crt_num number) on commit preserve rows';
          EXECUTE immediate v_query;
        END;
      END IF;
 END;