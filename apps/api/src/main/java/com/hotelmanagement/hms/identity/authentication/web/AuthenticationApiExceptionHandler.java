package com.hotelmanagement.hms.identity.authentication.web;

import com.hotelmanagement.hms.shared.web.GlobalApiExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthenticationController.class)
public class AuthenticationApiExceptionHandler extends GlobalApiExceptionHandler {}
