package com.example.wallapop.repositories;

import com.example.wallapop.entities.Anuncio;
import com.example.wallapop.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {
    List<Anuncio> findAllByOrderByFechaCreacionDesc();
    List<Anuncio> findByUsuario(Usuario usuario);


}

