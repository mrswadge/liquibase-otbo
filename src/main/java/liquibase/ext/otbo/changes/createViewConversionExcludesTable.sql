declare
v_sql LONG;
begin
v_sql:='create table mview_conversion_excludes ( view_name varchar(30) primary key not null )';
execute immediate v_sql;

EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -955 THEN
        NULL; -- suppresses ORA-00955 exception
      ELSE
         RAISE;
      END IF;
END;