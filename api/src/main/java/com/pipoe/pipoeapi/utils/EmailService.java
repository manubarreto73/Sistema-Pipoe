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

    /**
     * El código es el dato con el que se entra, así que va destacado y con su nombre completo.
     * El nombre del proyecto se sigue mandando, pero sólo para que la persona reconozca de cuál
     * se trata: ya no sirve para iniciar sesión, y el dueño puede cambiarlo cuando quiera.
     */
    public void enviarAccesoColaborador(
        String email, String nombreCompleto, String nombreProyecto, String codigoProyecto, String password
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Te agregaron como colaborador en " + nombreProyecto);
        message.setText("""
            Hola %s,

            Te agregaron como colaborador del proyecto "%s" en Pipoe.
            Estos son tus datos de acceso para el login de colaboradores:

            Código del proyecto: %s
            Email: %s
            Contraseña: %s

            El código es lo que te pide la pantalla de acceso de colaboradores. Da igual si lo
            escribes en mayúsculas o en minúsculas.

            Te recomendamos cambiar la contraseña la primera vez que ingreses.
            """.formatted(nombreCompleto, nombreProyecto, codigoProyecto, email, password));
        mailSender.send(message);
    }
}
