package hk.ust.csit5930.seachengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude= {DataSourceAutoConfiguration.class})
public class SeachEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeachEngineApplication.class, args);
    }

}
