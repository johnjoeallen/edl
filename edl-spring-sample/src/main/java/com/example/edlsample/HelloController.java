package com.example.edlsample;

import com.example.edlsamplewrapped.InvalidTokenException;
import com.example.edlsamplewrapped.MissingTokenException;
import com.example.edlsamplewrapped.WrappedErrorsContainerException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {
  @GetMapping("/hello")
  public String hello(@RequestParam(name = "name", required = false) String name) {
    if (name == null || name.isBlank()) {
      throw MissingNameException.builder().build();
    }
    throw InvalidNameException.builder().name(name).build();
  }

  @PostMapping("/hello")
  public String hello(@RequestBody HelloRequest request) {
    CommonContainerException container = new CommonContainerException();
    String name = request == null ? null : request.name();
    if (name == null || name.isBlank()) {
      container.add(MissingNameException.builder().build());
      container.add(InvalidNameException.builder().name("unknown").build());
      throw container;
    }
    container.add(InvalidNameException.builder().name(name).build());
    container.add(MissingNameException.builder().build());
    throw container;
  }

  @PostMapping("/wrapped")
  public String wrapped(@RequestBody HelloRequest request) {
    WrappedErrorsContainerException container = new WrappedErrorsContainerException();
    String token = request == null ? null : request.name();
    if (token == null || token.isBlank()) {
      container.add(MissingTokenException.builder().build());
      container.add(InvalidTokenException.builder().token("unknown").build());
      throw container;
    }
    container.add(InvalidTokenException.builder().token(token).build());
    container.add(MissingTokenException.builder().build());
    throw container;
  }
}
