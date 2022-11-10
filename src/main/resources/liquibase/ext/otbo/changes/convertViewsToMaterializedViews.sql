-- set serveroutput on;
DECLARE
  vcount                  NUMBER;
  existCount              NUMBER;
  v_query                 VARCHAR2(2000);
  globalCount             NUMBER;
  newDependent            VARCHAR2(4000);
  dependant_created_count NUMBER;
  allDepCreated           BOOLEAN;
  testNum                 INTEGER;
  c                       INT;
  viewCreated             INTEGER;
  FUNCTION checkAndCreateDependantView(
      viewName String)
    RETURN INTEGER
  IS
  BEGIN
    FOR all_views IN
    ( SELECT * FROM view_temp WHERE view_name = viewName
    )
    LOOP
      BEGIN
        SELECT COUNT(*)
        INTO existCount
        FROM view_temp
        WHERE view_name = viewName;
        IF existCount !=0 THEN
          BEGIN
            FOR dependant_names IN
            (SELECT regexp_substr(all_views.dependant_name,'[^,]+', 1, level) AS dep_name
            FROM dual
              CONNECT BY regexp_substr(all_views.dependant_name, '[^,]+', 1, level) IS NOT NULL
            )
            LOOP
              BEGIN
                IF all_views.total_count > 1 THEN
                  BEGIN
                    SELECT COUNT(*)
                    INTO dependant_created_count
                    FROM user_objects
                    WHERE object_name           = dependant_names.dep_name;
                    IF(dependant_created_count != 0) THEN
                      BEGIN
                        allDepCreated :=true;
                      END;
                    ELSE
                      testNum := checkAndCreateDependantView(dependant_names.dep_name);
                    END IF;
                  END;
                ELSE
                  BEGIN
                    SELECT COUNT(*)
                    INTO dependant_created_count
                    FROM user_objects
                    WHERE object_name          = dependant_names.dep_name ;
                    IF dependant_created_count!=0 THEN
                      BEGIN
                        SELECT MAX(order_crt_num) INTO globalCount FROM view_temp;
                        globalCount := globalCount+1;
                        EXECUTE immediate( 'update view_temp set order_crt_num = ' || globalCount || ' where view_name = '''|| viewName ||'''');
                      END;
                    ELSE
                      testNum:= checkAndCreateDependantView(dependant_names.dep_name);
                    END IF;
                  END;
                END IF;
              END;
            END LOOP;
            IF allDepCreated !=false THEN
              BEGIN
                SELECT MAX(order_crt_num) INTO globalCount FROM view_temp;
                globalCount := globalCount+1;
                EXECUTE immediate( 'update view_temp set order_crt_num = ' || globalCount || ' where view_name = '''|| viewName ||'''');
              END;
            END IF;
          END;
        END IF;
      END;
    END LOOP;
    RETURN 1;
  END checkAndCreateDependantView;
  BEGIN
  -- Due to bugs in Oracle, this script will only work on:
  -- 11.2.0.3 with patch set 30
  -- 11.2.0.4 with patch set 6 (not yet released)
  -- 12.2.0.2 
  EXECUTE immediate 'ALTER session SET "query_rewrite_enabled"      = false';
  EXECUTE immediate 'ALTER session SET "_query_rewrite_vop_cleanup" = false';
  EXECUTE immediate 'ALTER session SET "_complex_view_merging"      = false';
  EXECUTE immediate 'ALTER session SET "optimizer_features_enable"  = "11.1.0.7"';
  EXECUTE immediate 'ALTER session SET "_pred_move_around"          = false';
  dbms_metadata.set_transform_param(dbms_metadata.session_transform, 'SQLTERMINATOR', false );
      FOR cur_rec IN
    (SELECT o.object_name,
      o.object_type
    FROM user_objects o left join mview_conversion_excludes x on o.object_name = x.view_name
    WHERE o.object_type = 'VIEW' and x.view_name is null
    ORDER BY o.object_name
    )
    LOOP
      BEGIN
        SELECT COUNT(*)
        INTO vcount
        FROM user_dependencies
        WHERE name = cur_rec.object_name
        AND referenced_type = 'VIEW';
        IF vcount !=0 THEN
          FOR cur_view IN
          (SELECT       *
          FROM user_dependencies
          WHERE name = cur_rec.object_name
          AND referenced_type = 'VIEW'
          )
          LOOP
            BEGIN
              SELECT COUNT(*)
              INTO existCount
              FROM view_temp
              WHERE view_name = cur_view.name;
              IF existCount !=0 THEN
                BEGIN
                  SELECT dependant_name
                  INTO newDependent
                  FROM view_temp
                  WHERE view_name = cur_rec.object_name;
                  newDependent   := newDependent || ',' || cur_view.REFERENCED_NAME;
                  EXECUTE immediate ('update view_temp set dependant_name = ''' || newDependent || ''' where view_name = ''' || cur_view.name || '''');
                END;
              ELSE
                EXECUTE immediate 'insert into view_temp values (''' || cur_rec.object_name || ''' , ''' || cur_view.REFERENCED_NAME || ''','|| vcount || ', 0)' ;
              END IF;
            END;
          END LOOP;
        ELSE
          EXECUTE immediate 'insert into view_temp values (''' || cur_rec.object_name || ''' , ''Independent'','|| vcount || ', 0)' ;
        END IF;
      END;
    END LOOP;
    FOR all_views IN
    (SELECT * FROM view_temp ORDER BY total_count, view_name ASC
    )
    LOOP
      BEGIN
        allDepCreated            := false;
        IF all_views.total_count != 0 THEN
          BEGIN
            testNum:= checkAndCreateDependantView(all_views.view_name);
          END;
        ELSE
          BEGIN
            SELECT MAX(order_crt_num)
            INTO globalCount
            FROM view_temp;
            globalCount := globalCount+1;
            EXECUTE immediate( 'update view_temp set order_crt_num = ' || globalCount || ' where view_name = '''|| all_views.view_name ||'''');
          END;
        END IF;
      END;
    END LOOP;
    /*
    FOR final_view IN
    (SELECT * FROM view_temp ORDER BY order_crt_num
    )
    LOOP
      dbms_output.put_line('View Name: ' || final_view.view_name || ' , Dependent Name :' || final_view.dependant_name || ' , order created : ' || final_view.order_crt_num );
    END LOOP;
    */
  
  FOR cur_rec IN
  ( SELECT view_name object_name, 'VIEW' object_type FROM view_temp order by order_crt_num
  )
  LOOP
    BEGIN
      FOR cur_view IN
      (SELECT trim( REPLACE( REPLACE( dbms_metadata.get_ddl( 'VIEW', cur_rec.object_name, NULL, '11' ), 'CREATE OR REPLACE FORCE VIEW', 'CREATE MATERIALIZED VIEW' ), 'CREATE OR REPLACE VIEW', 'CREATE MATERIALIZED VIEW' ) ) "MATERIALIZED_VIEW",
        trim( dbms_metadata.get_ddl( 'VIEW', cur_rec.object_name, NULL, '11' ) ) "VIEW"
      FROM dual
      )
      LOOP
        BEGIN
          BEGIN
            -- dbms_output.put_line( 'Drop Materialized View: ' || cur_rec.object_name );
            EXECUTE immediate 'drop materialized view ' || cur_rec.object_name;
          EXCEPTION
          WHEN OTHERS THEN
            IF SQLCODE != -12003 THEN
              raise;
            END IF;
          END;
          BEGIN
            -- dbms_output.put_line( 'Drop View: ' || cur_rec.object_name );
            EXECUTE immediate 'drop view ' || cur_rec.object_name;
          EXCEPTION
          WHEN OTHERS THEN
            IF SQLCODE != -942 THEN
              raise;
            END IF;
          END;
          -- create the view as a materialized view.
          BEGIN
            -- dbms_output.put_line( 'Attempting to create materialized view named ' || cur_rec.object_name );
            EXECUTE immediate cur_view."MATERIALIZED_VIEW";
          EXCEPTION
          WHEN OTHERS THEN
            BEGIN
              -- dbms_output.put_line( 'Failed to create materialized view, recreating original view ' || cur_rec.object_name );
              -- dbms_output.put_line( 'Error was: ' || sqlerrm( sqlcode ) );
              EXECUTE immediate cur_view."VIEW";
            EXCEPTION
            WHEN OTHERS THEN
              -- dbms_output.put_line( 'ERROR: Could not recreate view named ' || cur_rec.object_name || '.' );
              -- dbms_output.put_line( 'SQL was:' || cur_view."VIEW" );
              raise;
            END;
          END;
        END;
      END LOOP;
    END;
  END LOOP;
EXCEPTION
WHEN OTHERS THEN
  raise;
END;