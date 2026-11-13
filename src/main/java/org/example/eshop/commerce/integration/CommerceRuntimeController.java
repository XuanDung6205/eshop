package org.example.eshop.commerce.integration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
/** Exposes only an isolation flag, never a database name or credentials. */
@RestController
public class CommerceRuntimeController {
    private final JdbcTemplate jdbc;
    public CommerceRuntimeController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record RuntimeInfo(String application,boolean isolatedData){}
    @GetMapping("/api/commerce/runtime") public RuntimeInfo runtime(){
        String database=jdbc.queryForObject("SELECT DATABASE()",String.class);
        boolean isolated=database!=null && database.matches("furniture_shop_(tv3_dev|test_[a-z0-9_]+)");
        return new RuntimeInfo("tv3-commerce",isolated);
    }
}
