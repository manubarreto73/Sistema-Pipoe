package com.pipoe.pipoeapi.dominio.landing.services;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.landing.dtos.TextoLandingResponse;
import com.pipoe.pipoeapi.dominio.landing.dtos.request.ActualizarTextoRequest;
import com.pipoe.pipoeapi.dominio.landing.entities.ClaveTexto;
import com.pipoe.pipoeapi.dominio.landing.entities.TextoLanding;
import com.pipoe.pipoeapi.dominio.landing.repositories.TextoLandingRepository;
import com.pipoe.pipoeapi.utils.HtmlSanitizer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TextoLandingService {

    private final TextoLandingRepository repository;

    /**
     * Todos los textos, en el orden en que aparecen en la portada.
     *
     * Si una clave todavía no tiene fila —porque se agregó al enum después de la migración—
     * se devuelve vacía en vez de romper: la portada muestra la sección sin texto y se ve
     * enseguida qué falta cargar.
     */
    public List<TextoLandingResponse> listar() {
        List<TextoLanding> guardados = repository.findAll();

        return Arrays.stream(ClaveTexto.values())
            .map(clave -> guardados.stream()
                .filter(texto -> texto.getClave() == clave)
                .findFirst()
                .orElseGet(() -> TextoLanding.builder().clave(clave).contenido("").build()))
            .sorted(Comparator.comparingInt(texto -> texto.getClave().getOrden()))
            .map(TextoLandingResponse::from)
            .toList();
    }

    @Transactional
    public TextoLandingResponse actualizar(ClaveTexto clave, ActualizarTextoRequest request) {
        TextoLanding texto = repository.findById(clave)
            .orElseGet(() -> TextoLanding.builder().clave(clave).build());

        // Los textos de la portada los ve todo internet sin iniciar sesión: si alguien tomara
        // la cuenta de administradora, sin esto podría inyectar un script en la home.
        texto.setContenido(clave.getTipo() == ClaveTexto.Tipo.RICO
            ? HtmlSanitizer.limpiar(request.getContenido())
            : HtmlSanitizer.aTextoPlano(request.getContenido()));
        texto.setActualizadoEn(LocalDateTime.now());

        return TextoLandingResponse.from(repository.save(texto));
    }
}
