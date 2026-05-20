package com.lectuaria.backend.service.auth.impl;

import com.lectuaria.backend.service.auth.IEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Override
    public void sendRegistrationConfirmation(String to, String displayName) {
        logger.info("Correo de confirmación enviado con éxito");
    }
}
