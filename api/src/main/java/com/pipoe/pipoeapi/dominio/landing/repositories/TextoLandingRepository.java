package com.pipoe.pipoeapi.dominio.landing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pipoe.pipoeapi.dominio.landing.entities.ClaveTexto;
import com.pipoe.pipoeapi.dominio.landing.entities.TextoLanding;

public interface TextoLandingRepository extends JpaRepository<TextoLanding, ClaveTexto> {
}
