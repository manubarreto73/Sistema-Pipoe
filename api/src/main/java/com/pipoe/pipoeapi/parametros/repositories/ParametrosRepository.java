package com.pipoe.pipoeapi.parametros.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pipoe.pipoeapi.parametros.entities.Parametros;

public interface ParametrosRepository extends JpaRepository<Parametros, Long> {
}
