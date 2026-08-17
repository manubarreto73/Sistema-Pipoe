package com.pipoe.pipoeapi.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void enviarCredencialesUsuario(String email, String nombreCompleto, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Tu acceso a Pipoe");
        message.setText("""
            Hola %s,

            Se creó tu usuario en Pipoe. Estos son tus datos de acceso:

            Email: %s
            Contraseña: %s

            Te recomendamos cambiar la contraseña la primera vez que ingreses.
            """.formatted(nombreCompleto, email, password));
        mailSender.send(message);
    }

    public void enviarAccesoColaborador(String email, String nombreCompleto, String nombreProyecto, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Te agregaron como colaborador en " + nombreProyecto);
        message.setText("""
            Hola %s,

            Te agregaron como colaborador del proyecto "%s" en Pipoe.
            Estos son tus datos de acceso para el login de colaboradores:

            Proyecto: %s
            Email: %s
            Contraseña: %s

            Te recomendamos cambiar la contraseña la primera vez que ingreses.
            """.formatted(nombreCompleto, nombreProyecto, nombreProyecto, email, password));
        mailSender.send(message);
    }
}
