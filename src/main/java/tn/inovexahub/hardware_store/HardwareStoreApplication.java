package tn.inovexahub.hardware_store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HardwareStoreApplication {

  public static void main(String[] args) {
    SpringApplication.run(HardwareStoreApplication.class, args);
  }
}
