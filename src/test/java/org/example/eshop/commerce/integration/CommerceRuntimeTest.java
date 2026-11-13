package org.example.eshop.commerce.integration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CommerceRuntimeTest {
    @Test void onlyExplicitlyNamedIsolatedDatabasesAllowMutationTests(){
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        CommerceRuntimeController controller=new CommerceRuntimeController(jdbc);
        when(jdbc.queryForObject("SELECT DATABASE()",String.class)).thenReturn("furniture_shop");
        assertFalse(controller.runtime().isolatedData());
        when(jdbc.queryForObject("SELECT DATABASE()",String.class)).thenReturn("furniture_shop_tv3_dev");
        assertTrue(controller.runtime().isolatedData());
        when(jdbc.queryForObject("SELECT DATABASE()",String.class)).thenReturn("furniture_shop_test_example");
        assertTrue(controller.runtime().isolatedData());
        when(jdbc.queryForObject("SELECT DATABASE()",String.class)).thenReturn("furniture_shop_test_",null);
        assertFalse(controller.runtime().isolatedData());assertFalse(controller.runtime().isolatedData());
    }
}
