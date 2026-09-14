package com.hotelmanagement.hms.platform.service;

import com.hotelmanagement.hms.identity.service.UserAccountService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit, one-time operator bootstrap. Never promotes an existing hotel identity. */
@Component
public class PlatformAdminBootstrap implements ApplicationRunner {
 private final JdbcTemplate jdbc; private final UserAccountService accounts;
 private final String email; private final String password;
 public PlatformAdminBootstrap(JdbcTemplate jdbc,UserAccountService accounts,
  @Value("${HMS_BOOTSTRAP_ADMIN_EMAIL:}") String email,@Value("${HMS_BOOTSTRAP_ADMIN_PASSWORD:}") String password){
  this.jdbc=jdbc;this.accounts=accounts;this.email=email;this.password=password;
 }
 @Override @Transactional public void run(ApplicationArguments args){
  if(email.isBlank()||password.isBlank())return;
  if(jdbc.queryForObject("select count(*) from platform_administrators",Integer.class)>0)return;
  var admin=accounts.createAccount(email,password,"System administrator",null,"en");
  jdbc.update("insert into platform_administrators(user_id) values(?)",admin.getId());
 }
}
