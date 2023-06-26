CREATE OR REPLACE FUNCTION generate_partition_id(p_date IN DATE)
    RETURN NUMBER IS
    p_id NUMBER;
BEGIN
    DECLARE
        date_format CONSTANT STRING(10) := 'YYMMDDHH24';
    BEGIN
        SELECT to_number(to_char(p_date, date_format))
        INTO p_id
        FROM dual;
        RETURN p_id;
    END;
END;
/

CREATE OR REPLACE PROCEDURE PARTITION_TB_USER_MESSAGE
AS
BEGIN
    DECLARE
        p_id   NUMBER;
        p_name VARCHAR2(20);
        p_high NUMBER;
    BEGIN
        select generate_partition_id(TRUNC(sysdate+1)) into p_id from dual;
        p_name := 'P' || p_id;
        p_high := p_id || '0000000000';
        EXECUTE IMMEDIATE 'ALTER TABLE TB_USER_MESSAGE MODIFY PARTITION BY RANGE (ID_PK) (PARTITION P1970 VALUES LESS THAN (220000000000000000), PARTITION ' || p_name || ' VALUES LESS THAN (' || p_high || ')) UPDATE INDEXES ( IDX_USER_MSG_MESSAGE_ID	LOCAL, IDX_USER_MSG_ACTION_ID LOCAL, IDX_USER_MSG_AGREEMENT_ID LOCAL, IDX_USER_MSG_SERVICE_ID LOCAL, IDX_USER_MSG_MPC_ID LOCAL, IDX_FROM_ROLE_ID LOCAL, IDX_USER_MSG_TO_PARTY_ID LOCAL, IDX_TO_ROLE_ID LOCAL, IDX_USER_MSG_FROM_PARTY_ID LOCAL, IDX_TEST_MESSAGE LOCAL )';
    END;
END;
/

CREATE OR REPLACE PROCEDURE drop_partition (partition_name IN VARCHAR2) IS
   BEGIN
      execute immediate 'ALTER TABLE TB_USER_MESSAGE DROP PARTITION ' || partition_name || ' UPDATE INDEXES';
   END;
/

DROP FUNCTION generate_partition;
DROP PROCEDURE PARTITIONSGEN;

BEGIN
    dbms_scheduler.drop_job(job_name => 'GENERATE_PARTITIONS_JOB');
END;
/
