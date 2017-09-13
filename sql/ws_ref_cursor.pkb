CREATE OR REPLACE PACKAGE BODY WS_REF_CURSOR_SQL AS

-------------------------------------------------------------------------------
--  Function Name  :  GET_OBJECTS_BY_NAME
-------------------------------------------------------------------------------
FUNCTION GET_OBJECTS_BY_NAME(I_xml IN  CLOB)
RETURN cursorType AS 
    
    L_cursor    cursorType;

BEGIN
    OPEN L_cursor FOR
        SELECT owner,
               object_name,
               object_type,
               status 
          FROM all_objects
         WHERE object_name IN (SELECT xmlt.object_name
                                 FROM XMLTABLE('/input/row'
                                               PASSING xmltype(I_xml)
                                               COLUMNS
                                               object_name     VARCHAR2(25)    PATH './objectName'
                                               ) xmlt
                               );
    
    RETURN L_cursor;

END GET_OBJECTS_BY_NAME;


-------------------------------------------------------------------------------
--  Function Name  :  GET_OBJECTS_BY_OWNER
-------------------------------------------------------------------------------
FUNCTION GET_OBJECTS_BY_OWNER(I_xml IN  CLOB)
RETURN cursorType AS 
    
    L_cursor    cursorType;

BEGIN
    OPEN L_cursor FOR
        SELECT owner,
               object_name,
               object_type,
               status 
          FROM all_objects
         WHERE owner IN (SELECT xmlt.object_owner
                           FROM XMLTABLE('/input/row'
                                         PASSING xmltype(I_xml)
                                         COLUMNS
                                         object_owner     VARCHAR2(25)    PATH './objectOwner'
                                        ) xmlt
                        );
    
    RETURN L_cursor;

END GET_OBJECTS_BY_OWNER;


END WS_REF_CURSOR_SQL;
/

show errors PACKAGE BODY WS_REF_CURSOR_SQL;
 