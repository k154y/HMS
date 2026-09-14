package com.hotelmanagement.hms;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
/** Uses isolated Compose services when configured, otherwise starts Testcontainers. */
final class IntegrationServices {
 private static PostgreSQLContainer postgres;
 private static GenericContainer<?> redis;
 static synchronized void configure(DynamicPropertyRegistry registry){
  String url=System.getenv("HMS_TEST_DB_URL");
  if(url!=null&&!url.isBlank()){
   registry.add("spring.datasource.url",()->url);
   registry.add("spring.datasource.username",()->System.getenv("HMS_TEST_DB_USER"));
   registry.add("spring.datasource.password",()->System.getenv("HMS_TEST_DB_PASSWORD"));
   registry.add("spring.data.redis.host",()->System.getenv("HMS_TEST_REDIS_HOST"));
   registry.add("spring.data.redis.port",()->6379);
  } else {
   if(postgres==null){postgres=new PostgreSQLContainer("postgres:16-alpine");redis=new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);postgres.start();redis.start();Runtime.getRuntime().addShutdownHook(new Thread(()->{redis.stop();postgres.stop();}));}
   registry.add("spring.datasource.url",postgres::getJdbcUrl);registry.add("spring.datasource.username",postgres::getUsername);registry.add("spring.datasource.password",postgres::getPassword);registry.add("spring.data.redis.host",redis::getHost);registry.add("spring.data.redis.port",()->redis.getMappedPort(6379));
  }
 }
}
