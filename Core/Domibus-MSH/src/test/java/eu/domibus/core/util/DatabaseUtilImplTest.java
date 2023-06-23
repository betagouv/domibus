package eu.domibus.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * @author Sebastian-Ion TINCU
 * @since 4.2
 */
@ExtendWith(MockitoExtension.class)
public class DatabaseUtilImplTest {

    @InjectMocks
    private DatabaseUtilImpl databaseUtil = new DatabaseUtilImpl();

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData databaseMetaData;

    @Test
    public void getDatabaseUserName() throws Exception {
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.getMetaData()).thenReturn(databaseMetaData);
        Mockito.when(databaseMetaData.getUserName()).thenReturn("current_db_user");

        databaseUtil.init();

        Mockito.verify(connection).close();
        Assertions.assertEquals("current_db_user", databaseUtil.getDatabaseUserName(), "Should have returned the correct user name");
    }

    @Test
    void getDatabaseUserName_throwsExceptionWhenFailingToAquireConnection() throws Exception {
        Mockito.when(dataSource.getConnection()).thenThrow(new SQLException());

        Assertions.assertThrows(IllegalStateException. class,() -> databaseUtil.init());
    }
}
