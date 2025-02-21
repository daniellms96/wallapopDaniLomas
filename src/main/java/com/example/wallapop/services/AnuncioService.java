package com.example.wallapop.services;

import com.example.wallapop.entities.Anuncio;
import com.example.wallapop.entities.Usuario;
import com.example.wallapop.repositories.AnuncioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;

    @Autowired
    public AnuncioService(AnuncioRepository anuncioRepository) {
        this.anuncioRepository = anuncioRepository;
    }

    public List<Anuncio> findByUsuario(Usuario usuario) {
        return anuncioRepository.findByUsuario(usuario);
    }

}
